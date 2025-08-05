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
import org.snaccooperative.openrefine.model.SNACModelField;
import org.snaccooperative.openrefine.model.SNACResourceModel;
import org.snaccooperative.openrefine.model.SNACResourceModel.ResourceModelField;
import org.snaccooperative.openrefine.schema.SNACSchema;

public class SNACResourceItem extends SNACAbstractItem {

  static final Logger logger = LoggerFactory.getLogger(SNACResourceItem.class);

  private Resource _item;

  private SNACResourceModel _model;

  public SNACResourceItem(
      Project project,
      SNACSchema schema,
      SNACAPIClient client,
      SNACLookupCache cache,
      Record record) {
    super(project, schema, client, cache, record);

    this._model = new SNACResourceModel();

    buildItemVerbatim();
  }

  protected void buildItem() {
    this._item = null;
    this._errors = new SNACValidationErrors();
    this._relatedIDs.put(ModelType.CONSTELLATION, new LinkedList<Integer>());

    SNACFieldValidator<ResourceModelField> validator =
        new SNACFieldValidator<ResourceModelField>(_model, _schema, _utils, _cache, _errors);

    Resource res = new Resource();
    res.setOperation(AbstractData.OPERATION_INSERT);

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String csvColumn = entry.getKey();
      String snacField = entry.getValue();

      ResourceModelField field = _model.getFieldType(snacField);
      SNACModelField<ResourceModelField> modelField = _model.getModelField(field);

      // initialize field tracker
      validator.initializeField(modelField);

      for (int i = _record.fromRowIndex; i < _record.toRowIndex; i++) {
        Row row = _project.rows.get(i);

        String cellValue = validator.getCellValue(row, csvColumn);

        if (cellValue.equals("")) {
          continue;
        }

        // quick check: ensure current field meets occurence and row dependency requirements
        // NOTE: fields are checked and counted even if they are invalid and would be skipped!
        if (!validator.checkAndCountField(modelField, cellValue, row)) {
          continue;
        }

        switch (field) {
          case RESOURCE_ID:
            Integer resourceID = validator.getIdentifier(field, cellValue);
            if (resourceID == null) {
              continue;
            }

            int rid = resourceID;
            res.setID(rid);
            res.setOperation(AbstractData.OPERATION_UPDATE);

            // record this id so that verifyRelatedIDs() checks for its existence
            _id = rid;

            continue;

          case RESOURCE_TYPE:
            Term resourceTypeTerm = validator.getTerm(field, cellValue, TermType.DOCUMENT_TYPE);
            if (resourceTypeTerm == null) {
              continue;
            }

            res.setDocumentType(resourceTypeTerm);

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

            Term languageCodeTerm = validator.getTerm(field, cellValue, TermType.LANGUAGE_CODE);
            if (languageCodeTerm == null) {
              continue;
            }

            // initialize language code portion
            Language lang = new Language();
            lang.setOperation(AbstractData.OPERATION_INSERT);
            lang.setLanguage(languageCodeTerm);

            // find and add optional 'script code' in this row
            Term optScriptCodeTerm =
                validator.getRelatedTerm(
                    row, field, cellValue, ResourceModelField.SCRIPT_CODE, TermType.SCRIPT_CODE);
            if (optScriptCodeTerm != null) {
              lang.setScript(optScriptCodeTerm);
            }

            res.addLanguage(lang);

            continue;

          case SCRIPT_CODE: // queried alongside LANGUAGE_CODE
            // NOTE: SNAC language type can contain any combination of language code and/or
            // script code.  Here, we check for the case when there is just a script code.

            // check whether there is an associated language code in this row; if so, skip
            String languageCode =
                validator.getRelatedCellValue(row, ResourceModelField.LANGUAGE_CODE);
            if (!languageCode.equals("")) {
              continue;
            }

            Term scriptCodeTerm = validator.getTerm(field, cellValue, TermType.SCRIPT_CODE);
            if (scriptCodeTerm == null) {
              continue;
            }

            // initialize script code portion
            Language script = new Language();
            script.setOperation(AbstractData.OPERATION_INSERT);
            script.setScript(scriptCodeTerm);

            res.addLanguage(script);

            continue;

          case HOLDING_REPOSITORY_ID:
            Integer holdingRepositoryID = validator.getIdentifier(field, cellValue);
            if (holdingRepositoryID == null) {
              continue;
            }

            int hrid = holdingRepositoryID;
            Constellation holdingRepository = new Constellation();
            holdingRepository.setID(hrid);
            res.setRepository(holdingRepository);

            // record this id so that verifyRelatedIDs() checks for its existence
            _relatedIDs.get(ModelType.CONSTELLATION).add(hrid);

            continue;
        }
      }

      // perform final checks on this field (for now, just ensures existence of required fields)
      validator.finalizeField(modelField);
    }

    _item = res;

    logger.debug("built resource: [" + toJSON() + "]");
  }

  public String getPreviewText() {
    Map<String, String> outFields = new TreeMap<>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacField = entry.getValue();

      ResourceModelField field = _model.getFieldType(snacField);

      switch (field) {
        case RESOURCE_TYPE:
          Term previewTerm = _item.getDocumentType();
          if (previewTerm != null) {
            outFields.put(snacField, previewTerm.getTerm());
          }
          continue;

        case TITLE:
          outFields.put(snacField, _item.getTitle());
          continue;

        case RESOURCE_URL:
          outFields.put(snacField, htmlLink(_item.getLink(), _item.getLink()));
          continue;

        case ABSTRACT:
          outFields.put(snacField, _item.getAbstract());
          continue;

        case EXTENT:
          outFields.put(snacField, _item.getExtent());
          continue;

        case DATE:
          outFields.put(snacField, _item.getDate());
          continue;

        case LANGUAGE_CODE:
          List<String> langList = new ArrayList<>();

          for (int i = 0; i < _item.getLanguages().size(); i++) {
            Term lang = _item.getLanguages().get(i).getLanguage();

            String langFull = "";

            if (lang != null) {
              String langCode = lang.getTerm();
              String langDesc = lang.getDescription();

              langFull = langCode;
              if (!langDesc.equals("")) {
                langFull += " (" + langDesc + ")";
              }
            }

            Term script = _item.getLanguages().get(i).getScript();

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

          continue;

        case HOLDING_REPOSITORY_ID:
          snacField = "Repository ID";
          if (_item.getRepository() != null) {
            int repo_id = _item.getRepository().getID();
            if (repo_id != 0) {
              outFields.put(
                  snacField,
                  htmlLink(_client.urlForConstellationID(repo_id), Integer.toString(repo_id)));
            }
          }
          continue;
      }
    }

    if (_item.getOperation().equals(AbstractData.OPERATION_UPDATE)) {
      outFields.put(
          "*** Operation ***",
          "Edit Resource with ID: "
              + htmlLink(_client.urlForResourceID(_item.getID()), Integer.toString(_item.getID())));
    } else {
      outFields.put("*** Operation ***", "Insert new Resource");
    }

    if (_errors.hasErrors()) {
      outFields.put("Validation Errors", htmlOrderedList(_errors.getErrors()));
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
    return Resource.toJSON(_item);
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
