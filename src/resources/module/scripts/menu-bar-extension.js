// Load the localization file
//menu-bar-extension.js

var dictionary = {};
$.ajax({
    url : "command/core/load-language?",
    type : "POST",
    async : false,
    data : {
      module : "snac",
    },
    success : function(data) {
        dictionary = data['dictionary'];
        lang = data['lang'];
    }
});
$.i18n().load(dictionary, lang);

ExporterManager.MenuItems.push({});
ExporterManager.MenuItems.push(
        {
            id:"performSNACEdits",
            label: $.i18n('snac-extension/upload-to-snac'),
            click: function() { SNACManageUploadDialog.launch(null, function(success) {}); }
        });
ExporterManager.MenuItems.push(
        {
            id:"exportSNACJson",
            label: $.i18n('snac-extension/export-to-json'),
            click: function() { SNACExporterMenuBar.checkSchemaAndExport(); }
        });

SNACExporterMenuBar = {};

SNACExporterMenuBar.checkSchemaAndExport = function() {
  var onSaved = function(callback) {
     SNACExporterMenuBar.exportJSON();
  };

  if (!SNACSchemaAlignmentDialog.isSetUp()) {
     SNACSchemaAlignmentDialog.launch(null);
  } else if (SNACSchemaAlignmentDialog._hasUnsavedChanges) {
     SNACSchemaAlignmentDialog._save(onSaved);
  } else {
     onSaved();
  }
}

SNACExporterMenuBar.exportJSON = function(){
   var schema = theProject.overlayModels.snacSchema;

   if (!schema) {
      //console.log("cannot export; no schema saved");
      alert("Cannot export until a SNAC schema is saved");
      return;
   }

   Refine.postCSRF(
      "command/snac/export-json?" + $.param({ project: theProject.id }),
      { schema: JSON.stringify(schema), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
      function(data) {
         let jsonStr = JSON.stringify(data, null, 2);

         let dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(jsonStr);

         var date = new Date();
         let exportFileDefaultName = `${date.toISOString().substr(0,10)}-SNAC-export-${schema.schemaType}s.json`;

         let linkElement = document.createElement('a');
         linkElement.setAttribute('href', dataUri);
         linkElement.setAttribute('download', exportFileDefaultName);
         linkElement.click();
      },
      "json"
   );
}

//extend the column header menu
$(function(){
    ExtensionBar.MenuItems.push(
        {
            "id":"reconcilesnac",
            "label": $.i18n('snac-extension/menu-label'),
            "submenu" : [
                {
                    id: "snac/edit-schema",
                    label: $.i18n('snac-extension/edit-snac-schema'),
                    click: function() { SNACSchemaAlignmentDialog.launch(false); }
                },
                {
                    id:"snac/api-key",
                    label: $.i18n('snac-extension/manage-api-key'),
                    click: function() { SNACManageKeysDialog.launch(null, function(success) {}); }
                },
                {},
                {
                    id:"snac/perform-edits",
                    label: $.i18n('snac-extension/upload-to-snac'),
                    click: function() { SNACManageUploadDialog.launch(null, function(success) {}); }
                },
                {
                    id:"snac/export-schema",
                    label: $.i18n('snac-extension/export-to-json'),
                    click: function() { SNACExporterMenuBar.checkSchemaAndExport(); }
                },
            ]
        }
    );
});
