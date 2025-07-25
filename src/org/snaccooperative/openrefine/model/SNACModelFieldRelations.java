package org.snaccooperative.openrefine.model;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNACModelFieldRelations<E extends Enum<E> & SNACModelFieldType> {

  protected ArrayList<SNACModelFieldRelation<E>> _relationList;

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

  public String getRelationsString() {
    ArrayList<String> relations = new ArrayList<String>();

    for (int i = 0; i < _relationList.size(); i++) {
      SNACModelFieldRelation<E> relation = _relationList.get(i);
      relations.add(relation.getFieldName() + " (" + relation.getRelationName() + ")");
    }

    return String.join(", ", relations);
  }
}
