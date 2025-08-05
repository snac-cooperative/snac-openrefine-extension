package org.snaccooperative.openrefine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACConstellationModel.ConstellationFieldType;
import org.snaccooperative.openrefine.model.SNACRelationModel.RelationFieldType;
import org.snaccooperative.openrefine.model.SNACResourceModel.ResourceFieldType;

@JsonPropertyOrder({"resource", "constellation", "relation"})
public class SNACModel {

  static final Logger logger = LoggerFactory.getLogger(SNACModel.class);

  private SNACConstellationModel _constellationModel;
  private SNACResourceModel _resourceModel;
  private SNACRelationModel _relationModel;

  public SNACModel() {
    _constellationModel = new SNACConstellationModel();
    _resourceModel = new SNACResourceModel();
    _relationModel = new SNACRelationModel();
  }

  @JsonProperty("constellation")
  public List<SNACModelField<ConstellationFieldType>> getConstellationModelFields() {
    return _constellationModel.getFields();
  }

  @JsonProperty("resource")
  public List<SNACModelField<ResourceFieldType>> getResourceModelFields() {
    return _resourceModel.getFields();
  }

  @JsonProperty("relation")
  public List<SNACModelField<RelationFieldType>> getRelationModelFields() {
    return _relationModel.getFields();
  }
}
