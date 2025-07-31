package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACModelField.FieldOccurence;
import org.snaccooperative.openrefine.model.SNACModelField.FieldRequirement;
import org.snaccooperative.openrefine.model.SNACModelField.FieldVocabulary;
import org.snaccooperative.openrefine.model.SNACModelFieldRelation.FieldRelationType;

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
        new SNACModelField.Builder<RelationModelField>(
                RelationModelField.CPF_TYPE,
                FieldRequirement.REQUIRED,
                FieldOccurence.SINGLE,
                FieldVocabulary.CONTROLLED,
                "Type of CPF entity.")
            .withDependencies(
                new SNACModelFieldRelations<RelationModelField>(
                    new ArrayList<SNACModelFieldRelation<RelationModelField>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<RelationModelField>(
                                RelationModelField.CPF_ID, FieldRelationType.OPTIONAL)))))
            .withSampleValues(
                new ArrayList<String>(Arrays.asList("corporateBody", "person", "family")))
            .build());

    addField(
        new SNACModelField.Builder<RelationModelField>(
                RelationModelField.CPF_ID,
                FieldRequirement.OPTIONAL,
                FieldOccurence.SINGLE,
                FieldVocabulary.IDENTIFIER,
                "SNAC identifier for the CPF entity.  Leave blank if the CPF is NOT in SNAC.")
            .withPreviousNames(new ArrayList<String>(Arrays.asList("SNAC CPF ID")))
            .withDependencies(
                new SNACModelFieldRelations<RelationModelField>(
                    new ArrayList<SNACModelFieldRelation<RelationModelField>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<RelationModelField>(
                                RelationModelField.CPF_TYPE, FieldRelationType.REQUIRED)))))
            .build());

    addField(
        new SNACModelField.Builder<RelationModelField>(
                RelationModelField.CPF_TO_CPF_RELATION_TYPE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "Nature of the relation of the CPF entity with the related CPF entity.")
            .withDependencies(
                new SNACModelFieldRelations<RelationModelField>(
                    new ArrayList<SNACModelFieldRelation<RelationModelField>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<RelationModelField>(
                                RelationModelField.RELATED_CPF_ID, FieldRelationType.REQUIRED)))))
            .withSampleValues(
                new ArrayList<String>(Arrays.asList("associatedWith", "correspondedWith")))
            .build());

    addField(
        new SNACModelField.Builder<RelationModelField>(
                RelationModelField.RELATED_CPF_ID,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.IDENTIFIER,
                "SNAC ID of a CPF entity in SNAC related to the CPF entity.")
            .withPreviousNames(new ArrayList<String>(Arrays.asList("Related SNAC CPF ID")))
            .withDependencies(
                new SNACModelFieldRelations<RelationModelField>(
                    new ArrayList<SNACModelFieldRelation<RelationModelField>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<RelationModelField>(
                                RelationModelField.CPF_TO_CPF_RELATION_TYPE,
                                FieldRelationType.REQUIRED)))))
            .build());

    addField(
        new SNACModelField.Builder<RelationModelField>(
                RelationModelField.CPF_TO_RESOURCE_RELATION_TYPE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "Role of the CPF entity in relation to the Resource.")
            .withDependencies(
                new SNACModelFieldRelations<RelationModelField>(
                    new ArrayList<SNACModelFieldRelation<RelationModelField>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<RelationModelField>(
                                RelationModelField.RELATED_RESOURCE_ID,
                                FieldRelationType.REQUIRED)))))
            .withSampleValues(
                new ArrayList<String>(
                    Arrays.asList("contributorOf", "creatorOf", "editorOf", "referencedIn")))
            .build());

    addField(
        new SNACModelField.Builder<RelationModelField>(
                RelationModelField.RELATED_RESOURCE_ID,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.IDENTIFIER,
                "SNAC ID for a related Resource in SNAC.")
            .withPreviousNames(new ArrayList<String>(Arrays.asList("SNAC Resource ID")))
            .withDependencies(
                new SNACModelFieldRelations<RelationModelField>(
                    new ArrayList<SNACModelFieldRelation<RelationModelField>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<RelationModelField>(
                                RelationModelField.CPF_TO_RESOURCE_RELATION_TYPE,
                                FieldRelationType.REQUIRED)))))
            .build());
  }
}
