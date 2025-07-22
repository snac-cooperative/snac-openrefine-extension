package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACAbstractModel.ModelType;
import org.snaccooperative.openrefine.model.SNACModelField.FieldOccurence;
import org.snaccooperative.openrefine.model.SNACModelField.FieldRequirement;
import org.snaccooperative.openrefine.model.SNACModelField.FieldVocabulary;

public class SNACRelationModel extends SNACAbstractModel<SNACRelationModel.RelationModelField> {

  public enum RelationModelField implements SNACModelFieldType {
    NONE,
    CPF_TYPE("CPF Type"),
    CPF_ID("CPF ID"),
    CPF_TO_CPF_RELATION_TYPE("CPF to CPF Relation Type"),
    RELATED_CPF_ID("Related CPF ID"),
    CPF_TO_RESOURCE_RELATION_TYPE("CPF to Resource Relation Type"),
    RELATED_RESOURCE_ID("Resource ID");

    private final String _name;

    RelationModelField() {
      this("");
    }

    RelationModelField(String name) {
      this._name = name;
    }

    public String getName() {
      return this._name;
    }
  }

  static final Logger logger = LoggerFactory.getLogger("SNACRelationModel");

  public SNACRelationModel() {
    super(ModelType.RELATION, RelationModelField.NONE);

    addField(
        new SNACModelField<RelationModelField>(
            RelationModelField.CPF_TYPE,
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.CONTROLLED,
            "Type of CPF entity.  Possible values are: corporateBody, person, family"));

    addField(
        new SNACModelField<RelationModelField>(
            RelationModelField.CPF_ID,
            new ArrayList<String>(Arrays.asList("SNAC CPF ID")), // previous name(s)
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC identifier for the CPF entity.  Leave blank if the CPF is NOT in SNAC."));

    addField(
        new SNACModelField<RelationModelField>(
            RelationModelField.CPF_TO_CPF_RELATION_TYPE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "Nature of the relation of the CPF entity with the related CPF entity.  The following values may be used: associatedWith, correspondedWith.  Only used if Related CPF ID field is defined in the same row."));

    addField(
        new SNACModelField<RelationModelField>(
            RelationModelField.RELATED_CPF_ID,
            new ArrayList<String>(Arrays.asList("Related SNAC CPF ID")), // previous name(s)
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC ID of a CPF entity in SNAC related to the CPF entity."));

    addField(
        new SNACModelField<RelationModelField>(
            RelationModelField.CPF_TO_RESOURCE_RELATION_TYPE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "Role of the CPF entity in relation to the Resource.  The following values may be used: contributorOf, creatorOf, editorOf, referencedIn.  Only used if Resource ID field is defined in the same row."));

    addField(
        new SNACModelField<RelationModelField>(
            RelationModelField.RELATED_RESOURCE_ID,
            new ArrayList<String>(Arrays.asList("SNAC Resource ID")), // previous name(s)
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC ID for a related Resource in SNAC."));
  }
}
