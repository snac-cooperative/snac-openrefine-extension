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
package org.snaccooperative.openrefine.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.api.SNACAPIClient;
import org.snaccooperative.openrefine.cache.SNACLookupCache;
import org.snaccooperative.openrefine.exporters.SNACConstellationItem;
import org.snaccooperative.openrefine.exporters.SNACResourceItem;
import org.snaccooperative.openrefine.exporters.SNACUploadItem;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SNACSchema implements OverlayModel {

  static final Logger logger = LoggerFactory.getLogger("SNACSchema");

  @JsonProperty("schemaType")
  protected String _schemaType;

  @JsonProperty("columnMappings")
  protected HashMap<String, String> _columnMappings;

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
      @JsonProperty("columnMappings") HashMap<String, String> columnMappings) {
    this._schemaType = schemaType;
    this._columnMappings = columnMappings;
  }

  @JsonProperty("schemaType")
  public String getSchemaType() {
    return _schemaType;
  }

  @JsonProperty("columnMappings")
  public HashMap<String, String> getColumnMappings() {
    return _columnMappings;
  }

  public List<SNACUploadItem> evaluateRecords(Project project, Engine engine) {
    return evaluateRecords(project, engine, 0);
  }

  public List<SNACUploadItem> evaluateRecords(Project project, Engine engine, int maxRecords) {
    Mode prevMode = engine.getMode();
    engine.setMode(Mode.RecordBased);

    List<SNACUploadItem> items = new ArrayList<SNACUploadItem>();
    FilteredRecords filteredRecords = engine.getFilteredRecords();
    filteredRecords.accept(project, new SNACRecordVisitor(items, this, maxRecords));

    engine.setMode(prevMode);

    return items;
  }

  protected class SNACRecordVisitor implements RecordVisitor {
    private List<SNACUploadItem> _items;
    private SNACSchema _schema;
    private SNACAPIClient _client;
    private SNACLookupCache _cache;
    private int _maxRecords;

    final Logger logger = LoggerFactory.getLogger("SNACRecordVisitor");

    public SNACRecordVisitor(List<SNACUploadItem> items, SNACSchema schema) {
      this(items, schema, 0);
    }

    public SNACRecordVisitor(List<SNACUploadItem> items, SNACSchema schema, int maxRecords) {
      this._items = items;
      this._schema = schema;
      this._maxRecords = maxRecords;

      this._client = new SNACAPIClient();
      this._cache = new SNACLookupCache(this._client);
    }

    @Override
    public void start(Project project) {}

    // FIXME:
    // "visit(com.google.refine.model.Project ,com.google.refine.model.Record) in
    //    com.google.refine.browsing.RecordVisitor has been deprecated"
    @Override
    public boolean visit(Project project, Record record) {
      if (_maxRecords > 0 && _items.size() >= _maxRecords) {
        return false;
      }

      SNACUploadItem item;

      switch (_schema.getSchemaType()) {
        case "constellation":
          item = new SNACConstellationItem(project, _schema, _client, _cache, record);
          break;

        case "relation":
          item = new SNACConstellationItem(project, _schema, _client, _cache, record);
          break;

        case "resource":
          item = new SNACResourceItem(project, _schema, _client, _cache, record);
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
        && _columnMappings.equals(otherSchema.getColumnMappings());
  }
}
