package org.snaccooperative.openrefine.cache;

import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.data.Language;
import org.snaccooperative.data.Term;
import org.snaccooperative.openrefine.api.SNACAPIClient;
import org.snaccooperative.openrefine.api.SNACAPIResponse;

public class SNACLookupCache {

  static final Logger logger = LoggerFactory.getLogger("SNACLookupCache");

  private SNACAPIClient _client;
  private SNACAPIClient _termClient;
  private HashMap<String, Language> _languageCodes;
  private HashMap<Integer, Boolean> _constellationExists;
  private HashMap<Integer, Boolean> _resourceExists;

  private SNACTermCache _subjectTerms;
  private SNACTermCache _activityTerms;
  private SNACTermCache _occupationTerms;
  private SNACTermCache _entityTypeTerms;
  private SNACTermCache _dateTypeTerms;
  private SNACTermCache _placeRoleTerms;
  private SNACTermCache _documentRoleTerms;
  private SNACTermCache _relationTypeTerms;
  private SNACTermCache _languageCodeTerms;
  private SNACTermCache _scriptCodeTerms;
  private SNACTermCache _documentTypeTerms;
  private SNACTermCache _recordTypeTerms;
  private SNACTermCache _placeTypeTerms;

  public SNACLookupCache(SNACAPIClient client) {
    this._client = client;
    this._termClient = null;

    this._languageCodes = new HashMap<String, Language>();
    this._constellationExists = new HashMap<Integer, Boolean>();
    this._resourceExists = new HashMap<Integer, Boolean>();

    this._subjectTerms = new SNACTermCache("subject");
    this._activityTerms = new SNACTermCache("activity");
    this._occupationTerms = new SNACTermCache("occupation");
    this._entityTypeTerms = new SNACTermCache("entity_type");
    this._dateTypeTerms = new SNACTermCache("date_type");
    this._placeRoleTerms = new SNACTermCache("place_role");
    this._documentRoleTerms = new SNACTermCache("document_role");
    this._relationTypeTerms = new SNACTermCache("relation_type");
    this._languageCodeTerms = new SNACTermCache("language_code");
    this._scriptCodeTerms = new SNACTermCache("script_code");
    this._documentTypeTerms = new SNACTermCache("document_type");
    this._recordTypeTerms = new SNACTermCache("record_type");
    this._placeTypeTerms = new SNACTermCache("place_type");
  }

  public void disableTermCache() {
    _termClient = null;
  }

  public void enableTermCache() {
    _termClient = _client;
  }

  public Term getSubjectTerm(String key) {
    return _subjectTerms.getTerm(_termClient, key);
  }

  public Term getActivityTerm(String key) {
    return _activityTerms.getTerm(_termClient, key);
  }

  public Term getOccupationTerm(String key) {
    return _occupationTerms.getTerm(_termClient, key);
  }

  public Term getEntityTypeTerm(String key) {
    return _entityTypeTerms.getTerm(_termClient, key);
  }

  public Term getDateTypeTerm(String key) {
    return _dateTypeTerms.getTerm(_termClient, key);
  }

  public Term getPlaceRoleTerm(String key) {
    return _placeRoleTerms.getTerm(_termClient, key);
  }

  public Term getDocumentRoleTerm(String key) {
    return _documentRoleTerms.getTerm(_termClient, key);
  }

  public Term getRelationTypeTerm(String key) {
    return _relationTypeTerms.getTerm(_termClient, key);
  }

  public Term getLanguageCodeTerm(String key) {
    return _languageCodeTerms.getTerm(_termClient, key);
  }

  public Term getScriptCodeTerm(String key) {
    return _scriptCodeTerms.getTerm(_termClient, key);
  }

  public Term getDocumentTypeTerm(String key) {
    return _documentTypeTerms.getTerm(_termClient, key);
  }

  public Term getRecordTypeTerm(String key) {
    return _recordTypeTerms.getTerm(_termClient, key);
  }

  public Term getPlaceTypeTerm(String key) {
    return _placeTypeTerms.getTerm(_termClient, key);
  }

  private Boolean lookupConstellation(Integer id) {
    // query existence of constellation ID via elasticsearch for speed

    try {
      JSONObject req = new JSONObject();

      req.put("command", "elastic");
      req.put(
          "query",
          new JSONObject().put("ids", new JSONObject().put("values", new JSONArray().put(id))));
      req.put("size", 0);

      SNACAPIResponse lookupResponse = _client.post(req);

      logger.debug(
          "lookupConstellation(): API response: [" + lookupResponse.getAPIResponse() + "]");

      // if we don't have a definitive answer, bail out now
      if (!lookupResponse.isSuccess()) {
        logger.debug(
            "lookupConstellation(): got unsuccessful API result: ["
                + lookupResponse.getResult()
                + "]");
        return null;
      }

      // existence check: verify that the total number of hits for this id is 1

      JSONObject results =
          (JSONObject) new JSONObject(lookupResponse.getAPIResponse()).get("results");
      JSONObject hits = (JSONObject) results.get("hits");
      JSONObject total = (JSONObject) hits.get("total");
      Integer value = (Integer) (((Number) total.get("value")).intValue());
      return (value == 1);
    } catch (JSONException e) {
      logger.warn("constellation lookup response parse failure: [" + e + "]");
      return null;
    }
  }

  public Boolean constellationExists(Integer id) {
    Boolean exists = _constellationExists.get(id);

    if (exists != null) {
      logger.info("constellation " + id + " existence: cached: " + exists);
      return exists;
    }

    exists = lookupConstellation(id);

    if (exists != null) {
      logger.info("constellation " + id + " existence: lookup: " + exists);
      _constellationExists.put(id, exists);
      return exists;
    }

    logger.warn("constellation " + id + " existence: no cache or lookup results; assuming no");

    return false;
  }

  private Boolean lookupResource(Integer id) {
    // perform lookup of ID by simply reading the resource

    try {
      JSONObject req = new JSONObject();

      req.put("command", "read_resource");
      req.put("resourceid", id);

      SNACAPIResponse lookupResponse = _client.post(req);

      logger.debug("lookupResource(): API response: [" + lookupResponse.getAPIResponse() + "]");

      // if we don't have a definitive answer, bail out now
      if (!lookupResponse.isSuccess()) {
        logger.debug(
            "lookupResource(): got unsuccessful API result: [" + lookupResponse.getResult() + "]");
        return null;
      }

      // existence check: it's probably sufficient that a resource was returned, but
      // we go the extra inch and verify that its id equals the one we requested

      JSONObject resource =
          (JSONObject) new JSONObject(lookupResponse.getAPIResponse()).get("resource");

      if (resource == null) {
        return false;
      }

      Integer resid = Integer.parseInt((String) resource.get("id"));
      return resid.equals(id);
    } catch (JSONException e) {
      logger.warn("resource lookup response parse failure: [" + e + "]");
      return null;
    }
  }

  public Boolean resourceExists(Integer id) {
    Boolean exists = _resourceExists.get(id);

    if (exists != null) {
      logger.info("resource " + id + " existence: cached: " + exists);
      return exists;
    }

    exists = lookupResource(id);

    if (exists != null) {
      logger.info("resource " + id + " existence: lookup: " + exists);
      _resourceExists.put(id, exists);
      return exists;
    }

    logger.warn("resource " + id + " existence: no cache or lookup results; assuming no");

    return false;
  }
}
