package org.snaccooperative.openrefine.exporters;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.api.SNACAPIClient;
import org.snaccooperative.openrefine.api.SNACAPIResponse;
import org.snaccooperative.openrefine.cache.SNACLookupCache;
import org.snaccooperative.openrefine.model.SNACAbstractModel.ModelType;
import org.snaccooperative.openrefine.schema.SNACSchema;
import org.snaccooperative.openrefine.schema.SNACSchemaUtilities;

public abstract class SNACAbstractItem {

  static final Logger logger = LoggerFactory.getLogger(SNACAbstractItem.class);

  protected Project _project;
  protected Record _record;
  protected SNACSchema _schema;
  protected SNACAPIClient _client;
  protected SNACLookupCache _cache;

  protected ModelType _modelType;
  protected SNACSchemaUtilities _utils;
  protected SNACValidationErrors _errors;

  protected Map<ModelType, List<Integer>> _relatedIDs;

  public SNACAbstractItem(
      Project project,
      SNACSchema schema,
      SNACAPIClient client,
      SNACLookupCache cache,
      Record record) {
    this._project = project;
    this._schema = schema;
    this._client = client;
    this._cache = cache;
    this._record = record;

    this._modelType = ModelType.fromString(_schema.getSchemaType());
    this._utils = new SNACSchemaUtilities(_project, _schema);
    this._errors = null;

    this._relatedIDs = new HashMap<ModelType, List<Integer>>();
    for (ModelType type : ModelType.values()) {
      this._relatedIDs.put(type, new LinkedList<Integer>());
    }
  }

  // abstract methods

  public abstract String getPreviewText();

  public abstract String toJSON();

  public abstract SNACAPIResponse performUpload();

  protected abstract void buildItem();

  // concrete methods

  public int rowIndex() {
    return _record.fromRowIndex;
  }

  protected void buildItemVerbatim() {
    _cache.disableTermCache();
    buildItem();
  }

  protected void buildItemAgainstSNAC() {
    _cache.enableTermCache();
    buildItem();
  }

  protected void verifyRelatedIDs() {
    // Before uploading, we verify existence of any related CPF and resource
    // IDs in the selected SNAC environment.  This is because SNAC will
    // accept related CPF IDs that do not actually exist, then crash when
    // reloading the original CPF, leaving the original CPF in a locked state.
    // Invalid resource IDs may not cause the same kind of fatal error,
    // but we check them anyway to keep the data clean.

    // These existence checks should really be made in SNAC proper, but
    // it's easier (and effective) to perform them here for now.

    // NOTE: only constellation/resource model types are used as map keys.
    // resource items (resource model type) might set related constellations.
    // constellation items (constellation/relation model types) might set either.

    // relation model: check for missing relations

    if (_modelType == ModelType.RELATION
        && _relatedIDs.get(ModelType.CONSTELLATION).size() == 0
        && _relatedIDs.get(ModelType.RESOURCE).size() == 0) {
      _errors.addError("No related CPFs or Resources defined for this record");
      return;
    }

    List<Integer> ids;

    // related constellations:
    // for constellation items, these are related CPF IDs
    // for resource items, this is a holding repository ID

    ids = _relatedIDs.get(ModelType.CONSTELLATION);
    for (int i = 0; i < ids.size(); i++) {
      int id = ids.get(i);
      if (!_cache.constellationExists(id)) {
        if (_modelType == ModelType.RESOURCE) {
          _errors.addMissingHoldingRepositoryError(id);
        } else {
          _errors.addMissingRelatedCPFError(id);
        }
      }
    }

    // related resources:
    // for constellation items, these are related resource IDs

    ids = _relatedIDs.get(ModelType.RESOURCE);
    for (int i = 0; i < ids.size(); i++) {
      int id = ids.get(i);
      if (!_cache.resourceExists(id)) {
        _errors.addMissingRelatedResourceError(id);
      }
    }
  }

  public SNACAPIResponse performValidation() {
    // create the item, validating any controlled vocabulary terms
    // against the selected SNAC environment

    buildItemAgainstSNAC();

    // verify existence of any related IDs

    verifyRelatedIDs();

    // return error if validation errors were encountered at any point

    if (_errors.hasErrors()) {
      return new SNACAPIResponse(_client, _errors.getAccumulatedErrorString());
    }

    return new SNACAPIResponse("success");
  }

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
