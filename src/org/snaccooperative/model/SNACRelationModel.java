package org.snaccooperative.model;

import org.apache.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.model.SNACModelField.FieldOccurence;
import org.snaccooperative.model.SNACModelField.FieldRequirement;
import org.snaccooperative.model.SNACModelField.FieldVocabulary;

public class SNACRelationModel extends SNACAbstractModel<SNACRelationModel.RelationModelField> {

  public enum RelationModelField {
    NONE,
    CPF_TYPE,
    CPF_ID,
    CPF_TO_CPF_RELATION_TYPE,
    RELATED_CPF_ID,
    CPF_TO_RESOURCE_RELATION_TYPE,
    RELATED_RESOURCE_ID
  }

  static final Logger logger = LoggerFactory.getLogger("SNACRelationModel");

  public SNACRelationModel() {
    super("relation", RelationModelField.NONE);

    addField(
        RelationModelField.CPF_TYPE,
        new SNACModelField(
            "CPF Type",
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.CONTROLLED,
            "Type of CPF entity.  Possible values are: corporateBody, person, family"));

    addField(
        RelationModelField.CPF_ID,
        new SNACModelField(
            "SNAC CPF ID", // TODO: rename?
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC identifier for the CPF entity.  Leave blank if the CPF is NOT in SNAC."));

    addField(
        RelationModelField.CPF_TO_CPF_RELATION_TYPE,
        new SNACModelField(
            "CPF to CPF Relation Type",
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "Nature of the relation of the CPF entity with the related CPF entity.  The following"
                + " values may be used: associatedWith, correspondedWith.  Only used if Related"
                + " SNAC CPF ID field is defined in the same row."));

    addField(
        RelationModelField.RELATED_CPF_ID,
        new SNACModelField(
            "Related SNAC CPF ID", // TODO: rename?
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC ID of a CPF entity in SNAC related to the CPF entity."));

    addField(
        RelationModelField.CPF_TO_RESOURCE_RELATION_TYPE,
        new SNACModelField(
            "CPF to Resource Relation Type",
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "Role of the CPF entity in relation to the Resource.  The following values may be used:"
                + " contributorOf, creatorOf, editorOf, referencedIn.  Only used if SNAC Resource"
                + " ID field is defined in the same row."));

    addField(
        RelationModelField.RELATED_RESOURCE_ID,
        new SNACModelField(
            "SNAC Resource ID", // TODO: rename?
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC ID for a related Resource in SNAC."));
  }
}
