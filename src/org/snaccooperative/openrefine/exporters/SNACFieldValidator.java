package org.snaccooperative.openrefine.exporters;

import com.google.refine.model.Row;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACAbstractModel;
import org.snaccooperative.openrefine.model.SNACModelField;
import org.snaccooperative.openrefine.model.SNACModelField.FieldOccurence;
import org.snaccooperative.openrefine.model.SNACModelFieldType;
import org.snaccooperative.openrefine.schema.SNACSchema;
import org.snaccooperative.openrefine.schema.SNACSchemaUtilities;

public class SNACFieldValidator<E extends Enum<E> & SNACModelFieldType> {

  static final Logger logger = LoggerFactory.getLogger(SNACFieldValidator.class);

  private SNACAbstractModel<E> _model;
  private SNACSchema _schema;
  private SNACSchemaUtilities _utils;
  private SNACValidationErrors _errors;

  private Map<E, SNACFieldTracker> _fields;

  public SNACFieldValidator(
      SNACAbstractModel<E> model,
      SNACSchema schema,
      SNACSchemaUtilities utils,
      SNACValidationErrors errors) {
    this._model = model;
    this._schema = schema;
    this._utils = utils;
    this._errors = errors;

    this._fields = new HashMap<E, SNACFieldTracker>();
  }

  private SNACFieldTracker getFieldTracker(SNACModelField<E> field) {
    E fieldType = field.getFieldType();

    _fields.putIfAbsent(fieldType, new SNACFieldTracker<E>(field, _model, _schema, _errors));

    return _fields.get(fieldType);
  }

  // field tracking helpers

  public void addOccurence(SNACModelField<E> field) {
    getFieldTracker(field).addOccurence();
  }

  public Boolean hasReachedLimit(SNACModelField<E> field) {
    return getFieldTracker(field).hasReachedLimit();
  }

  // field processing helpers

  public Boolean hasRequiredFieldsInRow(SNACModelField<E> field, String fieldValue, Row row) {
    return getFieldTracker(field).hasRequiredFieldsInRow(fieldValue, row);
  }

  // one call that does it all

  public Boolean checkAndCountField(SNACModelField<E> field, String fieldValue, Row row) {
    SNACFieldTracker tracker = getFieldTracker(field);

    // check field occurence limit, and increment if not reached
    if (tracker.hasReachedLimit()) {
      return false;
    }
    tracker.addOccurence();

    // check field dependency/dependent requirements
    if (!tracker.hasRequiredFieldsInRow(fieldValue, row)) {
      return false;
    }

    // field is okay to process
    return true;
  }

  // inner helper class

  private class SNACFieldTracker<E extends Enum<E> & SNACModelFieldType> {

    final Logger logger = LoggerFactory.getLogger(SNACFieldTracker.class);

    private final SNACModelField<E> _field;
    private final SNACAbstractModel<E> _model;
    private final SNACSchema _schema;
    private SNACValidationErrors _errors;

    private int _count;
    private Boolean _warned;

    public SNACFieldTracker(
        SNACModelField<E> field,
        SNACAbstractModel<E> model,
        SNACSchema schema,
        SNACValidationErrors errors) {
      this._field = field;
      this._model = model;
      this._schema = schema;
      this._errors = errors;

      this._count = 0;
      this._warned = false;
    }

    public void addOccurence() {
      _count++;
    }

    public Boolean hasReachedLimit() {
      if (_field.getOccurenceType() == FieldOccurence.SINGLE && _count > 0) {
        if (_warned == false) {
          _errors.addOccurenceLimitError(
              _field.getName(), _schema.getColumnFromSNACField(_field.getName()));
          _warned = true;
        }
        return true;
      }

      return false;
    }

    public Boolean hasRequiredFieldsInRow(String fieldValue, Row row) {
      Boolean retval = true;

      E fieldType = _field.getFieldType();
      String fieldColumn = _schema.getColumnFromSNACField(fieldType.getName());

      List<E> deps;

      // check required field dependencies

      deps = _field.getRequiredDependenciesFieldTypes();

      for (int i = 0; i < deps.size(); i++) {
        E requiredField = deps.get(i);

        String requiredColumn = _schema.getColumnFromSNACField(requiredField.getName());
        if (requiredColumn == null) {
          _errors.addRequiredDependencyFieldMissingError(
              fieldType.getName(), fieldColumn, requiredField.getName(), requiredColumn);
          retval = false;
          continue;
        }

        String requiredValue = _utils.getCellValueForRowByColumnName(row, requiredColumn);
        if (requiredValue.equals("")) {
          _errors.addRequiredDependencyFieldEmptyError(
              fieldType.getName(),
              fieldValue,
              fieldColumn,
              requiredField.getName(),
              requiredColumn);
          retval = false;
          continue;
        }
      }

      // check required field dependents

      deps = _model.getModelField(fieldType).getRequiredDependentsFieldTypes();

      for (int i = 0; i < deps.size(); i++) {
        E requiredField = deps.get(i);

        String requiredColumn = _schema.getColumnFromSNACField(requiredField.getName());
        if (requiredColumn == null) {
          _errors.addRequiredDependentFieldMissingError(
              fieldType.getName(), fieldColumn, requiredField.getName(), requiredColumn);
          retval = false;
          continue;
        }

        String requiredValue = _utils.getCellValueForRowByColumnName(row, requiredColumn);
        if (requiredValue.equals("")) {
          _errors.addRequiredDependentFieldEmptyError(
              fieldType.getName(),
              fieldValue,
              fieldColumn,
              requiredField.getName(),
              requiredColumn);
          retval = false;
          continue;
        }
      }

      return retval;
    }
  }
}
