var SNACManageUploadDialog = {};

SNACManageUploadDialog.launch = function(callback) {
  var schema = theProject.overlayModels.snacSchema;

  if (!schema) {
      //console.log("cannot upload; no schema saved");
      alert($.i18n('snac-upload/missing-schema'));
      callback(null);
      return;
   }

  $.get(
    "command/snac/preferences",
    function(data) {
      if (data.api_key === '') {
        alert($.i18n('snac-upload/missing-key'));
        callback(null);
      } else {
        SNACManageUploadDialog.display(data, callback);
      }
    });
};

SNACManageUploadDialog.display = function(data, callback) {
  var self = this;
  var frame = $(DOM.loadHTML("snac", "scripts/dialogs/manage-upload-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);

  this._elmts.dialogHeader.text($.i18n('snac-upload/dialog-header'));
  this._elmts.uploadEnvironment.html($.i18n('snac-upload/upload-environment')
    .replace('{environment}', `<a href="${data.web_url}" target="_blank">SNAC ${data.name}</a>`)
    .replace('{apikey}', data.api_key.substr(0,8))
  );
  this._elmts.uploadExplanation.html($.i18n('snac-upload/upload-explanation'));
  this._elmts.uploadDetails.html($.i18n('snac-upload/upload-details'));
  this._elmts.cancelButton.text($.i18n('snac-upload/close'));
  this._elmts.uploadButton.text($.i18n('snac-upload/upload'));

  this._level = DialogSystem.showDialog(frame);

  var dismiss = function() {
    DialogSystem.dismissUntil(self._level - 1);
  };

  frame.find('.cancel-btn').on('click', function() {
     dismiss();
     callback(null);
  });

  elmts.uploadButton.on('click', function() {
    Refine.postProcess(
      "snac",
      "perform-uploads",
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
