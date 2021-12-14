package org.snaccooperative.exporters;

import static org.snaccooperative.schema.SNACSchemaUtilities.getCellValueForRowByColumnName;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
    // Insert by default
    res.setOperation("insert");

    // things to accumulate
    List<Language> languages = new LinkedList<Language>();

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
              res.setOperation("update");
            } catch (NumberFormatException e) {
              // If no numeric ID, leave operation as "insert"
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
            Language lang = cache.getLanguageCode(cellValue);
            if (lang != null) {
              languages.add(lang);
            }

            continue;

          case "holding repository snac id":
            try {
              int id = Integer.parseInt(cellValue);

              Constellation cons = new Constellation();

              if (id != 0) {
                cons.setID(id);
                res.setRepository(cons);
              }
            } catch (NumberFormatException e) {
            }
            continue;

          default:
            continue;
        }
      }
    }

    // add accumulated languages
    res.setLanguages(languages);
    this._resource = res;
  }

  public String getPreviewText() {
    String preview = "";

    if (_resource.getOperation() == "update") {
      preview += "------Editing Resource with ID:" + _resource.getID() + "------\n";
    } else {
      preview += "------Inserting new resource" + "------\n";
    }

    Map<String, String> resourceFields = new HashMap<>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacText = entry.getValue();
      String snacField = snacText.toLowerCase();

      switch (snacField) {
        case "id":
          // Already added to Preview
          // resourceFields.put(snacText, String.valueOf(_resource.getID()));
          break;

        case "type":
          Term previewTerm = _resource.getDocumentType();
          if (previewTerm != null) {
            resourceFields.put(snacText, previewTerm.getTerm() + " (" + previewTerm.getID() + ")");
          }
          break;

        case "title":
          resourceFields.put(snacText, _resource.getTitle());
          break;

        case "display entry":
          resourceFields.put(snacText, _resource.getDisplayEntry());
          break;

        case "link":
          resourceFields.put(snacText, _resource.getLink());
          break;

        case "abstract":
          resourceFields.put(snacText, _resource.getAbstract());
          break;

        case "extent":
          resourceFields.put(snacText, _resource.getExtent());
          break;

        case "date":
          resourceFields.put(snacText, _resource.getDate());
          break;

        case "language":
          List<Language> languageList = _resource.getLanguages();
          String _resourceLanguages = "";

          if (languageList.size() > 0) {
            for (int i = 0; i < languageList.size(); i++) {
              if (languageList.get(i).getLanguage() == null) {
                continue;
              }
              String lang_var = languageList.get(i).getLanguage().getDescription();
              if (lang_var.equals("")) {
                continue;
              }
              _resourceLanguages += lang_var + ", ";
            }
          }

          resourceFields.put(snacText, _resourceLanguages);
          break;

        case "holding repository snac id":
          snacText = "Repository ID";
          if (_resource.getRepository() != null) {

          // TODO: handle missing repository. Could check for cell value type and show error in Issues tab.
          int repo_id = _resource.getRepository().getID();
          String repo_str = "";

          if (repo_id != 0) {
            repo_str = Integer.toString(repo_id);
          }
          resourceFields.put(snacText, repo_str);
        }
      }
    }

    // TODO: Print preview in set order for consistency.
    for (String key : resourceFields.keySet()) {
      preview += key + ": " + resourceFields.get(key) + "\n";
      System.out.println(key + " => " + resourceFields.get(key));
    }

    System.out.print("OPERATION: ");
    System.out.println(_resource.getOperation());

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
            + insertJSON
            + ",\"apikey\":\""
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
    t.setType("document_type");
    t.setTerm(term);
    t.setID(id);

    return t;
  }
}
