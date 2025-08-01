package org.snaccooperative.openrefine.exporters;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACModelField;
import org.snaccooperative.openrefine.model.SNACModelField.FieldOccurence;
import org.snaccooperative.openrefine.model.SNACModelFieldType;

public class SNACFieldValidator<E extends Enum<E> & SNACModelFieldType> {

  static final Logger logger = LoggerFactory.getLogger(SNACFieldValidator.class);

  private SNACValidationErrors _errors;

  private Map<E, SNACFieldTracker> _fields;

  private class SNACFieldTracker<E extends Enum<E> & SNACModelFieldType> {

    final Logger logger = LoggerFactory.getLogger(SNACFieldTracker.class);

    private final SNACModelField<E> _field;
    private SNACValidationErrors _errors;

    private int _count;
    private Boolean _warned;

    public SNACFieldTracker(SNACModelField<E> field, SNACValidationErrors errors) {
      this._field = field;
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
          _errors.addError("Field \"" + _field.getName() + "\" can only appear once per record");
          _warned = true;
        }
        return true;
      }

      return false;
    }
  }

  public SNACFieldValidator(SNACValidationErrors errors) {
    this._errors = errors;
    this._fields = new HashMap<E, SNACFieldTracker>();
  }

  private SNACFieldTracker getFieldTracker(SNACModelField<E> field) {
    E fieldType = field.getFieldType();

    _fields.putIfAbsent(fieldType, new SNACFieldTracker<E>(field, _errors));

    return _fields.get(fieldType);
  }

  public void addOccurence(SNACModelField<E> field) {
    getFieldTracker(field).addOccurence();
  }

  public Boolean hasReachedLimit(SNACModelField<E> field) {
    return getFieldTracker(field).hasReachedLimit();
  }
}
