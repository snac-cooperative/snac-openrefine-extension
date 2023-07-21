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
import org.snaccooperative.connection.SNACConnector;

public class SNACAPIClient {

  static final Logger logger = LoggerFactory.getLogger("SNACAPIClient");

  protected CloseableHttpClient _client;
  protected HttpPost _post;
  protected String _webURL;
  protected String _apiURL;
  protected String _apiKey;
  protected Boolean _isProd;

  public SNACAPIClient(String snacEnv) {
    SNACConnector keyManager = SNACConnector.getInstance();

    this._webURL = "";
    this._apiURL = "";
    this._apiKey = keyManager.getKey();
    this._isProd = false;

    switch (snacEnv.toLowerCase()) {
      case "prod":
        this._webURL = "https://snaccooperative.org/";
        this._apiURL = "https://api.snaccooperative.org/";
        this._isProd = true;
        break;

      case "dev":
        this._webURL = "https://snac-dev.iath.virginia.edu/";
        this._apiURL = "https://snac-dev.iath.virginia.edu/api/";
        this._isProd = false;
        break;
    }

    this._client = HttpClientBuilder.create().build();
    this._post = new HttpPost(this._apiURL);

    logger.debug("web url: [" + this._webURL + "]");
    logger.debug("api url: [" + this._apiURL + "]");
    logger.debug("api key: [" + this._apiKey + "]");
  }

  public String webURL() {
    return this._webURL;
  }

  public String apiURL() {
    return this._apiURL;
  }

  public String apiKey() {
    return this._apiKey;
  }

  public Boolean isProd() {
    return this._isProd;
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
