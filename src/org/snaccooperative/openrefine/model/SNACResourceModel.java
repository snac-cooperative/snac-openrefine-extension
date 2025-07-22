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

  public enum ResourceModelField implements SNACModelFieldType {
    NONE,
    RESOURCE_TYPE("Resource Type"),
    RESOURCE_ID("Resource ID"),
    TITLE("Title"),
    RESOURCE_URL("Resource URL"),
    HOLDING_REPOSITORY_ID("Holding Repository ID"),
    ABSTRACT("Abstract"),
    EXTENT("Extent"),
    DATE("Date"),
    LANGUAGE_CODE("Language Code"),
    SCRIPT_CODE("Script Code");

    private final String _name;

    ResourceModelField() {
      this("");
    }

    ResourceModelField(String name) {
      this._name = name;
    }

    public String getName() {
      return this._name;
    }
  }

  static final Logger logger = LoggerFactory.getLogger("SNACResourceModel");

  public SNACResourceModel() {
    super(ModelType.RESOURCE, ResourceModelField.NONE);

    addField(
        new SNACModelField<ResourceModelField>(
            ResourceModelField.RESOURCE_TYPE,
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.CONTROLLED,
            "Resource Type may have the following values: ArchivalResource, BibliographicResource, DigitalArchivalResource, OralHistoryResource"));

    addField(
        new SNACModelField<ResourceModelField>(
            ResourceModelField.RESOURCE_ID,
            new ArrayList<String>(Arrays.asList("SNAC Resource ID")), // previous name(s)
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC identifier for Resource Description.  Leave blank if Resource Description is NOT in SNAC."));

    addField(
        new SNACModelField<ResourceModelField>(
            ResourceModelField.TITLE,
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.FREETEXT,
            "Title of a resource that may or may not include dates (e.g. Jacob Miller Papers, 1809-1882)."));

    addField(
        new SNACModelField<ResourceModelField>(
            ResourceModelField.RESOURCE_URL,
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.FREETEXT,
            "URL of the local Resource Description"));

    addField(
        new SNACModelField<ResourceModelField>(
            ResourceModelField.HOLDING_REPOSITORY_ID,
            new ArrayList<String>(Arrays.asList("Holding Repository SNAC ID")), // previous name(s)
            FieldRequirement.REQUIRED,
            FieldOccurence.SINGLE,
            FieldVocabulary.IDENTIFIER,
            "SNAC identifier for the holding repository description.  The holding repository must be created in SNAC before adding Resource Descriptions."));

    addField(
        new SNACModelField<ResourceModelField>(
            ResourceModelField.ABSTRACT,
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.FREETEXT,
            "Brief prose abstract of scope and contents of the resource."));

    addField(
        new SNACModelField<ResourceModelField>(
            ResourceModelField.EXTENT,
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.FREETEXT,
            "Extent of the resource."));

    addField(
        new SNACModelField<ResourceModelField>(
            ResourceModelField.DATE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.SINGLE,
            FieldVocabulary.FREETEXT,
            "Date or dates of the resource (YYYY or YYYY-YYYY)"));

    addField(
        new SNACModelField<ResourceModelField>(
            ResourceModelField.LANGUAGE_CODE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "ISO 639 Language Code, e.g. 'eng', 'ger', 'jpn'.  Combinable with a Script Code in the same row."));

    addField(
        new SNACModelField<ResourceModelField>(
            ResourceModelField.SCRIPT_CODE,
            FieldRequirement.OPTIONAL,
            FieldOccurence.MULTIPLE,
            FieldVocabulary.CONTROLLED,
            "ISO 15924 Script Code, e.g. 'Latn', 'Cyrl', 'Grek'.  Combinable with a Language Code in the same row."));
  }
}
