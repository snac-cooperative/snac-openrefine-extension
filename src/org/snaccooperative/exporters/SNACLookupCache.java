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

public class SNACLookupCache {

  static final Logger logger = LoggerFactory.getLogger("SNACLookupCache");

  protected SNACAPIClient _client;
  protected HashMap<String, String> _languageCodes;

  public SNACLookupCache() {
    this._client = new SNACAPIClient("http://api.snaccooperative.org/");
    this._languageCodes = new HashMap<String, String>();
  }

  /**
   * Helps determine whether a given ISO language code exists on the SNAC database
   *
   * @param lang (ISO language code)
   * @return lang_term or null (ISO language code found in API Request)
   */
  private String lookupLanguageCode(String lang) {
    try {
      // Insert API request calls for lang (if exists: insert into language dict, if not: return
      // None)

      String apiQuery =
          "{\"command\": \"vocabulary\",\"query_string\": \""
              + lang
              + "\",\"type\": \"language_code\",\"entity_type\": null}";

      SNACAPIResponse lookupResponse = _client.post(apiQuery);

      JSONParser jp = new JSONParser();
      JSONArray json_result =
          (JSONArray) ((JSONObject) jp.parse(lookupResponse.getAPIResponse())).get("results");

      if (json_result.size() <= 0) {
        return null;
      } else {
        JSONObject json_val = (JSONObject) json_result.get(0);
        // String lang_id = (String) json_val.get("id");
        // String lang_desc = (String) json_val.get("description");
        String lang_term = (String) json_val.get("term");
        return lang_term;
      }
    } catch (ParseException e) {
      return null;
    }
  }

  public String getLanguageCode(String key) {
    String langCode = _languageCodes.get(key);

    if (langCode != null) {
      return langCode;
    }

    langCode = lookupLanguageCode(key);

    if (langCode != null) {
      logger.info("getLanguageCode(): looked up [" + key + "] => [" + langCode + "]");
      _languageCodes.put(key, langCode);
      return langCode;
    }

    logger.warn("getLanguageCode(): no cache or lookup results for [" + key + "]");

    return null;
  }
}
