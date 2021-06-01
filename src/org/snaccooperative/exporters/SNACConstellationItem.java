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
import org.snaccooperative.data.BiogHist;
import org.snaccooperative.data.Constellation;
import org.snaccooperative.data.NameEntry;
import org.snaccooperative.data.Occupation;
import org.snaccooperative.data.Place;
import org.snaccooperative.data.Resource;
import org.snaccooperative.data.ResourceRelation;
import org.snaccooperative.data.SNACDate;
import org.snaccooperative.data.SNACFunction;
import org.snaccooperative.data.SameAs;
import org.snaccooperative.data.Subject;
import org.snaccooperative.data.Term;
import org.snaccooperative.schema.SNACSchema;

public class SNACConstellationItem extends SNACUploadItem {

  static final Logger logger = LoggerFactory.getLogger("SNACConstellationItem");

  protected Project _project;
  protected SNACSchema _schema;
  protected SNACLookupCache _cache;
  protected Constellation _constellation;
  protected int _rowIndex;

  public SNACConstellationItem(
      Project project, SNACSchema schema, SNACLookupCache cache, Record record) {
    this._schema = schema;
    this._cache = cache;
    this._rowIndex = record.fromRowIndex;

    Constellation con = new Constellation();
    con.setOperation("insert");

    // things to accumulate
    List<NameEntry> nameEntries = new LinkedList<NameEntry>();
    List<SNACDate> dates = new LinkedList<SNACDate>();
    List<Subject> subjects = new LinkedList<Subject>();
    List<Place> places = new LinkedList<Place>();
    List<Occupation> occupations = new LinkedList<Occupation>();
    List<SNACFunction> functions = new LinkedList<SNACFunction>();
    List<BiogHist> biogHists = new LinkedList<BiogHist>();
    List<SameAs> sameAsRelations = new LinkedList<SameAs>();
    List<ResourceRelation> resourceRelations = new LinkedList<ResourceRelation>();

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
          case "id":
            try {
              int id = Integer.parseInt(cellValue);
              con.setID(id);
            } catch (NumberFormatException e) {
            }
            continue;

          case "entity type":
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

          case "date":
            // find and add required associated date type in this row
            String dateTypeColumn = schema.getReverseColumnMappings().get("date type");

            if (dateTypeColumn == null) {
              logger.error("no date type column found");
              continue;
            }

            String dateType =
                getCellValueForRowByColumnName(project, row, dateTypeColumn).toLowerCase();

            if (dateType == "") {
              logger.error("no matching date type for date: [" + cellValue + "]");
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

          case "date type": // queried alongside "date"
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

          case "occupation":
            Term occupationTerm = new Term();
            occupationTerm.setType("occupation");
            occupationTerm.setTerm(cellValue);

            Occupation occupation = new Occupation();
            occupation.setTerm(occupationTerm);
            occupation.setOperation("insert");

            occupations.add(occupation);

            continue;

          case "function":
            Term functionTerm = new Term();
            functionTerm.setType("function");
            functionTerm.setTerm(cellValue);

            SNACFunction funcshn = new SNACFunction();
            funcshn.setTerm(functionTerm);
            funcshn.setOperation("insert");

            functions.add(funcshn);

            continue;

          case "bioghist":
            BiogHist biogHist = new BiogHist();
            biogHist.setText(cellValue);
            biogHist.setOperation("insert");

            biogHists.add(biogHist);

            continue;

          case "sameas relation":
            SameAs sameAs = new SameAs();
            sameAs.setURI(cellValue);
            sameAs.setOperation("insert");

            sameAsRelations.add(sameAs);

            continue;

          case "resource id":
            Resource resource = new Resource();
            resource.setID(Integer.parseInt(cellValue));

            ResourceRelation resourceRelation = new ResourceRelation();
            resourceRelation.setOperation("insert");
            resourceRelation.setResource(resource);

            // find and add optional associated resource role in this row
            String resourceRoleColumn = schema.getReverseColumnMappings().get("resource role");

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

          case "resource role": // queried alongside "resource id"
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
    con.setFunctions(functions);
    con.setBiogHists(biogHists);
    con.setSameAsRelations(sameAsRelations);
    con.setResourceRelations(resourceRelations);
    con.setDateList(dateList);

    this._constellation = con;
  }

  public String getPreviewText() {
    String preview = "";

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacText = entry.getValue();
      String snacField = snacText.toLowerCase();

      switch (snacField) {
        case "id":
          preview += snacText + ": " + _constellation.getID() + "\n";
          break;

        case "entity type":
          Term previewTerm = _constellation.getEntityType();
          if (previewTerm != null) {
            preview += snacText + ": " + previewTerm.getTerm() + " (" + previewTerm.getID() + ")\n";
          }
          break;

        case "name entry":
          preview += snacText + ": " + _constellation.getNameEntries() + "\n";
          break;

        case "date":
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

        case "function":
          preview += snacText + ": " + _constellation.getFunctions() + "\n";
          break;

        case "bioghist":
          preview += snacText + ": " + _constellation.getBiogHists() + "\n";
          break;

        case "sameas relation":
          preview += snacText + ": " + _constellation.getSameAsRelations() + "\n";
          break;

          // TODO: Add Resource ID, Resource Role.
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

  public SNACAPIResponse performUpload(String url, String key) {
    SNACAPIClient client = new SNACAPIClient(url);

    String apiKey = "\"apikey\":\"" + key + "\",";
    String apiQuery = "";

    int myID = _constellation.getID();

    if (myID > 0) {
      // update existing constellation (edit/update)

      // checkout current version

      apiQuery = "{\"command\": \"edit\",\n" + apiKey + "\n" + "\"constellationid\":" + myID + "}";

      SNACAPIResponse checkoutResponse = client.post(apiQuery);

      Constellation checkoutCon = checkoutResponse.getConstellation();

      if (checkoutCon == null) {
        return checkoutResponse;
      }

      // update

      this._constellation.setOperation("update");
      this._constellation.setID(checkoutCon.getID());
      this._constellation.setVersion(checkoutCon.getVersion());
      this._constellation.setArk(checkoutCon.getArk());

      String updateJSON = this.toJSON();

      apiQuery =
          "{\"command\": \"update_constellation\",\n"
              + apiKey
              + "\n\"constellation\":"
              + updateJSON.substring(0, updateJSON.length() - 1)
              + "}}";

      SNACAPIResponse updateResponse = client.post(apiQuery);

      return updateResponse;
    } else {
      // insert new constellation (insert/publish)

      // insert

      String insertJSON = this.toJSON();

      apiQuery =
          "{\"command\": \"insert_constellation\",\n"
              + apiKey
              + "\n\"constellation\":"
              + insertJSON.substring(0, insertJSON.length() - 1)
              + "}}";

      SNACAPIResponse insertResponse = client.post(apiQuery);

      Constellation insertCon = insertResponse.getConstellation();

      if (insertCon == null) {
        return insertResponse;
      }

      // publish

      Constellation publishCon = new Constellation();
      publishCon.setID(insertCon.getID());
      publishCon.setVersion(insertCon.getVersion());

      String publishJSON = Constellation.toJSON(publishCon);

      apiQuery =
          "{\"command\": \"publish_constellation\",\n"
              + apiKey
              + "\n\"constellation\":"
              + publishJSON.substring(0, publishJSON.length() - 1)
              + "}}";

      SNACAPIResponse publishResponse = client.post(apiQuery);

      // use API response from insert operation, as it contains more valuable info
      return new SNACAPIResponse(publishResponse, insertResponse.getAPIResponse());
    }
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
        logger.warn("createEntityTypeTerm(): invalid/unhandled entity type: [" + entityType + "]");
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
        logger.warn("createDateTypeTerm(): invalid/unhandled date type: [" + dateType + "]");
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
        logger.warn("createPlaceRoleTerm(): invalid/unhandled place role: [" + placeRole + "]");
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
        logger.warn(
            "createResourceRoleTerm(): invalid/unhandled resource role: [" + resourceRole + "]");
        return null;
    }

    Term t = new Term();
    t.setType("document_role");
    t.setTerm(term);
    t.setID(id);

    return t;
  }
}
