package org.snaccooperative.exporters;

import static org.snaccooperative.schema.SNACSchemaUtilities.getCellValueForRowByColumnName;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.commands.SNACAPIClient;
import org.snaccooperative.commands.SNACAPIResponse;
import org.snaccooperative.data.Constellation;
import org.snaccooperative.data.Language;
import org.snaccooperative.data.Resource;
import org.snaccooperative.data.Term;
import org.snaccooperative.schema.SNACSchema;

public class SNACResourceItem extends SNACUploadItem {

  static final Logger logger = LoggerFactory.getLogger("SNACResourceItem");

  protected Project _project;
  protected SNACSchema _schema;
  protected SNACLookupCache _cache;
  protected Resource _resource;
  protected int _rowIndex;

  public SNACResourceItem(
      Project project, SNACSchema schema, SNACLookupCache cache, Record record) {
    this._schema = schema;
    this._cache = cache;
    this._rowIndex = record.fromRowIndex;

    Resource res = new Resource();

    for (Map.Entry<String, String> entry : schema.getColumnMappings().entrySet()) {
      String csvColumn = entry.getKey();
      String snacField = entry.getValue().toLowerCase();

      for (int i = record.fromRowIndex; i < record.toRowIndex; i++) {
        Row row = project.rows.get(i);

        String cellValue = getCellValueForRowByColumnName(project, row, csvColumn);

        if (cellValue.equals("")) {
          continue;
        }

        switch (snacField) {
          case "id":
            try {
              int id = Integer.parseInt(cellValue);
              res.setID(id);
            } catch (NumberFormatException e) {
            }
            continue;

          case "type":
            Term typeTerm = createTypeTerm(cellValue);

            if (typeTerm == null) {
              continue;
            }

            res.setDocumentType(typeTerm);

            continue;

          case "title":
            res.setTitle(cellValue);
            continue;

          case "display entry":
            res.setDisplayEntry(cellValue);
            continue;

          case "link":
            res.setLink(cellValue);
            continue;

          case "abstract":
            res.setAbstract(cellValue);
            continue;

          case "extent":
            res.setExtent(cellValue);
            continue;

          case "date":
            res.setDate(cellValue);
            continue;

          case "language":
            // reduced to just adding language codes, as script does not seem to be used right now
            String langCode = cache.getLanguageCode(cellValue);

            if (langCode != null) {
              Language lang = new Language();
              Term langTerm = new Term();

              langTerm.setType(cellValue);
              lang.setLanguage(langTerm);
              res.addLanguage(lang);
            }

            continue;

          case "holding repository snac id":
            try {
              int id = Integer.parseInt(cellValue);

              Constellation cons = new Constellation();

              cons.setID(id);
              res.setRepository(cons);
            } catch (NumberFormatException e) {
            }
            continue;

          default:
            continue;
        }
      }
    }

    this._resource = res;
  }

  public String getPreviewText() {
    String preview = "";

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacText = entry.getValue();
      String snacField = snacText.toLowerCase();

      switch (snacField) {
        case "id":
          preview += snacText + ": " + _resource.getID() + "\n";
          break;

        case "type":
          Term previewTerm = _resource.getDocumentType();
          if (previewTerm != null) {
            preview += snacText + ": " + previewTerm.getTerm() + " (" + previewTerm.getID() + ")\n";
          }
          break;

        case "title":
          preview += snacText + ": " + _resource.getTitle() + "\n";
          break;

        case "display entry":
          preview += snacText + ": " + _resource.getDisplayEntry() + "\n";
          break;

        case "link":
          preview += snacText + ": " + _resource.getLink() + "\n";
          break;

        case "abstract":
          preview += snacText + ": " + _resource.getAbstract() + "\n";
          break;

        case "extent":
          preview += snacText + ": " + _resource.getExtent() + "\n";
          break;

        case "date":
          preview += snacText + ": " + _resource.getDate() + "\n";
          break;

        case "language":
          List<Language> languageList = _resource.getLanguages();
          String _resourceLanguages = "";

          if (languageList.size() > 0) {
            List<String> valid_lang = new LinkedList<String>();
            for (int i = 0; i < languageList.size(); i++) {
              if (languageList.get(i).getLanguage() == null) {
                continue;
              }
              String lang_var = languageList.get(i).getLanguage().getType();
              if (lang_var.equals("")) {
                continue;
              }
              valid_lang.add(_cache.getLanguageCode(lang_var) + " (" + lang_var + ")");
            }
            for (int j = 0; j < valid_lang.size() - 1; j++) {
              _resourceLanguages += valid_lang.get(j) + ", ";
            }
            _resourceLanguages += valid_lang.get(valid_lang.size() - 1) + "\n";
          }

          preview += snacText + "(s): " + _resourceLanguages + "\n";
          break;

        case "holding repository snac id":
          snacText = "Repository ID";

          int repo_id = _resource.getRepository().getID();
          String repo_str = "";

          if (repo_id != 0) {
            repo_str = Integer.toString(repo_id);
          }

          preview += snacText + ": " + repo_str + "\n";
      }
    }

    return preview;
  }

  public int rowIndex() {
    return _rowIndex;
  }

  public String toJSON() {
    return Resource.toJSON(this._resource);
  }

  public SNACAPIResponse performUpload(String url, String key) {
    SNACAPIClient client = new SNACAPIClient(url);

    String insertJSON = this.toJSON();

    String apiQuery =
        "{\"command\": \"insert_resource\",\n \"resource\":"
            + insertJSON.substring(0, insertJSON.length() - 1)
            + ",\n\"operation\":\"insert\"\n},\"apikey\":\""
            + key
            + "\""
            + "}";

    SNACAPIResponse insertResponse = client.post(apiQuery);

    return insertResponse;
  }

  private Term createTypeTerm(String type) {
    String term;
    int id;

    switch (type.toLowerCase()) {
      case "archivalresource":
      case "696":
        term = "ArchivalResource";
        id = 696;
        break;

      case "bibliographicresource":
      case "697":
        term = "BibliographicResource";
        id = 697;
        break;

      case "digitalarchivalresource":
      case "400479":
        term = "DigitalArchivalResource";
        id = 400479;
        break;

      case "oralhistoryresource":
      case "400623":
        term = "OralHistoryResource";
        id = 400623;
        break;

      default:
        logger.warn("createTypeTerm(): invalid/unhandled type: [" + type + "]");
        return null;
    }

    Term t = new Term();
    t.setType("document_role");
    t.setTerm(term);
    t.setID(id);

    return t;
  }
}
