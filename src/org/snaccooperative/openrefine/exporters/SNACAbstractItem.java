package org.snaccooperative.openrefine.exporters;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.api.SNACAPIResponse;

public abstract class SNACAbstractItem {

  static final Logger logger = LoggerFactory.getLogger(SNACAbstractItem.class);

  public abstract String getPreviewText();

  public abstract int rowIndex();

  public abstract String toJSON();

  public abstract SNACAPIResponse performValidation();

  public abstract SNACAPIResponse performUpload();

  // preview text helpers

  protected String htmlTable(String s) {
    return "<table><tbody>" + s + "</tbody></table>";
  }

  protected String htmlTableRow(String s) {
    return "<tr class=\"snac-schema-preview-row\">" + s + "</tr>";
  }

  protected String htmlTableColumn(String s, String attr) {
    String html = "<td";
    if (attr != null) {
      html += " " + attr;
    }
    html += ">" + s + "</td>";
    return html;
  }

  protected String htmlTableColumnField(String s) {
    return htmlTableColumn(s, "class=\"snac-schema-preview-column-field\"");
  }

  protected String htmlTableColumnValue(String s) {
    return htmlTableColumn(s, "class=\"snac-schema-preview-column-value\"");
  }

  protected String htmlOrderedList(List<String> s) {
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

  protected String htmlLink(String url, String title) {
    return "<a href=\"" + url + "\" target=\"_blank\">" + title + "</a>";
  }
}
