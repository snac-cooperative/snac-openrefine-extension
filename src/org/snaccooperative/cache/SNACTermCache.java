package org.snaccooperative.cache;

import java.util.HashMap;
import org.apache.http.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.api.SNACAPIClient;
import org.snaccooperative.api.SNACAPIResponse;
import org.snaccooperative.data.Term;

public class SNACTermCache {

  static final Logger logger = LoggerFactory.getLogger("SNACTermCache");

  protected String _type;

  protected HashMap<String, Term> _terms;

  public SNACTermCache(String type) {
    this._type = type;
    this._terms = new HashMap<String, Term>();
  }

  private Term lookupTerm(SNACAPIClient client, String key) {
    try {
      JSONObject req = new JSONObject();

      req.put("command", "vocabulary");
      req.put("query_string", key);
      req.put("type", _type);

      SNACAPIResponse lookupResponse = client.post(req);

      JSONArray results = new JSONObject(lookupResponse.getAPIResponse()).optJSONArray("results");

      if (results == null || results.length() <= 0) {
        logger.error(
            "vocabulary [" + _type + "] query returned no results for term: [" + key + "]");
        return null;
      }

      for (int i = 0; i < results.length(); i++) {
        JSONObject result = results.optJSONObject(i);
        if (result == null) {
          logger.warn(
              "vocabulary [" + _type + "] query response contained missing/invalid array object");
          continue;
        }

        String gotTerm = result.optString("term", null);
        if (gotTerm == null) {
          logger.warn(
              "vocabulary ["
                  + _type
                  + "] query response contained missing/invalid term in array object");
          continue;
        }

        if (!gotTerm.toLowerCase().equals(key.toLowerCase())) {
          continue;
        }

        // Integer gotID = Integer.parseInt(result.optString("id", "0"));
        String gotType = result.optString("type", null);
        if (gotType == null) {
          logger.warn(
              "vocabulary ["
                  + _type
                  + "] query response contained missing/invalid type in array object");
          continue;
        }

        String gotDesc = result.optString("description", null);

        Term term = new Term();
        term.setType(gotType);
        term.setTerm(gotTerm);

        _terms.put(gotTerm.toLowerCase(), term);

        // store and map description, if it exists
        if (gotDesc != null) {
          term.setDescription(gotDesc);
          _terms.put(gotDesc.toLowerCase(), term);
        }

        return term;
      }
    } catch (JSONException e) {
      logger.error("vocabulary [" + _type + "] query response parse failure: [" + e + "]");
    }

    return null;
  }

  private Term dummyTerm(String key) {
    Term term = new Term();
    term.setType(_type);
    term.setTerm(key);
    term.setDescription("");

    return term;
  }

  private String getDescription(Term term) {
    String _term = term.getTerm();
    String _desc = term.getDescription();

    String desc = "term: [" + _term + "]";
    if (_desc != null) {
      desc += " desc: [" + _desc + "]";
    }

    return desc;
  }

  public Term getTerm(SNACAPIClient client, String key) {
    if (key == null || key.trim().equals("")) {
      return null;
    }

    if (client == null) {
      return dummyTerm(key);
    }

    Term term = _terms.get(key.toLowerCase());

    if (term != null) {
      return term;
    }

    term = lookupTerm(client, key);

    if (term != null) {
      return term;
    }

    logger.warn("type [" + _type + "] key [" + key + "] mapping: no cache or lookup results");

    return null;
  }
}
