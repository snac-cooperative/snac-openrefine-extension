package org.snaccooperative.openrefine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({"field", "relation"})
public class SNACModelFieldRelation<E extends Enum<E> & SNACModelFieldType> {

  public enum FieldRelationType {
    NONE,
    REQUIRED("required"),
    OPTIONAL("optional");

    private final String _type;

    FieldRelationType() {
      this("");
    }

    FieldRelationType(String type) {
      this._type = type;
    }

    public String getType() {
      return _type;
    }
  }

  private E _fieldType;
  private FieldRelationType _relationType;

  static final Logger logger = LoggerFactory.getLogger("SNACModelFieldRelation");

  public SNACModelFieldRelation(E fieldType, FieldRelationType relationType) {
    _fieldType = fieldType;
    _relationType = relationType;
  }

  @JsonIgnore
  public E getFieldType() {
    return _fieldType;
  }

  @JsonProperty("field")
  public String getFieldName() {
    return getFieldType().getName();
  }

  @JsonIgnore
  public FieldRelationType getRelationType() {
    return _relationType;
  }

  @JsonProperty("relation")
  public String getRelationName() {
    return getRelationType().getType();
  }
}
