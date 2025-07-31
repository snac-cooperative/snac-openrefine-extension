package org.snaccooperative.openrefine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACConstellationModel.ConstellationModelField;
import org.snaccooperative.openrefine.model.SNACRelationModel.RelationModelField;
import org.snaccooperative.openrefine.model.SNACResourceModel.ResourceModelField;

@JsonPropertyOrder({"resource", "constellation", "relation"})
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
  public List<SNACModelField<ConstellationModelField>> getConstellationModelFields() {
    return _constellationModel.getFields();
  }

  @JsonProperty("resource")
  public List<SNACModelField<ResourceModelField>> getResourceModelFields() {
    return _resourceModel.getFields();
  }

  @JsonProperty("relation")
  public List<SNACModelField<RelationModelField>> getRelationModelFields() {
    return _relationModel.getFields();
  }
}
