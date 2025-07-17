package org.snaccooperative.openrefine.exporters;

import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import java.util.List;
import org.snaccooperative.openrefine.api.SNACAPIResponse;

public abstract class SNACAbstractItem {

  public abstract String getPreviewText();

  public abstract int rowIndex();

  public abstract String toJSON();

  public abstract SNACAPIResponse performValidation();

  public abstract SNACAPIResponse performUpload();

  // openrefine row/column/cell helpers

  protected int getCellIndexForRowByColumnName(Project project, Row row, String name) {
    Column column = project.columnModel.getColumnByName(name);

    if (column == null) {
      return -1;
    }

    return column.getCellIndex();
  }

  protected String getCellValueForRowByCellIndex(Row row, int cellIndex) {
    if (cellIndex < 0) {
      return "";
    }

    Object cellValue = row.getCellValue(cellIndex);

    if (cellValue == null) {
      return "";
    }

    return cellValue.toString();
  }

  protected String getCellValueForRowByColumnName(Project project, Row row, String name) {
    int cellIndex = getCellIndexForRowByColumnName(project, row, name);

    if (cellIndex < 0) {
      return "";
    }

    return getCellValueForRowByCellIndex(row, cellIndex);
  }

  // preview text helpers

  public String htmlTable(String s) {
    return "<table><tbody>" + s + "</tbody></table>";
  }

  public String htmlTableRow(String s) {
    return "<tr class=\"snac-schema-preview-row\">" + s + "</tr>";
  }

  public String htmlTableColumn(String s, String attr) {
    String html = "<td";
    if (attr != null) {
      html += " " + attr;
    }
    html += ">" + s + "</td>";
    return html;
  }

  public String htmlTableColumnField(String s) {
    return htmlTableColumn(s, "class=\"snac-schema-preview-column-field\"");
  }

  public String htmlTableColumnValue(String s) {
    return htmlTableColumn(s, "class=\"snac-schema-preview-column-value\"");
  }

  public String htmlOrderedList(List<String> s) {
    if (s.size() == 0) {
      return "";
    }
    String html = "<ol>";
    for (int i = 0; i < s.size(); i++) {
      html += "<li class=\"snac-schema-preview-list-item\">" + s.get(i) + "</li>";
    }
    html += "</ol>";
    return html;
  }

  public String htmlLink(String url, String title) {
    return "<a href=\"" + url + "\" target=\"_blank\">" + title + "</a>";
  }
}
