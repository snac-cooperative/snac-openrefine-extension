package org.snaccooperative.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.snaccooperative.model.SNACAbstractModel;
import org.snaccooperative.model.SNACModelField;
import org.snaccooperative.model.SNACModelField.FieldRequirement;
import org.snaccooperative.model.SNACModelField.FieldOccurence;
import org.snaccooperative.model.SNACModelField.FieldVocabulary;

public class SNACConstellationModel extends SNACAbstractModel<SNACConstellationModel.ConstellationModelField> {

  public enum ConstellationModelField {
    NONE,
    CPF_TYPE,
    CPF_ID,
    NAME_ENTRY,
    VARIANT_NAME_ENTRY,
    EXIST_DATE,
    EXIST_DATE_TYPE,
    EXIST_DATE_DESCRIPTIVE_NOTE,
    SUBJECT,
    PLACE,
    PLACE_ROLE,
    PLACE_TYPE,
    OCCUPATION,
    ACTIVITY,
    LANGUAGE_CODE,
    SCRIPT_CODE,
    BIOG_HIST,
    EXTERNAL_RELATED_CPF_URL,
    SOURCE_CITATION,
    SOURCE_CITATION_URL,
    SOURCE_CITATION_FOUND_DATA
  }

  static final Logger logger = LoggerFactory.getLogger("SNACConstellationModel");

  public SNACConstellationModel() {
    super("constellation", ConstellationModelField.NONE);

    addField(ConstellationModelField.CPF_TYPE, new SNACModelField(
      "CPF Type",
      FieldRequirement.REQUIRED,
      FieldOccurence.SINGLE,
      FieldVocabulary.CONTROLLED,
      "Type of CPF entity.  Possible values are: corporateBody, person, family"
    ));

    addField(ConstellationModelField.CPF_ID, new SNACModelField(
      "SNAC CPF ID", // TODO: when testing, make this just "CPF ID", and possibly keep the change
       FieldRequirement.OPTIONAL,
       FieldOccurence.SINGLE,
       FieldVocabulary.IDENTIFIER,
       "SNAC identifier for the CPF entity.  Leave blank if the CPF is NOT in SNAC."
    ));

    addField(ConstellationModelField.NAME_ENTRY, new SNACModelField(
      "Name Entry",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.FREETEXT,
      "Preferred Name Entry of the CPF entity."
    ));

    addField(ConstellationModelField.VARIANT_NAME_ENTRY, new SNACModelField(
      "Variant Name Entry",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.FREETEXT,
      "Variant Name Entry of the CPF entity."
    ));

    addField(ConstellationModelField.EXIST_DATE, new SNACModelField(
      "Exist Date",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.FREETEXT,
      "Exist Date or Dates of the CPF entity."
    ));

    addField(ConstellationModelField.EXIST_DATE_TYPE, new SNACModelField(
      "Exist Date Type",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.CONTROLLED,
      "Type of Exist Date. The following values may be used: Active, Birth, Death, Establishment, Disestablishment.  Only used if Exist Date field is defined in the same row, in which case it is required."
    ));

    addField(ConstellationModelField.EXIST_DATE_DESCRIPTIVE_NOTE, new SNACModelField(
      "Exist Date Descriptive Note",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.FREETEXT,
      "Descriptive Note of Exist Date.  Only used if Exist Date field is defined in the same row."
    ));

    addField(ConstellationModelField.SUBJECT, new SNACModelField(
      "Subject",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.CONTROLLED,
      "Subject term associated with the CPF entity."
    ));

    addField(ConstellationModelField.PLACE, new SNACModelField(
      "Place",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.FREETEXT,
      "Place name associated with the CPF entity."
    ));

    addField(ConstellationModelField.PLACE_ROLE, new SNACModelField(
      "Place Role",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.CONTROLLED,
      "Role of the place in relation to the CPF entity.  The following values may be used: Birth, Death, Residence, Citizenship, Work.  Only used if Place field is defined in the same row."
    ));

    addField(ConstellationModelField.PLACE_TYPE, new SNACModelField(
      "Place Type",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.CONTROLLED,
      "Type of the place in relation to the CPF entity.  The following values may be used: AssociatedPlace, Address.  Defaults to AssociatedPlace if not supplied.  Only used if Place field is defined in the same row."
    ));

    addField(ConstellationModelField.OCCUPATION, new SNACModelField(
      "Occupation",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.CONTROLLED,
      "Occupation term associated with the CPF entity."
    ));

    addField(ConstellationModelField.ACTIVITY, new SNACModelField(
      "Activity",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.CONTROLLED,
      "Activity term associated with the CPF entity."
    ));

    addField(ConstellationModelField.LANGUAGE_CODE, new SNACModelField(
      "Language Code",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.CONTROLLED,
      "ISO 639 Language Code, e.g. 'eng', 'ger', 'jpn'.  Combinable with a Script Code in the same row."
    ));

    addField(ConstellationModelField.SCRIPT_CODE, new SNACModelField(
      "Script Code",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.CONTROLLED,
      "ISO 15924 Script Code, e.g. 'Latn', 'Cyrl', 'Grek'.  Combinable with a Script Code in the same row."
    ));

    addField(ConstellationModelField.BIOG_HIST, new SNACModelField(
      "BiogHist",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.FREETEXT,
      "Biography or History note associated with the CPF entity.  By exception, the note is encoded in XML based on a simplified version of the <biogHist> element in EAC-CPF."
    ));

    addField(ConstellationModelField.EXTERNAL_RELATED_CPF_URL, new SNACModelField(
      "External Related CPF URL",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.FREETEXT,
      "URL to a description of the CPF entity in an external authority.  Such links are limited to those found in the approved list linked above."
    ));

    addField(ConstellationModelField.SOURCE_CITATION, new SNACModelField(
      "Source Citation",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.FREETEXT,
      "Text citation for a source used in describing the CPF entity."
    ));

    addField(ConstellationModelField.SOURCE_CITATION_URL, new SNACModelField(
      "Source Citation URL",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.FREETEXT,
      "URL, if available, for the Source Citation.  Only used if Source Citation field is defined in the same row."
    ));

    addField(ConstellationModelField.SOURCE_CITATION_FOUND_DATA, new SNACModelField(
      "Source Citation Found Data",
      FieldRequirement.OPTIONAL,
      FieldOccurence.MULTIPLE,
      FieldVocabulary.FREETEXT,
      "Information found in the Source that is evidence used in the description of the CPF entity.  Only used if Source Citation field is defined in the same row."
    ));
  }
}
