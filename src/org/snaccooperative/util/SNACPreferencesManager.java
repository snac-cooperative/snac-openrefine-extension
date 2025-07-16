package org.snaccooperative.util;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNACPreferencesManager {

  static final Logger logger = LoggerFactory.getLogger("SNACPreferencesManager");

  // deprecated; existing value will be migrated on load of new preference keys
  private static final String LEGACY_PREF_STORE_KEY = "snac_apikey";
  private static final String LEGACY_PREF_STORE_APIKEY_KEY = "apikey";

  private static final String PREF_STORE_KEY_ENVIRONMENT = "snac.environment";
  private static final String PREF_STORE_KEY_APIKEY_PREFIX = "snac.apikey.";
  private static final String PREF_STORE_KEY_MAX_PREVIEW_ITEMS = "snac.preview.max_items";
  private static final String PREF_STORE_KEY_INCLUDE_API_RESPONSE = "snac.upload.api_response";

  private static final Integer SNAC_DEFAULT_MAX_PREVIEW_ITEMS = 10;
  private static final Boolean SNAC_DEFAULT_INCLUDE_API_RESPONSE = false;

  private static final String SNAC_ENV_DEV_ID = "dev";
  private static final String SNAC_ENV_DEV_NAME = "Development";
  private static final String SNAC_ENV_DEV_WEB_URL = "https://snac-dev.iath.virginia.edu/";
  private static final String SNAC_ENV_DEV_API_URL = "https://snac-dev.iath.virginia.edu/api/";

  private static final String SNAC_ENV_PROD_ID = "prod";
  private static final String SNAC_ENV_PROD_NAME = "Production";
  private static final String SNAC_ENV_PROD_WEB_URL = "https://snaccooperative.org/";
  private static final String SNAC_ENV_PROD_API_URL = "https://api.snaccooperative.org/";

  private String _id;
  private SNACEnvironment _dev;
  private SNACEnvironment _prod;
  private Integer _maxPreviewItems;
  private Boolean _includeAPIResponse;

  private HashMap<String, String> _current;
  private HashMap<String, SNACEnvironment> _env;

  private PreferenceStore prefStore;

  private static final SNACPreferencesManager instance = new SNACPreferencesManager();

  public static SNACPreferencesManager getInstance() {
    return instance;
  }

  private SNACPreferencesManager() {
    prefStore = ProjectManager.singleton.getPreferenceStore();

    _dev =
        new SNACEnvironment(
            SNAC_ENV_DEV_ID, SNAC_ENV_DEV_NAME, SNAC_ENV_DEV_WEB_URL, SNAC_ENV_DEV_API_URL);

    _prod =
        new SNACEnvironment(
            SNAC_ENV_PROD_ID, SNAC_ENV_PROD_NAME, SNAC_ENV_PROD_WEB_URL, SNAC_ENV_PROD_API_URL);

    _env = new HashMap<String, SNACEnvironment>();
    _env.put(SNAC_ENV_DEV_ID, _dev);
    _env.put(SNAC_ENV_PROD_ID, _prod);

    loadPreferences();
  }

  private void logPreferences() {
    // read from openrefine preferences store
    logger.info("stored id: [" + (String) prefStore.get(PREF_STORE_KEY_ENVIRONMENT) + "]");
    for (String key : _env.keySet()) {
      logger.info(
          "stored env ["
              + key
              + "] api key = ["
              + (String) prefStore.get(PREF_STORE_KEY_APIKEY_PREFIX + key)
              + "]");
    }

    logger.info("local id: [" + _id + "]");
    for (String key : _env.keySet()) {
      logger.info("local env [" + key + "] api key = [" + _env.get(key).getAPIKey() + "]");
    }
  }

  private Object valueWithFallback(Object val, Object fallback) {
    if (val == null) {
      return fallback;
    }
    return val;
  }

  private String getLegacyKey() {
    ArrayNode array = (ArrayNode) prefStore.get(LEGACY_PREF_STORE_KEY);

    if (array != null && array.size() > 0 && array.get(0) instanceof ObjectNode) {
      return array.get(0).get(LEGACY_PREF_STORE_APIKEY_KEY).asText().trim();
    }

    return "";
  }

  private void loadPreferences() {
    // read from openrefine preferences store
    // logger.info("loading preferences...");

    // get legacy api key preference to use as fallback when new preferences haven't been set
    String legacyKey = getLegacyKey();

    _id = (String) valueWithFallback(prefStore.get(PREF_STORE_KEY_ENVIRONMENT), _dev.getID());
    for (String key : _env.keySet()) {
      // pre-populate old api key as long as new preferences haven't been saved.
      // old api key was not environment-specific so populate it everywhere and let the user fix
      _env.get(key)
          .setAPIKey(
              (String)
                  valueWithFallback(prefStore.get(PREF_STORE_KEY_APIKEY_PREFIX + key), legacyKey));
    }

    _maxPreviewItems =
        (Integer)
            valueWithFallback(
                prefStore.get(PREF_STORE_KEY_MAX_PREVIEW_ITEMS), SNAC_DEFAULT_MAX_PREVIEW_ITEMS);

    _includeAPIResponse =
        (Boolean)
            valueWithFallback(
                prefStore.get(PREF_STORE_KEY_INCLUDE_API_RESPONSE),
                SNAC_DEFAULT_INCLUDE_API_RESPONSE);

    // logPreferences();
  }

  public void savePreferences(
      String id,
      String devKey,
      String prodKey,
      String maxPreviewItemsStr,
      String includeAPIResponseStr) {
    // logger.info("saving preferences...");

    // only check the ones that must be non-null
    if (id == null || devKey == null || prodKey == null || maxPreviewItemsStr == null) {
      logger.warn("cannot save preferences: some values are null");
      return;
    }

    Integer maxPreviewItems;
    try {
      maxPreviewItems = Integer.valueOf(maxPreviewItemsStr);
    } catch (NumberFormatException e) {
      logger.warn(
          "cannot save preferences: invalid value for maxPreviewItems: ["
              + maxPreviewItemsStr
              + "]");
      return;
    }

    Boolean includeAPIResponse;
    try {
      includeAPIResponse = Boolean.valueOf(includeAPIResponseStr);
    } catch (NumberFormatException e) {
      logger.warn(
          "cannot save preferences: invalid value for includeAPIResponse: ["
              + includeAPIResponseStr
              + "]");
      return;
    }

    // save new info locally
    _id = id.trim().toLowerCase();
    _env.get(SNAC_ENV_DEV_ID).setAPIKey(devKey.trim());
    _env.get(SNAC_ENV_PROD_ID).setAPIKey(prodKey.trim());
    _maxPreviewItems = maxPreviewItems;
    _includeAPIResponse = includeAPIResponse;

    // write to openrefine preferences store
    prefStore.put(PREF_STORE_KEY_ENVIRONMENT, _id);
    for (String key : _env.keySet()) {
      prefStore.put(PREF_STORE_KEY_APIKEY_PREFIX + key, _env.get(key).getAPIKey());
    }
    prefStore.put(PREF_STORE_KEY_MAX_PREVIEW_ITEMS, _maxPreviewItems);
    prefStore.put(PREF_STORE_KEY_INCLUDE_API_RESPONSE, _includeAPIResponse);

    // remove old legacy api key preference now that user has saved new preferences
    prefStore.put(LEGACY_PREF_STORE_KEY, null);

    // logPreferences();
  }

  public String[] getIDs() {
    return _env.keySet().toArray(new String[0]);
  }

  public String getID() {
    return getID(_id);
  }

  public String getID(String id) {
    return _env.get(id).getID();
  }

  public String getDevID() {
    return getID(SNAC_ENV_DEV_ID);
  }

  public String getProdID() {
    return getID(SNAC_ENV_PROD_ID);
  }

  public String getName() {
    return getName(_id);
  }

  public String getName(String id) {
    return _env.get(id).getName();
  }

  public String getDevName() {
    return getName(SNAC_ENV_DEV_ID);
  }

  public String getProdName() {
    return getName(SNAC_ENV_PROD_ID);
  }

  public String getAPIKey() {
    return getAPIKey(_id);
  }

  public String getAPIKey(String id) {
    return _env.get(id).getAPIKey();
  }

  public String getDevAPIKey() {
    return getAPIKey(SNAC_ENV_DEV_ID);
  }

  public String getProdAPIKey() {
    return getAPIKey(SNAC_ENV_PROD_ID);
  }

  public String getWebURL() {
    return getWebURL(_id);
  }

  public String getWebURL(String id) {
    return _env.get(id).getWebURL();
  }

  public String getDevWebURL() {
    return getWebURL(SNAC_ENV_DEV_ID);
  }

  public String getProdWebURL() {
    return getWebURL(SNAC_ENV_PROD_ID);
  }

  public String getAPIURL() {
    return getAPIURL(_id);
  }

  public String getAPIURL(String id) {
    return _env.get(id).getAPIURL();
  }

  public String getDevAPIURL() {
    return getAPIURL(SNAC_ENV_DEV_ID);
  }

  public String getProdAPIURL() {
    return getAPIURL(SNAC_ENV_PROD_ID);
  }

  public Boolean isProd() {
    return isProd(_id);
  }

  public Boolean isProd(String id) {
    return _env.get(id).isProd();
  }

  public Boolean isDevProd() {
    return isProd(SNAC_ENV_DEV_ID);
  }

  public Boolean isProdProd() {
    return isProd(SNAC_ENV_PROD_ID);
  }

  public SNACEnvironment getEnvironment() {
    return getEnvironment(_id);
  }

  public SNACEnvironment getEnvironment(String id) {
    return _env.get(id);
  }

  public SNACEnvironment getDevEnvironment() {
    return getEnvironment(SNAC_ENV_DEV_ID);
  }

  public SNACEnvironment getProdEnvironment() {
    return getEnvironment(SNAC_ENV_PROD_ID);
  }

  public Integer getMaxPreviewItems() {
    return _maxPreviewItems;
  }

  public Boolean includeAPIResponse() {
    return _includeAPIResponse;
  }
}
