package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import java.util.HashMap;
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

  private ModelType _type;
  protected ArrayList<SNACModelField> _fieldList;
  protected HashMap<E, SNACModelField> _fieldMap;
  protected E _defaultFieldType;

  static final Logger logger = LoggerFactory.getLogger("SNACAbstractModel");

  public SNACAbstractModel(ModelType type, E defaultFieldType) {
    _type = type;
    _defaultFieldType = defaultFieldType;

    _fieldList = new ArrayList<SNACModelField>();
    _fieldMap = new HashMap<E, SNACModelField>();
  }

  protected void addField(SNACModelField<E> field) {
    _fieldList.add(field);
    _fieldMap.put(field.getFieldType(), field);
  }

  public ModelType getType() {
    return _type;
  }

  public ArrayList<SNACModelField> getFields() {
    return _fieldList;
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

  public SNACModelField getModelField(E fieldType) {
    SNACModelField modelField = _fieldMap.get(fieldType);

    if (modelField == null) {
      return null;
    }

    return modelField;
  }

  public SNACModelField getModelField(String s) {
    return getModelField(getFieldType(s));
  }

  public String getFieldName(E fieldType) {
    SNACModelField modelField = _fieldMap.get(fieldType);

    if (modelField == null) {
      return null;
    }

    return modelField.getName();
  }

  public Boolean isNameForFieldType(E fieldType, String s) {
    SNACModelField modelField = _fieldMap.get(fieldType);

    if (modelField == null) {
      return null;
    }

    return modelField.isKnownName(s);
  }

  public String getEntryForFieldType(E fieldType, HashMap<String, String> list) {
    for (String key : list.keySet()) {
      String entry = list.get(key);
      if (isNameForFieldType(fieldType, entry)) {
        return key;
      }
    }

    return null;
  }
}
