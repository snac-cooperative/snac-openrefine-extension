package org.snaccooperative.commands;

import java.io.IOException;
import org.apache.http.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNACAPIClient {

  static final Logger logger = LoggerFactory.getLogger("SNACAPIClient");

  protected DefaultHttpClient _client;
  protected HttpPost _post;

  public SNACAPIClient(String apiURL) {
    this._client = new DefaultHttpClient();
    this._post = new HttpPost(apiURL);
  }

  public SNACAPIResponse post(String apiJSON) {
    try {
      StringEntity apiCasted = new StringEntity(apiJSON, "UTF-8");
      _post.setEntity(apiCasted);
      HttpResponse response = _client.execute(_post);
      String result = EntityUtils.toString(response.getEntity());

      return new SNACAPIResponse(result);
    } catch (IOException e) {
      logger.error(e.toString());
      return new SNACAPIResponse(e.toString());
    }
  }
}
