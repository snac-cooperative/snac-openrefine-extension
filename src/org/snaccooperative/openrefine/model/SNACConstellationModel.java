package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACAbstractModel.ModelType;
import org.snaccooperative.openrefine.model.SNACModelField.FieldOccurence;
import org.snaccooperative.openrefine.model.SNACModelField.FieldRequirement;
import org.snaccooperative.openrefine.model.SNACModelField.FieldVocabulary;

public class SNACConstellationModel
    extends SNACAbstractModel<SNACConstellationModel.ConstellationModelField> {

  public enum ConstellationModelField implements SNACModelFieldType {
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

    ConstellationModelField() {
      this("");
    }

    ConstellationModelField(String name) {
      this._name = name;
    }

    public String getName() {
      return this._name;
    }
  }

  static final Logger logger = LoggerFactory.getLogger("SNACConstellationModel");

  public SNACConstellationModel() {
    super(ModelType.CONSTELLATION, ConstellationModelField.NONE);

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.CPF_TYPE,
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.CONTROLLED,
            "Type of CPF entity.  Possible values are: corporateBody, person, family"));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.CPF_ID,
            new ArrayList<String>(Arrays.asList("SNAC CPF ID")), // previous name(s)
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC identifier for the CPF entity.  Leave blank if the CPF is NOT in SNAC."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.NAME_ENTRY,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.FREETEXT,
            "Preferred Name Entry of the CPF entity."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.VARIANT_NAME_ENTRY,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.FREETEXT,
            "Variant Name Entry of the CPF entity."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.EXIST_DATE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.FREETEXT,
            "Exist Date or Dates of the CPF entity."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.EXIST_DATE_TYPE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "Type of Exist Date. The following values may be used: Active, Birth, Death, Establishment, Disestablishment.  Only used if Exist Date field is defined in the same row, in which case it is required."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.EXIST_DATE_DESCRIPTIVE_NOTE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.FREETEXT,
            "Descriptive Note of Exist Date.  Only used if Exist Date field is defined in the same row."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.SUBJECT,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "Subject term associated with the CPF entity."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.PLACE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.FREETEXT,
            "Place name associated with the CPF entity."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.PLACE_ROLE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "Role of the place in relation to the CPF entity.  The following values may be used: Birth, Death, Residence, Citizenship, Work.  Only used if Place field is defined in the same row."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.PLACE_TYPE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "Type of the place in relation to the CPF entity.  The following values may be used: AssociatedPlace, Address.  Defaults to AssociatedPlace if not supplied.  Only used if Place field is defined in the same row."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.OCCUPATION,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "Occupation term associated with the CPF entity."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.ACTIVITY,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "Activity term associated with the CPF entity."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.LANGUAGE_CODE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "ISO 639 Language Code, e.g. 'eng', 'ger', 'jpn'.  Combinable with a Script Code in the same row."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.SCRIPT_CODE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "ISO 15924 Script Code, e.g. 'Latn', 'Cyrl', 'Grek'.  Combinable with a Script Code in the same row."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.BIOG_HIST,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.FREETEXT,
            "Biography or History note associated with the CPF entity.  By exception, the note is encoded in XML based on a simplified version of the <biogHist> element in EAC-CPF."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.EXTERNAL_RELATED_CPF_URL,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.FREETEXT,
            "URL to a description of the CPF entity in an external authority.  Such links are limited to those found in the approved list linked above."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.SOURCE_CITATION,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.FREETEXT,
            "Text citation for a source used in describing the CPF entity."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.SOURCE_CITATION_URL,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.FREETEXT,
            "URL, if available, for the Source Citation.  Only used if Source Citation field is defined in the same row."));

    addField(
        new SNACModelField<ConstellationModelField>(
            ConstellationModelField.SOURCE_CITATION_FOUND_DATA,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.FREETEXT,
            "Information found in the Source that is evidence used in the description of the CPF entity.  Only used if Source Citation field is defined in the same row."));
  }
}
