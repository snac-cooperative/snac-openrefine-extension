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
      String apiQuery =
          "{ \"command\": \"vocabulary\", \"query_string\": \"" + key + "\", \"type\": \"" + _type + "\" }";

      SNACAPIResponse lookupResponse = client.post(apiQuery);

      JSONParser jp = new JSONParser();
      JSONArray results =
          (JSONArray) ((JSONObject) jp.parse(lookupResponse.getAPIResponse())).get("results");

      if (results.size() <= 0) {
        logger.error("vocabulary [" + _type + "] query returned no results for term: [" + key + "]");
        return null;
      }

      for (int i = 0; i < results.size(); i++) {
        JSONObject result = (JSONObject) results.get(i);

        String gotTerm = (String) result.get("term");

        if (!gotTerm.toLowerCase().equals(key.toLowerCase())) {
          //logger.info("vocabulary [" + _type + "] result term [" + gotTerm + "] does not match term: [" + key + "]");
          continue;
        }

        //logger.info("vocabulary [" + _type + "] result term [" + gotTerm + "] matches term: [" + key + "]");

        //Integer gotID = Integer.parseInt((String) result.get("id"));
        String gotType = (String) result.get("type");
        String gotDesc = (String) result.get("description");

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
    } catch (ParseException e) {
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
      //logger.info("type [" + _type + "] key [" + key + "] mapping: cached: { " + getDescription(term) + " }");
      return term;
    }

    term = lookupTerm(client, key);

    if (term != null) {
      //logger.info("type [" + _type + "] key [" + key + "] mapping: lookup: { " + getDescription(term) + " }");
      return term;
    }

    logger.warn("type [" + _type + "] key [" + key + "] mapping: no cache or lookup results");

    return null;
  }
}
