package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACModelFieldRelation.FieldRelationType;

public class SNACModelFieldRelations<E extends Enum<E> & SNACModelFieldType> {

  private ArrayList<SNACModelFieldRelation<E>> _relationList;

  static final Logger logger = LoggerFactory.getLogger("SNACModelFieldRelations");

  SNACModelFieldRelations() {
    this(new ArrayList<SNACModelFieldRelation<E>>());
  }

  SNACModelFieldRelations(ArrayList<SNACModelFieldRelation<E>> relationList) {
    _relationList = relationList;
  }

  public ArrayList<SNACModelFieldRelation<E>> getRelations() {
    return _relationList;
  }

  public ArrayList<SNACModelFieldRelation<E>> getRequiredRelations() {
    ArrayList<SNACModelFieldRelation<E>> requiredRelations =
        new ArrayList<SNACModelFieldRelation<E>>();

    for (int i = 0; i < _relationList.size(); i++) {
      SNACModelFieldRelation<E> relation = _relationList.get(i);
      if (relation.getRelationType() == FieldRelationType.REQUIRED) {
        requiredRelations.add(relation);
      }
    }

    return requiredRelations;
  }

  public ArrayList<E> getRequiredRelationFieldTypes() {
    ArrayList<SNACModelFieldRelation<E>> requiredRelations = getRequiredRelations();
    ArrayList<E> requiredFieldTypes = new ArrayList<E>();

    for (int i = 0; i < requiredRelations.size(); i++) {
      requiredFieldTypes.add(requiredRelations.get(i).getFieldType());
    }

    return requiredFieldTypes;
  }
}
