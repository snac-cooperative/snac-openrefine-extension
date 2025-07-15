package org.snaccooperative.commands;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.data.Constellation;
import org.snaccooperative.data.Resource;

public class SNACAPIResponse {

  static final Logger logger = LoggerFactory.getLogger("SNACAPIResponse");

  protected String _apiResponse;
  protected String _result;
  protected String _message;
  protected String _uri;
  protected Resource _resource;
  protected Constellation _constellation;
  protected int _id;
  protected int _version;

  public SNACAPIResponse(SNACAPIClient client, String apiResponse) {
    this._apiResponse = apiResponse;
    this._result = "";
    this._message = "";
    this._uri = "";
    this._resource = null;
    this._constellation = null;
    this._id = 0;
    this._version = 0;

    // if supplied with an apiResponse of "success", create a simple success object
    if (apiResponse.equals("success")) {
      this._result = apiResponse;
      this._message = "";
      return;
    }

    // attempt to parse json; if it does not parse, it's either
    // a badly-formed response or (most likely) an exception

    try {
      JSONObject jsonResponse = new JSONObject(apiResponse);

      Object result = jsonResponse.get("result");
      Object message = jsonResponse.get("message");
      Object error = jsonResponse.get("error");
      Object resource = jsonResponse.get("resource");
      Object constellation = jsonResponse.get("constellation");
      Object results = jsonResponse.get("results");
      Object relatedConstellations = jsonResponse.get("related_constellations");

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
          // in the absence of other evidence, the presence of some fields can indicate success:
          // "results": returned by vocabulary lookups or elasticsearch queries
          // "related_constellations": returned by a "read_resource" for a non-existent resource
          if ((results instanceof JSONObject) || (relatedConstellations instanceof JSONArray)) {
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
        this._uri = client.urlForResourceID(res.getID());
      } else if (constellation instanceof JSONObject) {
        Constellation con = Constellation.fromJSON(((JSONObject) constellation).toString());
        this._constellation = con;
        this._id = con.getID();
        this._version = con.getVersion();

        // only use ARK for prod CPF, since they are only valid there
        if (client.isProd()) {
          this._uri = con.getArk();
        } else {
          this._uri = client.urlForConstellationID(con.getID());
        }
      }
    } catch (JSONException e) {
      // assume apiResponse is an exception string, not a badly-formed API response
      this._result = "exception";
      this._message = apiResponse;
    }
  }

  public SNACAPIResponse(SNACAPIResponse other, String apiResponse) {
    // returns copy of existing API response, with its apiResponse field overridden
    // with an alternate value.  this is useful for scenarios such as constellation
    // inserts (insert/publish), in which the second action (publish) has a less
    // informative API response than the first (insert) when displayed in the client.

    this._apiResponse = apiResponse;

    this._result = other.getResult();
    this._message = other.getMessage();
    this._resource = other.getResource();
    this._constellation = other.getConstellation();
    this._id = other.getID();
    this._uri = other.getURI();
    this._version = other.getVersion();
  }

  public String getAPIResponse() {
    return _apiResponse;
  }

  public String getURI() {
    return _uri;
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

  public Boolean isSuccess() {
    return _result.toLowerCase().contains("success");
  }
}
