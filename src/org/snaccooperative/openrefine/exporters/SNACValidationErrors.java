package org.snaccooperative.openrefine.exporters;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACModelField.FieldVocabulary;

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

    /*
        if (_validationErrors.size() > 0) {
          errs.add("Validation Errors:");
          errs.add("");
        }
    */

    for (int i = 0; i < _validationErrors.size(); i++) {
      errs.add((i + 1) + ". " + _validationErrors.get(i));
      errs.add("");
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

    err = "Field \"" + fieldName + "\"";
    if (fieldColumn != null && !fieldColumn.equals("")) {
      err += " (column \"" + fieldColumn + "\")";
    }
    err += " has invalid";
    if (fieldType != null && !fieldType.equals("")) {
      err += " " + fieldType;
    }
    err += " value";
    if (fieldValue != null && !fieldValue.equals("")) {
      err += ": [" + fieldValue + "]";
    }

    if (depName != null && !depName.equals("")) {
      err += " for related field \"" + depName + "\"";
      if (depColumn != null && !depColumn.equals("")) {
        err += " (column \"" + depColumn + "\")";
      }
      if (depValue != null && !depValue.equals("")) {
        err += " with value: [" + depValue + "]";
      }
    }

    addError(err);
  }

  public void addInvalidNumericFieldError(String fieldName, String fieldValue, String fieldColumn) {
    addInvalidFieldError(
        FieldVocabulary.IDENTIFIER.getName(), fieldName, fieldValue, fieldColumn, null, null, null);
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
        FieldVocabulary.CONTROLLED.getName(),
        fieldName,
        fieldValue,
        fieldColumn,
        depName,
        depValue,
        depColumn);
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

    err = "Field \"" + depName + "\"";
    if (depColumn != null && !depColumn.equals("")) {
      err += " (column \"" + depColumn + "\")";
    }

    err += ", a required " + depType;

    err += " of field \"" + fieldName + "\"";
    if (fieldColumn != null && !fieldColumn.equals("")) {
      err += " (column \"" + fieldColumn + "\")";
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
        "is not present in SNAC schema", fieldName, null, fieldColumn, depName, depColumn, depType);
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

  // snac id errors

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

  public void addMissingCPFError(int id) {
    addMissingIDError("CPF", id);
  }

  public void addMissingResourceError(int id) {
    addMissingIDError("Resource", id);
  }

  // field validation errors

  public void addOccurenceLimitError(String fieldName, String fieldColumn) {
    String err;

    err = "Field \"" + fieldName + "\"";
    if (fieldColumn != null && !fieldColumn.equals("")) {
      err += " (column \"" + fieldColumn + "\")";
    }

    err += " can only appear once per record";

    addError(err);
  }

  public void addRequiredFieldMissingError(String fieldName, String fieldColumn) {
    String err;

    err = "Field \"" + fieldName + "\"";
    if (fieldColumn != null && !fieldColumn.equals("")) {
      err += " (column \"" + fieldColumn + "\")";
    }

    err += " is required, but not defined in this record";

    addError(err);
  }

  public void addRequiredFieldMissingError(String fieldName) {
    String err;

    err = "Field \"" + fieldName + "\"";
    err += " is required, but not present in SNAC schema";

    addError(err);
  }

  public void addNoRelationsDefinedError() {
    addError("No related CPFs or Resources defined for this record");
  }
}
