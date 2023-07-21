package org.snaccooperative.exporters;

import static org.snaccooperative.schema.SNACSchemaUtilities.getCellValueForRowByColumnName;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.commands.SNACAPIClient;
import org.snaccooperative.commands.SNACAPIResponse;
import org.snaccooperative.data.Activity;
import org.snaccooperative.data.BiogHist;
import org.snaccooperative.data.Constellation;
import org.snaccooperative.data.ConstellationRelation;
import org.snaccooperative.data.NameEntry;
import org.snaccooperative.data.Occupation;
import org.snaccooperative.data.Place;
import org.snaccooperative.data.Resource;
import org.snaccooperative.data.ResourceRelation;
import org.snaccooperative.data.SNACDate;
import org.snaccooperative.data.SameAs;
import org.snaccooperative.data.Source;
import org.snaccooperative.data.Subject;
import org.snaccooperative.data.Term;
import org.snaccooperative.schema.SNACSchema;

public class SNACConstellationItem extends SNACUploadItem {

  static final Logger logger = LoggerFactory.getLogger("SNACConstellationItem");

  protected Project _project;
  protected SNACSchema _schema;
  protected SNACAPIClient _client;
  protected SNACLookupCache _cache;
  protected Constellation _constellation;
  protected int _rowIndex;
  protected List<Integer> _relatedConstellations;
  protected List<Integer> _relatedResources;

  public SNACConstellationItem(
      Project project,
      SNACSchema schema,
      SNACAPIClient client,
      SNACLookupCache cache,
      Record record) {
    this._schema = schema;
    this._client = client;
    this._cache = cache;
    this._rowIndex = record.fromRowIndex;

    Constellation con = new Constellation();
    con.setOperation("insert");

    // things to accumulate
    List<NameEntry> nameEntries = new LinkedList<NameEntry>();
    List<SNACDate> dates = new LinkedList<SNACDate>();
    List<Subject> subjects = new LinkedList<Subject>();
    List<Place> places = new LinkedList<Place>();
    List<Source> sources = new LinkedList<Source>();
    List<Occupation> occupations = new LinkedList<Occupation>();
    List<Activity> activities = new LinkedList<Activity>();
    List<BiogHist> biogHists = new LinkedList<BiogHist>();
    List<SameAs> sameAsRelations = new LinkedList<SameAs>();
    List<ResourceRelation> resourceRelations = new LinkedList<ResourceRelation>();
    List<ConstellationRelation> cpfRelations = new LinkedList<ConstellationRelation>();

    _relatedConstellations = new LinkedList<Integer>();
    _relatedResources = new LinkedList<Integer>();

    for (Map.Entry<String, String> entry : schema.getColumnMappings().entrySet()) {
      String csvColumn = entry.getKey();
      String snacField = entry.getValue().toLowerCase();

      for (int i = record.fromRowIndex; i < record.toRowIndex; i++) {
        Row row = project.rows.get(i);

        String cellValue = getCellValueForRowByColumnName(project, row, csvColumn);

        if (cellValue.equals("")) {
          continue;
        }

        switch (snacField) {
          case "snac cpf id":
            try {
              int id = Integer.parseInt(cellValue);
              con.setID(id);
            } catch (NumberFormatException e) {
            }
            continue;

          case "cpf type":
            Term entityTypeTerm = createEntityTypeTerm(cellValue);

            if (entityTypeTerm == null) {
              continue;
            }

            con.setEntityType(entityTypeTerm);

            continue;

          case "name entry":
            NameEntry name = new NameEntry();
            name.setOriginal(cellValue);
            name.setPreferenceScore(99); // TODO: Make only the first NE preferred?
            name.setOperation("insert");

            nameEntries.add(name);

            continue;

          case "exist date":
            // find and add required associated exist date type in this row
            String dateTypeColumn = schema.getReverseColumnMappings().get("exist date type");

            if (dateTypeColumn == null) {
              logger.warn("no exist date type column found");
              continue;
            }

            String dateType =
                getCellValueForRowByColumnName(project, row, dateTypeColumn).toLowerCase();

            if (dateType == "") {
              logger.warn("no matching exist date type for date: [" + cellValue + "]");
              continue;
            }

            Term dateTypeTerm = createDateTypeTerm(dateType);

            if (dateTypeTerm == null) {
              continue;
            }

            SNACDate date = new SNACDate();
            date.setDate(cellValue, cellValue, dateTypeTerm);
            date.setOperation("insert");

            dates.add(date);

            continue;

          case "exist date type": // queried alongside "exist date"
            continue;

          case "subject":
            Term subjectTerm = new Term();
            subjectTerm.setType("subject");
            subjectTerm.setTerm(cellValue);

            Subject subject = new Subject();
            subject.setTerm(subjectTerm);
            subject.setOperation("insert");

            subjects.add(subject);

            continue;

          case "place":
            // Init AssociatedPlace Term
            Term associated = new Term();
            associated.setType("place_match"); // TODO: fix place
            associated.setID(705); // TODO: fix place
            associated.setTerm("AssociatedPlace"); // TODO: fix place

            // Set the value of the place via a term. //
            Place place = new Place();
            place.setOriginal(cellValue);
            place.setType(associated);
            place.setOperation("insert");

            // find and add optional associated place role in this row
            String placeRoleColumn = schema.getReverseColumnMappings().get("place role");

            if (placeRoleColumn != null) {
              String placeRole = getCellValueForRowByColumnName(project, row, placeRoleColumn);

              Term placeRoleTerm = createPlaceRoleTerm(placeRole);

              if (placeRoleTerm != null) {
                place.setRole(placeRoleTerm);
              }
            }

            places.add(place);

            continue;

          case "place role": // queried alongside "place"
            continue;

          case "source citation":
            Source source = new Source();

            // set citation
            source.setCitation(cellValue);

            // set url
            String urlColumn = schema.getReverseColumnMappings().get("source citation url");
            if (urlColumn != null) {
              String url = getCellValueForRowByColumnName(project, row, urlColumn);
              source.setURI(url);
            }

            // set found data
            String foundColumn =
                schema.getReverseColumnMappings().get("source citation found data");
            if (foundColumn != null) {
              String foundData = getCellValueForRowByColumnName(project, row, foundColumn);
              source.setText(foundData);
            }

            source.setOperation("insert");
            sources.add(source);

            continue;

          case "source citation url": // queried alongside "source citation"
            continue;

          case "source citation found data": // queried alongside "source citation"
            continue;

          case "occupation":
            Term occupationTerm = new Term();
            occupationTerm.setType("occupation");
            occupationTerm.setTerm(cellValue);

            Occupation occupation = new Occupation();
            occupation.setTerm(occupationTerm);
            occupation.setOperation("insert");

            occupations.add(occupation);

            continue;

          case "activity":
            Term activityTerm = new Term();
            activityTerm.setType("activity");
            activityTerm.setTerm(cellValue);

            Activity activity = new Activity();
            activity.setTerm(activityTerm);
            activity.setOperation("insert");

            activities.add(activity);

            continue;

          case "bioghist":
            BiogHist biogHist = new BiogHist();
            biogHist.setText(cellValue);
            biogHist.setOperation("insert");

            biogHists.add(biogHist);

            continue;

          case "external related cpf url":
            SameAs sameAs = new SameAs();
            sameAs.setURI(cellValue);
            sameAs.setOperation("insert");

            Term sameAsTerm = new Term();
            sameAsTerm.setID(28225);
            sameAs.setType(sameAsTerm);

            sameAsRelations.add(sameAs);

            continue;

          case "snac resource id":
            int targetResource = Integer.parseInt(cellValue);
            _relatedResources.add(targetResource);

            Resource resource = new Resource();
            resource.setID(targetResource);

            ResourceRelation resourceRelation = new ResourceRelation();
            resourceRelation.setOperation("insert");
            resourceRelation.setResource(resource);

            // find and add optional associated 'cpf to resource relation type' in this row
            String resourceRoleColumn =
                schema.getReverseColumnMappings().get("cpf to resource relation type");

            if (resourceRoleColumn != null) {
              String resourceRole =
                  getCellValueForRowByColumnName(project, row, resourceRoleColumn);

              Term resourceRoleTerm = createResourceRoleTerm(resourceRole);

              if (resourceRoleTerm != null) {
                resourceRelation.setRole(resourceRoleTerm);
              }
            }

            resourceRelations.add(resourceRelation);

            continue;

          case "cpf to resource relation type": // queried alongside "resource id"
            continue;

          case "related snac cpf id":
            int targetConstellation = Integer.parseInt(cellValue);
            _relatedConstellations.add(targetConstellation);

            ConstellationRelation cpfRelation = new ConstellationRelation();
            cpfRelation.setSourceConstellation(con.getID());
            cpfRelation.setTargetConstellation(targetConstellation);

            // Get Relation Type.
            String cpfRelationTypeColumn =
                schema.getReverseColumnMappings().get("cpf to cpf relation type");

            if (cpfRelationTypeColumn != null) {
              String cpfRelationType =
                  getCellValueForRowByColumnName(project, row, cpfRelationTypeColumn);
              Term cpfRelationTypeTerm = createRelationTypeTerm(cpfRelationType);
              cpfRelation.setType(cpfRelationTypeTerm);
            }
            cpfRelation.setOperation("insert");
            cpfRelations.add(cpfRelation);
            continue;

          case "cpf to cpf relation type": // Queried alongside related snac cpf id
            break;

          default:
            continue;
        }
      }
    }

    // adjust dates
    List<SNACDate> dateList = new LinkedList<SNACDate>();

    switch (dates.size()) {
      case 0:
        break;

      case 2:
        // create range from original dates
        SNACDate from = dates.get(0);
        SNACDate to = dates.get(1);

        SNACDate range = new SNACDate();
        range.setRange(true);
        range.setFromDate(from.getFromDate(), from.getFromDate(), from.getFromType());
        range.setToDate(to.getFromDate(), from.getFromDate(), from.getFromType());
        range.setOperation("insert");

        dateList.add(range);

      default:
        // add as individual dates
        dateList = dates;
    }

    // add accumulated things
    con.setNameEntries(nameEntries);
    con.setSubjects(subjects);
    con.setPlaces(places);
    con.setOccupations(occupations);
    con.setActivities(activities);
    con.setBiogHists(biogHists);
    con.setSameAsRelations(sameAsRelations);
    con.setResourceRelations(resourceRelations);
    con.setRelations(cpfRelations);
    con.setDateList(dateList);
    con.setSources(sources);

    this._constellation = con;

    logger.debug("built constellation: [" + this.toJSON() + "]");
  }

  public String getPreviewText() {
    String preview = "";

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacText = entry.getValue();
      String snacField = snacText.toLowerCase();

      switch (snacField) {
        case "snac cpf id":
          preview += snacText + ": " + _constellation.getID() + "\n";
          break;

        case "cpf type":
          Term previewTerm = _constellation.getEntityType();
          if (previewTerm != null) {
            preview += snacText + ": " + previewTerm.getTerm() + " (" + previewTerm.getID() + ")\n";
          }
          break;

        case "name entry":
          preview += snacText + ": " + _constellation.getNameEntries() + "\n";
          break;

        case "exist date":
          preview += snacText + ": " + _constellation.getDateList() + "\n";
          break;

        case "subject":
          preview += snacText + ": " + _constellation.getSubjects() + "\n";
          break;

        case "place":
          preview += snacText + ": " + _constellation.getPlaces() + "\n";
          // TODO: Display place_role
          break;

        case "occupation":
          preview += snacText + ": " + _constellation.getOccupations() + "\n";
          break;

        case "activity":
          preview += snacText + ": " + _constellation.getActivities() + "\n";
          break;

        case "bioghist":
          preview += snacText + ": " + _constellation.getBiogHists() + "\n";
          break;

        case "source citation":
          preview += snacText + ": " + _constellation.getSources() + "\n";
          break;

        case "external related cpf url":
          preview += snacText + ": " + _constellation.getSameAsRelations() + "\n";
          break;

        case "related snac cpf id":
          preview += snacText + ": " + _constellation.getRelations() + "\n";
          break;

          // TODO: Add Resource ID, cpf to resource relation type.
          // TODO: Add Related CPFs
      }
    }

    return preview;
  }

  public int rowIndex() {
    return _rowIndex;
  }

  public String toJSON() {
    return Constellation.toJSON(this._constellation);
  }

  private SNACAPIResponse verifyRelatedIDs() {
    // Before uploading, we verify existence of any related CPF and resource
    // IDs in the selected SNAC environment.  This is because SNAC will
    // accept related CPF IDs that do not actually exist, then crash when
    // reloading the original CPF, leaving the original CPF in a locked state.
    // Invalid resource IDs may not cause the same kind of fatal error,
    // but we check them anyway to keep the data clean.

    // These existence checks should really be made in SNAC proper, but
    // it's easier (and effective) to perform them here for now.

    List<String> relationErrors = new LinkedList<String>();

    logger.info("verifying existence of related constellations...");

    for (int i = 0; i < _relatedConstellations.size(); i++) {
      int id = _relatedConstellations.get(i);
      logger.info("verifying existence of related constellation: " + id);
      if (!_cache.constellationExists(id)) {
        relationErrors.add("* Related CPF ID " + id + " not found in SNAC");
      }
    }

    logger.info("verifying existence of related resources...");

    for (int i = 0; i < _relatedResources.size(); i++) {
      int id = _relatedResources.get(i);
      logger.info("verifying existence of related resource: " + id);
      if (!_cache.resourceExists(id)) {
        relationErrors.add("* Related Resource ID " + id + " not found in SNAC");
      }
    }

    if (relationErrors.size() > 0) {
      String errMsg = String.join("\n\n", relationErrors);
      logger.warn("constellation upload error: [" + errMsg + "]");
      return new SNACAPIResponse(this._client, errMsg);
    }

    return null;
  }

  public SNACAPIResponse performUpload() {
    logger.info("preparing to upload constellation to SNAC...");

    // verify any related IDs before uploading
    SNACAPIResponse verifyErr = verifyRelatedIDs();
    if (verifyErr != null) {
      return verifyErr;
    }

    // so far so good, proceed with upload

    String apiStr = "\"apikey\": \"" + this._client.apiKey() + "\"";
    String apiQuery = "";

    int myID = _constellation.getID();

    if (myID > 0) {
      // update existing constellation (edit/update)

      // checkout current version

      logger.info("checking out existing constellation [" + myID + "]...");

      apiQuery = "{ \"command\": \"edit\", " + apiStr + ", \"constellationid\": " + myID + " }";

      SNACAPIResponse checkoutResponse = this._client.post(apiQuery);

      Constellation checkoutCon = checkoutResponse.getConstellation();

      if (checkoutCon == null) {
        logger.error("error checking out constellation");
        return checkoutResponse;
      }

      // set update information

      this._constellation.setOperation("update");
      this._constellation.setID(checkoutCon.getID());
      this._constellation.setVersion(checkoutCon.getVersion());
      this._constellation.setArk(checkoutCon.getArk());
    }

    logger.info("uploading constellation...");

    // Perform insert or update and publish
    String constellationJSON = this.toJSON();

    apiQuery =
        "{ \"command\": \"insert_and_publish_constellation\", "
            + apiStr
            + ", \"constellation\": "
            + constellationJSON
            + " }";

    SNACAPIResponse updateResponse = this._client.post(apiQuery);

    logger.info("constellation upload complete");

    return updateResponse;
  }

  private Term createEntityTypeTerm(String entityType) {
    String term;
    int id;

    switch (entityType.toLowerCase()) {
      case "person":
        term = "person";
        id = 700;
        break;

      case "family":
        term = "family";
        id = 699;
        break;

      case "corporatebody":
        term = "corporateBody";
        id = 698;
        break;

      default:
        logger.warn("invalid/unhandled entity type: [" + entityType + "]");
        return null;
    }

    Term t = new Term();
    t.setType("entity_type");
    t.setTerm(term);
    t.setID(id);

    return t;
  }

  private Term createDateTypeTerm(String dateType) {
    String term;
    int id;

    switch (dateType.toLowerCase()) {
      case "active":
        term = "Active";
        id = 688;
        break;

      case "death":
        term = "Death";
        id = 690;
        break;

      case "birth":
        term = "Birth";
        id = 689;
        break;

      case "suspiciousdate":
        term = "SuspiciousDate";
        id = 691;
        break;

      case "establishment":
        term = "Establishment";
        id = 400484;
        break;

      case "disestablishment":
        term = "Disestablishment";
        id = 400485;
        break;

      default:
        logger.warn("invalid/unhandled date type: [" + dateType + "]");
        return null;
    }

    Term t = new Term();
    t.setType("date_type");
    t.setTerm(term);
    t.setID(id);

    return t;
  }

  private Term createPlaceRoleTerm(String placeRole) {
    String term;
    int id;

    switch (placeRole.toLowerCase()) {
      case "birth":
        term = "Birth";
        id = 400238;
        break;

      case "death":
        term = "Death";
        id = 400239;
        break;

      case "residence":
        term = "Residence";
        id = 400241;
        break;

      case "work":
        term = "Work";
        id = 400625;
        break;

      default:
        logger.warn("invalid/unhandled place role: [" + placeRole + "]");
        return null;
    }

    Term t = new Term();
    t.setType("place_role");
    t.setTerm(term);
    t.setID(id);

    return t;
  }

  private Term createResourceRoleTerm(String resourceRole) {
    String term;
    int id;

    switch (resourceRole.toLowerCase()) {
      case "creatorof":
        term = "creatorOf";
        id = 692;
        break;

      case "referencedin":
        term = "referencedIn";
        id = 693;
        break;

      case "editorof":
        term = "editorOf";
        id = 694;
        break;

      case "contributorof":
        term = "contributorOf";
        id = 695;
        break;

      default:
        logger.warn("invalid/unhandled cpf to resource relation type: [" + resourceRole + "]");
        return null;
    }

    Term t = new Term();
    t.setType("document_role");
    t.setTerm(term);
    t.setID(id);

    return t;
  }

  private Term createRelationTypeTerm(String relationType) {
    String term;
    int id;
    switch (relationType.toLowerCase()) {
      case "acquaintanceof":
        term = "acquaintanceOf";
        id = 28227;
        break;
      case "almamaterof":
        term = "almaMaterOf";
        id = 28229;
        break;
      case "alumnusoralumnaof":
        term = "alumnusOrAlumnaOf";
        id = 28230;
        break;
      case "ancestorof":
        term = "ancestorOf";
        id = 28232;
        break;
      case "associatedwith":
        term = "associatedWith";
        id = 28234;
        break;
      case "auntoruncleof":
        term = "auntOrUncleOf";
        id = 28236;
        break;
      case "biological parent of":
        term = "biological parent of";
        id = 28237;
        break;
      case "child-in-law of":
        term = "child-in-law of";
        id = 28238;
        break;
      case "childof":
        term = "childOf";
        id = 28239;
        break;
      case "conferredhonorsto":
        term = "conferredHonorsTo";
        id = 28240;
        break;
      case "correspondedwith":
        term = "correspondedWith";
        id = 28243;
        break;
      case "createdby":
        term = "createdBy";
        id = 28245;
        break;
      case "creatorof":
        term = "creatorOf";
        id = 28246;
        break;
      case "descendantof":
        term = "descendantOf";
        id = 28248;
        break;
      case "employeeof":
        term = "employeeOf";
        id = 28250;
        break;
      case "employerof":
        term = "employerOf";
        id = 28251;
        break;
      case "foundedby":
        term = "foundedBy";
        id = 28253;
        break;
      case "founderof":
        term = "founderOf";
        id = 28254;
        break;
      case "grandchildof":
        term = "grandchildOf";
        id = 28255;
        break;
      case "grandparentof":
        term = "grandparentOf";
        id = 28256;
        break;
      case "hashonorarymember":
        term = "hasHonoraryMember";
        id = 28260;
        break;
      case "hasmember":
        term = "hasMember";
        id = 28261;
        break;
      case "hierarchical-child":
        term = "hierarchical-child";
        id = 28263;
        break;
      case "hierarchical-parent":
        term = "hierarchical-parent";
        id = 28264;
        break;
      case "honorarymemberof":
        term = "honoraryMemberOf";
        id = 28265;
        break;
      case "honoredby":
        term = "honoredBy";
        id = 28266;
        break;
      case "investigatedby":
        term = "investigatedBy";
        id = 28267;
        break;
      case "investigatorof":
        term = "investigatorOf";
        id = 28268;
        break;
      case "leaderof":
        term = "leaderOf";
        id = 28269;
        break;
      case "memberof":
        term = "memberOf";
        id = 28271;
        break;
      case "nieceornephewof":
        term = "nieceOrNephewOf";
        id = 28272;
        break;
      case "ownerof":
        term = "ownerOf";
        id = 28274;
        break;
      case "parent-in-law of":
        term = "parent-in-law of";
        id = 28275;
        break;
      case "parentof":
        term = "parentOf";
        id = 28276;
        break;
      case "participantin":
        term = "participantIn";
        id = 28277;
        break;
      case "politicalopponentof":
        term = "politicalOpponentOf";
        id = 28279;
        break;
      case "predecessorof":
        term = "predecessorOf";
        id = 28280;
        break;
      case "relativeof":
        term = "relativeOf";
        id = 28281;
        break;
      case "sibling-in-law of":
        term = "sibling-in-law of";
        id = 28282;
        break;
      case "sibling of":
        term = "sibling of";
        id = 28283;
        break;
      case "spouseof":
        term = "spouseOf";
        id = 28284;
        break;
      case "subordinateof":
        term = "subordinateOf";
        id = 28290;
        break;
      case "sucessorof":
        term = "sucessorOf";
        id = 28291;
        break;
      case "hasfamilyrelationto":
        term = "hasFamilyRelationTo";
        id = 400456;
        break;
      case "issuccessorof":
        term = "isSuccessorOf";
        id = 400459;
        break;
      case "ownedby":
        term = "ownedBy";
        id = 400478;
        break;

      default:
        logger.warn("invalid/unhandled cpf to cpf relation type: [" + relationType + "]");
        term = "associatedWith";
        id = 28234;
    }

    Term t = new Term();
    t.setType("relation_type");
    t.setTerm(term);
    t.setID(id);

    return t;
  }
}
