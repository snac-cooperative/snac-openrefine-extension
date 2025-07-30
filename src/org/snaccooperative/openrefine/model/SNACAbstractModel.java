package org.snaccooperative.openrefine.model;

import com.google.refine.model.Row;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACAbstractModel.ModelType;
import org.snaccooperative.openrefine.schema.SNACSchema;
import org.snaccooperative.openrefine.schema.SNACSchemaUtilities;

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
  private ArrayList<SNACModelField> _fieldList;
  private HashMap<E, SNACModelField> _fieldMap;
  private E _defaultFieldType;

  static final Logger logger = LoggerFactory.getLogger("SNACAbstractModel");

  public SNACAbstractModel(ModelType type, E defaultFieldType) {
    this._type = type;
    this._defaultFieldType = defaultFieldType;

    this._fieldList = new ArrayList<SNACModelField>();
    this._fieldMap = new HashMap<E, SNACModelField>();
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

  // field processing helpers

  private String depError(
      String fieldName,
      String fieldColumn,
      String requiredField,
      String requiredColumn,
      String depType,
      String errMsg) {
    String fieldFull = fieldName + " (column " + fieldColumn + ")";

    String reqFull = requiredField;
    if (requiredColumn != null) {
      reqFull += " (column " + requiredColumn + ")";
    }

    String errFull = "";
    errFull += "Field " + reqFull;
    errFull += ", a required " + depType + " of " + fieldFull;
    errFull += ", is " + errMsg;

    return errFull;
  }

  public Boolean hasRequiredFieldsInRow(
      E fieldType,
      String fieldValue,
      String fieldColumn,
      Row row,
      SNACSchema schema,
      SNACSchemaUtilities schemaUtils,
      List<String> validationErrors) {
    Boolean retval = true;

    for (int i = 0; i < getModelField(fieldType).getRequiredDependenciesFieldTypes().size(); i++) {

      @SuppressWarnings("unchecked")
      E requiredField = (E) getModelField(fieldType).getRequiredDependenciesFieldTypes().get(i);

      String requiredColumn = getEntryForFieldType(requiredField, schema.getColumnMappings());

      if (requiredColumn == null) {
        validationErrors.add(
            depError(
                fieldType.getName(),
                fieldColumn,
                requiredField.getName(),
                requiredColumn,
                "dependency",
                "not present in SNAC schema"));
        retval = false;
        continue;
      }

      String requiredValue = schemaUtils.getCellValueForRowByColumnName(row, requiredColumn);

      if (requiredValue.equals("")) {
        validationErrors.add(
            depError(
                fieldType.getName(),
                fieldColumn,
                requiredField.getName(),
                requiredColumn,
                "dependency",
                "empty for row with value \"" + fieldValue + "\""));
        retval = false;
        continue;
      }
    }

    for (int i = 0; i < getModelField(fieldType).getRequiredDependentsFieldTypes().size(); i++) {

      @SuppressWarnings("unchecked")
      E requiredField = (E) getModelField(fieldType).getRequiredDependentsFieldTypes().get(i);

      String requiredColumn = getEntryForFieldType(requiredField, schema.getColumnMappings());

      if (requiredColumn == null) {
        validationErrors.add(
            depError(
                fieldType.getName(),
                fieldColumn,
                requiredField.getName(),
                requiredColumn,
                "dependent",
                "not present in SNAC schema"));
        retval = false;
        continue;
      }

      String requiredValue = schemaUtils.getCellValueForRowByColumnName(row, requiredColumn);

      if (requiredValue.equals("")) {
        validationErrors.add(
            depError(
                fieldType.getName(),
                fieldColumn,
                requiredField.getName(),
                requiredColumn,
                "dependent",
                "empty for row with value \"" + fieldValue + "\""));
        retval = false;
        continue;
      }
    }

    return retval;
  }
}
