package org.snaccooperative.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.snaccooperative.model.SNACModelField;
import org.snaccooperative.model.SNACConstellationModel;
import org.snaccooperative.model.SNACResourceModel;
import org.snaccooperative.model.SNACRelationModel;

public class SNACModel {

  private SNACConstellationModel _constellationModel;
  private SNACResourceModel _resourceModel;
  private SNACRelationModel _relationModel;

  static final Logger logger = LoggerFactory.getLogger("SNACModel");

  public SNACModel() {
    _constellationModel = new SNACConstellationModel();
    _resourceModel = new SNACResourceModel();
    _relationModel = new SNACRelationModel();
  }

  @JsonProperty("constellation")
  public ArrayList<SNACModelField> getConstellationModelFields() {
    return _constellationModel.getFields();
  }

  @JsonProperty("resource")
  public ArrayList<SNACModelField> getResourceModelFields() {
    return _resourceModel.getFields();
  }

  @JsonProperty("relation")
  public ArrayList<SNACModelField> getRelationModelFields() {
    return _relationModel.getFields();
  }
}
