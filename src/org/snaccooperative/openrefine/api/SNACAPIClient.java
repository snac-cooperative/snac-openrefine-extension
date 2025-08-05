package org.snaccooperative.openrefine.api;

import java.io.IOException;
import org.apache.http.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.preferences.SNACEnvironment;
import org.snaccooperative.openrefine.preferences.SNACPreferencesManager;

public class SNACAPIClient {

  static final Logger logger = LoggerFactory.getLogger(SNACAPIClient.class);

  private SNACEnvironment _env;

  private CloseableHttpClient _client;
  private HttpPost _post;

  public SNACAPIClient() {
    this(null);
  }

  public SNACAPIClient(String env) {
    SNACPreferencesManager prefsManager = SNACPreferencesManager.getInstance();

    if (env == null) {
      _env = prefsManager.getEnvironment();
    } else {
      _env = prefsManager.getEnvironment(env);
    }

    this._client = HttpClientBuilder.create().build();
    this._post = new HttpPost(_env.getAPIURL());
  }

  public String id() {
    return _env.getID();
  }

  public String name() {
    return _env.getName();
  }

  public String webURL() {
    return _env.getWebURL();
  }

  public String apiURL() {
    return _env.getAPIURL();
  }

  public String apiKey() {
    return _env.getAPIKey();
  }

  public Boolean isProd() {
    return _env.isProd();
  }

  public String urlForConstellationID(int id) {
    return webURL() + "view/" + id;
  }

  public String urlForResourceID(int id) {
    return webURL() + "vocab_administrator/resources/" + id;
  }

  public SNACAPIResponse post(JSONObject req) {
    return post(req.toString());
  }

  public SNACAPIResponse post(String req) {
    try {
      // logger.debug("API POST data: [" + req + "]");
      StringEntity apiCasted = new StringEntity(req, "UTF-8");
      _post.setEntity(apiCasted);
      HttpResponse res = _client.execute(_post);
      String result = EntityUtils.toString(res.getEntity());
      // logger.debug("API response: [" + result + "]");
      return new SNACAPIResponse(this, result);
    } catch (IOException e) {
      logger.error(e.toString());
      return new SNACAPIResponse(this, e.toString());
    }
  }
}
