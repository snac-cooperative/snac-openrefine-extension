package org.snaccooperative.openrefine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({"name", "required", "repeatable", "controlled", "tooltip"})
public class SNACModelField {

  static final Logger logger = LoggerFactory.getLogger("SNACModelField");

  public enum FieldRequirement {
    REQUIRED,
    OPTIONAL
  };

  public enum FieldOccurence {
    SINGLE,
    MULTIPLE
  };

  public enum FieldVocabulary {
    CONTROLLED,
    FREETEXT,
    IDENTIFIER
  };

  // TODO: field dependencies:
  //
  // Some fields have a required or optional dependency on other fields being mapped. (*)
  // Currently these are conveyed through the field tooltips, but this can be improved:
  //   * we can have a separate "dependencies" text description field (defined here)
  //   * and/or a FieldDependencies enum to specify whether a field is dependent on other(s)
  // Then the front end can use this info for better tooltips/visual dependency indication/etc.
  //
  // (*) some examples:
  //  + "Exist Date Type" and "Exist Date Descriptive Note" depend on "Exist Date" being mapped
  //  + "Language Code" has an optional dependency on "Script Code", and vice versa

  private String _name;
  // names this field was formerly known as, for backwards compatibility
  private ArrayList<String> _previousNames;
  private FieldRequirement _requirement;
  private FieldOccurence _occurence;
  private FieldVocabulary _vocabulary;
  private String _tooltip;

  public SNACModelField(
      String name,
      FieldRequirement requirement,
      FieldOccurence occurence,
      FieldVocabulary vocabulary,
      String tooltip) {
    this(name, null, requirement, occurence, vocabulary, tooltip);
  }

  public SNACModelField(
      String name,
      ArrayList<String> previousNames,
      FieldRequirement requirement,
      FieldOccurence occurence,
      FieldVocabulary vocabulary,
      String tooltip) {
    _name = name;
    _previousNames = previousNames;
    _requirement = requirement;
    _occurence = occurence;
    _vocabulary = vocabulary;
    _tooltip = tooltip;

    if (_previousNames == null) {
      _previousNames = new ArrayList<String>();
    }
  }

  @JsonProperty("name")
  public String getName() {
    return _name;
  }

  @JsonProperty("required")
  public Boolean isRequired() {
    return (_requirement == FieldRequirement.REQUIRED);
  }

  @JsonProperty("repeatable")
  public Boolean isRepeatable() {
    return (_occurence == FieldOccurence.MULTIPLE);
  }

  @JsonProperty("controlled")
  public Boolean isControlled() {
    return (_vocabulary == FieldVocabulary.CONTROLLED);
  }

  @JsonProperty("tooltip")
  public String getTooltip() {
    return _tooltip;
  }

  private Boolean stringsAreEqual(String a, String b) {
    return a.toLowerCase().equals(b.toLowerCase());
  }

  public Boolean isNameForField(String s) {
    if (stringsAreEqual(s, _name)) {
      return true;
    }

    for (int i = 0; i < _previousNames.size(); i++) {
      if (stringsAreEqual(s, _previousNames.get(i))) {
        return true;
      }
    }

    return false;
  }
}
