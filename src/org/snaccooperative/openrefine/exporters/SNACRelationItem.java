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
import org.snaccooperative.data.ConstellationRelation;
import org.snaccooperative.data.Resource;
import org.snaccooperative.data.ResourceRelation;
import org.snaccooperative.data.Term;
import org.snaccooperative.openrefine.api.SNACAPIClient;
import org.snaccooperative.openrefine.api.SNACAPIResponse;
import org.snaccooperative.openrefine.cache.SNACLookupCache;
import org.snaccooperative.openrefine.cache.SNACLookupCache.TermType;
import org.snaccooperative.openrefine.model.SNACAbstractModel.ModelType;
import org.snaccooperative.openrefine.model.SNACRelationModel;
import org.snaccooperative.openrefine.model.SNACRelationModel.RelationModelField;
import org.snaccooperative.openrefine.schema.SNACSchema;

public class SNACRelationItem extends SNACAbstractItem {

  static final Logger logger = LoggerFactory.getLogger(SNACRelationItem.class);

  private Constellation _item;

  private SNACRelationModel _model;

  public SNACRelationItem(
      Project project,
      SNACSchema schema,
      SNACAPIClient client,
      SNACLookupCache cache,
      Record record) {
    super(project, schema, client, cache, record);

    this._model = new SNACRelationModel();

    buildItemVerbatim();
  }

  protected void buildItem() {
    this._item = null;
    this._relatedIDs.put(ModelType.CONSTELLATION, new LinkedList<Integer>());
    this._relatedIDs.put(ModelType.RESOURCE, new LinkedList<Integer>());
    this._errors = new SNACValidationErrors();

    SNACFieldValidator<RelationModelField> validator =
        new SNACFieldValidator<RelationModelField>(_model, _schema, _utils, _errors);

    Constellation con = new Constellation();
    con.setOperation(AbstractData.OPERATION_INSERT);

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String csvColumn = entry.getKey();
      String snacField = entry.getValue();

      for (int i = _record.fromRowIndex; i < _record.toRowIndex; i++) {
        Row row = _project.rows.get(i);

        String cellValue = _utils.getCellValueForRowByColumnName(row, csvColumn);

        if (cellValue.equals("")) {
          continue;
        }

        RelationModelField field = _model.getFieldType(snacField);

        // quick check: ensure current field meets occurence and dependency requirements
        // NOTE: fields are checked and counted even if they are invalid and would be skipped!
        if (!validator.checkAndCountField(_model.getModelField(field), cellValue, row)) {
          continue;
        }

        switch (field) {
          case CPF_ID:
            try {
              int id = Integer.parseInt(cellValue);
              con.setID(id);
            } catch (NumberFormatException e) {
              _errors.addInvalidNumericFieldError(field.getName(), cellValue, csvColumn);
            }

            continue;

          case CPF_TYPE:
            Term entityTypeTerm = _cache.getTerm(TermType.ENTITY_TYPE, cellValue);

            if (entityTypeTerm == null) {
              _errors.addInvalidVocabularyFieldError(field.getName(), cellValue, csvColumn);
              continue;
            }

            con.setEntityType(entityTypeTerm);

            continue;

          case RELATED_RESOURCE_ID:
            try {
              int targetResource = Integer.parseInt(cellValue);
              Resource resource = new Resource();
              resource.setID(targetResource);

              ResourceRelation resourceRelation = new ResourceRelation();
              resourceRelation.setOperation(AbstractData.OPERATION_INSERT);
              resourceRelation.setResource(resource);

              // find and add required 'cpf to resource relation type' in this row
              String resourceRoleColumn =
                  _model.getEntryForFieldType(
                      RelationModelField.CPF_TO_RESOURCE_RELATION_TYPE,
                      _schema.getColumnMappings());

              String resourceRole = _utils.getCellValueForRowByColumnName(row, resourceRoleColumn);

              Term resourceRoleTerm = _cache.getTerm(TermType.DOCUMENT_ROLE, resourceRole);

              if (resourceRoleTerm != null) {
                resourceRelation.setRole(resourceRoleTerm);
              } else {
                _errors.addInvalidVocabularyFieldError(
                    RelationModelField.CPF_TO_RESOURCE_RELATION_TYPE.getName(),
                    resourceRole,
                    resourceRoleColumn,
                    field.getName(),
                    cellValue,
                    csvColumn);
                continue;
              }

              con.addResourceRelation(resourceRelation);
              _relatedIDs.get(ModelType.RESOURCE).add(targetResource);
            } catch (NumberFormatException e) {
              _errors.addInvalidNumericFieldError(field.getName(), cellValue, csvColumn);
              continue;
            }

            continue;

          case CPF_TO_RESOURCE_RELATION_TYPE: // queried alongside RESOURCE_ID
            continue;

          case RELATED_CPF_ID:
            try {
              int targetConstellation = Integer.parseInt(cellValue);
              ConstellationRelation cpfRelation = new ConstellationRelation();
              cpfRelation.setSourceConstellation(con.getID());
              cpfRelation.setTargetConstellation(targetConstellation);

              // find and add required 'cpf to cpf relation type' in this row
              String cpfRelationTypeColumn =
                  _model.getEntryForFieldType(
                      RelationModelField.CPF_TO_CPF_RELATION_TYPE, _schema.getColumnMappings());

              String cpfRelationType =
                  _utils.getCellValueForRowByColumnName(row, cpfRelationTypeColumn);

              Term cpfRelationTypeTerm = _cache.getTerm(TermType.RELATION_TYPE, cpfRelationType);

              if (cpfRelationTypeTerm != null) {
                cpfRelation.setType(cpfRelationTypeTerm);
                cpfRelation.setOperation(AbstractData.OPERATION_INSERT);
              } else {
                _errors.addInvalidVocabularyFieldError(
                    RelationModelField.CPF_TO_CPF_RELATION_TYPE.getName(),
                    cpfRelationType,
                    cpfRelationTypeColumn,
                    field.getName(),
                    cellValue,
                    csvColumn);
                continue;
              }

              con.addRelation(cpfRelation);
              _relatedIDs.get(ModelType.CONSTELLATION).add(targetConstellation);
            } catch (NumberFormatException e) {
              _errors.addInvalidNumericFieldError(field.getName(), cellValue, csvColumn);
              continue;
            }

            continue;

          case CPF_TO_CPF_RELATION_TYPE: // queried alongside RELATED_CPF_ID
            continue;
        }
      }
    }

    logger.debug("built constellation: [" + toJSON() + "]");
  }

  public String getPreviewText() {
    Map<String, String> outFields = new TreeMap<>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacField = entry.getValue();

      switch (_model.getFieldType(entry.getValue())) {
        case CPF_TYPE:
          Term previewTerm = _item.getEntityType();
          if (previewTerm != null) {
            outFields.put(snacField, previewTerm.getTerm());
          }
          continue;

        case RELATED_CPF_ID:
          List<String> relations = new ArrayList<String>();
          for (int i = 0; i < _item.getRelations().size(); i++) {
            ConstellationRelation relation = _item.getRelations().get(i);
            int relationID = relation.getTargetConstellation();
            String relationAndType =
                htmlLink(_client.urlForConstellationID(relationID), Integer.toString(relationID));
            if (relation.getType() != null) {
              relationAndType += " (" + relation.getType().getTerm() + ")";
            }
            relations.add(relationAndType);
          }
          outFields.put(snacField, htmlOrderedList(relations));
          continue;

        case RELATED_RESOURCE_ID:
          List<String> resourceRelations = new ArrayList<String>();
          for (int i = 0; i < _item.getResourceRelations().size(); i++) {
            ResourceRelation resourceRelation = _item.getResourceRelations().get(i);
            int resourceRelationID = resourceRelation.getResource().getID();
            String resourceRelationAndRole =
                htmlLink(
                    _client.urlForResourceID(resourceRelationID),
                    Integer.toString(resourceRelationID));
            if (resourceRelation.getRole() != null) {
              resourceRelationAndRole += " (" + resourceRelation.getRole().getTerm() + ")";
            }
            resourceRelations.add(resourceRelationAndRole);
          }
          outFields.put(snacField, htmlOrderedList(resourceRelations));
          continue;
      }
    }

    if (_item.getOperation().equals(AbstractData.OPERATION_UPDATE)) {
      outFields.put(
          "*** Operation ***",
          "Edit Constellation with ID: "
              + htmlLink(
                  _client.urlForConstellationID(_item.getID()), Integer.toString(_item.getID())));
    } else {
      outFields.put("*** Operation ***", "Insert new Constellation");
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
    return Constellation.toJSON(_item);
  }

  public SNACAPIResponse performUpload() {
    logger.info("preparing to upload constellation to SNAC...");

    // validate constellation data before uploading
    SNACAPIResponse validationError = performValidation();
    if (validationError != null && !validationError.getResult().equals("success")) {
      return validationError;
    }

    // so far so good, proceed with upload

    int myID = _item.getID();

    if (myID > 0) {
      // update existing constellation (edit/update)

      // checkout current version

      logger.info("checking out existing constellation [" + myID + "]...");

      JSONObject req = new JSONObject();

      req.put("command", "edit");
      req.put("constellationid", myID);
      req.put("apikey", _client.apiKey());

      SNACAPIResponse checkoutResponse = _client.post(req);

      Constellation checkoutCon = checkoutResponse.getConstellation();

      if (checkoutCon == null) {
        logger.error("error checking out constellation");
        return checkoutResponse;
      }

      // set update information

      _item.setOperation(AbstractData.OPERATION_UPDATE);
      _item.setID(checkoutCon.getID());
      _item.setVersion(checkoutCon.getVersion());
      _item.setArk(checkoutCon.getArk());
    }

    logger.info("uploading constellation...");

    JSONObject req = new JSONObject();

    req.put("command", "insert_and_publish_constellation");
    req.put("apikey", _client.apiKey());
    req.put("constellation", new JSONObject(toJSON()));

    SNACAPIResponse updateResponse = _client.post(req);

    logger.info("constellation upload complete");

    return updateResponse;
  }
}
