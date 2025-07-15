package org.snaccooperative.model;

import java.util.ArrayList;
import java.util.HashMap;
import org.apache.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNACAbstractModel<E extends Enum<E>> {

  private String _type;
  protected ArrayList<SNACModelField> _fieldList;
  protected HashMap<E, SNACModelField> _fieldMap;
  protected E _defaultFieldType;

  static final Logger logger = LoggerFactory.getLogger("SNACAbstractModel");

  public SNACAbstractModel(String type, E defaultFieldType) {
    _type = type;
    _defaultFieldType = defaultFieldType;

    _fieldList = new ArrayList<SNACModelField>();
    _fieldMap = new HashMap<E, SNACModelField>();
  }

  protected void addField(E fieldType, SNACModelField field) {
    _fieldList.add(field);
    _fieldMap.put(fieldType, field);
  }

  public String getType() {
    return _type;
  }

  public ArrayList<SNACModelField> getFields() {
    return _fieldList;
  }

  public String getFieldName(E fieldType) {
    return _fieldMap.get(fieldType).getName();
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

  public Boolean isNameForFieldType(E fieldType, String s) {
    return _fieldMap.get(fieldType).isNameForField(s);
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
