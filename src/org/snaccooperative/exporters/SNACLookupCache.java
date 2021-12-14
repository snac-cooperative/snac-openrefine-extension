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

  public SNACLookupCache() {
    this._client = new SNACAPIClient("https://api.snaccooperative.org/");
    this._languageCodes = new HashMap<String, Language>();
  }

  /**
   * Helps determine whether a given ISO language code exists on the SNAC database
   *
   * @param lang (ISO language code)
   * @return Language or null (ISO language code found in API Request)
   */
  private Language lookupLanguageCode(String lang) {
    try {
      // Insert API request calls for lang (if exists: insert into language dict, if not: return
      // None)

      String apiQuery =
          "{\"command\": \"vocabulary\",\"query_string\": \""
              + lang
              + "\",\"type\": \"language_code\",\"entity_type\": null}";

      SNACAPIResponse lookupResponse = _client.post(apiQuery);

      JSONParser jp = new JSONParser();
      JSONArray json_result = (JSONArray) ((JSONObject) jp.parse(lookupResponse.getAPIResponse())).get("results");

      if (json_result.size() <= 0) {
        return null;
      } else {
        Language fetchedLanguage = new Language();
        Term langTerm = new Term();

        JSONObject json_val = (JSONObject) json_result.get(0);
        int term_id = Integer.parseInt((String) json_val.get("id"));
        String description = (String) json_val.get("description");
        String term_string = (String) json_val.get("term");
        fetchedLanguage.setOperation("insert");

        langTerm.setID(term_id);
        langTerm.setTerm(term_string);
        langTerm.setDescription(description);

        fetchedLanguage.setLanguage(langTerm);
        // fetchedLanguage.setScript(script_term);
        return fetchedLanguage;
      }
    } catch (ParseException e) {
      return null;
    }
  }

  public Language getLanguageCode(String key) {
    Language language = _languageCodes.get(key);

    if (language != null) {
      return language;
    }

    language = lookupLanguageCode(key);

    if (language != null) {
      logger.info("getLanguageCode(): looked up [" + key + "] => [" + language.toString() + "]");
      _languageCodes.put(key, language);
      return language;
    }

    logger.warn("getLanguageCode(): no cache or lookup results for [" + key + "]");

    return null;
  }
}
