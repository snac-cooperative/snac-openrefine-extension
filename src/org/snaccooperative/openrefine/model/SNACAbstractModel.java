package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNACAbstractModel<E extends Enum<E> & SNACModelFieldType> {

  public enum ModelType {
    NONE,
    CONSTELLATION("constellation"),
    RESOURCE("resource"),
    RELATION("relation");

    private final String _type;

    ModelType() {
      this("");
    }

    ModelType(String type) {
      this._type = type;
    }

    public String getType() {
      return _type;
    }

    public static ModelType fromString(String s) {
      for (ModelType type : ModelType.values()) {
        if (type._type.equalsIgnoreCase(s)) {
          return type;
        }
      }

      return null;
    }
  }

  static final Logger logger = LoggerFactory.getLogger(SNACAbstractModel.class);

  private ModelType _type;
  private List<SNACModelField<E>> _fieldList;
  private HashMap<E, SNACModelField<E>> _fieldMap;
  private E _defaultFieldType;

  public SNACAbstractModel(ModelType type, E defaultFieldType) {
    this._type = type;
    this._defaultFieldType = defaultFieldType;

    this._fieldList = new ArrayList<SNACModelField<E>>();
    this._fieldMap = new HashMap<E, SNACModelField<E>>();
  }

  protected void addField(SNACModelField<E> field) {
    _fieldList.add(field);
    _fieldMap.put(field.getFieldType(), field);
  }

  public ModelType getType() {
    return _type;
  }

  public List<SNACModelField<E>> getFields() {
    return _fieldList;
  }

  public List<SNACModelField<E>> getRequiredFields() {
    List<SNACModelField<E>> requiredFields = new ArrayList<SNACModelField<E>>();

    for (int i = 0; i < _fieldList.size(); i++) {
      SNACModelField<E> field = _fieldList.get(i);
      if (field.isRequired()) {
        requiredFields.add(field);
      }
    }

    return requiredFields;
  }

  public E getFieldType(String s) {
    if (s == null || s.equals("")) {
      return _defaultFieldType;
    }

    for (E fieldType : _fieldMap.keySet()) {
      if (isNameForFieldType(fieldType, s)) {
        return fieldType;
      }
    }

    return _defaultFieldType;
  }

  public SNACModelField<E> getModelField(E fieldType) {
    return _fieldMap.get(fieldType);
  }

  public SNACModelField<E> getModelField(String s) {
    return getModelField(getFieldType(s));
  }

  public String getFieldName(E fieldType) {
    SNACModelField<E> modelField = _fieldMap.get(fieldType);

    if (modelField == null) {
      return null;
    }

    return modelField.getName();
  }

  public Boolean isNameForFieldType(E fieldType, String s) {
    SNACModelField<E> modelField = _fieldMap.get(fieldType);

    if (modelField == null) {
      return null;
    }

    return modelField.isKnownName(s);
  }
}
