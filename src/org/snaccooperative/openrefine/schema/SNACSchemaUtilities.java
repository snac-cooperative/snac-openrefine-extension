package org.snaccooperative.openrefine.schema;

import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNACSchemaUtilities {

  static final Logger logger = LoggerFactory.getLogger(SNACSchemaUtilities.class);

  private Project _project;
  private SNACSchema _schema;

  public SNACSchemaUtilities(Project project, SNACSchema schema) {
    this._project = project;
    this._schema = schema;
  }

  // openrefine row/column/cell helpers

  private int getCellIndexForRowByColumnName(Row row, String name) {
    Column column = _project.columnModel.getColumnByName(name);

    if (column == null) {
      return -1;
    }

    return column.getCellIndex();
  }

  private String getCellValueForRowByCellIndex(Row row, int cellIndex) {
    if (cellIndex < 0) {
      return "";
    }

    Object cellValue = row.getCellValue(cellIndex);

    if (cellValue == null) {
      return "";
    }

    return cellValue.toString().trim();
  }

  public String getCellValueForRowByColumnName(Row row, String name) {
    int cellIndex = getCellIndexForRowByColumnName(row, name);

    if (cellIndex < 0) {
      return "";
    }

    return getCellValueForRowByCellIndex(row, cellIndex);
  }
}
