package org.snaccooperative.openrefine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({
  "name",
  "required",
  "requirement",
  "repeatable",
  "occurence",
  "controlled",
  "vocabulary",
  "dependencies",
  "dependents",
  "sample_values",
  "default_value",
  "tooltip"
})
public class SNACModelField<E extends Enum<E> & SNACModelFieldType> {

  static final Logger logger = LoggerFactory.getLogger(SNACModelField.class);

  public enum FieldRequirement implements SNACModelFieldType {
    REQUIRED("Required"),
    OPTIONAL("Optional");

    private final String _name;

    FieldRequirement() {
      this("");
    }

    FieldRequirement(String name) {
      this._name = name;
    }

    public String getName() {
      return this._name;
    }
  };

  public enum FieldOccurence implements SNACModelFieldType {
    SINGLE("One per record"),
    MULTIPLE("Multiple per record");

    private final String _name;

    FieldOccurence() {
      this("");
    }

    FieldOccurence(String name) {
      this._name = name;
    }

    public String getName() {
      return this._name;
    }
  };

  public enum FieldVocabulary implements SNACModelFieldType {
    CONTROLLED("SNAC Controlled Vocabulary"),
    FREETEXT("Free Text"),
    IDENTIFIER("Numeric SNAC Identifier");

    private final String _name;

    FieldVocabulary() {
      this("");
    }

    FieldVocabulary(String name) {
      this._name = name;
    }

    public String getName() {
      return this._name;
    }
  };

  private final E _fieldType;
  private final FieldRequirement _requirement;
  private final FieldOccurence _occurence;
  private final FieldVocabulary _vocabulary;
  private final List<String> _previousNames;
  private final List<String> _sampleValues;
  private final String _defaultValue;
  private final String _tooltip;
  private final SNACModelFieldRelations<E> _dependencies;
  private final SNACModelFieldRelations<E> _dependents;

  private SNACModelField(Builder<E> builder) {
    _fieldType = builder._fieldType;
    _requirement = builder._requirement;
    _occurence = builder._occurence;
    _vocabulary = builder._vocabulary;
    _tooltip = builder._tooltip;
    _previousNames = builder._previousNames;
    _sampleValues = builder._sampleValues;
    _defaultValue = builder._defaultValue;
    _dependencies = builder._dependencies;
    _dependents = builder._dependents;
  }

  public static class Builder<E extends Enum<E> & SNACModelFieldType> {
    // required fields
    private final E _fieldType;
    private final FieldRequirement _requirement;
    private final FieldOccurence _occurence;
    private final FieldVocabulary _vocabulary;
    private final String _tooltip;
    // optional fields
    private List<String> _previousNames = new ArrayList<String>();
    private List<String> _sampleValues = new ArrayList<String>();
    private String _defaultValue = "";
    private SNACModelFieldRelations<E> _dependencies = new SNACModelFieldRelations<E>();
    private SNACModelFieldRelations<E> _dependents = new SNACModelFieldRelations<E>();

    public Builder(
        E fieldType,
        FieldRequirement requirement,
        FieldOccurence occurence,
        FieldVocabulary vocabulary,
        String tooltip) {
      this._fieldType = fieldType;
      this._requirement = requirement;
      this._occurence = occurence;
      this._vocabulary = vocabulary;

      if (tooltip != null) {
        this._tooltip = tooltip;
      } else {
        this._tooltip = "";
      }
    }

    public Builder<E> withPreviousNames(List<String> previousNames) {
      if (previousNames != null) {
        this._previousNames = previousNames;
      }
      return this;
    }

    public Builder<E> withSampleValues(List<String> sampleValues) {
      if (sampleValues != null) {
        this._sampleValues = sampleValues;
      }
      return this;
    }

    public Builder<E> withDefaultValue(String defaultValue) {
      if (defaultValue != null) {
        this._defaultValue = defaultValue;
      }
      return this;
    }

    public Builder<E> withDependencies(SNACModelFieldRelations<E> dependencies) {
      if (dependencies != null) {
        this._dependencies = dependencies;
      }
      return this;
    }

    public Builder<E> withDependents(SNACModelFieldRelations<E> dependents) {
      if (dependents != null) {
        this._dependents = dependents;
      }
      return this;
    }

    public SNACModelField<E> build() {
      return new SNACModelField<E>(this);
    }
  }

  @JsonProperty("name")
  public String getName() {
    return _fieldType.getName();
  }

  @JsonProperty("required")
  public Boolean isRequired() {
    return (_requirement == FieldRequirement.REQUIRED);
  }

  @JsonProperty("requirement")
  public String getRequirement() {
    return _requirement.getName();
  }

  @JsonIgnore
  public FieldRequirement getRequirementType() {
    return _requirement;
  }

  @JsonProperty("repeatable")
  public Boolean isRepeatable() {
    return (_occurence == FieldOccurence.MULTIPLE);
  }

  @JsonProperty("occurence")
  public String getOccurence() {
    return _occurence.getName();
  }

  @JsonIgnore
  public FieldOccurence getOccurenceType() {
    return _occurence;
  }

  @JsonProperty("controlled")
  public Boolean isControlled() {
    return (_vocabulary == FieldVocabulary.CONTROLLED);
  }

  @JsonProperty("vocabulary")
  public String getVocabulary() {
    return _vocabulary.getName();
  }

  @JsonIgnore
  public FieldVocabulary getVocabularyType() {
    return _vocabulary;
  }

  @JsonProperty("dependencies")
  public List<SNACModelFieldRelation<E>> getDependencies() {
    return _dependencies.getRelations();
  }

  @JsonIgnore
  public List<SNACModelFieldRelation<E>> getRequiredDependencies() {
    return _dependencies.getRequiredRelations();
  }

  @JsonIgnore
  public List<E> getRequiredDependenciesFieldTypes() {
    return _dependencies.getRequiredRelationFieldTypes();
  }

  @JsonProperty("dependents")
  public List<SNACModelFieldRelation<E>> getDependents() {
    return _dependents.getRelations();
  }

  @JsonIgnore
  public List<SNACModelFieldRelation<E>> getRequiredDependents() {
    return _dependents.getRequiredRelations();
  }

  @JsonIgnore
  public List<E> getRequiredDependentsFieldTypes() {
    return _dependents.getRequiredRelationFieldTypes();
  }

  @JsonProperty("sample_values")
  public List<String> getSampleValues() {
    return _sampleValues;
  }

  @JsonProperty("default_value")
  public String getDefaultValue() {
    return _defaultValue;
  }

  @JsonProperty("tooltip")
  public String getTooltip() {
    return _tooltip;
  }

  @JsonIgnore
  public E getFieldType() {
    return _fieldType;
  }

  public Boolean isCurrentName(String s) {
    if (s.equalsIgnoreCase(getName())) {
      return true;
    }

    return false;
  }

  public Boolean isPreviousName(String s) {
    for (int i = 0; i < _previousNames.size(); i++) {
      if (s.equalsIgnoreCase(_previousNames.get(i))) {
        return true;
      }
    }

    return false;
  }

  public Boolean isKnownName(String s) {
    if (isCurrentName(s) || isPreviousName(s)) {
      return true;
    }

    return false;
  }
}
