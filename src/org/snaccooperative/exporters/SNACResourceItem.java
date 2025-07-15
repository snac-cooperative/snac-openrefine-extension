package org.snaccooperative.exporters;

import static org.snaccooperative.schema.SNACSchemaUtilities.getCellValueForRowByColumnName;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import org.snaccooperative.model.SNACResourceModel;
import org.snaccooperative.model.SNACResourceModel.ResourceModelField;

public class SNACResourceItem extends SNACUploadItem {

  static final Logger logger = LoggerFactory.getLogger("SNACResourceItem");

  protected Project _project;
  protected Record _record;
  protected SNACSchema _schema;
  protected SNACAPIClient _client;
  protected SNACLookupCache _cache;
  protected int _rowIndex;

  protected Resource _resource;
  protected List<Integer> _relatedConstellations;
  protected List<String> _validationErrors;

  protected SNACResourceModel _resourceModel;

  public SNACResourceItem(
      Project project,
      SNACSchema schema,
      SNACAPIClient client,
      SNACLookupCache cache,
      Record record) {
    this._project = project;
    this._schema = schema;
    this._client = client;
    this._cache = cache;
    this._record = record;

    this._rowIndex = record.fromRowIndex;

    this._resourceModel = new SNACResourceModel();
  }

  private void buildResource() {
    _resource = null;

    _relatedConstellations = new LinkedList<Integer>();
    _validationErrors = new ArrayList<String>();

    Resource res = new Resource();
    res.setOperation("insert");

    // things to accumulate
    List<Language> languages = new LinkedList<Language>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String csvColumn = entry.getKey();

      ResourceModelField resourceField = _resourceModel.getFieldType(entry.getValue());

      for (int i = _record.fromRowIndex; i < _record.toRowIndex; i++) {
        Row row = _project.rows.get(i);

        String cellValue = getCellValueForRowByColumnName(_project, row, csvColumn);

        if (cellValue.equals("")) {
          continue;
        }

        switch (resourceField) {
          case RESOURCE_ID:
            try {
              int id = Integer.parseInt(cellValue);
              res.setID(id);
              res.setOperation("update");
            } catch (NumberFormatException e) {
              // If no numeric ID, leave operation as "insert"
              _validationErrors.add("Invalid SNAC Resource ID: [" + cellValue + "]");
            }
            continue;

          case RESOURCE_TYPE:
            Term typeTerm = _cache.getDocumentTypeTerm(cellValue);

            if (typeTerm == null) {
              logger.warn("skipping unknown resource type [" + cellValue + "]");
              _validationErrors.add("Invalid SNAC Resource Type: [" + cellValue + "]");
              continue;
            }

            res.setDocumentType(typeTerm);

            continue;

          case TITLE:
            res.setTitle(cellValue);
            continue;

          case RESOURCE_URL:
            res.setLink(cellValue);
            continue;

          case ABSTRACT:
            res.setAbstract(cellValue);
            continue;

          case EXTENT:
            res.setExtent(cellValue);
            continue;

          case DATE:
            res.setDate(cellValue);
            continue;

          case LANGUAGE_CODE: // queried alongside SCRIPT_CODE
            // NOTE: SNAC language type can contain any combination of language code and/or
            // script code.  Here, we check for the cases that contain a language code.

            Term languageCodeTerm = _cache.getLanguageCodeTerm(cellValue);

            if (languageCodeTerm == null) {
              logger.warn("skipping unknown language code [" + cellValue + "]");
              _validationErrors.add("Invalid Language Code: [" + cellValue + "]");
              continue;
            }

            // initialize language code portion
            Language lang = new Language();
            lang.setOperation("insert");
            lang.setLanguage(languageCodeTerm);

            // find and add optional associated script code in this row
            String scriptCodeColumn = _resourceModel.getEntryForFieldType(ResourceModelField.SCRIPT_CODE, _schema.getColumnMappings());

            if (scriptCodeColumn != null) {
              String scriptCode = getCellValueForRowByColumnName(_project, row, scriptCodeColumn);

              if (!scriptCode.equals("")) {
                Term scriptCodeTerm = _cache.getScriptCodeTerm(scriptCode);
                if (scriptCodeTerm != null) {
                  // add script code portion
                  lang.setScript(scriptCodeTerm);
                } else {
                  logger.warn("omitting invalid script code [" + scriptCode + "]");
                  _validationErrors.add(
                      "Invalid Script Code: ["
                          + scriptCode
                          + "] for Language Code: ["
                          + cellValue
                          + "]");
                  continue;
                }
              } else {
                // logger.info("no associated script code value found; skipping");
              }
            } else {
              // logger.info("no associated script code column found; skipping");
            }

            languages.add(lang);

            continue;

          case SCRIPT_CODE: // queried alongside LANGUAGE_CODE
            // NOTE: SNAC language type can contain any combination of language code and/or
            // script code.  Here, we check for the case when there is just a script code.

            // check whether there is an associated language code in this row; if so, skip
            String languageCodeColumn = _resourceModel.getEntryForFieldType(ResourceModelField.LANGUAGE_CODE, _schema.getColumnMappings());

            if (languageCodeColumn != null) {
              String languageCode =
                  getCellValueForRowByColumnName(_project, row, languageCodeColumn);

              if (!languageCode.equals("")) {
                // this scenario is handled in the "language code" section
                // logger.info("skipping script code with associated language code");
                continue;
              } else {
                // logger.info("no associated language code value found; proceeding");
              }
            } else {
              // logger.info("no associated language code column found; proceeding");
            }

            Term scriptCodeTerm = _cache.getScriptCodeTerm(cellValue);

            if (scriptCodeTerm == null) {
              logger.warn("skipping unknown script code [" + cellValue + "]");
              _validationErrors.add("Invalid Script Code: [" + cellValue + "]");
              continue;
            }

            // initialize script code portion
            Language script = new Language();
            script.setOperation("insert");
            script.setScript(scriptCodeTerm);

            languages.add(script);

            continue;

          case HOLDING_REPOSITORY_ID:
            try {
              int id = Integer.parseInt(cellValue);

              Constellation cons = new Constellation();

              if (id != 0) {
                cons.setID(id);
                res.setRepository(cons);
                _relatedConstellations.add(id);
              }
            } catch (NumberFormatException e) {
              _validationErrors.add("Invalid Holding Repository SNAC ID: [" + cellValue + "]");
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

    logger.debug("built resource: [" + this.toJSON() + "]");
  }

  private void buildResourceVerbatim() {
    _cache.disableTermCache();
    buildResource();
  }

  private void buildResourceAgainstSNAC() {
    _cache.enableTermCache();
    buildResource();
  }

  public String getPreviewText() {
    buildResourceVerbatim();

    Map<String, String> outFields = new TreeMap<>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacText = entry.getValue();

      ResourceModelField resourceField = _resourceModel.getFieldType(entry.getValue());

      switch (resourceField) {
        case RESOURCE_TYPE:
          Term previewTerm = _resource.getDocumentType();
          if (previewTerm != null) {
            outFields.put(snacText, previewTerm.getTerm());
          }
          break;

        case TITLE:
          outFields.put(snacText, _resource.getTitle());
          break;

        case RESOURCE_URL:
          outFields.put(snacText, htmlLink(_resource.getLink(), _resource.getLink()));
          break;

        case ABSTRACT:
          outFields.put(snacText, _resource.getAbstract());
          break;

        case EXTENT:
          outFields.put(snacText, _resource.getExtent());
          break;

        case DATE:
          outFields.put(snacText, _resource.getDate());
          break;

        case LANGUAGE_CODE:
          List<String> langList = new ArrayList<>();

          for (int i = 0; i < _resource.getLanguages().size(); i++) {
            Term lang = _resource.getLanguages().get(i).getLanguage();

            String langFull = "";

            if (lang != null) {
              String langCode = lang.getTerm();
              String langDesc = lang.getDescription();

              langFull = langCode;
              if (!langDesc.equals("")) {
                langFull += " (" + langDesc + ")";
              }
            }

            Term script = _resource.getLanguages().get(i).getScript();

            String scriptFull = "";

            if (script != null) {
              String scriptCode = script.getTerm();
              String scriptDesc = script.getDescription();

              scriptFull = scriptCode;
              if (!scriptDesc.equals("")) {
                scriptFull += " (" + scriptDesc + ")";
              }
            }

            if (langFull.equals("")) {
              if (scriptFull.equals("")) {
                // no language or script?  shouldn't happen
              } else {
                langList.add("Script: " + scriptFull);
              }
            } else {
              if (scriptFull.equals("")) {
                langList.add("Language: " + langFull);
              } else {
                langList.add("Language: " + langFull + " / Script: " + scriptFull);
              }
            }
          }

          if (langList.size() > 0) {
            outFields.put(snacText, htmlOrderedList(langList));
          }

          break;

        case HOLDING_REPOSITORY_ID:
          snacText = "Repository ID";
          if (_resource.getRepository() != null) {
            int repo_id = _resource.getRepository().getID();
            if (repo_id != 0) {
              outFields.put(
                  snacText,
                  htmlLink(_client.urlForConstellationID(repo_id), Integer.toString(repo_id)));
            }
          }
      }
    }

    if (_resource.getOperation() == "update") {
      outFields.put(
          "*** Operation ***",
          "Edit Resource with ID: "
              + htmlLink(
                  _client.urlForResourceID(_resource.getID()),
                  Integer.toString(_resource.getID())));
    } else {
      outFields.put("*** Operation ***", "Insert new Resource");
    }

    String preview = "";
    // FIXME: output in predetermined order (not just alphabetical)?
    for (String key : outFields.keySet()) {
      String out = outFields.get(key);
      if (out == null || out.equals("")) {
        continue;
      }
      preview += htmlTableRow(htmlTableColumnField(key) + htmlTableColumnValue(out));
      // logger.info(key + " => " + out);
    }
    preview = htmlTable(preview);

    return preview;
  }

  public int rowIndex() {
    return _rowIndex;
  }

  public String toJSON() {
    return Resource.toJSON(this._resource);
  }

  private SNACAPIResponse verifyRelatedIDs() {
    // Before uploading, we verify existence of any related holding
    // repository IDs in the selected SNAC environment.

    List<String> relationErrors = new LinkedList<String>();

    logger.info("verifying existence of holding repository constellations...");

    for (int i = 0; i < _relatedConstellations.size(); i++) {
      int id = _relatedConstellations.get(i);
      logger.info("verifying existence of holding repository constellation: " + id);
      if (!_cache.constellationExists(id)) {
        relationErrors.add("* Holding Repository SNAC ID " + id + " not found in SNAC");
      }
    }

    if (relationErrors.size() > 0) {
      String errMsg = String.join("\n\n", relationErrors);
      logger.warn("resource validation error: [" + errMsg + "]");
      return new SNACAPIResponse(this._client, errMsg);
    }

    return new SNACAPIResponse(this._client, "success");
  }

  public SNACAPIResponse performValidation() {
    buildResourceAgainstSNAC();

    logger.info("validating resource data...");

    // return error if validation errors were encountered earlier
    if (_validationErrors.size() > 0) {
      for (int i = 0; i < _validationErrors.size(); i++) {
        _validationErrors.set(i, "* " + _validationErrors.get(i));
      }
      String errMsg = String.join("\n", _validationErrors);
      return new SNACAPIResponse(this._client, errMsg);
    }

    // verify any related IDs
    return verifyRelatedIDs();
  }

  public SNACAPIResponse performUpload() {
    logger.info("preparing to upload resource to SNAC...");

    // validate resource data before uploading
    SNACAPIResponse validationError = performValidation();
    if (validationError != null && !validationError.getResult().equals("success")) {
      return validationError;
    }

    String resourceJSON = this.toJSON();

    String apiStr = "\"apikey\": \"" + this._client.apiKey() + "\"";
    String apiQuery =
        "{ \"command\": \"insert_resource\", " + apiStr + ", \"resource\": " + resourceJSON + " }";

    SNACAPIResponse insertResponse = this._client.post(apiQuery);

    logger.info("resource upload complete");

    return insertResponse;
  }
}
