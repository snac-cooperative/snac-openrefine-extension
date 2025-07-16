package org.snaccooperative.openrefine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import org.apache.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
