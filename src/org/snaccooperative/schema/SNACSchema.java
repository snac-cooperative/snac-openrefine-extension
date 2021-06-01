/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2018 Antonin Delpeuch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.snaccooperative.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.RecordVisitor;
import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.util.ParsingUtilities;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.exporters.SNACConstellationItem;
import org.snaccooperative.exporters.SNACLookupCache;
import org.snaccooperative.exporters.SNACResourceItem;
import org.snaccooperative.exporters.SNACUploadItem;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SNACSchema implements OverlayModel {

  static final Logger logger = LoggerFactory.getLogger("SNACSchema");

  @JsonProperty("schemaType")
  protected String _schemaType;

  @JsonProperty("idColumn")
  protected String _idColumn;

  @JsonProperty("columnMappings")
  protected HashMap<String, String> _columnMappings;

  @JsonIgnore protected HashMap<String, String> _reverseColumnMappings;

  /*
   * Constructor.
   */
  public SNACSchema() {}

  /*
   * Constructor for deserialization via Jackson
   */
  @JsonCreator
  public SNACSchema(
      @JsonProperty("schemaType") String schemaType,
      @JsonProperty("idColumn") String idColumn,
      @JsonProperty("columnMappings") HashMap<String, String> columnMappings) {
    HashMap<String, String> forwardMap = new HashMap<String, String>();
    HashMap<String, String> reverseMap = new HashMap<String, String>();

    for (Map.Entry<String, String> entry : columnMappings.entrySet()) {
      String csvColumn = entry.getKey();
      String snacText = entry.getValue();
      String snacField = snacText.toLowerCase();

      forwardMap.put(csvColumn, snacText);
      reverseMap.put(snacText, csvColumn);
      reverseMap.put(snacField, csvColumn);
    }

    this._schemaType = schemaType;
    this._idColumn = idColumn;
    this._columnMappings = forwardMap;
    this._reverseColumnMappings = reverseMap;
  }

  @JsonProperty("schemaType")
  public String getSchemaType() {
    return _schemaType;
  }

  @JsonProperty("idColumn")
  public String getIdColumn() {
    return _idColumn;
  }

  @JsonProperty("columnMappings")
  public HashMap<String, String> getColumnMappings() {
    return _columnMappings;
  }

  @JsonIgnore
  public HashMap<String, String> getReverseColumnMappings() {
    return _reverseColumnMappings;
  }

  public List<SNACUploadItem> evaluateRecords(Project project, Engine engine) {
    Mode prevMode = engine.getMode();
    engine.setMode(Mode.RecordBased);

    List<SNACUploadItem> items = new ArrayList<SNACUploadItem>();
    FilteredRecords filteredRecords = engine.getFilteredRecords();
    filteredRecords.accept(project, new SNACRecordVisitor(items, this));

    engine.setMode(prevMode);

    return items;
  }

  protected class SNACRecordVisitor implements RecordVisitor {
    private List<SNACUploadItem> _items;
    private SNACSchema _schema;
    private SNACLookupCache _cache = new SNACLookupCache();

    public SNACRecordVisitor(List<SNACUploadItem> items, SNACSchema schema) {
      this._items = items;
      this._schema = schema;
    }

    @Override
    public void start(Project project) {}

    @Override
    public boolean visit(Project project, Record record) {
      SNACUploadItem item;

      switch (_schema.getSchemaType()) {
        case "constellation":
          item = new SNACConstellationItem(project, _schema, _cache, record);
          break;

        case "resource":
          item = new SNACResourceItem(project, _schema, _cache, record);
          break;

        default:
          return false;
      }

      _items.add(item);

      return false;
    }

    @Override
    public void end(Project project) {}
  }

  public static SNACSchema reconstruct(String json) throws IOException {
    return ParsingUtilities.mapper.readValue(json, SNACSchema.class);
  }

  public static SNACSchema load(Project project, String obj) throws Exception {
    return reconstruct(obj);
  }

  @Override
  public void onBeforeSave(Project project) {}

  @Override
  public void onAfterSave(Project project) {}

  @Override
  public void dispose(Project project) {}

  @Override
  public boolean equals(Object other) {
    if (other == null || !SNACSchema.class.isInstance(other)) {
      return false;
    }
    SNACSchema otherSchema = (SNACSchema) other;
    return (_schemaType == otherSchema.getSchemaType())
        && (_idColumn == otherSchema.getIdColumn())
        && _columnMappings.equals(otherSchema.getColumnMappings());
  }
}
