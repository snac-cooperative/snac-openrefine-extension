package org.snaccooperative.schema;

import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import org.apache.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNACSchemaUtilities {

  static final Logger logger = LoggerFactory.getLogger("SNACSchemaUtilities");

  public static int getCellIndexForRowByColumnName(Project project, Row row, String name) {
    Column column = project.columnModel.getColumnByName(name);

    if (column == null) {
      return -1;
    }

    return column.getCellIndex();
  }

  public static String getCellValueForRowByCellIndex(Row row, int cellIndex) {
    if (cellIndex < 0) {
      return "";
    }

    Object cellValue = row.getCellValue(cellIndex);

    if (cellValue == null) {
      return "";
    }

    return cellValue.toString();
  }

  public static String getCellValueForRowByColumnName(Project project, Row row, String name) {
    int cellIndex = getCellIndexForRowByColumnName(project, row, name);

    if (cellIndex < 0) {
      return "";
    }

    return getCellValueForRowByCellIndex(row, cellIndex);
  }
}
