package org.snaccooperative.exporters;

import java.util.HashMap;
import org.apache.http.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.commands.SNACAPIClient;
import org.snaccooperative.commands.SNACAPIResponse;
import org.snaccooperative.data.Language;
import org.snaccooperative.data.Term;

public class SNACLookupCache {

  static final Logger logger = LoggerFactory.getLogger("SNACLookupCache");

  protected SNACAPIClient _client;
  protected HashMap<String, Language> _languageCodes;
  protected HashMap<Integer, Boolean> _constellationExists;
  protected HashMap<Integer, Boolean> _resourceExists;

  public SNACLookupCache(SNACAPIClient client) {
    this._client = client;
    this._languageCodes = new HashMap<String, Language>();
    this._constellationExists = new HashMap<Integer, Boolean>();
    this._resourceExists = new HashMap<Integer, Boolean>();
  }

  /**
   * Helps determine whether a given ISO language code exists on the SNAC database
   *
   * @param lang (ISO language code)
   * @return Language or null (ISO language code found in API Request)
   */
  private Language lookupLanguage(String lang) {
    try {
      // Insert API request calls for lang (if exists: insert into language dict, if not: return
      // None)

      String apiQuery =
          "{ \"command\": \"vocabulary\", \"query_string\": \""
              + lang
              + "\", \"type\": \"language_code\", \"entity_type\": null }";

      SNACAPIResponse lookupResponse = _client.post(apiQuery);

      JSONParser jp = new JSONParser();
      JSONArray results =
          (JSONArray) ((JSONObject) jp.parse(lookupResponse.getAPIResponse())).get("results");

      if (results.size() <= 0) {
        return null;
      }

      Language fetchedLanguage = new Language();
      Term langTerm = new Term();

      JSONObject result = (JSONObject) results.get(0);
      Integer term_id = Integer.parseInt((String) result.get("id"));
      String description = (String) result.get("description");
      String term_string = (String) result.get("term");
      fetchedLanguage.setOperation("insert");

      langTerm.setID(term_id);
      langTerm.setTerm(term_string);
      langTerm.setDescription(description);

      fetchedLanguage.setLanguage(langTerm);
      // fetchedLanguage.setScript(script_term);
      return fetchedLanguage;
    } catch (ParseException e) {
      logger.warn("language lookup response parse failure: [" + e + "]");
      return null;
    }
  }

  public Language getLanguage(String key) {
    Language language = _languageCodes.get(key);

    if (language != null) {
      logger.info("language [" + key + "] mapping: cached: [" + language.toString() + "]");
      return language;
    }

    language = lookupLanguage(key);

    if (language != null) {
      logger.info("language [" + key + "] mapping: lookup: [" + language.toString() + "]");
      _languageCodes.put(key, language);
      return language;
    }

    logger.warn("language [" + key + "] mapping: no cache or lookup results");

    return null;
  }

  private Boolean lookupConstellation(Integer id) {
    // query existence of constellation ID via elasticsearch for speed

    try {
      String apiQuery =
          "{ \"command\": \"elastic\", \"query\": { \"ids\": { \"values\": [ "
              + id
              + " ] } }, \"size\": 0 }";

      SNACAPIResponse lookupResponse = _client.post(apiQuery);

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

      JSONParser jp = new JSONParser();
      JSONObject results =
          (JSONObject) ((JSONObject) jp.parse(lookupResponse.getAPIResponse())).get("results");
      JSONObject hits = (JSONObject) results.get("hits");
      JSONObject total = (JSONObject) hits.get("total");
      Integer value = (Integer) (((Number) total.get("value")).intValue());
      return (value == 1);
    } catch (ParseException e) {
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
      String apiQuery = "{ \"command\": \"read_resource\", \"resourceid\": " + id + " }";

      SNACAPIResponse lookupResponse = _client.post(apiQuery);

      logger.debug("lookupResource(): API response: [" + lookupResponse.getAPIResponse() + "]");

      // if we don't have a definitive answer, bail out now
      if (!lookupResponse.isSuccess()) {
        logger.debug(
            "lookupResource(): got unsuccessful API result: [" + lookupResponse.getResult() + "]");
        return null;
      }

      // existence check: it's probably sufficient that a resource was returned, but
      // we go the extra mile and verify that its id equals the one we requested

      JSONParser jp = new JSONParser();
      JSONObject resource =
          (JSONObject) ((JSONObject) jp.parse(lookupResponse.getAPIResponse())).get("resource");

      if (resource == null) {
        return false;
      }

      Integer resid = Integer.parseInt((String) resource.get("id"));
      return resid.equals(id);
    } catch (ParseException e) {
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
