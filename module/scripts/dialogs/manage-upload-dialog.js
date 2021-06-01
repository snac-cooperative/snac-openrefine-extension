var SNACManageUploadDialog = {};

SNACManageUploadDialog.launch = function(apikey, callback) {
  var schema = theProject.overlayModels.snacSchema;

  if (!schema) {
      //console.log("cannot upload; no schema saved");
      alert("Cannot upload until a SNAC schema is saved");
      callback(null);
      return;
   }

  $.get(
    "command/snac/apikey",
    function(data) {
      SNACManageUploadDialog.display(apikey, data.apikey, callback);
    });
};

SNACManageUploadDialog.display = function(apikey, saved_apikey, callback) {
  var self = this;
  var frame = $(DOM.loadHTML("snac", "scripts/dialogs/manage-upload-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);

  this._elmts.dialogHeader.text($.i18n('snac-upload/dialog-header'));
  this._elmts.explainUpload.html($.i18n('snac-upload/explain-key'));
  this._elmts.keyLabel.text($.i18n('snac-upload/key-label'));
  this._elmts.cancelButton.text($.i18n('snac-upload/close'));
  this._elmts.uploadButton.text($.i18n('snac-upload/upload'));

  if (apikey != null) {
    this._elmts.keyInput.text(apikey);
  } else if (saved_apikey != null) {
    this._elmts.keyInput.text(saved_apikey);
  }

  this._level = DialogSystem.showDialog(frame);

  var dismiss = function() {
    DialogSystem.dismissUntil(self._level - 1);
  };

  frame.find('.cancel-btn').click(function() {
     dismiss();
     callback(null);
  });

  var rad = document.getElementsByName('uploadOption');
  var prev = null;
  var prod_or_dev = "dev";
  for (var i = 0; i < rad.length; i++) {
    rad[i].addEventListener('change', function() {
      (prev) ? prev.value: null;
      if (this !== prev) {
        prev = this;
      }
      prod_or_dev = this.value;
    });
  }

  elmts.uploadButton.click(function() {
    //console.log(prod_or_dev);
    //console.log(elmts.apiKeyForm.serialize());

    Refine.postProcess(
      "snac",
      "perform-uploads",
      {},
      { snacenv: prod_or_dev },
      { includeEngine: true, cellsChanged: true, columnStatsChanged: true },
      { onDone:
        function() {
          dismiss();
          callback(null); 
        }
      });
  });
};
