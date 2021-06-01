package org.snaccooperative.commands;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.data.Constellation;
import org.snaccooperative.data.Resource;

public class SNACAPIResponse {

  static final Logger logger = LoggerFactory.getLogger("SNACAPIResponse");

  protected String _apiResponse;
  protected String _result;
  protected String _message;
  protected Resource _resource;
  protected Constellation _constellation;
  protected int _id;
  protected int _version;

  public SNACAPIResponse(String apiResponse) {
    this._apiResponse = apiResponse;

    // attempt to parse json; if it does not parse, it's either
    // a badly-formed response or (most likely) an exception

    try {
      JSONParser jsonParser = new JSONParser();
      Object parseResult = jsonParser.parse(apiResponse);

      if (!(parseResult instanceof JSONObject)) {
        this._result = "failure";
        this._message = apiResponse;
        return;
      }

      JSONObject jsonResponse = (JSONObject) parseResult;

      Object result = jsonResponse.get("result");
      Object message = jsonResponse.get("message");
      Object error = jsonResponse.get("error");
      Object resource = jsonResponse.get("resource");
      Object constellation = jsonResponse.get("constellation");
      Object results = jsonResponse.get("results");

      // populate result/message
      if (result instanceof String) {
        // result = "success", "success-notice", etc. -- could contain optional message
        this._result = result.toString();
        if (message instanceof JSONObject) {
          this._message = ((JSONObject) message).get("text").toString();
        }
      } else {
        if (error instanceof JSONObject) {
          // result = "error", plus any error type/message
          String errorType = ((JSONObject) error).get("type").toString();
          String errorMessage = ((JSONObject) error).get("message").toString();
          String errorFull = "";

          if (errorType != "") {
            errorFull = errorType;
            if (errorMessage != "") {
              errorFull += " - " + errorMessage;
            }
          } else {
            errorFull = errorMessage;
          }

          this._result = "error";
          this._message = errorFull;
        } else {
          if (results instanceof JSONObject) {
            // things like vocabulary lookups may just have a "results" section
            this._result = "success";
          } else {
            this._result = "unknown";
            this._message = "Unable to determine result";
          }
        }
      }

      // populate any resource/constellation, plus id/version

      if (resource instanceof JSONObject) {
        Resource res = Resource.fromJSON(((JSONObject) resource).toString());
        this._resource = res;
        this._id = res.getID();
        this._version = res.getVersion();
      } else if (constellation instanceof JSONObject) {
        Constellation con = Constellation.fromJSON(((JSONObject) constellation).toString());
        this._constellation = con;
        this._id = con.getID();
        this._version = con.getVersion();
      }
    } catch (ParseException e) {
      // assume apiResponse is an exception string, not a badly-formed API responsee
      this._result = "exception";
      this._message = apiResponse;
    }
  }

  public SNACAPIResponse(SNACAPIResponse other, String apiResponse) {
    // returns copy of existing API response, with its apiResponse field overridden with an
    // alternate value.
    // this is useful for scenarios such as constellation inserts (insert/publish), in which the
    // second
    // action (publish) has a less informative API response than the first (insert) when displayed
    // in the client.

    this._apiResponse = apiResponse;

    this._result = other.getResult();
    this._message = other.getMessage();
    this._resource = other.getResource();
    this._constellation = other.getConstellation();
    this._id = other.getID();
    this._version = other.getVersion();
  }

  public String getAPIResponse() {
    return _apiResponse;
  }

  public String getResult() {
    return _result;
  }

  public String getMessage() {
    return _message;
  }

  public int getID() {
    return _id;
  }

  public String getIDString() {
    if (_id > 0) {
      return Integer.toString(_id);
    }

    return "";
  }

  public int getVersion() {
    return _version;
  }

  public String getVersionString() {
    if (_version > 0) {
      return Integer.toString(_version);
    }

    return "";
  }

  public Resource getResource() {
    return _resource;
  }

  public Constellation getConstellation() {
    return _constellation;
  }
}
