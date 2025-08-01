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
import org.snaccooperative.data.AbstractData;
import org.snaccooperative.data.Constellation;
import org.snaccooperative.data.Language;
import org.snaccooperative.data.Resource;
import org.snaccooperative.data.Term;
import org.snaccooperative.openrefine.api.SNACAPIClient;
import org.snaccooperative.openrefine.api.SNACAPIResponse;
import org.snaccooperative.openrefine.cache.SNACLookupCache;
import org.snaccooperative.openrefine.cache.SNACLookupCache.TermType;
import org.snaccooperative.openrefine.model.SNACAbstractModel.ModelType;
import org.snaccooperative.openrefine.model.SNACResourceModel;
import org.snaccooperative.openrefine.model.SNACResourceModel.ResourceModelField;
import org.snaccooperative.openrefine.schema.SNACSchema;

public class SNACResourceItem extends SNACAbstractItem {

  static final Logger logger = LoggerFactory.getLogger(SNACResourceItem.class);

  private Resource _resource;

  private SNACResourceModel _resourceModel;

  public SNACResourceItem(
      Project project,
      SNACSchema schema,
      SNACAPIClient client,
      SNACLookupCache cache,
      Record record) {
    super(project, schema, client, cache, record);

    this._resourceModel = new SNACResourceModel();

    buildItemVerbatim();
  }

  protected void buildItem() {
    this._resource = null;
    this._relatedIDs.put(ModelType.CONSTELLATION, new LinkedList<Integer>());
    this._errors = new SNACValidationErrors();

    SNACFieldValidator<ResourceModelField> resourceValidator =
        new SNACFieldValidator<ResourceModelField>(_schema, _errors);

    Resource res = new Resource();
    res.setOperation(AbstractData.OPERATION_INSERT);

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String csvColumn = entry.getKey();
      String snacField = entry.getValue();

      ResourceModelField resourceField = _resourceModel.getFieldType(snacField);

      for (int i = _record.fromRowIndex; i < _record.toRowIndex; i++) {
        Row row = _project.rows.get(i);

        String cellValue = _utils.getCellValueForRowByColumnName(row, csvColumn);

        if (cellValue.equals("")) {
          continue;
        }

        // quick check: ensure current field can be populated (right now this just
        // prevents single-occurence fields from being specified multiple times)
        // NOTE: fields are counted even if they are invalid and would be skipped!
        if (resourceValidator.hasReachedLimit(_resourceModel.getModelField(resourceField))) {
          continue;
        }
        resourceValidator.addOccurence(_resourceModel.getModelField(resourceField));

        // quick check: ensure all required dependency/dependent fields exist and are not empty
        if (!_resourceModel.hasRequiredFieldsInRow(
            resourceField, cellValue, csvColumn, row, _schema, _utils, _errors)) {
          continue;
        }

        switch (resourceField) {
          case RESOURCE_ID:
            try {
              int id = Integer.parseInt(cellValue);
              res.setID(id);
              res.setOperation(AbstractData.OPERATION_UPDATE);
            } catch (NumberFormatException e) {
              _errors.addInvalidNumericFieldError(resourceField.getName(), cellValue, csvColumn);
            }

            continue;

          case RESOURCE_TYPE:
            Term typeTerm = _cache.getTerm(TermType.DOCUMENT_TYPE, cellValue);

            if (typeTerm == null) {
              _errors.addInvalidVocabularyFieldError(resourceField.getName(), cellValue, csvColumn);
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

            Term languageCodeTerm = _cache.getTerm(TermType.LANGUAGE_CODE, cellValue);

            if (languageCodeTerm == null) {
              _errors.addInvalidVocabularyFieldError(resourceField.getName(), cellValue, csvColumn);
              continue;
            }

            // initialize language code portion
            Language lang = new Language();
            lang.setOperation(AbstractData.OPERATION_INSERT);
            lang.setLanguage(languageCodeTerm);

            // find and add optional 'script code' in this row
            String scriptCodeColumn =
                _resourceModel.getEntryForFieldType(
                    ResourceModelField.SCRIPT_CODE, _schema.getColumnMappings());

            if (scriptCodeColumn != null) {
              String scriptCode = _utils.getCellValueForRowByColumnName(row, scriptCodeColumn);

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

            res.addLanguage(lang);

            continue;

          case SCRIPT_CODE: // queried alongside LANGUAGE_CODE
            // NOTE: SNAC language type can contain any combination of language code and/or
            // script code.  Here, we check for the case when there is just a script code.

            // check whether there is an associated language code in this row; if so, skip
            String languageCodeColumn =
                _resourceModel.getEntryForFieldType(
                    ResourceModelField.LANGUAGE_CODE, _schema.getColumnMappings());

            if (languageCodeColumn != null) {
              String languageCode = _utils.getCellValueForRowByColumnName(row, languageCodeColumn);

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
            script.setOperation(AbstractData.OPERATION_INSERT);
            script.setScript(scriptCodeTerm);

            res.addLanguage(script);

            continue;

          case HOLDING_REPOSITORY_ID:
            try {
              int id = Integer.parseInt(cellValue);

              Constellation cons = new Constellation();

              if (id != 0) {
                cons.setID(id);
                res.setRepository(cons);
                _relatedIDs.get(ModelType.CONSTELLATION).add(id);
              }
            } catch (NumberFormatException e) {
              _errors.addInvalidNumericFieldError(resourceField.getName(), cellValue, csvColumn);
            }

            continue;
        }
      }
    }

    _resource = res;

    logger.debug("built resource: [" + toJSON() + "]");
  }

  public String getPreviewText() {
    Map<String, String> outFields = new TreeMap<>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacField = entry.getValue();

      ResourceModelField resourceField = _resourceModel.getFieldType(snacField);

      switch (resourceField) {
        case RESOURCE_TYPE:
          Term previewTerm = _resource.getDocumentType();
          if (previewTerm != null) {
            outFields.put(snacField, previewTerm.getTerm());
          }
          break;

        case TITLE:
          outFields.put(snacField, _resource.getTitle());
          break;

        case RESOURCE_URL:
          outFields.put(snacField, htmlLink(_resource.getLink(), _resource.getLink()));
          break;

        case ABSTRACT:
          outFields.put(snacField, _resource.getAbstract());
          break;

        case EXTENT:
          outFields.put(snacField, _resource.getExtent());
          break;

        case DATE:
          outFields.put(snacField, _resource.getDate());
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
            outFields.put(snacField, htmlOrderedList(langList));
          }

          break;

        case HOLDING_REPOSITORY_ID:
          snacField = "Repository ID";
          if (_resource.getRepository() != null) {
            int repo_id = _resource.getRepository().getID();
            if (repo_id != 0) {
              outFields.put(
                  snacField,
                  htmlLink(_client.urlForConstellationID(repo_id), Integer.toString(repo_id)));
            }
          }
      }
    }

    if (_resource.getOperation().equals(AbstractData.OPERATION_UPDATE)) {
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

  public String toJSON() {
    return Resource.toJSON(_resource);
  }

  public SNACAPIResponse performUpload() {
    logger.info("preparing to upload resource to SNAC...");

    // validate resource data before uploading
    SNACAPIResponse validationError = performValidation();
    if (validationError != null && !validationError.getResult().equals("success")) {
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
