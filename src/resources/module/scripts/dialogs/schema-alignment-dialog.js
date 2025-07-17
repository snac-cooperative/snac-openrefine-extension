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

var SNACSchemaAlignmentDialog = {
   _ignoreChanges: true,
   _debug: false
};

var snacDebug = function(s) {
   if (SNACSchemaAlignmentDialog._debug) {
      console.log(s);
   }
};

SNACSchemaAlignmentDialog.getCases = function(str) {
  var lower = str.toLowerCase();                              // e.g. 'resource'
  var upper = lower.charAt(0).toUpperCase() + lower.slice(1); // e.g. 'Resource'
  return { lower: lower, upper: upper };
}

SNACSchemaAlignmentDialog.setup = function(onDone) {
   this.setupModel(onDone);
}

SNACSchemaAlignmentDialog.setupModel = function(onDone) {
   snacDebug(`***** [ setupModel ] *****`);

   var _this = this;

   $.get(
      "command/snac/get-model",
      function(data) {
         snacDebug(`setupModel(): SUCCESS  data = [${JSON.stringify(data)}]`);

         _this._model = data;

         _this.setupTabs();

         if (onDone) onDone();
      });
};

/**
 * Installs the tabs in the UI the first time the snac
 * extension is called.
 */
SNACSchemaAlignmentDialog.setupTabs = function() {
   snacDebug(`***** [ setupTabs ] *****`);

   var _this = this;

   // set up the initial schema
   this._defaultSchema = { schemaType: 'resource', columnMappings: {} }
   this._schema = theProject.overlayModels.snacSchema || this._defaultSchema;

   this._ignoreChanges = true;

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
      .attr('href', '#snac-schema-panel')
      .text($.i18n('snac-schema/schema-tab-header'))
      .appendTo(this._toolPanel)

   var issuesButton = $('<div></div>')
      .addClass('main-view-panel-tab-header')
      .attr('href', '#snac-issues-panel')
      .text($.i18n('snac-schema/warnings-tab-header')+' ')
      .appendTo(this._toolPanel);

   this.issuesTabCount = $('<span></span>')
      .addClass('snac-schema-alignment-total-warning-count')
      .appendTo(issuesButton)
      .hide();

   this.issueSpinner = $('<img />')
      .attr('src', 'images/large-spinner.gif')
      .attr('width', '16px')
      .appendTo(issuesButton)
      .hide();

   var previewButton = $('<div></div>')
      .addClass('main-view-panel-tab-header')
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

   $('.main-view-panel-tab-header').hide();

   $('.main-view-panel-tab-header').on('click', function(e) {
      var targetTab = $(this).attr('href');
      _this.switchTab(targetTab);
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
      .on('click', function() { _this.save(); });
   schemaElmts.discardButton
      .text($.i18n('snac-schema/discard-button'))
      .attr('title', $.i18n('snac-schema/discard-schema-changes-alt'))
      .prop('disabled', true)
      .addClass('disabled')
      .on('click', function() { _this.discardChanges(); });

   this.snacPrefix = "http://www.snaccooperative.org/entity/"; // hardcoded for now

   // init the radio buttons

   $('#uploadingResourceButton').on('click', function() {
      // Show Resources
      $(".snac-schema-alignment-dialog-openrefine-names-area-resource").show();
      $(".snac-schema-alignment-dialog-schema-fields-area-resource").show();

      // Hide Constellations
      $(".snac-schema-alignment-dialog-openrefine-names-area-constellation").hide();
      $(".snac-schema-alignment-dialog-schema-fields-area-constellation").hide();

      // Hide Relations
      $(".snac-schema-alignment-dialog-openrefine-names-area-relation").hide();
      $(".snac-schema-alignment-dialog-schema-fields-area-relation").hide();

      snacDebug(`uploadingRes calling hasChanged()`);
      _this.hasChanged();
   });

   $('#uploadingConstellationButton').on('click', function() {
      // Hide Resources
      $(".snac-schema-alignment-dialog-openrefine-names-area-resource").hide();
      $(".snac-schema-alignment-dialog-schema-fields-area-resource").hide();

      // Show Constellations
      $(".snac-schema-alignment-dialog-openrefine-names-area-constellation").show();
      $(".snac-schema-alignment-dialog-schema-fields-area-constellation").show();

      // Hide Relations
      $(".snac-schema-alignment-dialog-openrefine-names-area-relation").hide();
      $(".snac-schema-alignment-dialog-schema-fields-area-relation").hide();

      snacDebug(`uploadingCon calling hasChanged()`);
      _this.hasChanged();
   });

   $('#uploadingRelationButton').on('click', function() {
      // Hide Resources
      $(".snac-schema-alignment-dialog-openrefine-names-area-resource").hide();
      $(".snac-schema-alignment-dialog-schema-fields-area-resource").hide();

      // Hide Constellations
      $(".snac-schema-alignment-dialog-openrefine-names-area-constellation").hide();
      $(".snac-schema-alignment-dialog-schema-fields-area-constellation").hide();

      // Show Relations
      $(".snac-schema-alignment-dialog-openrefine-names-area-relation").show();
      $(".snac-schema-alignment-dialog-schema-fields-area-relation").show();

      snacDebug(`uploadingRelation calling hasChanged()`);
      _this.hasChanged();
   });

   // Init the column area
   this.updateColumns(this._schema);

   $('.snac-schema-alignment-dialog-openrefine-names-area-constellation').hide();
   $('.snac-schema-alignment-dialog-schema-fields-area-constellation').hide();

   var url = ReconciliationManager.ensureDefaultServicePresent();
   this._reconService = ReconciliationManager.getServiceFromUrl(url);

   /**
   * Init the issues tab
   */
   var issuesTab = $(DOM.loadHTML("snac", "scripts/issues-tab.html")).appendTo(this._issuesPanel);
   var issuesElmts = this._issuesElmts = DOM.bind(issuesTab);

   /**
   * Init the preview tab
   */
   var previewTab = $(DOM.loadHTML("snac", "scripts/preview-tab.html")).appendTo(this._previewPanel);
   var previewElmts = this._previewElmts = DOM.bind(previewTab);
   previewElmts.reloadPreviewButton
      .text($.i18n('snac-schema/reload-preview-button'))
      .on('click', function() { _this.preview(); });
   this.updateItemPreviewText("item", 0, 0);

   this._previewPanes = $('.snac-schema-alignment-dialog-preview');

   // Load the existing schema
   this.reset(theProject.overlayModels.snacSchema);

   // Perform initial preview
   this.evaluateCurrentSchema(false);

   this._ignoreChanges = false;
}

/*******************************************************************
* Schema Tab Matching for Resources, Constellations, and Relations *
*******************************************************************/

// Create a table for the fields of the given type
SNACSchemaAlignmentDialog.addTable = function (schema, type, names) {
   var types = this.getCases(type);

   snacDebug(`***** [ addTable(${types.lower}) ] *****`);

   //snacDebug(`addTable(): schema: [${JSON.stringify(schema)}]`);

   var columns = theProject.columnModel.columns;

   var columnMappings = {}

   if (schema && schema.schemaType == types.lower) {
      columnMappings = schema.columnMappings;
   }

   //snacDebug(`addTable(${types.lower}): columnMappings: [${JSON.stringify(columnMappings)}]`);

   var myTableDiv = document.getElementById(`snacDynamicTable${types.upper}`);
   if (myTableDiv == null) {
      var myClassDiv = document.getElementsByClassName('snac-dynamic-data-container');
      var myTableDiv = document.createElement("div");
      myTableDiv.setAttribute('id', `snacDynamicTable${types.upper}`);
      myClassDiv[0].parentNode.insertBefore(myTableDiv, myClassDiv[0]);
   }

   var table = document.createElement("table");
   var tableBody = document.createElement("tbody");
   table.appendChild(tableBody);
   myTableDiv.appendChild(table);

   // Create column
   for (var i = 0; i < columns.length; i++) {
      var tr = document.createElement("tr");
      tableBody.appendChild(tr);
      var column = columns[i];

      var td1 = document.createElement("td");

      var cell = $('<div></div>');
      cell.addClass('snac-cell');
      cell.addClass('snac-cell-openrefine-name');
      cell.text(column.name);
      // this is used when dropping a snac schema field onto an
      // openrefine column name to update the correct dropdown list
      cell.prop('id', `${types.lower}:${i}`);

      td1.appendChild(cell[0]);
      tr.appendChild(td1);

      var selectList = $('<select></select>');
      selectList.addClass('snac-cell')
      selectList.addClass('snac-cell-select-dropdown')
      //selectList.addClass('snac-select-field')
      selectList.addClass(`snac-select-values-${types.lower}`)
      selectList.addClass(`${column.name}${types.upper}DropDown`);

      // Create and append the options
      var defaultoption = document.createElement("option");
      defaultoption.setAttribute('value', 'default');
      defaultoption.text = 'Select an Option';
      defaultoption.classList.add(`dropdown-default-${types.lower}`);
      selectList.append(defaultoption);

      for (var j = 0; j < names.length; j++) {
         var option = document.createElement("option");
         option.setAttribute('value', names[j]);
         option.text = names[j];
         option.classList.add(`dropdown-option-${types.lower}`);
         selectList.append(option);
      }

      if (columnMappings[column.name] != "" && columnMappings[column.name]!= undefined) {
         selectList[0].value = columnMappings[column.name];
      }

      var td2 = document.createElement("td");
      td2.appendChild(selectList[0]);
      tr.appendChild(td2);
   }

   return myTableDiv;
}

SNACSchemaAlignmentDialog.hideAndDisable = function(type) {
   var types = this.getCases(type);

   snacDebug(`***** [ hideAndDisable(${types.lower}) ] *****`);

   var dragItems = $.makeArray($(`.snac-drag-${types.lower}`));

   const selectedValue = []; // Array to hold selected values
   $(`.snac-select-values-${types.lower}`).find(':selected').filter(function(i, el) { // Filter selected values and push to array
      return $(el).val();
   }).each(function(i, el) {
      selectedValue.push($(el).val());
   });

   $(`.snac-select-values-${types.lower}`).find(`.dropdown-option-${types.lower}`).each(function(i, option) { // Loop through all of the options
      if (selectedValue.indexOf($(option).val()) > -1) { // Re-enable option if array does not contain current value
         if ($(option).is(':checked')) {  // Disable if current value is selected, else skip
            return;
         } else {
            $(this).attr('disabled', true);
            dragItems.forEach(r => {
               if (r.value==this.innerHTML) {
                  r.style.visibility = 'hidden';   // Hide value
               };
            });
         }
      } else {
         $(this).attr('disabled', false);
         dragItems.forEach(c => {
            if (c.value==this.innerHTML) {
               c.style.visibility = 'visible';  // Show value
            };
         });
      }
   });
};

SNACSchemaAlignmentDialog.hideAndDisableResource = function() {
   this.hideAndDisable('resource');
}

SNACSchemaAlignmentDialog.hideAndDisableConstellation = function() {
   this.hideAndDisable('constellation');
}

SNACSchemaAlignmentDialog.hideAndDisableRelation = function() {
   this.hideAndDisable('relation');
}

SNACSchemaAlignmentDialog.hideAndDisableAll = function() {
   this.hideAndDisableResource();
   this.hideAndDisableConstellation();
   this.hideAndDisableRelation();
}

SNACSchemaAlignmentDialog.updateColumn = function(schema, type, header) {
   var types = this.getCases(type);

   snacDebug(`***** [ updateColumn(${types.lower}) ] *****`);

   var _this = this;

   var names = this._model[type].map(x => x.name);
   var tooltips = this._model[type].map(x => x.tooltips);

   // openrefine column names and dropdowns are appended here
   var columnArea = $(`.snac-schema-alignment-dialog-openrefine-names-area-${types.lower}`);
   columnArea.addClass('snac-area-openrefine-name');
   columnArea.empty();
   columnArea.html("<h2>OpenRefine Columns</h2>");

   var columnDiv = $('<div></div>');
   columnDiv.addClass('snac-dynamic-table');
   columnDiv.append(this.addTable(schema, types.lower, names));

   columnArea.append(columnDiv);

   // snac schema field names are appended here
   var refcolumnArea = $(`.snac-schema-alignment-dialog-schema-fields-area-${types.lower}`);
   refcolumnArea.addClass('snac-area-schema-field');
   refcolumnArea.empty();
   refcolumnArea.html(`<h2>${header}</h2>`);

   var refDiv = $('<div></div>');
   refDiv.addClass('snac-dynamic-table');

   var refTable = document.createElement("table");
   var refTableBody = document.createElement("tbody");
   refTable.appendChild(refTableBody);

   this._model[type].forEach((field) => {
      var tr = document.createElement("tr");
      var td = document.createElement("td");

      var cell = $('<div></div>');
      cell.addClass('snac-cell');
      cell.addClass('snac-cell-schema-field');
      cell.addClass(`snac-drag-${types.lower}`);
      cell.addClass('snac-tooltip');
      if (field.required) {
         cell.addClass('snac-cell-schema-field-required');
      }
      cell.text(field.name);
      cell.val(field.name).trigger('change');

      td.appendChild(cell[0]);
      tr.appendChild(td);
      refTableBody.appendChild(tr);
   });

   refDiv.append(refTable);
   refcolumnArea.append(refDiv);

   // add tooltips to elements of this type's drag class by index
   for (var i = 0; i < tooltips.length; i++) {
      var toolTipSpan = document.createElement("span");
      var toolTiptext = document.createTextNode(tooltips[i]);
      toolTipSpan.classList.add('snac-tooltip-text');
      toolTipSpan.appendChild(toolTiptext);
      $(`.snac-drag-${types.lower}`)[i].appendChild(toolTipSpan);
   }

   // setup for redraw on change
   $(`.snac-select-values-${types.lower}`).on("change", function () {
      _this.hideAndDisable(types.lower);
      snacDebug(`snac-select-values-${types.lower} calling hasChanged()`);
      _this.hasChanged();
   });
}

SNACSchemaAlignmentDialog.updateColumns = function(schema) {
   snacDebug(`***** [ updateColumns ] *****`);

   var _this = this;

   //snacDebug(`updateColumns(): schema: [${JSON.stringify(schema)}]`);

   var columns = theProject.columnModel.columns;

   this.updateColumn(schema, 'resource',      "SNAC Resource Description Model");
   this.updateColumn(schema, 'constellation', "SNAC CPF Model");
   this.updateColumn(schema, 'relation',      "SNAC Join Model");

   // Allow names column (first column) to be droppable
   $(".snac-cell-openrefine-name").droppable({
      hoverClass: "snac-cell-droppable-hover",
      tolerance: "pointer",
      drop: function (event, ui) {
         // use the type:id of the element dropped onto, as an index into the correct dropdown list
         var typeId = $(this).attr('id').split(':');
         $(`.snac-select-values-${typeId[0]}`)[typeId[1]].value = $(ui.draggable).val();
         _this.hideAndDisableAll();
         snacDebug(`droppable('snac-cell-openrefine-name') calling hasChanged()`);
         _this.hasChanged();
      },
   });

   // Allow dropdown column to be droppable
   $(".snac-cell-select-dropdown").droppable({
      hoverClass: "snac-cell-droppable-hover",
      tolerance: "pointer",
      drop: function (event, ui) {
         this.value = $(ui.draggable).val();
         _this.hideAndDisableAll();
         snacDebug(`droppable('snac-cell-select-dropdown') calling hasChanged()`);
         _this.hasChanged();
      },
   });

   // allow schema fields column to be draggable
   $(".snac-cell-schema-field").draggable({
      helper: "clone",
      helper: function (e) {
         var original = $(e.target).hasClass("ui-draggable")
            ? $(e.target)
            : $(e.target).closest(".ui-draggable");
         var clone = original.clone();
         clone.find('.snac-tooltip-text').remove();
         return clone.css({
            width: original.width(), // or outerWidth*
            opacity: 0.8,
         });
      },
      cursor: "crosshair",
      snap: false,
      zIndex: 100,
   });

   this.hideAndDisableAll();
}

SNACSchemaAlignmentDialog.switchTab = function(targetTab) {
   snacDebug(`***** [ switchTab ] *****`);

   $('.main-view-panel-tab').hide();
   $('.main-view-panel-tab-header').removeClass('active');
   $('.main-view-panel-tab-header[href="'+targetTab+'"]').addClass('active');

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

SNACSchemaAlignmentDialog.switchToSchemaPanel = function() {
   $('.main-view-panel-tab-header').show();
   SNACSchemaAlignmentDialog.switchTab('#snac-schema-panel');
}

SNACSchemaAlignmentDialog.launch = function(onDone) {
   snacDebug(`***** [ launch ] *****`);

   this._onDone = onDone;
   this._hasUnsavedChanges = false;

   if (!this.isSetUp()) {
      this.setup(this.switchToSchemaPanel);
   } else {
      this.switchToSchemaPanel()
   }
}

var beforeUnload = function(e) {
   if (SNACSchemaAlignmentDialog.isSetUp() && SNACSchemaAlignmentDialog._hasUnsavedChanges === true) {
      return (e = $.i18n('snac-schema/unsaved-warning'));
   }
};

$(window).on('beforeunload', beforeUnload);

SNACSchemaAlignmentDialog.reset = function(schema) {
   snacDebug(`***** [ reset ] *****`);

   this._ignoreChanges = true;

   this._originalSchema = schema || this._defaultSchema;
   this._schema = cloneDeep(this._originalSchema); // this is what can be munched on

   var types = this.getCases(this._schema.schemaType);

   $('#snac-areas-container').empty();
   this.updateColumns(this._schema);
   $(`#uploading${types.upper}Button`).trigger('click');

   this._ignoreChanges = false;
};

SNACSchemaAlignmentDialog.save = function(onDone) {
   snacDebug(`***** [ save ] *****`);

   var _this = this;
   var schema = this.getJSON();

   snacDebug(`save(): cur overlay model: [${JSON.stringify(theProject.overlayModels.snacSchema)}]`);
   snacDebug(`save(): new schema 2 save: [${JSON.stringify(schema)}]`);

   Refine.postProcess(
      "snac",
      "save-schema",
      {},
      { schema: JSON.stringify(schema) },
      {},
      {
         onDone: function() {
            snacDebug(`save(): SUCCESS`);
            theProject.overlayModels.snacSchema = schema;
            snacDebug(`save(): new overlay model: [${JSON.stringify(theProject.overlayModels.snacSchema)}]`);

            _this.updateColumns(theProject.overlayModels.snacSchema);

            _this.changesCleared();

            if (onDone) onDone();
         },
         onError: function(e) {
            snacDebug(`save(): FAILURE`);
            alert($.i18n('snac-schema/incomplete-schema-could-not-be-saved'));
         },
      }
   );
};

SNACSchemaAlignmentDialog.discardChanges = function() {
   snacDebug(`***** [ discardChanges ] *****`);

   this.reset(theProject.overlayModels.snacSchema);
   this.changesCleared();
   this.evaluateCurrentSchema(false);
}

SNACSchemaAlignmentDialog.changesCleared = function() {
   snacDebug(`***** [ changesCleared ] *****`);

   this._hasUnsavedChanges = false;
   this._unsavedIndicator.hide();
   this._schemaElmts.saveButton
      .prop('disabled', true)
      .addClass('disabled');
   this._schemaElmts.discardButton
      .prop('disabled', true)
      .addClass('disabled');
}

SNACSchemaAlignmentDialog.getJSON = function() {
   snacDebug(`***** [ getJSON ] *****`);

   // determine source of schema values

   var schemaType;

   if ($('#uploadingResourceButton').is(':checked')) {
      schemaType = 'resource';
   } else if ($('#uploadingConstellationButton').is(':checked')) {
      schemaType = 'constellation';
   } else if ($('#uploadingRelationButton').is(':checked')) {
      schemaType = 'relation';
   } else {
      // should never get here... but just in case, force a default
      schemaType = this._defaultSchema.schemaType;
   }

   var dropDownColumn = $(`.snac-select-values-${schemaType}`);

   var dropDownValues = $.map(dropDownColumn, function(option) {
      return option.value;
   });

   // build schema from selected values

   var schema = { schemaType: schemaType, columnMappings: {} };

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

   var required = this._model[schema.schemaType].filter(x => x.required === true).map(x => x.name);

   var warnings = [];
   var mappedColumns = Object.values(schema.columnMappings);

   //snacDebug(`mappedColumns:`);
   //snacDebug(mappedColumns);

   // ensure each required column is present in the list
   for (var i = 0; i < required.length; i++) {
      if (!mappedColumns.includes(required[i])) {
         snacDebug(`schemaIsValid(): ${schema.schemaType}: required field [${required[i]}] is not present`);
         warnings.push({ title: "Missing Required Field", body: `The <strong>${required[i]}</strong> field must be assigned to a column.` });
      }
   }

   this.updateWarnings(warnings, warnings.length);

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
      //this.issueSpinner.hide();
      //this.previewSpinner.hide();

      this._schemaElmts.saveButton
         .prop('disabled', true)
         .addClass('disabled');
   } else {
      if (enableButtons) {
         this._schemaElmts.saveButton
            .prop('disabled', false)
            .removeClass('disabled');
      }

      this.updateWarnings([], 0);
   }

   if (enableButtons) {
      this._schemaElmts.discardButton
         .prop('disabled', false)
         .removeClass('disabled');
   }
};

// Update everything when schema has changed
SNACSchemaAlignmentDialog.hasChanged = function() {
   snacDebug(`***** [ hasChanged ] *****`);

   if (this._ignoreChanges) {
      snacDebug(`hasChanged(): ignoring changes`);
      return;
   }

   this._hasUnsavedChanges = true;
   this._unsavedIndicator.show();

   this.evaluateCurrentSchema(true);
}

SNACSchemaAlignmentDialog.updateItemPreviewText = function(itemType, itemCount, totalCount) {
   this._previewElmts.previewExplanation.text(
      $.i18n('snac-schema/preview-explanation')
         .replace('{preview_type}', itemType)
         .replace('{preview_count}', itemCount)
         .replace('{total_count}', totalCount));
   this._previewElmts.previewNote.html($.i18n('snac-schema/preview-note'));
}

SNACSchemaAlignmentDialog.preview = function() {
   snacDebug(`***** [ preview ] *****`);

   var _this = this;

   this._previewPanes.empty();
   this.updateItemPreviewText("item", 0, 0);
//   this.issueSpinner.show();
   this.previewSpinner.show();
   var schema = this.getJSON();
   if (schema === null) {
      return;
   }

   Refine.postCSRF(
      "command/snac/preview-schema?" + $.param({ project: theProject.id }),
      { schema: JSON.stringify(schema), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
      function(data) {
         //_this.issueSpinner.hide();
         _this.previewSpinner.hide();
         //snacDebug(`preview(): SUCCESS  data = [${JSON.stringify(data)}]`);

         if ('preview' in data) {
            var previewContainer = _this._previewPanes[0];

            data.preview.forEach(function(item) {
              $('<hr>').appendTo(previewContainer);
              $('<p></p>').html(item).appendTo(previewContainer);
            });

            $('<hr>').appendTo(previewContainer);

            _this.updateItemPreviewText(schema.schemaType, data.preview.length, data.total);
         }

         snacDebug(`preview(): added ${data.preview.length} items`);

         //_this.updateWarnings([], 0);
      },
      "json"
   );
};

Refine.registerUpdateFunction(function(options) {
   snacDebug(`***** [ Refine.registerUpdateFunction ] *****`);

   // Inject tabs in any project where the schema has been defined
   if (theProject.overlayModels.snacSchema && !SNACSchemaAlignmentDialog.isSetUp()) {
      SNACSchemaAlignmentDialog.setup();
   }
   if (SNACSchemaAlignmentDialog.isSetUp() && (options.everythingChanged || options.modelsChanged ||
      options.rowsChanged || options.rowMetadataChanged || options.cellsChanged || options.engineChanged)) {
         SNACSchemaAlignmentDialog.discardChanges();
   }
});

/*************************
 * WARNINGS RENDERING *
 *************************/

SNACSchemaAlignmentDialog.updateWarnings = function(warnings, totalCount) {
   var mainDiv = $('#snac-issues-panel');
   var countsElem = this.issuesTabCount;

   // clear everything
   mainDiv.empty();
   countsElem.hide();

   // Add any warnings
   var table = $('<table></table>').appendTo(mainDiv);
   for (const warning of warnings) {
      var tr = $('<tr></tr>').addClass('snac-warning');
      var bodyTd = $('<td></td>')
         .addClass('snac-warning-body')
         .appendTo(tr);
      var h1 = $('<h1></h1>')
         .html(warning.title)
         .appendTo(bodyTd);
      var p = $('<p></p>')
         .html(warning.body)
         .appendTo(bodyTd);
      var countTd = $('<td></td>')
         .addClass('snac-warning-count')
         .appendTo(tr);
      tr.appendTo(table);
   }

   // update the warning counts
   if (totalCount) {
      countsElem.text(totalCount);
      countsElem.show();
   } else {
      table.before($('<p></p>')
         .addClass('snac-panel-explanation')
          .text($.i18n('snac-schema/no-issues-detected')));
   }

   return totalCount;
}
