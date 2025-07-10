package org.snaccooperative.exporters;

import static org.snaccooperative.schema.SNACSchemaUtilities.getCellValueForRowByColumnName;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import org.snaccooperative.data.Language;
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
  protected Record _record;
  protected SNACSchema _schema;
  protected SNACAPIClient _client;
  protected SNACLookupCache _cache;
  protected int _rowIndex;

  protected Constellation _constellation;
  protected List<Integer> _relatedConstellations;
  protected List<Integer> _relatedResources;
  protected List<String> _validationErrors;

  public SNACConstellationItem(
      Project project,
      SNACSchema schema,
      SNACAPIClient client,
      SNACLookupCache cache,
      Record record) {
    this._project = project;
    this._schema = schema;
    this._client = client;
    this._cache = cache;
    this._record = record;

    this._rowIndex = record.fromRowIndex;
  }

  private void buildConstellation() {
    _constellation = null;

    _relatedConstellations = new LinkedList<Integer>();
    _relatedResources = new LinkedList<Integer>();
    _validationErrors = new ArrayList<String>();

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
    List<Language> languages = new LinkedList<Language>();
    List<BiogHist> biogHists = new LinkedList<BiogHist>();
    List<SameAs> sameAsRelations = new LinkedList<SameAs>();
    List<ResourceRelation> resourceRelations = new LinkedList<ResourceRelation>();
    List<ConstellationRelation> cpfRelations = new LinkedList<ConstellationRelation>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String csvColumn = entry.getKey();
      String snacField = entry.getValue().toLowerCase();

      for (int i = _record.fromRowIndex; i < _record.toRowIndex; i++) {
        Row row = _project.rows.get(i);

        String cellValue = getCellValueForRowByColumnName(_project, row, csvColumn);

        if (cellValue.equals("")) {
          continue;
        }

        switch (snacField) {
          case "snac cpf id":
            try {
              int id = Integer.parseInt(cellValue);
              con.setID(id);
            } catch (NumberFormatException e) {
              _validationErrors.add("Invalid SNAC CPF ID: [" + cellValue + "]");
            }
            continue;

          case "cpf type":
            Term entityTypeTerm = _cache.getEntityTypeTerm(cellValue);

            if (entityTypeTerm == null) {
              _validationErrors.add("Invalid CPF Type: [" + cellValue + "]");
              continue;
            }

            con.setEntityType(entityTypeTerm);

            continue;

          case "name entry":
            NameEntry preferredName = new NameEntry();
            preferredName.setOriginal(cellValue);
            preferredName.setPreferenceScore(99);
            preferredName.setOperation("insert");

            nameEntries.add(preferredName);

            continue;

          case "variant name entry":
            NameEntry variantName = new NameEntry();
            variantName.setOriginal(cellValue);
            variantName.setPreferenceScore(0);
            variantName.setOperation("insert");

            nameEntries.add(variantName);

            continue;

          case "exist date":
            // find and add required associated exist date type in this row
            String dateTypeColumn = _schema.getReverseColumnMappings().get("exist date type");

            if (dateTypeColumn == null) {
              logger.warn("no exist date type column found");
              _validationErrors.add("Missing required Exist Date Type column for Exist Date: [" + cellValue + "]");
              continue;
            }

            String dateType =
                getCellValueForRowByColumnName(_project, row, dateTypeColumn);

            if (dateType == "") {
              logger.warn("no matching exist date type for date: [" + cellValue + "]");
              _validationErrors.add("Invalid Exist Date Type: [" + dateType + "] for Exist Date: [" + cellValue + "]");
              continue;
            }

            Term dateTypeTerm = _cache.getDateTypeTerm(dateType);

            if (dateTypeTerm == null) {
              _validationErrors.add("Invalid Exist Date Type: [" + dateType + "] for Exist Date: [" + cellValue + "]");
              continue;
            }

            // find and add optional exist date descriptive note in this row
            String dateNoteColumn = _schema.getReverseColumnMappings().get("exist date descriptive note");

            String dateNote = "";
            if (dateNoteColumn != null) {
              dateNote = getCellValueForRowByColumnName(_project, row, dateNoteColumn);
            }

            SNACDate date = new SNACDate();
            date.setDate(cellValue, cellValue, dateTypeTerm);
            date.setNote(dateNote);
            date.setOperation("insert");

            dates.add(date);

            continue;

          case "exist date descriptive note": // queried alongside "exist date"
            continue;

          case "exist date type": // queried alongside "exist date"
            continue;

          case "subject":
            Term subjectTerm = _cache.getSubjectTerm(cellValue);

            if (subjectTerm == null) {
              _validationErrors.add("Invalid Subject: [" + cellValue + "]");
              continue;
            }

            Subject subject = new Subject();
            subject.setTerm(subjectTerm);
            subject.setOperation("insert");

            subjects.add(subject);

            continue;

          case "place":
            Place place = new Place();
            place.setOriginal(cellValue);
            place.setOperation("insert");

            // we need to supply a place type, so use this if no other is supplied
            String placeType = "AssociatedPlace";

            // find and add optional associated place type in this row
            String placeTypeColumn = _schema.getReverseColumnMappings().get("place type");
            if (placeTypeColumn != null) {
              placeType = getCellValueForRowByColumnName(_project, row, placeTypeColumn);
            }

            Term placeTypeTerm = _cache.getPlaceTypeTerm(placeType);

            if (placeTypeTerm != null) {
              place.setType(placeTypeTerm);
            } else {
              _validationErrors.add("Invalid Place Type: [" + placeType + "] for Place: [" + cellValue + "]");
              continue;
            }

            // find and add optional associated place role in this row
            String placeRoleColumn = _schema.getReverseColumnMappings().get("place role");

            if (placeRoleColumn != null) {
              String placeRole = getCellValueForRowByColumnName(_project, row, placeRoleColumn);

              Term placeRoleTerm = _cache.getPlaceRoleTerm(placeRole);

              if (placeRoleTerm != null) {
                place.setRole(placeRoleTerm);
              } else {
                _validationErrors.add("Invalid Place Role: [" + placeRole + "] for Place: [" + cellValue + "]");
                continue;
              }
            }

            places.add(place);

            continue;

          case "place role": // queried alongside "place"
            continue;

          case "place type": // queried alongside "place"
            continue;

          case "source citation":
            Source source = new Source();

            // set citation
            source.setCitation(cellValue);

            // set url
            String urlColumn = _schema.getReverseColumnMappings().get("source citation url");
            if (urlColumn != null) {
              String url = getCellValueForRowByColumnName(_project, row, urlColumn);
              source.setURI(url);
            }

            // set found data
            String foundColumn =
                _schema.getReverseColumnMappings().get("source citation found data");
            if (foundColumn != null) {
              String foundData = getCellValueForRowByColumnName(_project, row, foundColumn);
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
            Term occupationTerm = _cache.getOccupationTerm(cellValue);

            if (occupationTerm == null) {
              _validationErrors.add("Invalid Occupation: [" + cellValue + "]");
              continue;
            }

            Occupation occupation = new Occupation();
            occupation.setTerm(occupationTerm);
            occupation.setOperation("insert");

            occupations.add(occupation);

            continue;

          case "activity":
            Term activityTerm = _cache.getActivityTerm(cellValue);

            if (activityTerm == null) {
              _validationErrors.add("Invalid Activity: [" + cellValue + "]");
              continue;
            }

            Activity activity = new Activity();
            activity.setTerm(activityTerm);
            activity.setOperation("insert");

            activities.add(activity);

            continue;

          case "language code": // queried alongside script code
            // NOTE: SNAC language type can contain any combination of language code and/or script code.
            // Here, we check for the cases that contain a language code.

            Term languageCodeTerm = _cache.getLanguageCodeTerm(cellValue);

            if (languageCodeTerm == null) {
              logger.warn("skipping unknown language code [" + cellValue + "]");
              _validationErrors.add("Invalid Language Code: [" + cellValue + "]");
              continue;
            }

            // initialize language code portion
            Language lang = new Language();
            lang.setOperation("insert");
            lang.setLanguage(languageCodeTerm);

            // find and add optional associated script code in this row
            String scriptCodeColumn = _schema.getReverseColumnMappings().get("script code");

            if (scriptCodeColumn != null) {
              String scriptCode = getCellValueForRowByColumnName(_project, row, scriptCodeColumn);

              if (!scriptCode.equals("")) {
                Term scriptCodeTerm = _cache.getScriptCodeTerm(scriptCode);
                if (scriptCodeTerm != null) {
                  // add script code portion
                  lang.setScript(scriptCodeTerm);
                } else {
                  logger.warn("omitting invalid script code [" + scriptCode + "]");
                  _validationErrors.add("Invalid Script Code: [" + scriptCode + "] for Language Code: [" + cellValue + "]");
                  continue;
                }
              } else {
                //logger.info("no associated script code value found; skipping");
              }
            } else {
              //logger.info("no associated script code column found; skipping");
            }

            languages.add(lang);

            continue;

          case "script code": // queried alongside language code
            // NOTE: SNAC language type can contain any combination of language code and/or script code.
            // Here, we check for the case when there is just a script code.

            // check whether there is an associated language code in this row; if so, skip
            String languageCodeColumn = _schema.getReverseColumnMappings().get("language code");

            if (languageCodeColumn != null) {
              String languageCode = getCellValueForRowByColumnName(_project, row, languageCodeColumn);

              if (!languageCode.equals("")) {
                // found associated language code; this scenario is handled in the "language code" section
                //logger.info("skipping script code with associated language code");
                continue;
              } else {
                //logger.info("no associated language code value found; proceeding");
              }
            } else {
              //logger.info("no associated language code column found; proceeding");
            }

            Term scriptCodeTerm = _cache.getScriptCodeTerm(cellValue);

            if (scriptCodeTerm == null) {
              logger.warn("skipping unknown script code [" + cellValue + "]");
              _validationErrors.add("Invalid Script Code: [" + cellValue + "]");
              continue;
            }

            // initialize script code portion
            Language script = new Language();
            script.setOperation("insert");
            script.setScript(scriptCodeTerm);

            languages.add(script);

            continue;

          case "bioghist":
            BiogHist biogHist = new BiogHist();
            biogHist.setText(cellValue);
            biogHist.setOperation("insert");

            biogHists.add(biogHist);

            continue;

          case "external related cpf url":
            // external related CPF URLs are always sameAs relations
            String defaultExternalRelatedCPFUrlType = "sameAs";
            Term sameAsTerm = _cache.getRecordTypeTerm(defaultExternalRelatedCPFUrlType);

            if (sameAsTerm == null) {
              _validationErrors.add("Invalid Record Type: [" + defaultExternalRelatedCPFUrlType + "] for External Related CPF URL: [" + cellValue + "]");
              continue;
            }

            SameAs sameAs = new SameAs();
            sameAs.setURI(cellValue);
            sameAs.setType(sameAsTerm);
            sameAs.setOperation("insert");

            sameAsRelations.add(sameAs);

            continue;

          case "snac resource id":
            try {
              int targetResource = Integer.parseInt(cellValue);
              Resource resource = new Resource();
              resource.setID(targetResource);

              ResourceRelation resourceRelation = new ResourceRelation();
              resourceRelation.setOperation("insert");
              resourceRelation.setResource(resource);

              // find and add optional associated 'cpf to resource relation type' in this row
              String resourceRoleColumn =
                  _schema.getReverseColumnMappings().get("cpf to resource relation type");

              if (resourceRoleColumn != null) {
                String resourceRole =
                    getCellValueForRowByColumnName(_project, row, resourceRoleColumn);

                Term resourceRoleTerm = _cache.getDocumentRoleTerm(resourceRole);

                if (resourceRoleTerm != null) {
                  resourceRelation.setRole(resourceRoleTerm);
                } else {
                  _validationErrors.add("Invalid CPF to Resource Relation Type: [" + resourceRole + "] for SNAC Resource ID: [" + cellValue + "]");
                  continue;
                }
              }

              resourceRelations.add(resourceRelation);
              _relatedResources.add(targetResource);
            } catch (NumberFormatException e) {
              _validationErrors.add("Invalid Related SNAC CPF ID: [" + cellValue + "]");
              continue;
            }

            continue;

          case "cpf to resource relation type": // queried alongside "resource id"
            continue;

          case "related snac cpf id":
            try {
              int targetConstellation = Integer.parseInt(cellValue);
              ConstellationRelation cpfRelation = new ConstellationRelation();
              cpfRelation.setSourceConstellation(con.getID());
              cpfRelation.setTargetConstellation(targetConstellation);

              // Get Relation Type.
              String cpfRelationTypeColumn =
                  _schema.getReverseColumnMappings().get("cpf to cpf relation type");

              if (cpfRelationTypeColumn != null) {
                String cpfRelationType =
                    getCellValueForRowByColumnName(_project, row, cpfRelationTypeColumn);

                Term cpfRelationTypeTerm = _cache.getRelationTypeTerm(cpfRelationType);

                if (cpfRelationTypeTerm != null) {
                  cpfRelation.setType(cpfRelationTypeTerm);
                  cpfRelation.setOperation("insert");
                } else {
                  _validationErrors.add("Invalid CPF to CPF Relation Type: [" + cpfRelationType + "] for Related SNAC CPF ID: [" + cellValue + "]");
                  continue;
                }
              }

              cpfRelations.add(cpfRelation);
              _relatedConstellations.add(targetConstellation);
            } catch (NumberFormatException e) {
              _validationErrors.add("Invalid Related SNAC CPF ID: [" + cellValue + "]");
              continue;
            }

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
    con.setLanguagesUsed(languages);
    con.setBiogHists(biogHists);
    con.setSameAsRelations(sameAsRelations);
    con.setResourceRelations(resourceRelations);
    con.setRelations(cpfRelations);
    con.setDateList(dateList);
    con.setSources(sources);

    this._constellation = con;

    logger.debug("built constellation: [" + this.toJSON() + "]");
  }

  private void buildConstellationVerbatim() {
    _cache.disableTermCache();
    buildConstellation();
  }

  private void buildConstellationAgainstSNAC() {
    _cache.enableTermCache();
    buildConstellation();
  }

  public String getPreviewText() {
    buildConstellationVerbatim();

    Map<String, String> outFields = new TreeMap<>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacText = entry.getValue();
      String snacField = snacText.toLowerCase();

      switch (snacField) {
        case "cpf type":
          Term previewTerm = _constellation.getEntityType();
          if (previewTerm != null) {
            outFields.put(snacText, previewTerm.getTerm());
          }
          break;

        case "name entry":
          List<String> namesAndPreferenceScores = new ArrayList<String>();
          for (int i = 0; i < _constellation.getNameEntries().size(); i++) {
            NameEntry name = _constellation.getNameEntries().get(i);
            String nameAndPreferenceScore = name.toString();
            if (name.getPreferenceScore() == 99) {
              nameAndPreferenceScore += " (preferred)";
            } else {
              nameAndPreferenceScore += " (variant)";
            }
            namesAndPreferenceScores.add(nameAndPreferenceScore.replaceFirst("^Name Entry: ",""));
          }
          outFields.put(snacText, htmlOrderedList(namesAndPreferenceScores));
          break;

        case "exist date":
          List<String> dates = new ArrayList<String>();
          for (int i = 0; i < _constellation.getDateList().size(); i++) {
            dates.add(_constellation.getDateList().get(i).toString().replaceFirst("^Date: ",""));
          }
          outFields.put(snacText, htmlOrderedList(dates));
          break;

        case "subject":
          List<String> subjects = new ArrayList<String>();
          for (int i = 0; i < _constellation.getSubjects().size(); i++) {
            subjects.add(_constellation.getSubjects().get(i).getTerm().getTerm());
          }
          outFields.put(snacText, htmlOrderedList(subjects));
          break;

        case "place":
          List<String> placesAndRoles = new ArrayList<String>();
          for (int i = 0; i < _constellation.getPlaces().size(); i++) {
            Place place = _constellation.getPlaces().get(i);
            // assumes we are not working with geo terms
            String placeAndRoleAndType = place.toString();
            if (place.getRole() != null) {
              placeAndRoleAndType += " (" + place.getRole().getTerm() + ")";
            }
            if (place.getType() != null) {
              placeAndRoleAndType += " (" + place.getType().getTerm() + ")";
            }
            placesAndRoles.add(placeAndRoleAndType.replaceFirst("^Place: ",""));
          }
          outFields.put(snacText, htmlOrderedList(placesAndRoles));
          break;

        case "occupation":
          List<String> occupations = new ArrayList<String>();
          for (int i = 0; i < _constellation.getOccupations().size(); i++) {
            occupations.add(_constellation.getOccupations().get(i).getTerm().getTerm());
          }
          outFields.put(snacText, htmlOrderedList(occupations));
          break;

        case "activity":
          List<String> activities = new ArrayList<String>();
          for (int i = 0; i < _constellation.getActivities().size(); i++) {
            activities.add(_constellation.getActivities().get(i).getTerm().getTerm());
          }
          outFields.put(snacText, htmlOrderedList(activities));
          break;

        case "language code":
          List<String> langList = new ArrayList<>();

          for (int i = 0; i < _constellation.getLanguagesUsed().size(); i++) {
            Term lang = _constellation.getLanguagesUsed().get(i).getLanguage();

            String langFull = "";

            if (lang != null) {
              String langCode = lang.getTerm();
              String langDesc = lang.getDescription();

              langFull = langCode;
              if (!langDesc.equals("")) {
                langFull += " (" + langDesc + ")";
              }
            }

            Term script = _constellation.getLanguagesUsed().get(i).getScript();

            String scriptFull = "";

            if (script != null) {
              String scriptCode = script.getTerm();
              String scriptDesc = script.getDescription();

              scriptFull = scriptCode;
              if (!scriptDesc.equals("")) {
                scriptFull += " (" + scriptDesc + ")";
              }
            }

            if (langFull.equals("")) {
              if (scriptFull.equals("")) {
                // no language or script?  shouldn't happen
              } else {
                langList.add("Script: " + scriptFull);
              }
            } else {
              if (scriptFull.equals("")) {
                langList.add("Language: " + langFull);
              } else {
                langList.add("Language: " + langFull + " / Script: " + scriptFull);
              }
            }
          }

          if (langList.size() > 0) {
            outFields.put(snacText, htmlOrderedList(langList));
          }

          break;

        case "bioghist":
          List<String> bioghists = new ArrayList<String>();
          for (int i = 0; i < _constellation.getBiogHists().size(); i++) {
            bioghists.add(_constellation.getBiogHists().get(i).getText());
          }
          outFields.put(snacText, htmlOrderedList(bioghists));
          break;

        case "source citation":
          List<String> sources = new ArrayList<String>();
          for (int i = 0; i < _constellation.getSources().size(); i++) {
            Source source = _constellation.getSources().get(i);
            String sourceAndFoundData = source.getCitation();
            if (source.getURI() != null) {
              sourceAndFoundData += " (" + htmlLink(source.getURI(), source.getURI()) + ")";
            }
            if (source.getText() != null) {
              sourceAndFoundData += " (Found Data: " + source.getText() + ")";
            }
            sources.add(sourceAndFoundData);
          }
          outFields.put(snacText, htmlOrderedList(sources));
          break;

        case "external related cpf url":
          List<String> sameAsURIs = new ArrayList<String>();
          for (int i = 0; i < _constellation.getSameAsRelations().size(); i++) {
            SameAs sameAs = _constellation.getSameAsRelations().get(i);
            sameAsURIs.add(htmlLink(sameAs.getURI(), sameAs.getURI()));
          }
          outFields.put(snacText, htmlOrderedList(sameAsURIs));
          break;

        case "related snac cpf id":
          List<String> relations = new ArrayList<String>();
          for (int i = 0; i < _constellation.getRelations().size(); i++) {
            ConstellationRelation relation = _constellation.getRelations().get(i);
            int relationID = relation.getTargetConstellation();
            String relationAndType = htmlLink(_client.urlForConstellationID(relationID), Integer.toString(relationID));
            if (relation.getType() != null) {
              relationAndType += " (" + relation.getType().getTerm() + ")";
            }
            relations.add(relationAndType);
          }
          outFields.put(snacText, htmlOrderedList(relations));
          break;

        case "snac resource id":
          List<String> resourceRelations = new ArrayList<String>();
          for (int i = 0; i < _constellation.getResourceRelations().size(); i++) {
            ResourceRelation resourceRelation = _constellation.getResourceRelations().get(i);
            int resourceRelationID = resourceRelation.getResource().getID();
            String resourceRelationAndRole = htmlLink(_client.urlForResourceID(resourceRelationID), Integer.toString(resourceRelationID));
            if (resourceRelation.getRole() != null) {
              resourceRelationAndRole += " (" + resourceRelation.getRole().getTerm() + ")";
            }
            resourceRelations.add(resourceRelationAndRole);
          }
          outFields.put(snacText, htmlOrderedList(resourceRelations));
          break;
      }
    }

    if (_constellation.getOperation() == "update") {
      outFields.put("*** Operation ***", "Edit Constellation with ID: " + 
        htmlLink(_client.urlForConstellationID(_constellation.getID()), Integer.toString(_constellation.getID())));
    } else {
      outFields.put("*** Operation ***", "Insert new Constellation");
    }

    String preview = "";
    // FIXME: output in predetermined order (not just alphabetical)?
    for (String key : outFields.keySet()) {
      String out = outFields.get(key);
      if (out == null || out.equals("")) {
        continue;
      }
      preview += htmlTableRow(htmlTableColumnField(key) + htmlTableColumnValue(out));
      //logger.info(key + " => " + out);
    }
    preview = htmlTable(preview);

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
      logger.warn("constellation validation error: [" + errMsg + "]");
      return new SNACAPIResponse(this._client, errMsg);
    }

    return new SNACAPIResponse(this._client, "success");
  }

  public SNACAPIResponse performValidation() {
    buildConstellationAgainstSNAC();

    logger.info("validating constellation data...");

    // return error if validation errors were encountered earlier
    if (_validationErrors.size() > 0) {
      for (int i = 0; i < _validationErrors.size(); i++) {
        _validationErrors.set(i, "* " + _validationErrors.get(i));
      }
      String errMsg = String.join("\n", _validationErrors);
      return new SNACAPIResponse(this._client, errMsg);
    }

    // verify any related IDs
    return verifyRelatedIDs();
  }

  public SNACAPIResponse performUpload() {
    logger.info("preparing to upload constellation to SNAC...");

    // validate constellation data before uploading
    SNACAPIResponse validationError = performValidation();
    if (validationError != null && !validationError.getResult().equals("success")) {
      return validationError;
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

}
