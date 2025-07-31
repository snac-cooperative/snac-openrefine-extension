package org.snaccooperative.openrefine.exporters;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.data.Constellation;
import org.snaccooperative.data.Language;
import org.snaccooperative.data.Resource;
import org.snaccooperative.data.Term;
import org.snaccooperative.openrefine.api.SNACAPIClient;
import org.snaccooperative.openrefine.api.SNACAPIResponse;
import org.snaccooperative.openrefine.cache.SNACLookupCache;
import org.snaccooperative.openrefine.cache.SNACLookupCache.TermType;
import org.snaccooperative.openrefine.model.SNACResourceModel;
import org.snaccooperative.openrefine.model.SNACResourceModel.ResourceModelField;
import org.snaccooperative.openrefine.schema.SNACSchema;
import org.snaccooperative.openrefine.schema.SNACSchemaUtilities;

public class SNACResourceItem extends SNACAbstractItem {

  static final Logger logger = LoggerFactory.getLogger("SNACResourceItem");

  private Project _project;
  private Record _record;
  private SNACSchema _schema;
  private SNACAPIClient _client;
  private SNACLookupCache _cache;
  private int _rowIndex;

  private Resource _resource;
  private List<Integer> _relatedConstellations;

  private SNACResourceModel _resourceModel;

  private SNACValidationErrors _errors;

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

    buildResourceVerbatim();
  }

  private void buildResource() {
    _resource = null;

    SNACSchemaUtilities schemaUtils = new SNACSchemaUtilities(_project, _schema);

    _relatedConstellations = new LinkedList<Integer>();
    _errors = new SNACValidationErrors();

    Resource res = new Resource();
    res.setOperation("insert");

    // things to accumulate
    List<Language> languages = new LinkedList<Language>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String csvColumn = entry.getKey();

      ResourceModelField resourceField = _resourceModel.getFieldType(entry.getValue());

      for (int i = _record.fromRowIndex; i < _record.toRowIndex; i++) {
        Row row = _project.rows.get(i);

        String cellValue = schemaUtils.getCellValueForRowByColumnName(row, csvColumn);

        if (cellValue.equals("")) {
          continue;
        }

        // quick check: ensure all required dependency/dependent fields exist and are not empty
        if (!_resourceModel.hasRequiredFieldsInRow(
            resourceField, cellValue, csvColumn, row, _schema, schemaUtils, _errors)) {
          continue;
        }

        switch (resourceField) {
          case RESOURCE_ID:
            // FIXME: does not enforce one value per record

            try {
              int id = Integer.parseInt(cellValue);
              res.setID(id);
              res.setOperation("update");
            } catch (NumberFormatException e) {
              _errors.addInvalidNumericFieldError(resourceField.getName(), cellValue, csvColumn);
            }

            continue;

          case RESOURCE_TYPE:
            // FIXME: does not enforce one value per record

            Term typeTerm = _cache.getTerm(TermType.DOCUMENT_TYPE, cellValue);

            if (typeTerm == null) {
              _errors.addInvalidVocabularyFieldError(resourceField.getName(), cellValue, csvColumn);
              continue;
            }

            res.setDocumentType(typeTerm);

            continue;

          case TITLE:
            // FIXME: does not enforce one value per record

            res.setTitle(cellValue);

            continue;

          case RESOURCE_URL:
            // FIXME: does not enforce one value per record

            res.setLink(cellValue);

            continue;

          case ABSTRACT:
            // FIXME: does not enforce one value per record

            res.setAbstract(cellValue);

            continue;

          case EXTENT:
            // FIXME: does not enforce one value per record

            res.setExtent(cellValue);

            continue;

          case DATE:
            // FIXME: does not enforce one value per record

            res.setDate(cellValue);

            continue;

          case LANGUAGE_CODE: // queried alongside SCRIPT_CODE
            // NOTE: SNAC language type can contain any combination of language code and/or
            // script code.  Here, we check for the cases that contain a language code.

            Term languageCodeTerm = _cache.getTerm(TermType.LANGUAGE_CODE, cellValue);

            if (languageCodeTerm == null) {
              _errors.addInvalidVocabularyFieldError(resourceField.getName(), cellValue, csvColumn);
              continue;
            }

            // initialize language code portion
            Language lang = new Language();
            lang.setOperation("insert");
            lang.setLanguage(languageCodeTerm);

            // find and add optional 'script code' in this row
            String scriptCodeColumn =
                _resourceModel.getEntryForFieldType(
                    ResourceModelField.SCRIPT_CODE, _schema.getColumnMappings());

            if (scriptCodeColumn != null) {
              String scriptCode = schemaUtils.getCellValueForRowByColumnName(row, scriptCodeColumn);

              if (!scriptCode.equals("")) {
                Term scriptCodeTerm = _cache.getTerm(TermType.SCRIPT_CODE, scriptCode);
                if (scriptCodeTerm != null) {
                  // add script code portion
                  lang.setScript(scriptCodeTerm);
                } else {
                  _errors.addInvalidVocabularyFieldError(
                      ResourceModelField.SCRIPT_CODE.getName(),
                      scriptCode,
                      scriptCodeColumn,
                      resourceField.getName(),
                      cellValue,
                      csvColumn);
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
            String languageCodeColumn =
                _resourceModel.getEntryForFieldType(
                    ResourceModelField.LANGUAGE_CODE, _schema.getColumnMappings());

            if (languageCodeColumn != null) {
              String languageCode =
                  schemaUtils.getCellValueForRowByColumnName(row, languageCodeColumn);

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

            Term scriptCodeTerm = _cache.getTerm(TermType.SCRIPT_CODE, cellValue);

            if (scriptCodeTerm == null) {
              _errors.addInvalidVocabularyFieldError(resourceField.getName(), cellValue, csvColumn);
              continue;
            }

            // initialize script code portion
            Language script = new Language();
            script.setOperation("insert");
            script.setScript(scriptCodeTerm);

            languages.add(script);

            continue;

          case HOLDING_REPOSITORY_ID:
            // FIXME: does not enforce one value per record

            try {
              int id = Integer.parseInt(cellValue);

              Constellation cons = new Constellation();

              if (id != 0) {
                cons.setID(id);
                res.setRepository(cons);
                _relatedConstellations.add(id);
              }
            } catch (NumberFormatException e) {
              _errors.addInvalidNumericFieldError(resourceField.getName(), cellValue, csvColumn);
            }

            continue;
        }
      }
    }

    // add accumulated languages
    res.setLanguages(languages);

    _resource = res;

    logger.debug("built resource: [" + toJSON() + "]");
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
    return Resource.toJSON(_resource);
  }

  private void verifyRelatedIDs() {
    // verify existence of any related holding repository IDs in the
    // selected SNAC environment (there will be at most one)

    for (int i = 0; i < _relatedConstellations.size(); i++) {
      int id = _relatedConstellations.get(i);
      if (!_cache.constellationExists(id)) {
        _errors.addMissingHoldingRepositoryError(id);
      }
    }
  }

  public SNACAPIResponse performValidation() {
    // create the resource, validating any controlled vocabulary terms
    // against the selected SNAC environment

    buildResourceAgainstSNAC();

    // verify existence of any related IDs

    verifyRelatedIDs();

    // return error if validation errors were encountered at any point

    if (_errors.hasErrors()) {
      return new SNACAPIResponse(_client, _errors.getAccumulatedErrorString());
    }

    return null;
  }

  public SNACAPIResponse performUpload() {
    logger.info("preparing to upload resource to SNAC...");

    // validate resource data before uploading
    SNACAPIResponse validationError = performValidation();
    if (validationError != null) {
      return validationError;
    }

    JSONObject req = new JSONObject();

    req.put("command", "insert_resource");
    req.put("apikey", _client.apiKey());
    req.put("resource", new JSONObject(toJSON()));

    SNACAPIResponse insertResponse = _client.post(req);

    logger.info("resource upload complete");

    return insertResponse;
  }
}
