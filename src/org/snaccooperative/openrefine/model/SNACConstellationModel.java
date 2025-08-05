package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACModelField.FieldOccurence;
import org.snaccooperative.openrefine.model.SNACModelField.FieldRequirement;
import org.snaccooperative.openrefine.model.SNACModelField.FieldVocabulary;
import org.snaccooperative.openrefine.model.SNACModelFieldRelation.FieldRelationType;

public class SNACConstellationModel
    extends SNACAbstractModel<SNACConstellationModel.ConstellationFieldType> {

  public enum ConstellationFieldType implements SNACModelFieldType {
    NONE,
    CPF_TYPE("CPF Type"),
    CPF_ID("CPF ID"),
    NAME_ENTRY("Name Entry"),
    VARIANT_NAME_ENTRY("Variant Name Entry"),
    EXIST_DATE("Exist Date"),
    EXIST_DATE_TYPE("Exist Date Type"),
    EXIST_DATE_DESCRIPTIVE_NOTE("Exist Date Descriptive Note"),
    SUBJECT("Subject"),
    PLACE("Place"),
    PLACE_ROLE("Place Role"),
    PLACE_TYPE("Place Type"),
    OCCUPATION("Occupation"),
    ACTIVITY("Activity"),
    LANGUAGE_CODE("Language Code"),
    SCRIPT_CODE("Script Code"),
    BIOG_HIST("BiogHist"),
    EXTERNAL_RELATED_CPF_URL("External Related CPF URL"),
    SOURCE_CITATION("Source Citation"),
    SOURCE_CITATION_URL("Source Citation URL"),
    SOURCE_CITATION_FOUND_DATA("Source Citation Found Data");

    private final String _name;

    ConstellationFieldType() {
      this("");
    }

    ConstellationFieldType(String name) {
      this._name = name;
    }

    public String getName() {
      return this._name;
    }
  }

  static final Logger logger = LoggerFactory.getLogger(SNACConstellationModel.class);

  public SNACConstellationModel() {
    super(ModelType.CONSTELLATION, ConstellationFieldType.NONE);

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.CPF_TYPE,
                FieldRequirement.REQUIRED,
                FieldOccurence.SINGLE,
                FieldVocabulary.CONTROLLED,
                "Type of CPF entity.")
            .withDependencies(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.CPF_ID, FieldRelationType.OPTIONAL)))))
            .withSampleValues(
                new ArrayList<String>(Arrays.asList("corporateBody", "person", "family")))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.CPF_ID,
                FieldRequirement.OPTIONAL,
                FieldOccurence.SINGLE,
                FieldVocabulary.IDENTIFIER,
                "SNAC identifier for the CPF entity.  Leave blank if the CPF is NOT in SNAC.")
            .withPreviousNames(new ArrayList<String>(Arrays.asList("SNAC CPF ID")))
            .withDependencies(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.CPF_TYPE, FieldRelationType.REQUIRED)))))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.NAME_ENTRY,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.FREETEXT,
                "Preferred Name Entry of the CPF entity.")
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.VARIANT_NAME_ENTRY,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.FREETEXT,
                "Variant Name Entry of the CPF entity.")
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.EXIST_DATE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.FREETEXT,
                "Exist Date or Dates of the CPF entity.")
            .withDependents(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.EXIST_DATE_TYPE, FieldRelationType.REQUIRED),
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.EXIST_DATE_DESCRIPTIVE_NOTE,
                                FieldRelationType.OPTIONAL)))))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.EXIST_DATE_TYPE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "Type of Exist Date.")
            .withDependencies(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.EXIST_DATE, FieldRelationType.REQUIRED)))))
            .withSampleValues(
                new ArrayList<String>(
                    Arrays.asList("Active", "Birth", "Death", "Establishment", "Disestablishment")))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.EXIST_DATE_DESCRIPTIVE_NOTE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.FREETEXT,
                "Descriptive Note of Exist Date.")
            .withDependencies(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.EXIST_DATE, FieldRelationType.REQUIRED)))))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.SUBJECT,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "Subject term associated with the CPF entity.")
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.PLACE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.FREETEXT,
                "Place name associated with the CPF entity.")
            .withDependents(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.PLACE_ROLE, FieldRelationType.OPTIONAL),
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.PLACE_TYPE, FieldRelationType.OPTIONAL)))))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.PLACE_ROLE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "Role of the place in relation to the CPF entity.")
            .withDependencies(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.PLACE, FieldRelationType.REQUIRED)))))
            .withSampleValues(
                new ArrayList<String>(
                    Arrays.asList("Birth", "Death", "Residence", "Citizenship", "Work")))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.PLACE_TYPE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "Type of the place in relation to the CPF entity.")
            .withDependencies(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.PLACE, FieldRelationType.REQUIRED)))))
            .withSampleValues(new ArrayList<String>(Arrays.asList("AssociatedPlace", "Address")))
            .withDefaultValue("AssociatedPlace")
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.OCCUPATION,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "Occupation term associated with the CPF entity.")
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.ACTIVITY,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "Activity term associated with the CPF entity.")
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.LANGUAGE_CODE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "ISO 639 Language Code.")
            .withDependencies(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.SCRIPT_CODE, FieldRelationType.OPTIONAL)))))
            .withSampleValues(new ArrayList<String>(Arrays.asList("eng", "ger", "jpn")))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.SCRIPT_CODE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "ISO 15924 Script Code.")
            .withDependencies(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.LANGUAGE_CODE,
                                FieldRelationType.OPTIONAL)))))
            .withSampleValues(new ArrayList<String>(Arrays.asList("Latn", "Cyrl", "Grek")))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.BIOG_HIST,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.FREETEXT,
                "Biography or History note associated with the CPF entity.  By exception, the note is encoded in XML based on a simplified version of the <biogHist> element in EAC-CPF.")
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.EXTERNAL_RELATED_CPF_URL,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.FREETEXT,
                "URL to a description of the CPF entity in an external authority.  Such links are limited to those found in the approved list linked above.")
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.SOURCE_CITATION,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.FREETEXT,
                "Text citation for a source used in describing the CPF entity.")
            .withDependents(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.SOURCE_CITATION_URL,
                                FieldRelationType.OPTIONAL),
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.SOURCE_CITATION_FOUND_DATA,
                                FieldRelationType.OPTIONAL)))))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.SOURCE_CITATION_URL,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.FREETEXT,
                "URL, if available, for the Source Citation.")
            .withDependencies(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.SOURCE_CITATION,
                                FieldRelationType.REQUIRED)))))
            .build());

    addField(
        new SNACModelField.Builder<ConstellationFieldType>(
                ConstellationFieldType.SOURCE_CITATION_FOUND_DATA,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.FREETEXT,
                "Information found in the Source that is evidence used in the description of the CPF entity.")
            .withDependencies(
                new SNACModelFieldRelations<ConstellationFieldType>(
                    new ArrayList<SNACModelFieldRelation<ConstellationFieldType>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ConstellationFieldType>(
                                ConstellationFieldType.SOURCE_CITATION,
                                FieldRelationType.REQUIRED)))))
            .build());
  }
}
