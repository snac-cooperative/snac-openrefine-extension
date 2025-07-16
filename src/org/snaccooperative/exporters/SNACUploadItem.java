package org.snaccooperative.exporters;

import java.util.List;
import org.snaccooperative.api.SNACAPIResponse;

public abstract class SNACUploadItem {

  public abstract String getPreviewText();

  public abstract int rowIndex();

  public abstract String toJSON();

  public abstract SNACAPIResponse performValidation();

  public abstract SNACAPIResponse performUpload();

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
