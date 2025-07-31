package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACModelField.FieldOccurence;
import org.snaccooperative.openrefine.model.SNACModelField.FieldRequirement;
import org.snaccooperative.openrefine.model.SNACModelField.FieldVocabulary;
import org.snaccooperative.openrefine.model.SNACModelFieldRelation.FieldRelationType;

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
        new SNACModelField.Builder<ResourceModelField>(
                ResourceModelField.RESOURCE_TYPE,
                FieldRequirement.REQUIRED,
                FieldOccurence.SINGLE,
                FieldVocabulary.CONTROLLED,
                "Type of the Resource Description.")
            .withSampleValues(
                new ArrayList<String>(
                    Arrays.asList(
                        "ArchivalResource",
                        "BibliographicResource",
                        "DigitalArchivalResource",
                        "OralHistoryResource")))
            .build());

    addField(
        new SNACModelField.Builder<ResourceModelField>(
                ResourceModelField.RESOURCE_ID,
                FieldRequirement.OPTIONAL,
                FieldOccurence.SINGLE,
                FieldVocabulary.IDENTIFIER,
                "SNAC identifier for Resource Description.  Leave blank if Resource Description is NOT in SNAC.")
            .withPreviousNames(new ArrayList<String>(Arrays.asList("SNAC Resource ID")))
            .build());

    addField(
        new SNACModelField.Builder<ResourceModelField>(
                ResourceModelField.TITLE,
                FieldRequirement.REQUIRED,
                FieldOccurence.SINGLE,
                FieldVocabulary.FREETEXT,
                "Title of a resource that may or may not include dates (e.g. Jacob Miller Papers, 1809-1882).")
            .build());

    addField(
        new SNACModelField.Builder<ResourceModelField>(
                ResourceModelField.RESOURCE_URL,
                FieldRequirement.REQUIRED,
                FieldOccurence.SINGLE,
                FieldVocabulary.FREETEXT,
                "URL of the local Resource Description.")
            .build());

    addField(
        new SNACModelField.Builder<ResourceModelField>(
                ResourceModelField.HOLDING_REPOSITORY_ID,
                FieldRequirement.REQUIRED,
                FieldOccurence.SINGLE,
                FieldVocabulary.IDENTIFIER,
                "SNAC identifier for the holding repository description.  The holding repository must be created in SNAC before adding Resource Descriptions.")
            .withPreviousNames(new ArrayList<String>(Arrays.asList("Holding Repository SNAC ID")))
            .build());

    addField(
        new SNACModelField.Builder<ResourceModelField>(
                ResourceModelField.ABSTRACT,
                FieldRequirement.OPTIONAL,
                FieldOccurence.SINGLE,
                FieldVocabulary.FREETEXT,
                "Brief prose abstract of scope and contents of the resource.")
            .build());

    addField(
        new SNACModelField.Builder<ResourceModelField>(
                ResourceModelField.EXTENT,
                FieldRequirement.OPTIONAL,
                FieldOccurence.SINGLE,
                FieldVocabulary.FREETEXT,
                "Extent of the resource.")
            .build());

    addField(
        new SNACModelField.Builder<ResourceModelField>(
                ResourceModelField.DATE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.SINGLE,
                FieldVocabulary.FREETEXT,
                "Date or dates of the resource (YYYY or YYYY-YYYY).")
            .build());

    addField(
        new SNACModelField.Builder<ResourceModelField>(
                ResourceModelField.LANGUAGE_CODE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "ISO 639 Language Code.")
            .withDependencies(
                new SNACModelFieldRelations<ResourceModelField>(
                    new ArrayList<SNACModelFieldRelation<ResourceModelField>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ResourceModelField>(
                                ResourceModelField.SCRIPT_CODE, FieldRelationType.OPTIONAL)))))
            .withSampleValues(new ArrayList<String>(Arrays.asList("eng", "ger", "jpn")))
            .build());

    addField(
        new SNACModelField.Builder<ResourceModelField>(
                ResourceModelField.SCRIPT_CODE,
                FieldRequirement.OPTIONAL,
                FieldOccurence.MULTIPLE,
                FieldVocabulary.CONTROLLED,
                "ISO 15924 Script Code.")
            .withDependencies(
                new SNACModelFieldRelations<ResourceModelField>(
                    new ArrayList<SNACModelFieldRelation<ResourceModelField>>(
                        Arrays.asList(
                            new SNACModelFieldRelation<ResourceModelField>(
                                ResourceModelField.LANGUAGE_CODE, FieldRelationType.OPTIONAL)))))
            .withSampleValues(new ArrayList<String>(Arrays.asList("Latn", "Cyrl", "Grek")))
            .build());
  }
}
