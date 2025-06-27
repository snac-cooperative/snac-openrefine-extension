/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

/*

FIXME ISSUES:

* editing cells does not re-render preview: added a "reload preview" button as a workaround

*/

var SNACSchemaAlignment = {};

SNACSchemaAlignment._cleanName = function(s) {
   return s.replace(/\W/g, " ").replace(/\s+/g, " ").toLowerCase();
};

var SNACSchemaAlignmentDialog = {
   _ignoreChanges: true,
   _debug: true
};

var snacDebug = function(x) {
   if (SNACSchemaAlignmentDialog._debug) {
      console.log(x);
   }
};

SNACSchemaAlignmentDialog.getSNACModel = function() {
   snacDebug(`***** [ getSNACModel ] *****`);

   this.SNACResourceModel = [
      {
         name:     "SNAC Resource ID",
         required: false,
         tooltip:  "SNAC identifier for Resource Description.  Leave blank if Resource Description is NOT in SNAC."
      },
      {
         name:     "Resource Type",
         required: true,
         tooltip:  "Resource Type may have the following values: ArchivalResource, BibliographicResource, DigitalArchivalResource, OralHistoryResource"
      },
      {
         name:     "Title",
         required: true,
         tooltip:  "Title of a resource (that may or may not include dates) (e.g. Jacob Miller Papers, 1809-1882)"
      },
      {
         name:     "Resource URL",
         required: true,
         tooltip:  "URL of the local Resource Description"
      },
      {
         name:     "Abstract",
         required: false,
         tooltip:  "Brief prose abstract of scope and contents of the resource"
      },
      {
         name:     "Extent",
         required: false,
         tooltip:  "Extent of the resource"
      },
      {
         name:     "Date",
         required: false,
         tooltip:  "Date or dates of the resource (YYYY or YYYY-YYYY)"
      },
      {
         name:     "Language Code",
         required: false,
         tooltip:  "ISO 639 Language Code, e.g. 'eng', 'ger', 'jpn'.  Repeatable in relation to resource."
      },
      {
         name:     "Script Code",
         required: false,
         tooltip:  "ISO 15924 Script Code, e.g. 'Latn', 'Cyrl', 'Grek'.  Repeatable in relation to resource."
      },
      {
         name:     "Holding Repository SNAC ID",
         required: true,
         tooltip:  "SNAC identifier for the holding repository description.  The holding repository must be created in SNAC before adding Resource Descriptions."
      }
   ];

   this.SNACConstellationModel = [
      {
         name:     "CPF Type",
         required: true,
         tooltip:  "Type of CPF entity.  Possible values are: corporateBody, person, family"
      },
      {
         name:     "SNAC CPF ID",
         required: false,
         tooltip:  "SNAC identifier for the CPF entity.  Leave blank if the CPF is NOT in SNAC."
      },
      {
         name:     "Name Entry",
         required: false,
         tooltip:  "Preferred Name Entry of the CPF entity.  Repeatable in relation to entity."
      },
      {
         name:     "Variant Name Entry",
         required: false,
         tooltip:  "Variant Name Entry of the CPF entity.  Repeatable in relation to entity."
      },
      {
         name:     "Exist Date",
         required: false,
         tooltip:  "Exist Date or Dates of the CPF entity.  Repeatable in relation to entity."
      },
      {
         name:     "Exist Date Type",
         required: false,
         tooltip:  "Type of Exist Date.  Repeatable in relation to entity.  The following values may be used: Active, Birth, Death, Establishment, Disestablishment"
      },
      {
         name:     "Exist Date Descriptive Note",
         required: false,
         tooltip:  "Descriptive Note of Exist Date.  Repeatable in relation to entity."
      },
      {
         name:     "Subject",
         required: false,
         tooltip:  "Subject term associated with the CPF entity.  Repeatable in relation to entity."
      },
      {
         name:     "Place",
         required: false,
         tooltip:  "Place name associated with the CPF entity.  Repeatable in relation to entity."
      },
      {
         name:     "Place Role",
         required: false,
         tooltip:  "Role of the place in relation to the CPF entity.  The following values may be used: Birth, Death, Residence, Citizenship, Work"
      },
      {
         name:     "Place Type",
         required: false,
         tooltip:  "Type of the place in relation to the CPF entity.  The following values may be used: AssociatedPlace, Address.  Defaults to AssociatedPlace if not supplied."
      },
      {
         name:     "Occupation",
         required: false,
         tooltip:  "Occupation term associated with the CPF entity.  Repeatable in relation to entity."
      },
      {
         name:     "Activity",
         required: false,
         tooltip:  "Activity term associated with the CPF entity.  Repeatable in relation to entity."
      },
      {
         name:     "Language Code",
         required: false,
         tooltip:  "ISO 639 Language Code, e.g. 'eng', 'ger', 'jpn'.  Repeatable in relation to entity."
      },
      {
         name:     "Script Code",
         required: false,
         tooltip:  "ISO 15924 Script Code, e.g. 'Latn', 'Cyrl', 'Grek'.  Repeatable in relation to entity."
      },
      {
         name:     "BiogHist",
         required: false,
         tooltip:  "Biography or History note associated with the CPF entity.  By exception, the note is encoded in XML based on a simplified version of the <biogHist> element in EAC-CPF.  Repeatable in relation to entity."
      },
      {
         name:     "External Related CPF URL",
         required: false,
         tooltip:  "URL to a description of the CPF entity in an external authority.  Such links are limited to those found in the approved list linked above.  Repeatable in relation to entity."
      },
      {
         name:     "Source Citation",
         required: false,
         tooltip:  "Text citation for a source used in describing the CPF entity.  Repeatable in relation to entity."
      },
      {
         name:     "Source Citation URL",
         required: false,
         tooltip:  "URL, if available, for the Source Citation.  Repeatable in relation to entity."
      },
      {
         name:     "Source Citation Found Data",
         required: false,
         tooltip:  "Information found in the Source that is evidence used in the description of the CPF entity.  Repeatable in relation to entity."
      }
   ];

   this.SNACRelationModel = [
      {
         name:     "CPF Type",
         required: true,
         tooltip:  "Type of CPF entity.  Possible values are: corporateBody, person, family"
      },
      {
         name:     "SNAC CPF ID",
         required: false,
         tooltip:  "SNAC identifier for the CPF entity.  Leave blank if the CPF is NOT in SNAC."
      },
      {
         name:     "CPF to CPF Relation Type",
         required: false,
         tooltip:  "Nature of the relation of the CPF entity with the related CPF entity.  Repeatable in relation to entity.  The following values may be used: associatedWith, correspondedWith"
      },
      {
         name:     "Related SNAC CPF ID",
         required: false,
         tooltip:  "SNAC ID of a CPF entity in SNAC related to the CPF entity.  Repeatable in relation to entity."
      },
      {
         name:     "CPF to Resource Relation Type",
         required: false,
         tooltip:  "Role of the CPF entity in relation to the Resource.  Repeatable in relation to entity.  The following values may be used: contributorOf, creatorOf, editorOf, referencedIn"
      },
      {
         name:     "SNAC Resource ID",  //TODO: SNAC RESOURCE ID rename
         required: false,
         tooltip:  "SNAC ID for a related Resource in SNAC.  Repeatable in relation to entity."
      }
   ];

   this.SNACResourceNames = this.SNACResourceModel.map(x => x.name);
   this.SNACResourceNamesRequired = this.SNACResourceModel.filter(x => x.required === true).map(x => x.name);
   this.SNACResourceTooltips = this.SNACResourceModel.map(x => x.tooltip);

   this.SNACConstellationNames = this.SNACConstellationModel.map(x => x.name);
   this.SNACConstellationNamesRequired = this.SNACConstellationModel.filter(x => x.required === true).map(x => x.name);
   this.SNACConstellationTooltips = this.SNACConstellationModel.map(x => x.tooltip);

   this.SNACRelationNames = this.SNACRelationModel.map(x => x.name);
   this.SNACRelationNamesRequired = this.SNACRelationModel.filter(x => x.required === true).map(x => x.name);
   this.SNACRelationTooltips = this.SNACRelationModel.map(x => x.tooltip);


   snacDebug(`this.SNACResourceNames:`);
   snacDebug(this.SNACResourceNames);
   snacDebug(`this.SNACResourceNamesRequired:`);
   snacDebug(this.SNACResourceNamesRequired);
   snacDebug(`this.SNACResourceTooltips:`);
   snacDebug(this.SNACResourceTooltips);
   snacDebug(`this.SNACConstellationNames:`);
   snacDebug(this.SNACConstellationNames);
   snacDebug(`this.SNACConstellationNamesRequired:`);
   snacDebug(this.SNACConstellationNamesRequired);
   snacDebug(`this.SNACConstellationTooltips:`);
   snacDebug(this.SNACConstellationTooltips);
   snacDebug(`this.SNACRelationNames:`);
   snacDebug(this.SNACRelationNames);
   snacDebug(`this.SNACRelationNamesRequired:`);
   snacDebug(this.SNACRelationNamesRequired);
   snacDebug(`this.SNACRelationTooltips:`);
   snacDebug(this.SNACRelationTooltips);

   // probably should get the above info from the backend instead at some point, a la:
/*
   var self = this;

   Refine.postCSRF(
      "command/snac/get-model?" + $.param({ project: theProject.id }),
      { schema: JSON.stringify(schema), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
      function(data) {
         snacDebug(`getSNACModel(): SUCCESS  data = [${JSON.stringify(data)}]`);
      },
      "json"
   );
*/
};

/**
 * Installs the tabs in the UI the first time the snac
 * extension is called.
 */
SNACSchemaAlignmentDialog.setUpTabs = function() {
   snacDebug(`***** [ setUpTabs ] *****`);

   var self = this;

   this._ignoreChanges = true;

   this.getSNACModel();

   this._rightPanel = $('#right-panel');
   this._viewPanel = $('#view-panel').addClass('main-view-panel-tab');
   this._toolPanel = $('#tool-panel');
   this._summaryBar = $('#summary-bar')
      .addClass('main-view-panel-tab-header')
      .addClass('active')
      .attr('href', '#view-panel');

   this._schemaPanel = $('<div id="snac-schema-panel"></div>')
      .addClass('main-view-panel-tab')
      .appendTo(this._rightPanel);
   this._issuesPanel = $('<div id="snac-issues-panel"></div>')
      .addClass('main-view-panel-tab')
      .appendTo(this._rightPanel);
   this._previewPanel = $('<div id="snac-preview-panel"></div>')
      .addClass('main-view-panel-tab')
      .appendTo(this._rightPanel);

   var schemaButton = $('<div></div>')
      .addClass('main-view-panel-tab-header')
      .addClass('main-view-panel-tabs-snac')
      .attr('href', '#snac-schema-panel')
      .text($.i18n('snac-schema/schema-tab-header'))
      .appendTo(this._toolPanel)

   var issuesButton = $('<div></div>')
      .addClass('main-view-panel-tab-header')
      .addClass('main-view-panel-tabs-snac')
      .attr('href', '#snac-issues-panel')
      .text($.i18n('snac-schema/warnings-tab-header')+' ')
      .appendTo(this._toolPanel);
   this.issuesTabCount = $('<span></span>')
      .addClass('schema-alignment-total-warning-count')
      .appendTo(issuesButton)
      .hide();
   this.issueSpinner = $('<img />')
      .attr('src', 'images/large-spinner.gif')
      .attr('width', '16px')
      .appendTo(issuesButton)
      .hide();
   var previewButton = $('<div></div>')
      .addClass('main-view-panel-tab-header')
      .addClass('main-view-panel-tabs-snac')
      .attr('href', '#snac-preview-panel')
      .text($.i18n('snac-schema/items-preview-tab-header'))
      .appendTo(this._toolPanel);
   this.previewSpinner = $('<img />')
      .attr('src', 'images/large-spinner.gif')
      .attr('width', '16px')
      .appendTo(previewButton)
      .hide();

   this._unsavedIndicator = $('<span></span>')
      .html('&nbsp;*')
      .attr('title', $.i18n('snac-schema/unsaved-changes-alt'))
      .hide()
      .appendTo(schemaButton);

   $('.main-view-panel-tabs-snac').hide();

   $('.main-view-panel-tab-header').on('click', function(e) {
      var targetTab = $(this).attr('href');
      self.switchTab(targetTab);
      e.preventDefault();
   });

  /**
   * Init the schema tab
   */
   var schemaTab = $(DOM.loadHTML("snac", "scripts/schema-alignment-tab.html")).appendTo(this._schemaPanel);
   var schemaElmts = this._schemaElmts = DOM.bind(schemaTab);
   schemaElmts.dialogExplanation.text($.i18n('snac-schema/dialog-explanation'));
   schemaElmts.dialogExplanation2.html($.i18n('snac-schema/dialog-explanation2'));
   schemaElmts.saveButton
      .text($.i18n('snac-schema/save-button'))
      .attr('title', $.i18n('snac-schema/save-schema-alt'))
      .prop('disabled', true)
      .addClass('disabled')
      .on('click', function() { self._save(); });
   schemaElmts.discardButton
      .text($.i18n('snac-schema/discard-button'))
      .attr('title', $.i18n('snac-schema/discard-schema-changes-alt'))
      .prop('disabled', true)
      .addClass('disabled')
      .on('click', function() { self._discardChanges(); });

   this.snacPrefix = "http://www.snaccooperative.org/entity/"; // hardcoded for now

   // init the radio buttons

   $('#uploadingResourceButton').on('click', function() {
     // Show Resources
     $(".schema-alignment-dialog-columns-area-resource").show();
     $(".schema-alignment-dialog-columns-area-resource--ref").show();

     // Hide Constellations
     $(".schema-alignment-dialog-columns-area-constellation").hide();
     $(".schema-alignment-dialog-columns-area-constellation--ref").hide();

     // Hide Relations
     $(".schema-alignment-dialog-columns-area-relation").hide();
     $(".schema-alignment-dialog-columns-area-relation--ref").hide();

     snacDebug(`uploadingRes calling _hasChanged()`);
     self._hasChanged();
   });

   $('#uploadingConstellationButton').on('click', function() {
      // Hide Resources
      $(".schema-alignment-dialog-columns-area-resource").hide();
      $(".schema-alignment-dialog-columns-area-resource--ref").hide();

      // Hide Relations
      $(".schema-alignment-dialog-columns-area-relation").hide();
      $(".schema-alignment-dialog-columns-area-relation--ref").hide();

      // Show Constellations
      $(".schema-alignment-dialog-columns-area-constellation").show();
      $(".schema-alignment-dialog-columns-area-constellation--ref").show();

     snacDebug(`uploadingCon calling _hasChanged()`);
     self._hasChanged();
   });

   $('#uploadingRelationButton').on('click', function() {
     // Hide Resources
     $(".schema-alignment-dialog-columns-area-resource").hide();
     $(".schema-alignment-dialog-columns-area-resource--ref").hide();

     // Hide Constellations
     $(".schema-alignment-dialog-columns-area-constellation").hide();
     $(".schema-alignment-dialog-columns-area-constellation--ref").hide();

     // Show Relations
     $(".schema-alignment-dialog-columns-area-relation").show();
     $(".schema-alignment-dialog-columns-area-relation--ref").show();

     snacDebug(`uploadingRelation calling _hasChanged()`);
     self._hasChanged();
   });

   // Init the column area
   this.updateColumns(theProject.overlayModels.snacSchema);

   $('.schema-alignment-dialog-columns-area-constellation').hide();
   $('.schema-alignment-dialog-dropdown-area-constellation').hide();
   $('.schema-alignment-dialog-columns-area-constellation--ref').hide();

   var url = ReconciliationManager.ensureDefaultServicePresent();
   this._reconService = ReconciliationManager.getServiceFromUrl(url);

   /**
   * Init the issues tab
   */
   var issuesTab = $(DOM.loadHTML("snac", "scripts/issues-tab.html")).appendTo(this._issuesPanel);
   var issuesElmts = this._issuesElmts = DOM.bind(issuesTab);
   //issuesElmts.invalidSchemaWarningIssues.text($.i18n('snac-schema/invalid-schema-warning-issues'));

   /**
   * Init the preview tab
   */
   var previewTab = $(DOM.loadHTML("snac", "scripts/preview-tab.html")).appendTo(this._previewPanel);
   var previewElmts = this._previewElmts = DOM.bind(previewTab);
   previewElmts.reloadPreviewButton
      .text($.i18n('snac-schema/reload-preview-button'))
      .on('click', function() { self.preview(); });
   this.updateItemPreviewText("item", 0, 0);

   //previewElmts.invalidSchemaWarningPreview.text($.i18n('snac-schema/invalid-schema-warning-preview'));

   this._previewPanes = $('.schema-alignment-dialog-preview');

   // Load the existing schema
   this._reset(theProject.overlayModels.snacSchema);

   // Perform initial preview
   this.evaluateCurrentSchema(false);

   this._ignoreChanges = false;
}

/*******************************************************
* Schema Tab Matching for Resources and Constellations *
********************************************************/

//Create a table for the resource page function
SNACSchemaAlignmentDialog.addResourceTable = function (schema) {
   snacDebug(`***** [ addResourceTable ] *****`);

   //snacDebug(`addResourceTable(): schema: [${JSON.stringify(schema)}]`);

   var columns = theProject.columnModel.columns;
   var SNACcolumns = this.SNACResourceNames;

   var columnMappings = {}

   if (schema && schema.schemaType == "resource") {
      columnMappings = schema.columnMappings;
   }

   //snacDebug(`addResourceTable(): columnMappings: [${JSON.stringify(columnMappings)}]`);

   var myTableDiv = document.getElementById('myDynamicTableResource');
   if (myTableDiv == null){
      var myClassDiv = document.getElementsByClassName('snac-columns-container');
      var myTableDiv = document.createElement('div');
      myTableDiv.setAttribute('id', 'myDynamicTableResource');
      myClassDiv[0].parentNode.insertBefore(myTableDiv, myClassDiv[0]);
   }

   var table = document.createElement('TABLE');
   var tableBody = document.createElement('TBODY');
   table.appendChild(tableBody);
   myTableDiv.appendChild(table);

   makeDropdown = () => {
      let dropdownOptionsArray = [document.getElementsByClassName('dropdown-default-resource')[0]];
      for (var i = 0; i < 10; i++) {
         let dropdownOptions = document.getElementsByClassName('dropdown-option-resource')[i];
         dropdownOptionsArray.push(dropdownOptions)
      }
      return dropdownOptionsArray;
   }

   // Append Sheet columns
   for (var i = 0; i < columns.length; i++) {
      var tr = document.createElement('TR');
      tableBody.appendChild(tr);
      var column = columns[i];

      for (var j = 0; j < 2; j+=2) {
         var td = document.createElement('TD');
         td.width = '100';
         var reconConfig = column.reconConfig;
         var cell = this._createDraggableColumn(column.name,
         reconConfig && reconConfig.identifierSpace === this.snacPrefix && column.reconStats);
         var dragDivElement = cell[0];
         var dragNode = document.createElement('div');
         dragNode.className += 'wbs-draggable-column wbs-unreconciled-column-undraggable';
         dragNode.style = 'width: 150px';
         dragNode.id = i;
         dragNode.append(dragDivElement.innerHTML);
         td.appendChild(dragNode);
         tr.appendChild(td);
      }

      var selectList = $('<select></select>')
         .addClass('selectColumn')
         .addClass('selectColumnRes')
         .addClass(column.name + "ResDropDown")
         .attr('style', 'width: 180px');

      //Create and append the options
      var defaultoption = document.createElement('option');
      defaultoption.setAttribute('value', 'default');
      defaultoption.text = 'Select an Option';
      defaultoption.classList.add('dropdown-default-resource');
      selectList.append(defaultoption);

      for (var j = 0; j < SNACcolumns.length; j++) {
         var option = document.createElement('option');
         option.setAttribute('value', SNACcolumns[j]);
         option.text = SNACcolumns[j];
         option.classList.add('dropdown-option-resource');
         selectList.append(option);
      }

      if(columnMappings[column.name] != "" && columnMappings[column.name]!= undefined){
         selectList[0].value = columnMappings[column.name];
      }

      for (var j = 1; j < 2; j+=2) {
         var td = document.createElement('TD');
         td.appendChild(selectList[0]);
         tr.appendChild(td);
      }
   }

   return myTableDiv;
}

//Create a table for the constellation page function
SNACSchemaAlignmentDialog.addConstellationTable = function (schema) {
   snacDebug(`***** [ addConstellationTable ] *****`);

   //snacDebug(`addConstellationTable(): schema: [${JSON.stringify(schema)}]`);

   var columns = theProject.columnModel.columns;
   var SNACcolumns = this.SNACConstellationNames;

   var columnMappings = {}

   if (schema && schema.schemaType == "constellation") {
      columnMappings = schema.columnMappings;
   }

   //snacDebug(`addConstellationTable(): columnMappings: [${JSON.stringify(columnMappings)}]`);

   var myTableDiv = document.getElementById('myDynamicTableConstellation');
   if (myTableDiv == null){
      var myClassDiv = document.getElementsByClassName('snac-columns-container');
      var myTableDiv = document.createElement('div');
      myTableDiv.setAttribute('id', 'myDynamicTableConstellation');
      myClassDiv[0].parentNode.insertBefore(myTableDiv, myClassDiv[0]);
   }

   var table = document.createElement('TABLE');
   var tableBody = document.createElement('TBODY');
   table.appendChild(tableBody);
   myTableDiv.appendChild(table);

   makeDropdown = () => {
      let dropdownOptionsArray = [document.getElementsByClassName('dropdown-default-const')[0]];
      for (var i = 0; i < 10; i++) {
         let dropdownOptions = document.getElementsByClassName('dropdown-option-const')[i];
         dropdownOptionsArray.push(dropdownOptions)
      }
      return dropdownOptionsArray;
   }

   // Create Constellation
   for (var i = 0; i < columns.length; i++) {
      var tr = document.createElement('TR');
      tableBody.appendChild(tr);
      var column = columns[i];

      for (var j = 0; j < 2; j+=2) {
         var td = document.createElement('TD');
         td.width = '100';
         var reconConfig = column.reconConfig;
         var cell = this._createDraggableColumn(column.name,
         reconConfig && reconConfig.identifierSpace === this.snacPrefix && column.reconStats);
         var dragDivElement = cell[0];
         var dragNode = document.createElement('div');
         dragNode.className += 'wbs-draggable-column wbs-unreconciled-column-undraggable';
         dragNode.style = 'width: 150px';
         dragNode.id = i;
         dragNode.append(dragDivElement.innerHTML);
         td.appendChild(dragNode);
         tr.appendChild(td);
      }

      var selectList = $('<select></select>')
         .addClass('selectColumn')
         .addClass('selectColumnConst')
         .addClass(column.name + "ConstDropDown")
         .attr('style', 'width: 180px');

      //Create and append the options
      var defaultoption = document.createElement('option');
      defaultoption.setAttribute('value', 'default');
      defaultoption.text = 'Select an Option';
      defaultoption.classList.add('dropdown-default-const');
      selectList.append(defaultoption);

      for (var j = 0; j < SNACcolumns.length; j++) {
         var option = document.createElement('option');
         option.setAttribute('value', SNACcolumns[j]);
         option.text = SNACcolumns[j];
         option.classList.add('dropdown-option-const');
         selectList.append(option);
      }

      if(columnMappings[column.name] != "" && columnMappings[column.name]!= undefined){
         selectList[0].value = columnMappings[column.name];
      }

      for (var j = 1; j < 2; j+=2) {
         var td = document.createElement('TD');
         td.appendChild(selectList[0]);
         tr.appendChild(td);
      }
   }

   return myTableDiv;
}

//Create a table for the relation page function
SNACSchemaAlignmentDialog.addRelationTable = function (schema) {
   snacDebug(`***** [ addRelationTable ] *****`);

   //snacDebug(`addRelationTable(): schema: [${JSON.stringify(schema)}]`);

   var columns = theProject.columnModel.columns;
   var SNACcolumns = this.SNACRelationNames;

   var columnMappings = {}

   if (schema && schema.schemaType == "relation") {
      columnMappings = schema.columnMappings;
   }

   //snacDebug(`addRelationTable(): columnMappings: [${JSON.stringify(columnMappings)}]`);

   var myTableDiv = document.getElementById('myDynamicTableRelation');
   if (myTableDiv == null){
      var myClassDiv = document.getElementsByClassName('snac-columns-container');
      var myTableDiv = document.createElement('div');
      myTableDiv.setAttribute('id', 'myDynamicTableRelation');
      myClassDiv[0].parentNode.insertBefore(myTableDiv, myClassDiv[0]);
   }

   var table = document.createElement('TABLE');
   var tableBody = document.createElement('TBODY');
   table.appendChild(tableBody);
   myTableDiv.appendChild(table);

   makeDropdown = () => {
      let dropdownOptionsArray = [document.getElementsByClassName('dropdown-default-relation')[0]];
      for (var i = 0; i < 10; i++) {
         let dropdownOptions = document.getElementsByClassName('dropdown-option-relation')[i];
         dropdownOptionsArray.push(dropdownOptions)
      }
      return dropdownOptionsArray;
   }

   // Create Constellation
   for (var i = 0; i < columns.length; i++) {
      var tr = document.createElement('TR');
      tableBody.appendChild(tr);
      var column = columns[i];

      for (var j = 0; j < 2; j+=2) {
         var td = document.createElement('TD');
         td.width = '100';
         var reconConfig = column.reconConfig;
         var cell = this._createDraggableColumn(column.name,
         reconConfig && reconConfig.identifierSpace === this.snacPrefix && column.reconStats);
         var dragDivElement = cell[0];
         var dragNode = document.createElement('div');
         dragNode.className += 'wbs-draggable-column wbs-unreconciled-column-undraggable';
         dragNode.style = 'width: 150px';
         dragNode.id = i;
         dragNode.append(dragDivElement.innerHTML);
         td.appendChild(dragNode);
         tr.appendChild(td);
      }

      var selectList = $('<select></select>')
         .addClass('selectColumn')
         .addClass('selectColumnRelation')
         .addClass(column.name + "RelationDropDown")
         .attr('style', 'width: 180px');

      //Create and append the options
      var defaultoption = document.createElement('option');
      defaultoption.setAttribute('value', 'default');
      defaultoption.text = 'Select an Option';
      defaultoption.classList.add('dropdown-default-relation');
      selectList.append(defaultoption);

      for (var j = 0; j < SNACcolumns.length; j++) {
         var option = document.createElement('option');
         option.setAttribute('value', SNACcolumns[j]);
         option.text = SNACcolumns[j];
         option.classList.add('dropdown-option-relation');
         selectList.append(option);
      }

      if(columnMappings[column.name] != "" && columnMappings[column.name]!= undefined){
         selectList[0].value = columnMappings[column.name];
      }

      for (var j = 1; j < 2; j+=2) {
         var td = document.createElement('TD');
         td.appendChild(selectList[0]);
         tr.appendChild(td);
      }
   }

   return myTableDiv;
}

// TODO: Collapse hideAndDisable functions into one function
SNACSchemaAlignmentDialog.hideAndDisableRes = function() {
   var dragResources =$.makeArray($('.dragResource'));

   const selectedValue = [];  //Array to hold selected values
   $('.selectColumnRes').find(':selected').filter(function(i, el) { // Filter selected values and push to array
      return $(el).val();
   }).each(function(i, el) {
      selectedValue.push($(el).val());
   });

   $('.selectColumnRes').find('.dropdown-option-resource').each(function(i, option) {   // Loop through all of the options
      if (selectedValue.indexOf($(option).val()) > -1) { // Re-enable option if array does not contain current value
         if ($(option).is(':checked')) {  // Disable if current value is selected, else skip
            return;
         } else {
            $(this).attr('disabled', true);
            dragResources.forEach(r => {
               if(r.value==this.innerHTML){
                  r.style.visibility = 'hidden';   // Hide value
               };
            });
         }
      } else {
         $(this).attr('disabled', false);
         dragResources.forEach(c => {
            if(c.value==this.innerHTML){
               c.style.visibility = 'visible';  // Show value
            };
         });
      }
   });
};

SNACSchemaAlignmentDialog.hideAndDisableConst = function() {
   var dragConstellations =$.makeArray($('.dragConstellation'));

   const selectedValue = [];  //Array to hold selected values
   $('.selectColumnConst').find(':selected').filter(function(i, el) { //Filter selected values and push to array
      return $(el).val();
   }).each(function(i, el) {
      selectedValue.push($(el).val());
   });

   // loop all the options
   $('.selectColumnConst').find('.dropdown-option-const').each(function(i, option) {
      if (selectedValue.indexOf($(option).val()) > -1) { //Re-enable option if array does not contain current value
         if ($(option).is(':checked')) {  //Disable if current value is selected, else skip
            return;
         } else {
            $(this).attr('disabled', true);
            dragConstellations.forEach(r => {
               if(r.value==this.innerHTML){
                  r.style.visibility = 'hidden';   //Hide value
               };
            });
         }
      } else {
         $(this).attr('disabled', false);
         dragConstellations.forEach(c => {
            if(c.value==this.innerHTML){
               c.style.visibility = 'visible';  //Show value
            };
         });
      }
   });
};

SNACSchemaAlignmentDialog.hideAndDisableRelation = function() {
   var dragRelations =$.makeArray($('.dragRelation'));

   const selectedValue = [];  //Array to hold selected values
   $('.selectColumnRelation').find(':selected').filter(function(i, el) { //Filter selected values and push to array
      return $(el).val();
   }).each(function(i, el) {
      selectedValue.push($(el).val());
   });

   // loop all the options
   $('.selectColumnRelation').find('.dropdown-option-relation').each(function(i, option) {
      if (selectedValue.indexOf($(option).val()) > -1) { //Re-enable option if array does not contain current value
         if ($(option).is(':checked')) {  //Disable if current value is selected, else skip
            return;
         } else {
            $(this).attr('disabled', true);
            //TODO: replace with filter
            dragRelations.forEach(r => {
               if(r.value==this.innerHTML){
                  r.style.visibility = 'hidden';   //Hide value
               };
            });
         }
      } else {
         $(this).attr('disabled', false);
         //TODO: replace with filter
         dragRelations.forEach(c => {
            if(c.value==this.innerHTML){
               c.style.visibility = 'visible';  //Show value
            };
         });
      }
   });
};


SNACSchemaAlignmentDialog.updateColumns = function(schema) {
  snacDebug(`***** [ updateColumns ] *****`);

  var self = this;

  //snacDebug(`updateColumns(): schema: [${JSON.stringify(schema)}]`);

  var columns = theProject.columnModel.columns;

  // TODO: Consider collapsing resources/constellations/relations in a refactor

  // ******* RESOURCES PAGE ******* //
  this._columnAreaResource = $(
    ".schema-alignment-dialog-columns-area-resource"
  );
  this._columnAreaResource.addClass("snac-tab");
  this._columnAreaResource.empty();
  this._columnAreaResource.html("<h2>Columns</h2>");

  this._dropdownAreaResource = $(
    ".schema-alignment-dialog-dropdown-area-resource"
  );
  this._dropdownAreaResource.addClass("snac-tab");
  this._dropdownAreaResource.empty();

  this._refcolumnAreaResource = $(
    ".schema-alignment-dialog-columns-area-resource--ref"
  );
  this._refcolumnAreaResource.addClass("snac-tab");
  this._refcolumnAreaResource.html("<h2>SNAC Resource Description Model</h2>");

  var myTableDivResource = this.addResourceTable(schema);
  this._columnAreaResource.append(myTableDivResource);

  this.SNACResourceModel.forEach((resource) => {
    var cell = this._createDraggableColumn(resource.name, false);
    cell.addClass("dragResource");
    cell.val(resource.name).trigger('change');
    this._refcolumnAreaResource.append(cell);
  });
  $(".dragResource").addClass("tooltip");

  for (var i = 0; i < this.SNACResourceTooltips.length; i++) {
    var toolTipSpan = document.createElement("span");
    var toolTiptext = document.createTextNode(this.SNACResourceTooltips[i]);
    toolTipSpan.classList.add("tooltiptext");
    toolTipSpan.appendChild(toolTiptext);
    $(".dragResource")[i].appendChild(toolTipSpan);
  }

  // ******* CONSTELLATIONS PAGE ******* //
  this._columnAreaConstellation = $(
    ".schema-alignment-dialog-columns-area-constellation"
  );
  this._columnAreaConstellation.addClass("snac-tab");
  this._columnAreaConstellation.empty();
  this._columnAreaConstellation.html("<h2>Columns</h2>");

  this._dropdownAreaConestellation = $(
    ".schema-alignment-dialog-dropdown-area-constellation"
  );
  this._dropdownAreaConestellation.addClass("snac-tab");
  this._dropdownAreaConestellation.empty();

  // Based on SNACConstellationCreator
  this._refcolumnAreaConestellation = $(
    ".schema-alignment-dialog-columns-area-constellation--ref"
  );
  this._refcolumnAreaConestellation.addClass("snac-tab");
  this._refcolumnAreaConestellation.html("<h2>SNAC CPF Model</h2>");

  var myTableDivConstellation = this.addConstellationTable(schema);
  this._columnAreaConstellation.append(myTableDivConstellation);

  // Create draggable constellation column
  this.SNACConstellationModel.forEach((field) => {
    var cell = this._createDraggableColumn(field.name, false);
    cell.addClass("dragConstellation");
    cell.val(field.name).trigger('change');
    this._refcolumnAreaConestellation.append(cell);
  });

  $(".dragConstellation").addClass("tooltip");

  for (var i = 0; i < this.SNACConstellationTooltips.length; i++) {
    var toolTipSpan = document.createElement("span");
    var toolTiptext = document.createTextNode(
      this.SNACConstellationTooltips[i]
    );
    toolTipSpan.classList.add("tooltiptext");
    toolTipSpan.appendChild(toolTiptext);
    $(".dragConstellation")[i].appendChild(toolTipSpan);
  }

  // ******* RELATIONS PAGE ******* //
  this._columnAreaRelation = $(
    ".schema-alignment-dialog-columns-area-relation"
  );
  this._columnAreaRelation.addClass("snac-tab");
  this._columnAreaRelation.empty();
  this._columnAreaRelation.html("<h2>Columns</h2>");

  this._dropdownAreaConestellation = $(
    ".schema-alignment-dialog-dropdown-area-relation"
  );
  this._dropdownAreaConestellation.addClass("snac-tab");
  this._dropdownAreaConestellation.empty();

  // Based on SNACRelationCreator
  this._refcolumnAreaConestellation = $(
    ".schema-alignment-dialog-columns-area-relation--ref"
  );
  this._refcolumnAreaConestellation.addClass("snac-tab");
  this._refcolumnAreaConestellation.html("<h2>SNAC Join Model</h2>");

  var myTableDivRelation = this.addRelationTable(schema);
  this._columnAreaRelation.append(myTableDivRelation);

  // Create draggable Relation column
  this.SNACRelationModel.forEach((field) => {
    var cell = this._createDraggableColumn(field.name, false);
    cell.addClass("dragRelation");
    cell.val(field.name).trigger('change');
    this._refcolumnAreaConestellation.append(cell);
  });

  $(".dragRelation").addClass("tooltip");

  for (var i = 0; i < this.SNACRelationTooltips.length; i++) {
    var toolTipSpan = document.createElement("span");
    var toolTiptext = document.createTextNode(
      this.SNACRelationTooltips[i]
    );
    toolTipSpan.classList.add("tooltiptext");
    toolTipSpan.appendChild(toolTiptext);
    $(".dragRelation")[i].appendChild(toolTipSpan);
  }
  // ******* END RELATIONS PAGE ******* //


  // Resource Validator Call onChange
  $(".selectColumnRes").on("change", function () {
    self.hideAndDisableRes();
    snacDebug(`selectColumnRes calling _hasChanged()`);
    self._hasChanged();
  });

  // Constellation Validator Call onChange
  $(".selectColumnConst").on("change", function () {
    self.hideAndDisableConst();
    snacDebug(`selectColumnConst calling _hasChanged()`);
    self._hasChanged();
  });

  // TODO: DONE: Add selectColumnRelations
  // Relation Validator Call onChange
  $(".selectColumnRelation").on("change", function () {
    self.hideAndDisableRelation();
    snacDebug(`selectColumnRelation calling _hasChanged()`);
    self._hasChanged();
  });


  /*
   $(document).ready(function() {
      self.hideAndDisableRes();
      self.hideAndDisableConst();
   });
*/

  //Allow names column (first column) to be droppable
  $(".wbs-unreconciled-column-undraggable").droppable({
    hoverClass: "active",
    drop: function (event, ui) {
      var id = $(this).attr("id");
      $(".selectColumn")[id].value = $(ui.draggable).val();
      self.hideAndDisableRes();
      self.hideAndDisableConst();
      self.hideAndDisableRelation();
      snacDebug(`selectColumn[${id}] (names) calling _hasChanged()`);
      self._hasChanged();
    },
  });


  //Allow dropdown column to be droppable
  $(".selectColumn").droppable({
    hoverClass: "active",
    drop: function (event, ui) {
      this.value = $(ui.draggable).val();
      self.hideAndDisableRes();
      self.hideAndDisableConst();
      self.hideAndDisableRelation();
      snacDebug(`selectColumn (dropdowns) calling _hasChanged()`);
      self._hasChanged();
    },
  });
  $(".wbs-reconciled-column").draggable({
    helper: "clone",
    cursor: "crosshair",
    snap: ".wbs-item-input input, .wbs-target-input input",
    zIndex: 100,
  });
  $(".wbs-unreconciled-column").draggable({
    helper: "clone",
    helper: function (e) {
      var original = $(e.target).hasClass("ui-draggable")
        ? $(e.target)
        : $(e.target).closest(".ui-draggable");
      return original.clone().css({
        width: original.width(), // or outerWidth*
      });
    },
    cursor: "crosshair",
    snap: ".wbs-target-input input",
    zIndex: 100,
  });

  this.hideAndDisableRes();
  this.hideAndDisableConst();
  this.hideAndDisableRelation();
}

SNACSchemaAlignmentDialog.switchTab = function(targetTab) {
   snacDebug(`***** [ switchTab ] *****`);

   $('.main-view-panel-tab').hide();
   $('.main-view-panel-tab-header').removeClass('active');
   $('.main-view-panel-tab-header[href="'+targetTab+'"]').addClass('active');

/*
   if (targetTab === "#snac-preview-panel") {
     this.preview();
   }
*/

   $(targetTab).show();
   resizeAll();
   var panelHeight = this._viewPanel.height();
   this._schemaPanel.height(panelHeight);
   this._issuesPanel.height(panelHeight);
   this._previewPanel.height(panelHeight);
   // Resize the inside of the schema panel
   var headerHeight = this._schemaElmts.schemaHeader.outerHeight();
   this._schemaElmts.canvas.height(panelHeight - headerHeight - 10);

   if (targetTab === "#view-panel") {
      ui.dataTableView.render();
   }
}

SNACSchemaAlignmentDialog.isSetUp = function() {
   snacDebug(`***** [ isSetUp ] *****`);

   return $('#snac-schema-panel').length !== 0;
}

SNACSchemaAlignmentDialog.launch = function(onDone) {
   snacDebug(`***** [ launch ] *****`);

   this._onDone = onDone;
   this._hasUnsavedChanges = false;

   if (!this.isSetUp()) {
      this.setUpTabs();
   }

   $('.main-view-panel-tabs-snac').show();
   $('.main-view-panel-tabs-wiki').hide();

   this.switchTab('#snac-schema-panel');
}

var beforeUnload = function(e) {
   if (SNACSchemaAlignmentDialog.isSetUp() && SNACSchemaAlignmentDialog._hasUnsavedChanges === true) {
      return (e = $.i18n('snac-schema/unsaved-warning'));
   }
};

$(window).on('beforeunload', beforeUnload);

SNACSchemaAlignmentDialog._reset = function(schema) {
   snacDebug(`***** [ _reset ] *****`);

   this._ignoreChanges = true;

   this._originalSchema = schema || { schemaType: "", idColumn: "", columnMappings: {} };
   this._schema = cloneDeep(this._originalSchema); // this is what can be munched on

   $('#snac-columns-container').empty();
   this.updateColumns(schema);
   if (this._schema.schemaType == "constellation") {
      $('#uploadingConstellationButton').trigger('click');
   } else if (this._schema.schemaType == 'relation') {
      $('#uploadingRelationButton').trigger('click');
   } else {
      $('#uploadingResourceButton').trigger('click');

   }

   this._ignoreChanges = false;
};

SNACSchemaAlignmentDialog._save = function(onDone) {
   snacDebug(`***** [ _save ] *****`);

   var self = this;
   var schema = this.getJSON();

   snacDebug(`_save(): cur overlay model: [${JSON.stringify(theProject.overlayModels.snacSchema)}]`);
   snacDebug(`_save(): new schema 2 save: [${JSON.stringify(schema)}]`);

   Refine.postProcess(
      "snac",
      "save-schema",
      {},
      { schema: JSON.stringify(schema) },
      {},
      {
         onDone: function() {
            snacDebug(`_save(): SUCCESS`);
            theProject.overlayModels.snacSchema = schema;
            snacDebug(`_save(): new overlay model: [${JSON.stringify(theProject.overlayModels.snacSchema)}]`);

            self.updateColumns(theProject.overlayModels.snacSchema);

            $('.invalid-schema-warning').hide();
            self._changesCleared();

            if (onDone) onDone();
         },
         onError: function(e) {
            snacDebug(`_save(): FAILURE`);
            alert($.i18n('snac-schema/incomplete-schema-could-not-be-saved'));
         },
      }
   );
};

SNACSchemaAlignmentDialog._discardChanges = function() {
   snacDebug(`***** [ _discardChanges ] *****`);

   this._reset(theProject.overlayModels.snacSchema);
   this._changesCleared();
   this.evaluateCurrentSchema(false);
}

SNACSchemaAlignmentDialog._changesCleared = function() {
   snacDebug(`***** [ _changesCleared ] *****`);

   this._hasUnsavedChanges = false;
   this._unsavedIndicator.hide();
   this._schemaElmts.saveButton
      .prop('disabled', true)
      .addClass('disabled');
   this._schemaElmts.discardButton
      .prop('disabled', true)
      .addClass('disabled');
}

//format cells for columns
SNACSchemaAlignmentDialog._createDraggableColumn = function(name, reconciled, org) {
   snacDebug(`***** [ _createDraggableColumn ] *****`);

   var cell = $('<div></div>').addClass('wbs-draggable-column').text(name);
   if (reconciled) {
      cell.addClass('wbs-reconciled-column');
   } else {
      cell.addClass('wbs-unreconciled-column');
   }

   if (name == 'SNAC Resource ID' || name == 'SNAC CPF ID') {
      cell.addClass('idcolumn')
   }

   return cell;
}

SNACSchemaAlignmentDialog.getJSON = function() {
   snacDebug(`***** [ getJSON ] *****`);

   var schemaType = "unknown";
   var idColumn = "";
   var dropDownColumn;

   // determine source of schema values

   if ($('#uploadingResourceButton').is(':checked')) {
      schemaType = "resource";
      dropDownColumn = $('.selectColumnRes');

   } else if ($('#uploadingRelationButton').is(':checked')) {
      schemaType = 'relation';
      dropDownColumn = $('.selectColumnRelation');
   } else {
      schemaType = 'constellation';
      dropDownColumn = $('.selectColumnConst');
   }

   var dropDownValues = $.map(dropDownColumn, function(option) {
      return option.value;
   });

   // build schema from selected values

   var schema = { schemaType: schemaType, idColumn: idColumn, columnMappings: {} };

   var columns = theProject.columnModel.columns;
   for (var i = 0; i < columns.length; i++) {
      let value = dropDownValues[i];
      if (value != "default") {
         schema.columnMappings[columns[i].name] = value;
      }
   }

   snacDebug(`getJSON(): schema: [${JSON.stringify(schema)}]`);

   return schema;
};

SNACSchemaAlignmentDialog.schemaIsValid = function() {
   snacDebug(`***** [ schemaIsValid ] *****`);

   var schema = this.getJSON();

   var required;

   if (schema.schemaType == "constellation") {
      required = this.SNACConstellationNamesRequired;
   } else if (schema.schemaType == "resource") {
      required = this.SNACResourceNamesRequired;
   } else {
      required = this.SNACRelationNamesRequired;
   }

   var warnings = [];
   var mappedColumns = Object.values(schema.columnMappings);

   //snacDebug(`mappedColumns:`);
   //snacDebug(mappedColumns);

   // ensure each required column is present in the list
   for (var i = 0; i < required.length; i++) {
      if (!mappedColumns.includes(required[i])) {
         snacDebug(`schemaIsValid(): ${schema.schemaType}: required field [${required[i]}] is not present`);
         warnings.push({ title: "Missing Required Field", body: `The ${required[i]} field must be assigned to a column.` });
      }
   }

   this._updateWarnings(warnings, warnings.length);

   if (warnings.length > 0) {
      return false;
   }

   return true;
};

SNACSchemaAlignmentDialog.evaluateCurrentSchema = function(enableButtons) {
   // disable save button if schema is not valid

    snacDebug(`evaluateCurrentSchema() calling preview()`);
    this.preview();

   if (!this.schemaIsValid()) {
//      this.issueSpinner.hide();
//      this.previewSpinner.hide();

      this._schemaElmts.saveButton
         .prop('disabled', true)
         .addClass('disabled');
   } else {
      if (enableButtons) {
         this._schemaElmts.saveButton
            .prop('disabled', false)
            .removeClass('disabled');
      }

      this._updateWarnings([], 0);
   }

   if (enableButtons) {
      this._schemaElmts.discardButton
         .prop('disabled', false)
         .removeClass('disabled');
   }
};

// Update everything when schema has changed
SNACSchemaAlignmentDialog._hasChanged = function() {
   snacDebug(`***** [ _hasChanged ] *****`);

   if (this._ignoreChanges) {
      snacDebug(`_hasChanged(): ignoring changes`);
      return;
   }

   this._hasUnsavedChanges = true;
   this._unsavedIndicator.show();

   this.evaluateCurrentSchema(true);
}

SNACSchemaAlignmentDialog.updateItemPreviewText = function(itemType, itemCount, previewCount) {
   this._previewElmts.previewExplanation.text(
      $.i18n('snac-schema/preview-explanation')
         .replace('{preview_count}', previewCount)
         .replace('{item_type}', itemType)
         .replace('{item_count}', itemCount));
   this._previewElmts.previewNote.html($.i18n('snac-schema/preview-note'));
}

SNACSchemaAlignmentDialog.preview = function() {
   snacDebug(`***** [ preview ] *****`);

   var self = this;

   $('.invalid-schema-warning').hide();
   this._previewPanes.empty();
   this.updateItemPreviewText("item", 0, 0);
//   this.issueSpinner.show();
   this.previewSpinner.show();
   var schema = this.getJSON();
   if (schema === null) {
      $('.invalid-schema-warning').show();
      return;
   }

   Refine.postCSRF(
      "command/snac/preview-schema?" + $.param({ project: theProject.id }),
      { schema: JSON.stringify(schema), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
      function(data) {
//         self.issueSpinner.hide();
         self.previewSpinner.hide();
         //snacDebug(`preview(): SUCCESS  data = [${JSON.stringify(data)}]`);

         if ("items_preview" in data) {
            var previewContainer = self._previewPanes[0];

            data.items_preview.forEach(function(item) {
              $('<hr>').appendTo(previewContainer);
              $('<p></p>').html(item).appendTo(previewContainer);
            });

            $('<hr>').appendTo(previewContainer);

            self.updateItemPreviewText(schema.schemaType, data["item_count"], data.items_preview.length);
         }

         snacDebug(`preview(): added ${data["item_count"]} items`);

         //self._updateWarnings([], 0);

         if ("code" in data && data.code === "error") {
            $('.invalid-schema-warning').show();
         }
      },
      "json"
   );
};

Refine.registerUpdateFunction(function(options) {
   snacDebug(`***** [ Refine.registerUpdateFunction ] *****`);

   // Inject tabs in any project where the schema has been defined
   if(theProject.overlayModels.snacSchema && !SNACSchemaAlignmentDialog.isSetUp()) {
      SNACSchemaAlignmentDialog.setUpTabs();
   }
   if (SNACSchemaAlignmentDialog.isSetUp() && (options.everythingChanged || options.modelsChanged ||
      options.rowsChanged || options.rowMetadataChanged || options.cellsChanged || options.engineChanged)) {
         // always discard changes (not even sure this logic is correct anyway)
//         if (!SNACSchemaAlignmentDialog._hasUnsavedChanges) {
//            SNACSchemaAlignmentDialog._discardChanges();
//         }
         SNACSchemaAlignmentDialog._discardChanges();

//         SNACSchemaAlignmentDialog.updateColumns(theProject.overlayModels.snacSchema);
//         SNACSchemaAlignmentDialog.preview();
   }
});

/*************************
 * WARNINGS RENDERING *
 *************************/

SNACSchemaAlignmentDialog._updateWarnings = function(warnings, totalCount) {
   var mainDiv = $('#snac-issues-panel');
   var countsElem = this.issuesTabCount;

   // clear everything
   mainDiv.empty();
   countsElem.hide();

   // FIXME: this differs...
   // Add any warnings
   var table = $('<table></table>').appendTo(mainDiv);
   for (const warning of warnings) {
      var tr = $('<tr></tr>').addClass('wb-warning');
      var bodyTd = $('<td></td>')
         .addClass('wb-warning-body')
         .appendTo(tr);
      var h1 = $('<h1></h1>')
         .html(warning.title)
         .appendTo(bodyTd);
      var p = $('<p></p>')
         .html(warning.body)
         .appendTo(bodyTd);
      var countTd = $('<td></td>')
         .addClass('wb-warning-count')
         .appendTo(tr);
      tr.appendTo(table);
   }

   // update the warning counts
   if (totalCount) {
      countsElem.text(totalCount);
      countsElem.show();
   }

   return totalCount;
}
