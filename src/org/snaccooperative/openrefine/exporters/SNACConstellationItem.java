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
import org.snaccooperative.data.ConstellationRelation;
import org.snaccooperative.data.Language;
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
import org.snaccooperative.openrefine.api.SNACAPIClient;
import org.snaccooperative.openrefine.api.SNACAPIResponse;
import org.snaccooperative.openrefine.cache.SNACLookupCache;
import org.snaccooperative.openrefine.cache.SNACLookupCache.TermType;
import org.snaccooperative.openrefine.model.SNACAbstractModel.ModelType;
import org.snaccooperative.openrefine.model.SNACConstellationModel;
import org.snaccooperative.openrefine.model.SNACConstellationModel.ConstellationModelField;
import org.snaccooperative.openrefine.model.SNACRelationModel;
import org.snaccooperative.openrefine.model.SNACRelationModel.RelationModelField;
import org.snaccooperative.openrefine.schema.SNACSchema;

public class SNACConstellationItem extends SNACAbstractItem {

  static final Logger logger = LoggerFactory.getLogger(SNACConstellationItem.class);

  private Constellation _constellation;

  private SNACConstellationModel _constellationModel;
  private SNACRelationModel _relationModel;

  public SNACConstellationItem(
      Project project,
      SNACSchema schema,
      SNACAPIClient client,
      SNACLookupCache cache,
      Record record) {
    super(project, schema, client, cache, record);

    this._constellationModel = new SNACConstellationModel();
    this._relationModel = new SNACRelationModel();

    buildItemVerbatim();
  }

  protected void buildItem() {
    this._constellation = null;
    this._relatedIDs.put(ModelType.CONSTELLATION, new LinkedList<Integer>());
    this._relatedIDs.put(ModelType.RESOURCE, new LinkedList<Integer>());
    this._errors = new SNACValidationErrors();

    SNACFieldValidator<ConstellationModelField> constellationValidator =
        new SNACFieldValidator<ConstellationModelField>(_errors);
    SNACFieldValidator<RelationModelField> relationValidator =
        new SNACFieldValidator<RelationModelField>(_errors);

    Constellation con = new Constellation();
    con.setOperation(AbstractData.OPERATION_INSERT);

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String csvColumn = entry.getKey();
      String snacField = entry.getValue();

      for (int i = _record.fromRowIndex; i < _record.toRowIndex; i++) {
        Row row = _project.rows.get(i);

        String cellValue = _utils.getCellValueForRowByColumnName(row, csvColumn);

        if (cellValue.equals("")) {
          continue;
        }

        switch (_modelType) {
          case CONSTELLATION:
            ConstellationModelField constellationField =
                _constellationModel.getFieldType(snacField);

            // quick check: ensure current field can be populated (right now this just
            // prevents single-occurence fields from being specified multiple times)
            // NOTE: fields are counted even if they are invalid and would be skipped!
            if (constellationValidator.hasReachedLimit(
                _constellationModel.getModelField(constellationField))) {
              continue;
            }
            constellationValidator.addOccurence(
                _constellationModel.getModelField(constellationField));

            // quick check: ensure all required dependency/dependent fields exist and are not empty
            if (!_constellationModel.hasRequiredFieldsInRow(
                constellationField, cellValue, csvColumn, row, _schema, _utils, _errors)) {
              continue;
            }

            switch (constellationField) {
              case CPF_ID:
                try {
                  int id = Integer.parseInt(cellValue);
                  con.setID(id);
                } catch (NumberFormatException e) {
                  _errors.addInvalidNumericFieldError(
                      constellationField.getName(), cellValue, csvColumn);
                }
                continue;

              case CPF_TYPE:
                Term entityTypeTerm = _cache.getTerm(TermType.ENTITY_TYPE, cellValue);

                if (entityTypeTerm == null) {
                  _errors.addInvalidVocabularyFieldError(
                      constellationField.getName(), cellValue, csvColumn);
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
                String dateTypeColumn =
                    _constellationModel.getEntryForFieldType(
                        ConstellationModelField.EXIST_DATE_TYPE, _schema.getColumnMappings());

                String dateType = _utils.getCellValueForRowByColumnName(row, dateTypeColumn);

                Term dateTypeTerm = _cache.getTerm(TermType.DATE_TYPE, dateType);

                if (dateTypeTerm == null) {
                  _errors.addInvalidVocabularyFieldError(
                      ConstellationModelField.EXIST_DATE_TYPE.getName(),
                      dateType,
                      dateTypeColumn,
                      constellationField.getName(),
                      cellValue,
                      csvColumn);
                  continue;
                }

                // find and add optional 'exist date descriptive note' in this row
                String dateNoteColumn =
                    _constellationModel.getEntryForFieldType(
                        ConstellationModelField.EXIST_DATE_DESCRIPTIVE_NOTE,
                        _schema.getColumnMappings());

                String dateNote = "";
                if (dateNoteColumn != null) {
                  dateNote = _utils.getCellValueForRowByColumnName(row, dateNoteColumn);
                }

                SNACDate date = new SNACDate();

                date.setDate(cellValue, cellValue, dateTypeTerm);
                date.setNote(dateNote);
                date.setOperation(AbstractData.OPERATION_INSERT);

                con.addDate(date);

                continue;

              case EXIST_DATE_TYPE: // queried alongside EXIST_DATE
                continue;

              case EXIST_DATE_DESCRIPTIVE_NOTE: // queried alongside EXIST_DATE
                continue;

              case SUBJECT:
                Term subjectTerm = _cache.getTerm(TermType.SUBJECT, cellValue);

                if (subjectTerm == null) {
                  _errors.addInvalidVocabularyFieldError(
                      constellationField.getName(), cellValue, csvColumn);
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

                // we need to supply a place type, so use this if no other is supplied
                String placeType = "AssociatedPlace";

                // find and add optional 'place type' in this row
                String placeTypeColumn =
                    _constellationModel.getEntryForFieldType(
                        ConstellationModelField.PLACE_TYPE, _schema.getColumnMappings());

                if (placeTypeColumn != null) {
                  placeType = _utils.getCellValueForRowByColumnName(row, placeTypeColumn);
                }

                Term placeTypeTerm = _cache.getTerm(TermType.PLACE_TYPE, placeType);

                if (placeTypeTerm != null) {
                  place.setType(placeTypeTerm);
                } else {
                  _errors.addInvalidVocabularyFieldError(
                      ConstellationModelField.PLACE_TYPE.getName(),
                      placeType,
                      placeTypeColumn,
                      constellationField.getName(),
                      cellValue,
                      csvColumn);
                  continue;
                }

                // find and add optional 'place role' in this row
                String placeRoleColumn =
                    _constellationModel.getEntryForFieldType(
                        ConstellationModelField.PLACE_ROLE, _schema.getColumnMappings());

                if (placeRoleColumn != null) {
                  String placeRole = _utils.getCellValueForRowByColumnName(row, placeRoleColumn);

                  Term placeRoleTerm = _cache.getTerm(TermType.PLACE_ROLE, placeRole);

                  if (placeRoleTerm != null) {
                    place.setRole(placeRoleTerm);
                  } else {
                    _errors.addInvalidVocabularyFieldError(
                        ConstellationModelField.PLACE_ROLE.getName(),
                        placeRole,
                        placeRoleColumn,
                        constellationField.getName(),
                        cellValue,
                        csvColumn);
                    continue;
                  }
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
                String urlColumn =
                    _constellationModel.getEntryForFieldType(
                        ConstellationModelField.SOURCE_CITATION_URL, _schema.getColumnMappings());

                if (urlColumn != null) {
                  String url = _utils.getCellValueForRowByColumnName(row, urlColumn);
                  source.setURI(url);
                }

                // find and add optional 'source citation found data' in this row
                String foundColumn =
                    _constellationModel.getEntryForFieldType(
                        ConstellationModelField.SOURCE_CITATION_FOUND_DATA,
                        _schema.getColumnMappings());

                if (foundColumn != null) {
                  String foundData = _utils.getCellValueForRowByColumnName(row, foundColumn);
                  source.setText(foundData);
                }

                source.setOperation(AbstractData.OPERATION_INSERT);
                con.addSource(source);

                continue;

              case SOURCE_CITATION_URL: // queried alongside SOURCE_CITATION
                continue;

              case SOURCE_CITATION_FOUND_DATA: // queried alongside SOURCE_CITATION
                continue;

              case OCCUPATION:
                Term occupationTerm = _cache.getTerm(TermType.OCCUPATION, cellValue);

                if (occupationTerm == null) {
                  _errors.addInvalidVocabularyFieldError(
                      constellationField.getName(), cellValue, csvColumn);
                  continue;
                }

                Occupation occupation = new Occupation();
                occupation.setTerm(occupationTerm);
                occupation.setOperation(AbstractData.OPERATION_INSERT);

                con.addOccupation(occupation);

                continue;

              case ACTIVITY:
                Term activityTerm = _cache.getTerm(TermType.ACTIVITY, cellValue);

                if (activityTerm == null) {
                  _errors.addInvalidVocabularyFieldError(
                      constellationField.getName(), cellValue, csvColumn);
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

                Term languageCodeTerm = _cache.getTerm(TermType.LANGUAGE_CODE, cellValue);

                if (languageCodeTerm == null) {
                  _errors.addInvalidVocabularyFieldError(
                      constellationField.getName(), cellValue, csvColumn);
                  continue;
                }

                // initialize language code portion
                Language lang = new Language();
                lang.setOperation(AbstractData.OPERATION_INSERT);
                lang.setLanguage(languageCodeTerm);

                // find and add optional 'script code' in this row
                String scriptCodeColumn =
                    _constellationModel.getEntryForFieldType(
                        ConstellationModelField.SCRIPT_CODE, _schema.getColumnMappings());

                if (scriptCodeColumn != null) {
                  String scriptCode = _utils.getCellValueForRowByColumnName(row, scriptCodeColumn);

                  if (!scriptCode.equals("")) {
                    Term scriptCodeTerm = _cache.getTerm(TermType.SCRIPT_CODE, scriptCode);
                    if (scriptCodeTerm != null) {
                      // add script code portion
                      lang.setScript(scriptCodeTerm);
                    } else {
                      _errors.addInvalidVocabularyFieldError(
                          ConstellationModelField.SCRIPT_CODE.getName(),
                          scriptCode,
                          scriptCodeColumn,
                          constellationField.getName(),
                          cellValue,
                          csvColumn);
                      continue;
                    }
                  } else {
                    // logger.info("no associated script code value found; skipping");
                  }
                } else {
                  // logger.info("no associated script code column found; skipping");
                }

                con.addLanguageUsed(lang);

                continue;

              case SCRIPT_CODE: // queried alongside LANGUAGE_CODE
                // NOTE: SNAC language type can contain any combination of language code and/or
                // script code.  Here, we check for the case when there is just a script code.

                // check whether there is an associated language code in this row; if so, skip
                String languageCodeColumn =
                    _constellationModel.getEntryForFieldType(
                        ConstellationModelField.LANGUAGE_CODE, _schema.getColumnMappings());

                if (languageCodeColumn != null) {
                  String languageCode =
                      _utils.getCellValueForRowByColumnName(row, languageCodeColumn);

                  if (!languageCode.equals("")) {
                    // this scenario is handled in the "language code" section
                    // logger.info("skipping script code with associated language code");
                    continue;
                  } else {
                    // logger.info("no associated language code value found; proceeding");
                  }
                } else {
                  // logger.info("no associated language code column found; proceeding");
                }

                Term scriptCodeTerm = _cache.getTerm(TermType.SCRIPT_CODE, cellValue);

                if (scriptCodeTerm == null) {
                  _errors.addInvalidVocabularyFieldError(
                      constellationField.getName(), cellValue, csvColumn);
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

                String defaultExternalRelatedCPFUrlType = "sameAs";
                Term sameAsTerm =
                    _cache.getTerm(TermType.RECORD_TYPE, defaultExternalRelatedCPFUrlType);

                if (sameAsTerm == null) {
                  _errors.addInvalidVocabularyFieldError(
                      "Record Type",
                      defaultExternalRelatedCPFUrlType,
                      null,
                      constellationField.getName(),
                      cellValue,
                      csvColumn);
                  continue;
                }

                SameAs sameAs = new SameAs();
                sameAs.setURI(cellValue);
                sameAs.setType(sameAsTerm);
                sameAs.setOperation(AbstractData.OPERATION_INSERT);

                con.addSameAsRelation(sameAs);

                continue;
            }

            continue;

          case RELATION:
            RelationModelField relationField = _relationModel.getFieldType(snacField);

            // quick check: ensure current field can be populated (right now this just
            // prevents single-occurence fields from being specified multiple times)
            // NOTE: fields are counted even if they are invalid and would be skipped!
            if (relationValidator.hasReachedLimit(_relationModel.getModelField(relationField))) {
              continue;
            }
            relationValidator.addOccurence(_relationModel.getModelField(relationField));

            // quick check: ensure all required dependen{cy,t} fields exist and are not empty
            if (!_relationModel.hasRequiredFieldsInRow(
                relationField, cellValue, csvColumn, row, _schema, _utils, _errors)) {
              continue;
            }

            switch (relationField) {
              case CPF_ID:
                try {
                  int id = Integer.parseInt(cellValue);
                  con.setID(id);
                } catch (NumberFormatException e) {
                  _errors.addInvalidNumericFieldError(
                      relationField.getName(), cellValue, csvColumn);
                }

                continue;

              case CPF_TYPE:
                Term entityTypeTerm = _cache.getTerm(TermType.ENTITY_TYPE, cellValue);

                if (entityTypeTerm == null) {
                  _errors.addInvalidVocabularyFieldError(
                      relationField.getName(), cellValue, csvColumn);
                  continue;
                }

                con.setEntityType(entityTypeTerm);

                continue;

              case RELATED_RESOURCE_ID:
                try {
                  int targetResource = Integer.parseInt(cellValue);
                  Resource resource = new Resource();
                  resource.setID(targetResource);

                  ResourceRelation resourceRelation = new ResourceRelation();
                  resourceRelation.setOperation(AbstractData.OPERATION_INSERT);
                  resourceRelation.setResource(resource);

                  // find and add required 'cpf to resource relation type' in this row
                  String resourceRoleColumn =
                      _relationModel.getEntryForFieldType(
                          RelationModelField.CPF_TO_RESOURCE_RELATION_TYPE,
                          _schema.getColumnMappings());

                  String resourceRole =
                      _utils.getCellValueForRowByColumnName(row, resourceRoleColumn);

                  Term resourceRoleTerm = _cache.getTerm(TermType.DOCUMENT_ROLE, resourceRole);

                  if (resourceRoleTerm != null) {
                    resourceRelation.setRole(resourceRoleTerm);
                  } else {
                    _errors.addInvalidVocabularyFieldError(
                        RelationModelField.CPF_TO_RESOURCE_RELATION_TYPE.getName(),
                        resourceRole,
                        resourceRoleColumn,
                        relationField.getName(),
                        cellValue,
                        csvColumn);
                    continue;
                  }

                  con.addResourceRelation(resourceRelation);
                  _relatedIDs.get(ModelType.RESOURCE).add(targetResource);
                } catch (NumberFormatException e) {
                  _errors.addInvalidNumericFieldError(
                      relationField.getName(), cellValue, csvColumn);
                  continue;
                }

                continue;

              case CPF_TO_RESOURCE_RELATION_TYPE: // queried alongside RESOURCE_ID
                continue;

              case RELATED_CPF_ID:
                try {
                  int targetConstellation = Integer.parseInt(cellValue);
                  ConstellationRelation cpfRelation = new ConstellationRelation();
                  cpfRelation.setSourceConstellation(con.getID());
                  cpfRelation.setTargetConstellation(targetConstellation);

                  // find and add required 'cpf to cpf relation type' in this row
                  String cpfRelationTypeColumn =
                      _relationModel.getEntryForFieldType(
                          RelationModelField.CPF_TO_CPF_RELATION_TYPE, _schema.getColumnMappings());

                  String cpfRelationType =
                      _utils.getCellValueForRowByColumnName(row, cpfRelationTypeColumn);

                  Term cpfRelationTypeTerm =
                      _cache.getTerm(TermType.RELATION_TYPE, cpfRelationType);

                  if (cpfRelationTypeTerm != null) {
                    cpfRelation.setType(cpfRelationTypeTerm);
                    cpfRelation.setOperation(AbstractData.OPERATION_INSERT);
                  } else {
                    _errors.addInvalidVocabularyFieldError(
                        RelationModelField.CPF_TO_CPF_RELATION_TYPE.getName(),
                        cpfRelationType,
                        cpfRelationTypeColumn,
                        relationField.getName(),
                        cellValue,
                        csvColumn);
                    continue;
                  }

                  con.addRelation(cpfRelation);
                  _relatedIDs.get(ModelType.CONSTELLATION).add(targetConstellation);
                } catch (NumberFormatException e) {
                  _errors.addInvalidNumericFieldError(
                      relationField.getName(), cellValue, csvColumn);
                  continue;
                }

                continue;

              case CPF_TO_CPF_RELATION_TYPE: // Queried alongside RELATED_CPF_ID
                continue;
            }

            continue;
        }
      }
    }

    // if user provided two dates, convert it into a range (for reasons lost to time)
    // TODO: add support for specifying date ranges instead?

    List<SNACDate> dates = con.getDateList();

    if (dates.size() == 2) {
        SNACDate from = dates.get(0);
        SNACDate to = dates.get(1);

        SNACDate range = new SNACDate();
        range.setRange(true);
        // TODO: next two lines seem suspicious, need to verify this is correct
        range.setFromDate(from.getFromDate(), from.getFromDate(), from.getFromType());
        range.setToDate(to.getFromDate(), from.getFromDate(), from.getFromType());
        range.setOperation(AbstractData.OPERATION_INSERT);

        con.setDateList(new LinkedList<SNACDate>());
        con.addDate(range);
    }

    _constellation = con;

    logger.debug("built constellation: [" + toJSON() + "]");
  }

  public String getPreviewText() {
    Map<String, String> outFields = new TreeMap<>();

    for (Map.Entry<String, String> entry : _schema.getColumnMappings().entrySet()) {
      String snacField = entry.getValue();

      switch (_modelType) {
        case CONSTELLATION:
          switch (_constellationModel.getFieldType(snacField)) {
            case CPF_TYPE:
              Term previewTerm = _constellation.getEntityType();
              if (previewTerm != null) {
                outFields.put(snacField, previewTerm.getTerm());
              }
              break;

            case NAME_ENTRY:
              List<String> namesAndPreferenceScores = new ArrayList<String>();
              for (int i = 0; i < _constellation.getNameEntries().size(); i++) {
                NameEntry name = _constellation.getNameEntries().get(i);
                String nameAndPreferenceScore = name.toString();
                if (name.getPreferenceScore() == 99) {
                  nameAndPreferenceScore += " (preferred)";
                } else {
                  nameAndPreferenceScore += " (variant)";
                }
                namesAndPreferenceScores.add(
                    nameAndPreferenceScore.replaceFirst("^Name Entry: ", ""));
              }
              outFields.put(snacField, htmlOrderedList(namesAndPreferenceScores));
              break;

            case EXIST_DATE:
              List<String> dates = new ArrayList<String>();
              for (int i = 0; i < _constellation.getDateList().size(); i++) {
                dates.add(
                    _constellation.getDateList().get(i).toString().replaceFirst("^Date: ", ""));
              }
              outFields.put(snacField, htmlOrderedList(dates));
              break;

            case SUBJECT:
              List<String> subjects = new ArrayList<String>();
              for (int i = 0; i < _constellation.getSubjects().size(); i++) {
                subjects.add(_constellation.getSubjects().get(i).getTerm().getTerm());
              }
              outFields.put(snacField, htmlOrderedList(subjects));
              break;

            case PLACE:
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
                placesAndRoles.add(placeAndRoleAndType.replaceFirst("^Place: ", ""));
              }
              outFields.put(snacField, htmlOrderedList(placesAndRoles));
              break;

            case OCCUPATION:
              List<String> occupations = new ArrayList<String>();
              for (int i = 0; i < _constellation.getOccupations().size(); i++) {
                occupations.add(_constellation.getOccupations().get(i).getTerm().getTerm());
              }
              outFields.put(snacField, htmlOrderedList(occupations));
              break;

            case ACTIVITY:
              List<String> activities = new ArrayList<String>();
              for (int i = 0; i < _constellation.getActivities().size(); i++) {
                activities.add(_constellation.getActivities().get(i).getTerm().getTerm());
              }
              outFields.put(snacField, htmlOrderedList(activities));
              break;

            case LANGUAGE_CODE:
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
                outFields.put(snacField, htmlOrderedList(langList));
              }

              break;

            case BIOG_HIST:
              List<String> bioghists = new ArrayList<String>();
              for (int i = 0; i < _constellation.getBiogHists().size(); i++) {
                bioghists.add(_constellation.getBiogHists().get(i).getText());
              }
              outFields.put(snacField, htmlOrderedList(bioghists));
              break;

            case SOURCE_CITATION:
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
              outFields.put(snacField, htmlOrderedList(sources));
              break;

            case EXTERNAL_RELATED_CPF_URL:
              List<String> sameAsURIs = new ArrayList<String>();
              for (int i = 0; i < _constellation.getSameAsRelations().size(); i++) {
                SameAs sameAs = _constellation.getSameAsRelations().get(i);
                sameAsURIs.add(htmlLink(sameAs.getURI(), sameAs.getURI()));
              }
              outFields.put(snacField, htmlOrderedList(sameAsURIs));
              break;
          }
          break;

        case RELATION:
          switch (_relationModel.getFieldType(entry.getValue())) {
            case CPF_TYPE:
              Term previewTerm = _constellation.getEntityType();
              if (previewTerm != null) {
                outFields.put(snacField, previewTerm.getTerm());
              }
              break;

            case RELATED_CPF_ID:
              List<String> relations = new ArrayList<String>();
              for (int i = 0; i < _constellation.getRelations().size(); i++) {
                ConstellationRelation relation = _constellation.getRelations().get(i);
                int relationID = relation.getTargetConstellation();
                String relationAndType =
                    htmlLink(
                        _client.urlForConstellationID(relationID), Integer.toString(relationID));
                if (relation.getType() != null) {
                  relationAndType += " (" + relation.getType().getTerm() + ")";
                }
                relations.add(relationAndType);
              }
              outFields.put(snacField, htmlOrderedList(relations));
              break;

            case RELATED_RESOURCE_ID:
              List<String> resourceRelations = new ArrayList<String>();
              for (int i = 0; i < _constellation.getResourceRelations().size(); i++) {
                ResourceRelation resourceRelation = _constellation.getResourceRelations().get(i);
                int resourceRelationID = resourceRelation.getResource().getID();
                String resourceRelationAndRole =
                    htmlLink(
                        _client.urlForResourceID(resourceRelationID),
                        Integer.toString(resourceRelationID));
                if (resourceRelation.getRole() != null) {
                  resourceRelationAndRole += " (" + resourceRelation.getRole().getTerm() + ")";
                }
                resourceRelations.add(resourceRelationAndRole);
              }
              outFields.put(snacField, htmlOrderedList(resourceRelations));
              break;
          }
          break;
      }
    }

    if (_constellation.getOperation().equals(AbstractData.OPERATION_UPDATE)) {
      outFields.put(
          "*** Operation ***",
          "Edit Constellation with ID: "
              + htmlLink(
                  _client.urlForConstellationID(_constellation.getID()),
                  Integer.toString(_constellation.getID())));
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
    return Constellation.toJSON(_constellation);
  }

  public SNACAPIResponse performUpload() {
    logger.info("preparing to upload constellation to SNAC...");

    // validate constellation data before uploading
    SNACAPIResponse validationError = performValidation();
    if (validationError != null && !validationError.getResult().equals("success")) {
      return validationError;
    }

    // so far so good, proceed with upload

    int myID = _constellation.getID();

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

      _constellation.setOperation(AbstractData.OPERATION_UPDATE);
      _constellation.setID(checkoutCon.getID());
      _constellation.setVersion(checkoutCon.getVersion());
      _constellation.setArk(checkoutCon.getArk());
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
