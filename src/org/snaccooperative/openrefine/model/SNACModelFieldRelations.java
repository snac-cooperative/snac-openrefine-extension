package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACModelFieldRelation.FieldRelationType;

public class SNACModelFieldRelations<E extends Enum<E> & SNACModelFieldType> {

  static final Logger logger = LoggerFactory.getLogger(SNACModelFieldRelations.class);

  private List<SNACModelFieldRelation<E>> _relationList;

  SNACModelFieldRelations() {
    this(new ArrayList<SNACModelFieldRelation<E>>());
  }

  SNACModelFieldRelations(List<SNACModelFieldRelation<E>> relationList) {
    _relationList = relationList;
  }

  public List<SNACModelFieldRelation<E>> getRelations() {
    return _relationList;
  }

  public List<SNACModelFieldRelation<E>> getRequiredRelations() {
    List<SNACModelFieldRelation<E>> requiredRelations = new ArrayList<SNACModelFieldRelation<E>>();

    for (int i = 0; i < _relationList.size(); i++) {
      SNACModelFieldRelation<E> relation = _relationList.get(i);
      if (relation.getRelationType() == FieldRelationType.REQUIRED) {
        requiredRelations.add(relation);
      }
    }

    return requiredRelations;
  }

  public List<E> getRequiredRelationFieldTypes() {
    List<SNACModelFieldRelation<E>> requiredRelations = getRequiredRelations();
    List<E> requiredFieldTypes = new ArrayList<E>();

    for (int i = 0; i < requiredRelations.size(); i++) {
      requiredFieldTypes.add(requiredRelations.get(i).getFieldType());
    }

    return requiredFieldTypes;
  }
}
