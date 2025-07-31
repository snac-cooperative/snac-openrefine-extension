package org.snaccooperative.openrefine.exporters;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNACValidationErrors {

  static final Logger logger = LoggerFactory.getLogger(SNACValidationErrors.class);

  private List<String> _validationErrors;

  public SNACValidationErrors() {
    this._validationErrors = new ArrayList<String>();
  }

  public List<String> getErrors() {
    return _validationErrors;
  }

  public int getCount() {
    return _validationErrors.size();
  }

  public Boolean hasErrors() {
    return getCount() > 0;
  }

  public void addError(String err) {
    _validationErrors.add(err);
  }

  public String getAccumulatedErrorString() {
    List<String> errs = new ArrayList<String>();

    for (int i = 0; i < _validationErrors.size(); i++) {
      errs.add((i + 1) + ". " + _validationErrors.get(i));
      errs.add("\n");
    }

    return String.join("\n", errs);
  }

  // invalid field value errors

  private void addInvalidFieldError(
      String fieldType,
      String fieldName,
      String fieldValue,
      String fieldColumn,
      String depName,
      String depValue,
      String depColumn) {
    String err;

    err = "Invalid";

    if (fieldType != null && !fieldType.equals("")) {
      err += " " + fieldType;
    }

    err += " field " + fieldName;
    if (fieldColumn != null && !fieldColumn.equals("")) {
      err += " (column " + fieldColumn + ")";
    }
    if (fieldValue != null && !fieldValue.equals("")) {
      err += ": [" + fieldValue + "]";
    }

    if (depName != null && !depName.equals("")) {
      err += " for field " + depName;
      if (depColumn != null && !depColumn.equals("")) {
        err += " (column " + depColumn + ")";
      }
      if (depValue != null && !depValue.equals("")) {
        err += ": [" + depValue + "]";
      }
    }

    addError(err);
  }

  public void addInvalidNumericFieldError(String fieldName, String fieldValue, String fieldColumn) {
    addInvalidFieldError("numeric", fieldName, fieldValue, fieldColumn, null, null, null);
  }

  public void addInvalidVocabularyFieldError(
      String fieldName, String fieldValue, String fieldColumn) {
    addInvalidVocabularyFieldError(fieldName, fieldValue, fieldColumn, null, null, null);
  }

  public void addInvalidVocabularyFieldError(
      String fieldName,
      String fieldValue,
      String fieldColumn,
      String depName,
      String depValue,
      String depColumn) {
    addInvalidFieldError(
        "controlled vocabulary", fieldName, fieldValue, fieldColumn, depName, depValue, depColumn);
  }

  // required field dependency errors

  private void addRequiredFieldError(
      String errMsg,
      String fieldName,
      String fieldValue,
      String fieldColumn,
      String depName,
      String depColumn,
      String depType) {
    String err;

    err = "Field " + depName;
    if (depColumn != null && !depColumn.equals("")) {
      err += " (column " + depColumn + ")";
    }

    err += ", a required " + depType;

    err += " of field " + fieldName;
    if (fieldColumn != null && !fieldColumn.equals("")) {
      err += " (column " + fieldColumn + ")";
    }

    err += ", " + errMsg;

    if (fieldValue != null && !fieldValue.equals("")) {
      err += " for row with value: [" + fieldValue + "]";
    }

    addError(err);
  }

  private void addRequiredFieldMissingError(
      String fieldName, String fieldColumn, String depName, String depColumn, String depType) {
    addRequiredFieldError(
        "not present in SNAC schema", fieldName, null, fieldColumn, depName, depColumn, depType);
  }

  public void addRequiredDependencyFieldMissingError(
      String fieldName, String fieldColumn, String depName, String depColumn) {
    addRequiredFieldMissingError(fieldName, fieldColumn, depName, depColumn, "dependency");
  }

  public void addRequiredDependentFieldMissingError(
      String fieldName, String fieldColumn, String depName, String depColumn) {
    addRequiredFieldMissingError(fieldName, fieldColumn, depName, depColumn, "dependent");
  }

  private void addRequiredFieldEmptyError(
      String fieldName,
      String fieldValue,
      String fieldColumn,
      String depName,
      String depColumn,
      String depType) {
    addRequiredFieldError(
        "is blank", fieldName, fieldValue, fieldColumn, depName, depColumn, depType);
  }

  public void addRequiredDependencyFieldEmptyError(
      String fieldName, String fieldValue, String fieldColumn, String depName, String depColumn) {
    addRequiredFieldEmptyError(
        fieldName, fieldValue, fieldColumn, depName, depColumn, "dependency");
  }

  public void addRequiredDependentFieldEmptyError(
      String fieldName, String fieldValue, String fieldColumn, String depName, String depColumn) {
    addRequiredFieldEmptyError(fieldName, fieldValue, fieldColumn, depName, depColumn, "dependent");
  }

  // related SNAC entity errors

  private void addMissingIDError(String idType, int id) {
    String err;

    err = idType + " ID " + id + " not found in SNAC";

    addError(err);
  }

  public void addMissingRelatedCPFError(int id) {
    addMissingIDError("Related CPF", id);
  }

  public void addMissingRelatedResourceError(int id) {
    addMissingIDError("Related Resource", id);
  }

  public void addMissingHoldingRepositoryError(int id) {
    addMissingIDError("Holding Repository", id);
  }
}
