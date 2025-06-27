package org.snaccooperative.commands;

import java.io.IOException;
import org.apache.http.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.util.SNACPreferencesManager;
import org.snaccooperative.util.SNACEnvironment;

public class SNACAPIClient {

  static final Logger logger = LoggerFactory.getLogger("SNACAPIClient");

  protected SNACEnvironment _env;

  protected CloseableHttpClient _client;
  protected HttpPost _post;

  public SNACAPIClient() {
    SNACPreferencesManager prefsManager = SNACPreferencesManager.getInstance();

    _env = prefsManager.getEnvironment();

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

  public SNACAPIResponse post(String apiJSON) {
    try {
      logger.debug("API POST data: [" + apiJSON + "]");
      StringEntity apiCasted = new StringEntity(apiJSON, "UTF-8");
      _post.setEntity(apiCasted);
      HttpResponse response = _client.execute(_post);
      String result = EntityUtils.toString(response.getEntity());
      return new SNACAPIResponse(this, result);
    } catch (IOException e) {
      logger.error(e.toString());
      return new SNACAPIResponse(this, e.toString());
    }
  }
}
