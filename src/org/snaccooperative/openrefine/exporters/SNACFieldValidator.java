package org.snaccooperative.openrefine.exporters;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACModelField;
import org.snaccooperative.openrefine.model.SNACModelField.FieldOccurence;
import org.snaccooperative.openrefine.model.SNACModelFieldType;
import org.snaccooperative.openrefine.schema.SNACSchema;

public class SNACFieldValidator<E extends Enum<E> & SNACModelFieldType> {

  static final Logger logger = LoggerFactory.getLogger(SNACFieldValidator.class);

  private SNACSchema _schema;
  private SNACValidationErrors _errors;

  private Map<E, SNACFieldTracker> _fields;

  private class SNACFieldTracker<E extends Enum<E> & SNACModelFieldType> {

    final Logger logger = LoggerFactory.getLogger(SNACFieldTracker.class);

    private final SNACModelField<E> _field;
    private final SNACSchema _schema;
    private SNACValidationErrors _errors;

    private int _count;
    private Boolean _warned;

    public SNACFieldTracker(
        SNACModelField<E> field, SNACSchema schema, SNACValidationErrors errors) {
      this._field = field;
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
  }

  public SNACFieldValidator(SNACSchema schema, SNACValidationErrors errors) {
    this._schema = schema;
    this._errors = errors;
    this._fields = new HashMap<E, SNACFieldTracker>();
  }

  private SNACFieldTracker getFieldTracker(SNACModelField<E> field) {
    E fieldType = field.getFieldType();

    _fields.putIfAbsent(fieldType, new SNACFieldTracker<E>(field, _schema, _errors));

    return _fields.get(fieldType);
  }

  public void addOccurence(SNACModelField<E> field) {
    getFieldTracker(field).addOccurence();
  }

  public Boolean hasReachedLimit(SNACModelField<E> field) {
    return getFieldTracker(field).hasReachedLimit();
  }
}
