package org.snaccooperative.openrefine.preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNACEnvironment {

  static final Logger logger = LoggerFactory.getLogger(SNACEnvironment.class);

  private String _id;
  private String _name;
  private String _webURL;
  private String _apiURL;
  private String _apiKey;

  public SNACEnvironment(String id, String name, String webURL, String apiURL) {
    this._id = id;
    this._name = name;
    this._webURL = webURL;
    this._apiURL = apiURL;
    this._apiKey = "";
  }

  public SNACEnvironment(SNACEnvironment env) {
    this._id = env.getID();
    this._name = env.getName();
    this._webURL = env.getWebURL();
    this._apiURL = env.getAPIURL();
    this._apiKey = env.getAPIKey();
  }

  public Boolean isProd() {
    return _id.toLowerCase().equals("prod");
  }

  public String getID() {
    return _id;
  }

  public void setID(String id) {
    _id = id;
  }

  public String getName() {
    return _name;
  }

  public void setName(String name) {
    _name = name;
  }

  public String getWebURL() {
    return _webURL;
  }

  public void setWebURL(String webURL) {
    _webURL = webURL;
  }

  public String getAPIURL() {
    return _apiURL;
  }

  public void setAPIURL(String apiURL) {
    _apiURL = apiURL;
  }

  public String getAPIKey() {
    return _apiKey;
  }

  public void setAPIKey(String apiKey) {
    if (apiKey != null) {
      _apiKey = apiKey;
    }
  }
}
