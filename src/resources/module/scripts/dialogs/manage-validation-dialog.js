var SNACManageValidationDialog = {};

SNACManageValidationDialog.launch = function(callback) {
  var schema = theProject.overlayModels.snacSchema;

  if (!schema) {
      //console.log("cannot validate; no schema saved");
      alert($.i18n('snac-validation/missing-schema'));
      callback(null);
      return;
   }

  Refine.postCSRF(
    "command/snac/preferences",
    {},
    function(data) {
      if ("code" in data && data.code === "error") {
        alert(`${$.i18n('snac-preferences/error-loading')}: ${data.message}`);
      } else {
        SNACManageValidationDialog.display(data, callback);
      }
    },
    "json"
  );
};

SNACManageValidationDialog.display = function(data, callback) {
  var _this = this;
  var frame = $(DOM.loadHTML("snac", "scripts/dialogs/manage-validation-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);

  this._elmts.dialogHeader.text($.i18n('snac-validation/dialog-header'));
  this._elmts.validationEnvironment.html($.i18n('snac-validation/validation-environment')
    .replace('{environment}', `<a href="${data[data.env].web_url}" target="_blank">SNAC ${data[data.env].name}</a>`)
  );
  this._elmts.validationExplanation.html($.i18n('snac-validation/validation-explanation'));
  this._elmts.validationDetails.html($.i18n('snac-validation/validation-details'));
  this._elmts.cancelButton.text($.i18n('snac-validation/close'));
  this._elmts.validationButton.text($.i18n('snac-validation/validate'));

  this._level = DialogSystem.showDialog(frame);

  var dismiss = function() {
    DialogSystem.dismissUntil(_this._level - 1);
  };

  frame.find('.cancel-btn').on('click', function() {
     dismiss();
     callback(null);
  });

  elmts.validationButton.on('click', function() {
    Refine.postProcess(
      "snac",
      "perform-validation",
      {},
      {},
      { includeEngine: true, cellsChanged: true, columnStatsChanged: true },
      { onDone:
        function() {
          dismiss();
          callback(null); 
        }
      });
  });
};
