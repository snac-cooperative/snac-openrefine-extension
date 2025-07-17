package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACAbstractModel.ModelType;
import org.snaccooperative.openrefine.model.SNACModelField.FieldOccurence;
import org.snaccooperative.openrefine.model.SNACModelField.FieldRequirement;
import org.snaccooperative.openrefine.model.SNACModelField.FieldVocabulary;

public class SNACResourceModel extends SNACAbstractModel<SNACResourceModel.ResourceModelField> {

  public enum ResourceModelField {
    NONE,
    RESOURCE_TYPE,
    RESOURCE_ID,
    TITLE,
    RESOURCE_URL,
    HOLDING_REPOSITORY_ID,
    ABSTRACT,
    EXTENT,
    DATE,
    LANGUAGE_CODE,
    SCRIPT_CODE
  }

  static final Logger logger = LoggerFactory.getLogger("SNACResourceModel");

  public SNACResourceModel() {
    super(ModelType.RESOURCE, ResourceModelField.NONE);

    addField(
        ResourceModelField.RESOURCE_TYPE,
        new SNACModelField(
            "Resource Type",
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.CONTROLLED,
            "Resource Type may have the following values: ArchivalResource, BibliographicResource, DigitalArchivalResource, OralHistoryResource"));

    addField(
        ResourceModelField.RESOURCE_ID,
        new SNACModelField(
            "Resource ID",
            new ArrayList<String>(Arrays.asList("SNAC Resource ID")), // previous name(s)
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC identifier for Resource Description.  Leave blank if Resource Description is NOT in SNAC."));

    addField(
        ResourceModelField.TITLE,
        new SNACModelField(
            "Title",
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.FREETEXT,
            "Title of a resource that may or may not include dates (e.g. Jacob Miller Papers, 1809-1882)."));

    addField(
        ResourceModelField.RESOURCE_URL,
        new SNACModelField(
            "Resource URL",
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.FREETEXT,
            "URL of the local Resource Description"));

    addField(
        ResourceModelField.HOLDING_REPOSITORY_ID,
        new SNACModelField(
            "Holding Repository ID",
            new ArrayList<String>(Arrays.asList("Holding Repository SNAC ID")), // previous name(s)
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC identifier for the holding repository description.  The holding repository must be created in SNAC before adding Resource Descriptions."));

    addField(
        ResourceModelField.ABSTRACT,
        new SNACModelField(
            "Abstract",
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.FREETEXT,
            "Brief prose abstract of scope and contents of the resource."));

    addField(
        ResourceModelField.EXTENT,
        new SNACModelField(
            "Extent",
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.FREETEXT,
            "Extent of the resource."));

    addField(
        ResourceModelField.DATE,
        new SNACModelField(
            "Date",
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.FREETEXT,
            "Date or dates of the resource (YYYY or YYYY-YYYY)"));

    addField(
        ResourceModelField.LANGUAGE_CODE,
        new SNACModelField(
            "Language Code",
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "ISO 639 Language Code, e.g. 'eng', 'ger', 'jpn'.  Combinable with a Script Code in the same row."));

    addField(
        ResourceModelField.SCRIPT_CODE,
        new SNACModelField(
            "Script Code",
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "ISO 15924 Script Code, e.g. 'Latn', 'Cyrl', 'Grek'.  Combinable with a Language Code in the same row."));
  }
}
