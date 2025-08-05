package org.snaccooperative.openrefine.exporters;

import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.data.AbstractData;
import org.snaccooperative.data.Activity;
import org.snaccooperative.data.BiogHist;
import org.snaccooperative.data.Constellation;
import org.snaccooperative.data.Language;
import org.snaccooperative.data.NameEntry;
import org.snaccooperative.data.Occupation;
import org.snaccooperative.data.Place;
import org.snaccooperative.data.SNACDate;
import org.snaccooperative.data.SameAs;
import org.snaccooperative.data.Source;
import org.snaccooperative.data.Subject;
import org.snaccooperative.data.Term;
import org.snaccooperative.openrefine.api.SNACAPIClient;
import org.snaccooperative.openrefine.api.SNACAPIResponse;
import org.snaccooperative.openrefine.cache.SNACLookupCache;
import org.snaccooperative.openrefine.cache.SNACLookupCache.TermType;
import org.snaccooperative.openrefine.model.SNACConstellationModel;
import org.snaccooperative.openrefine.model.SNACConstellationModel.ConstellationModelField;
import org.snaccooperative.openrefine.model.SNACModelField;
import org.snaccooperative.openrefine.schema.SNACSchema;

public class SNACConstellationItem extends SNACAbstractItem {

  static final Logger logger = LoggerFactory.getLogger(SNACConstellationItem.class);

  private Constellation _item;

  private SNACConstellationModel _model;

  public SNACConstellationItem(
      Project project,
      SNACSchema schema,
      SNACAPIClient client,
      SNACLookupCache cache,
      Record record) {
    super(project, schema, client, cache, record);

    this._model = new SNACConstellationModel();

    buildItemVerbatim();
  }

  protected void buildItem() {
    this._item = null;
    this._errors = new SNACValidationErrors();

    SNACFieldValidator<ConstellationModelField> validator =
        new SNACFieldValidator<ConstellationModelField>(_model, _schema, _utils, _cache, _errors);

    Constellation con = new Constellation();
    con.setOperation(AbstractData.OPERATION_INSERT);

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String csvColumn = entry.getKey();
      String snacField = entry.getValue();

      ConstellationModelField field = _model.getFieldType(snacField);
      SNACModelField<ConstellationModelField> modelField = _model.getModelField(field);

      // initialize field tracker
      validator.initializeField(modelField);

      for (int i = _record.fromRowIndex; i < _record.toRowIndex; i++) {
        Row row = _project.rows.get(i);

        String cellValue = validator.getCellValue(row, csvColumn);

        if (cellValue.equals("")) {
          continue;
        }

        // quick check: ensure current field meets occurence and row dependency requirements
        // NOTE: fields are checked and counted even if they are invalid and would be skipped!
        if (!validator.checkAndCountField(modelField, cellValue, row)) {
          continue;
        }

        switch (field) {
          case CPF_ID:
            Integer constellationID = validator.getIdentifier(field, cellValue);
            if (constellationID == null) {
              continue;
            }

            int cid = constellationID;
            con.setID(cid);
            con.setOperation(AbstractData.OPERATION_UPDATE);

            // record this id so that verifyRelatedIDs() checks for its existence
            _id = cid;

            continue;

          case CPF_TYPE:
            Term entityTypeTerm = validator.getTerm(field, cellValue, TermType.ENTITY_TYPE);
            if (entityTypeTerm == null) {
              continue;
            }

            con.setEntityType(entityTypeTerm);

            continue;

          case NAME_ENTRY:
            NameEntry preferredName = new NameEntry();

            preferredName.setOriginal(cellValue);
            preferredName.setPreferenceScore(99);
            preferredName.setOperation(AbstractData.OPERATION_INSERT);

            con.addNameEntry(preferredName);

            continue;

          case VARIANT_NAME_ENTRY:
            NameEntry variantName = new NameEntry();

            variantName.setOriginal(cellValue);
            variantName.setPreferenceScore(0);
            variantName.setOperation(AbstractData.OPERATION_INSERT);

            con.addNameEntry(variantName);

            continue;

          case EXIST_DATE:

            // find and add required 'exist date type' in this row
            Term existDateTypeTerm =
                validator.getRelatedTerm(
                    row,
                    field,
                    cellValue,
                    ConstellationModelField.EXIST_DATE_TYPE,
                    TermType.DATE_TYPE);
            if (existDateTypeTerm == null) {
              continue;
            }

            // find and add optional 'exist date descriptive note' in this row
            String existDateDescriptiveNote =
                validator.getRelatedCellValue(
                    row, ConstellationModelField.EXIST_DATE_DESCRIPTIVE_NOTE);

            SNACDate date = new SNACDate();

            date.setDate(cellValue, cellValue, existDateTypeTerm);
            date.setNote(existDateDescriptiveNote);
            date.setOperation(AbstractData.OPERATION_INSERT);

            con.addDate(date);

            continue;

          case EXIST_DATE_TYPE: // queried alongside EXIST_DATE
            continue;

          case EXIST_DATE_DESCRIPTIVE_NOTE: // queried alongside EXIST_DATE
            continue;

          case SUBJECT:
            Term subjectTerm = validator.getTerm(field, cellValue, TermType.SUBJECT);
            if (subjectTerm == null) {
              continue;
            }

            Subject subject = new Subject();
            subject.setTerm(subjectTerm);
            subject.setOperation(AbstractData.OPERATION_INSERT);

            con.addSubject(subject);

            continue;

          case PLACE:
            Place place = new Place();
            place.setOriginal(cellValue);
            place.setOperation(AbstractData.OPERATION_INSERT);

            // find and add required 'place type' in this row
            Term placeTypeTerm =
                validator.getRelatedTerm(
                    row,
                    field,
                    cellValue,
                    ConstellationModelField.PLACE_TYPE,
                    TermType.PLACE_TYPE,
                    "AssociatedPlace");
            if (placeTypeTerm == null) {
              continue;
            }

            place.setType(placeTypeTerm);

            // find and add optional 'place role' in this row
            Term placeRoleTerm =
                validator.getRelatedTerm(
                    row, field, cellValue, ConstellationModelField.PLACE_ROLE, TermType.PLACE_ROLE);
            if (placeRoleTerm != null) {
              place.setRole(placeRoleTerm);
            }

            con.addPlace(place);

            continue;

          case PLACE_ROLE: // queried alongside PLACE
            continue;

          case PLACE_TYPE: // queried alongside PLACE
            continue;

          case SOURCE_CITATION:
            Source source = new Source();

            // set citation
            source.setCitation(cellValue);

            // find and add optional 'source citation url' in this row
            String sourceCitationURL =
                validator.getRelatedCellValue(row, ConstellationModelField.SOURCE_CITATION_URL);
            if (sourceCitationURL != "") {
              source.setURI(sourceCitationURL);
            }

            // find and add optional 'source citation found data' in this row
            String sourceCitationFoundData =
                validator.getRelatedCellValue(
                    row, ConstellationModelField.SOURCE_CITATION_FOUND_DATA);
            if (sourceCitationFoundData != "") {
              source.setText(sourceCitationFoundData);
            }

            source.setOperation(AbstractData.OPERATION_INSERT);
            con.addSource(source);

            continue;

          case SOURCE_CITATION_URL: // queried alongside SOURCE_CITATION
            continue;

          case SOURCE_CITATION_FOUND_DATA: // queried alongside SOURCE_CITATION
            continue;

          case OCCUPATION:
            Term occupationTerm = validator.getTerm(field, cellValue, TermType.OCCUPATION);
            if (occupationTerm == null) {
              continue;
            }

            Occupation occupation = new Occupation();
            occupation.setTerm(occupationTerm);
            occupation.setOperation(AbstractData.OPERATION_INSERT);

            con.addOccupation(occupation);

            continue;

          case ACTIVITY:
            Term activityTerm = validator.getTerm(field, cellValue, TermType.ACTIVITY);
            if (activityTerm == null) {
              continue;
            }

            Activity activity = new Activity();
            activity.setTerm(activityTerm);
            activity.setOperation(AbstractData.OPERATION_INSERT);

            con.addActivity(activity);

            continue;

          case LANGUAGE_CODE: // queried alongside SCRIPT_CODE
            // NOTE: SNAC language type can contain any combination of language code and/or
            // script code.  Here, we check for the cases that contain a language code.

            Term languageCodeTerm = validator.getTerm(field, cellValue, TermType.LANGUAGE_CODE);
            if (languageCodeTerm == null) {
              continue;
            }

            // initialize language code portion
            Language lang = new Language();
            lang.setOperation(AbstractData.OPERATION_INSERT);
            lang.setLanguage(languageCodeTerm);

            // find and add optional 'script code' in this row
            Term optScriptCodeTerm =
                validator.getRelatedTerm(
                    row,
                    field,
                    cellValue,
                    ConstellationModelField.SCRIPT_CODE,
                    TermType.SCRIPT_CODE);
            if (optScriptCodeTerm != null) {
              lang.setScript(optScriptCodeTerm);
            }

            con.addLanguageUsed(lang);

            continue;

          case SCRIPT_CODE: // queried alongside LANGUAGE_CODE
            // NOTE: SNAC language type can contain any combination of language code and/or
            // script code.  Here, we check for the case when there is just a script code.

            // check whether there is an associated language code in this row; if so, skip
            String languageCode =
                validator.getRelatedCellValue(row, ConstellationModelField.LANGUAGE_CODE);
            if (!languageCode.equals("")) {
              continue;
            }

            Term scriptCodeTerm = validator.getTerm(field, cellValue, TermType.SCRIPT_CODE);
            if (scriptCodeTerm == null) {
              continue;
            }

            // initialize script code portion
            Language script = new Language();
            script.setOperation(AbstractData.OPERATION_INSERT);
            script.setScript(scriptCodeTerm);

            con.addLanguageUsed(script);

            continue;

          case BIOG_HIST:
            BiogHist biogHist = new BiogHist();
            biogHist.setText(cellValue);
            biogHist.setOperation(AbstractData.OPERATION_INSERT);

            con.addBiogHist(biogHist);

            continue;

          case EXTERNAL_RELATED_CPF_URL:
            // external related CPF URLs are always sameAs relations

            Term sameAsTerm = validator.getTerm("sameAs", TermType.RECORD_TYPE);
            if (sameAsTerm == null) {
              continue;
            }

            SameAs sameAs = new SameAs();
            sameAs.setURI(cellValue);
            sameAs.setType(sameAsTerm);
            sameAs.setOperation(AbstractData.OPERATION_INSERT);

            con.addSameAsRelation(sameAs);

            continue;
        }
      }

      // perform final checks on this field (for now, just ensures existence of required fields)
      validator.finalizeField(modelField);
    }

    // if user provided two dates, convert it into a range (for reasons lost to time)
    // TODO: add support for specifying date ranges instead?

    List<SNACDate> dates = con.getDateList();

    if (dates.size() == 2) {
      SNACDate from = dates.get(0);
      SNACDate to = dates.get(1);

      SNACDate range = new SNACDate();
      range.setRange(true);
      range.setFromDate(from.getFromDate(), from.getFromDate(), from.getFromType());
      range.setToDate(to.getFromDate(), to.getFromDate(), to.getFromType());
      range.setOperation(AbstractData.OPERATION_INSERT);

      con.setDateList(new LinkedList<SNACDate>());
      con.addDate(range);
    }

    _item = con;

    logger.debug("built constellation: [" + toJSON() + "]");
  }

  public String getPreviewText() {
    Map<String, String> outFields = new TreeMap<>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacField = entry.getValue();

      ConstellationModelField field = _model.getFieldType(snacField);

      switch (field) {
        case CPF_TYPE:
          Term previewTerm = _item.getEntityType();
          if (previewTerm != null) {
            outFields.put(snacField, previewTerm.getTerm());
          }
          continue;

        case NAME_ENTRY:
          List<String> namesAndPreferenceScores = new ArrayList<String>();
          for (int i = 0; i < _item.getNameEntries().size(); i++) {
            NameEntry name = _item.getNameEntries().get(i);
            String nameAndPreferenceScore = name.toString();
            if (name.getPreferenceScore() == 99) {
              nameAndPreferenceScore += " (preferred)";
            } else {
              nameAndPreferenceScore += " (variant)";
            }
            namesAndPreferenceScores.add(nameAndPreferenceScore.replaceFirst("^Name Entry: ", ""));
          }
          outFields.put(snacField, htmlOrderedList(namesAndPreferenceScores));
          continue;

        case EXIST_DATE:
          List<String> dates = new ArrayList<String>();
          for (int i = 0; i < _item.getDateList().size(); i++) {
            dates.add(_item.getDateList().get(i).toString().replaceFirst("^Date: ", ""));
          }
          outFields.put(snacField, htmlOrderedList(dates));
          continue;

        case SUBJECT:
          List<String> subjects = new ArrayList<String>();
          for (int i = 0; i < _item.getSubjects().size(); i++) {
            subjects.add(_item.getSubjects().get(i).getTerm().getTerm());
          }
          outFields.put(snacField, htmlOrderedList(subjects));
          continue;

        case PLACE:
          List<String> placesAndRoles = new ArrayList<String>();
          for (int i = 0; i < _item.getPlaces().size(); i++) {
            Place place = _item.getPlaces().get(i);
            // assumes we are not working with geo terms
            String placeAndRoleAndType = place.toString();
            if (place.getRole() != null) {
              placeAndRoleAndType += " (" + place.getRole().getTerm() + ")";
            }
            if (place.getType() != null) {
              placeAndRoleAndType += " (" + place.getType().getTerm() + ")";
            }
            placesAndRoles.add(placeAndRoleAndType.replaceFirst("^Place: ", ""));
          }
          outFields.put(snacField, htmlOrderedList(placesAndRoles));
          continue;

        case OCCUPATION:
          List<String> occupations = new ArrayList<String>();
          for (int i = 0; i < _item.getOccupations().size(); i++) {
            occupations.add(_item.getOccupations().get(i).getTerm().getTerm());
          }
          outFields.put(snacField, htmlOrderedList(occupations));
          continue;

        case ACTIVITY:
          List<String> activities = new ArrayList<String>();
          for (int i = 0; i < _item.getActivities().size(); i++) {
            activities.add(_item.getActivities().get(i).getTerm().getTerm());
          }
          outFields.put(snacField, htmlOrderedList(activities));
          continue;

        case LANGUAGE_CODE:
          List<String> langList = new ArrayList<>();
          for (int i = 0; i < _item.getLanguagesUsed().size(); i++) {
            Term lang = _item.getLanguagesUsed().get(i).getLanguage();

            String langFull = "";

            if (lang != null) {
              String langCode = lang.getTerm();
              String langDesc = lang.getDescription();

              langFull = langCode;
              if (!langDesc.equals("")) {
                langFull += " (" + langDesc + ")";
              }
            }

            Term script = _item.getLanguagesUsed().get(i).getScript();

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
            outFields.put(snacField, htmlOrderedList(langList));
          }

          continue;

        case BIOG_HIST:
          List<String> bioghists = new ArrayList<String>();
          for (int i = 0; i < _item.getBiogHists().size(); i++) {
            bioghists.add(_item.getBiogHists().get(i).getText());
          }
          outFields.put(snacField, htmlOrderedList(bioghists));
          continue;

        case SOURCE_CITATION:
          List<String> sources = new ArrayList<String>();
          for (int i = 0; i < _item.getSources().size(); i++) {
            Source source = _item.getSources().get(i);
            String sourceAndFoundData = source.getCitation();
            if (source.getURI() != null) {
              sourceAndFoundData += " (" + htmlLink(source.getURI(), source.getURI()) + ")";
            }
            if (source.getText() != null) {
              sourceAndFoundData += " (Found Data: " + source.getText() + ")";
            }
            sources.add(sourceAndFoundData);
          }
          outFields.put(snacField, htmlOrderedList(sources));
          continue;

        case EXTERNAL_RELATED_CPF_URL:
          List<String> sameAsURIs = new ArrayList<String>();
          for (int i = 0; i < _item.getSameAsRelations().size(); i++) {
            SameAs sameAs = _item.getSameAsRelations().get(i);
            sameAsURIs.add(htmlLink(sameAs.getURI(), sameAs.getURI()));
          }
          outFields.put(snacField, htmlOrderedList(sameAsURIs));
          continue;
      }
    }

    if (_item.getOperation().equals(AbstractData.OPERATION_UPDATE)) {
      outFields.put(
          "*** Operation ***",
          "Edit Constellation with ID: "
              + htmlLink(
                  _client.urlForConstellationID(_item.getID()), Integer.toString(_item.getID())));
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
      // logger.info(key + " => " + out);
    }
    preview = htmlTable(preview);

    return preview;
  }

  public String toJSON() {
    return Constellation.toJSON(_item);
  }

  public SNACAPIResponse performUpload() {
    logger.info("preparing to upload constellation to SNAC...");

    // validate constellation data before uploading
    SNACAPIResponse validationError = performValidation();
    if (validationError != null && !validationError.getResult().equals("success")) {
      return validationError;
    }

    // so far so good, proceed with upload

    int myID = _item.getID();

    if (myID > 0) {
      // update existing constellation (edit/update)

      // checkout current version

      logger.info("checking out existing constellation [" + myID + "]...");

      JSONObject req = new JSONObject();

      req.put("command", "edit");
      req.put("constellationid", myID);
      req.put("apikey", _client.apiKey());

      SNACAPIResponse checkoutResponse = _client.post(req);

      Constellation checkoutCon = checkoutResponse.getConstellation();

      if (checkoutCon == null) {
        logger.error("error checking out constellation");
        return checkoutResponse;
      }

      // set update information

      _item.setOperation(AbstractData.OPERATION_UPDATE);
      _item.setID(checkoutCon.getID());
      _item.setVersion(checkoutCon.getVersion());
      _item.setArk(checkoutCon.getArk());
    }

    logger.info("uploading constellation...");

    JSONObject req = new JSONObject();

    req.put("command", "insert_and_publish_constellation");
    req.put("apikey", _client.apiKey());
    req.put("constellation", new JSONObject(toJSON()));

    SNACAPIResponse updateResponse = _client.post(req);

    logger.info("constellation upload complete");

    return updateResponse;
  }
}
