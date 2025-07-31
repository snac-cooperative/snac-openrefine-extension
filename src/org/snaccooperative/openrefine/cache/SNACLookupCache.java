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

  public enum TermType {
    NONE,
    ACTIVITY("activity", "Activity"),
    DATE_TYPE("date_type", "Date Type"),
    DOCUMENT_ROLE("document_role", "Document Role"),
    DOCUMENT_TYPE("document_type", "Document Type"),
    ENTITY_TYPE("entity_type", "Entity Type"),
    LANGUAGE_CODE("language_code", "Language Code"),
    OCCUPATION("occupation", "Occupation"),
    PLACE_ROLE("place_role", "Place Role"),
    PLACE_TYPE("place_type", "Place Type"),
    RECORD_TYPE("record_type", "Record Type"),
    RELATION_TYPE("relation_type", "Relation Type"),
    SCRIPT_CODE("script_code", "Script Code"),
    SUBJECT("subject", "Subject");

    private final String _type;
    private final String _name;

    TermType() {
      this("", "");
    }

    TermType(String type, String name) {
      this._type = type;
      this._name = name;
    }

    public String getType() {
      return _type;
    }

    public String getName() {
      return _name;
    }
  }

  static final Logger logger = LoggerFactory.getLogger(SNACLookupCache.class);

  private SNACAPIClient _client;
  private SNACAPIClient _termClient;
  private HashMap<String, Language> _languageCodes;
  private HashMap<Integer, Boolean> _constellationExists;
  private HashMap<Integer, Boolean> _resourceExists;
  private HashMap<TermType, SNACTermCache> _termCaches;

  public SNACLookupCache(SNACAPIClient client) {
    this._client = client;
    this._termClient = null;

    this._languageCodes = new HashMap<String, Language>();
    this._constellationExists = new HashMap<Integer, Boolean>();
    this._resourceExists = new HashMap<Integer, Boolean>();
    this._termCaches = new HashMap<TermType, SNACTermCache>();

    for (TermType term : TermType.values()) {
      if (term != TermType.NONE) {
        newTermCache(term);
      }
    }
  }

  private void newTermCache(TermType term) {
    SNACTermCache termCache = new SNACTermCache(term.getType());
    _termCaches.put(term, termCache);
  }

  public void disableTermCache() {
    _termClient = null;
  }

  public void enableTermCache() {
    _termClient = _client;
  }

  public Term getTerm(TermType term, String key) {
    return _termCaches.get(term).getTerm(_termClient, key);
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
