package org.snaccooperative.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.snaccooperative.model.SNACModelField;
import org.snaccooperative.model.SNACModelField.FieldRequirement;
import org.snaccooperative.model.SNACModelField.FieldOccurence;
import org.snaccooperative.model.SNACModelField.FieldVocabulary;

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
