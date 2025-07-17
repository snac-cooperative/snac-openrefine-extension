var SNACManagePreferencesDialog = {};

SNACManagePreferencesDialog.launch = function(callback) {
  Refine.postCSRF(
    "command/snac/preferences",
    {},
    function(data) {
      if ("code" in data && data.code === "error") {
        alert(`${$.i18n('snac-preferences/error-loading')}: ${data.message}`);
      } else {
        SNACManagePreferencesDialog.display(data, callback);
      }
    },
    "json"
  );
};

SNACManagePreferencesDialog.display = function(data, callback) {
  var _this = this;
  var frame = $(DOM.loadHTML("snac", "scripts/dialogs/manage-preferences-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);
  
  SNACManagePreferencesDialog.firstLaunch = false;

  this._elmts.dialogHeader.text($.i18n('snac-preferences/dialog-header'));
  this._elmts.prefsExplanation.html($.i18n('snac-preferences/prefs-explanation'));
  this._elmts.prefsNote.html($.i18n('snac-preferences/prefs-note'));
  this._elmts.snacEnvHeader.html($.i18n('snac-preferences/header-env'));
  this._elmts.snacLabelEnvDev.html(`<a href="${data.dev.web_url}" target="_blank">${data.dev.name}</a>`);
  this._elmts.snacLabelEnvProd.html(`<a href="${data.prod.web_url}" target="_blank">${data.prod.name}</a>`);
  this._elmts.snacKeyHeader.html($.i18n('snac-preferences/header-key'));
  this._elmts.snacKeyDev.attr("placeholder", $.i18n('snac-preferences/key-placeholder').replace('{environment}', data.dev.name));
  this._elmts.snacKeyProd.attr("placeholder", $.i18n('snac-preferences/key-placeholder').replace('{environment}', data.prod.name));
  this._elmts.snacLabelMaxPreviewItems.text($.i18n('snac-preferences/max-preview-items'));
  this._elmts.snacLabelIncludeAPIResponse.text($.i18n('snac-preferences/include-api-response'));
  this._elmts.snacExtensionVersion.text($.i18n('snac-preferences/extension-version'));
  this._elmts.cancelButton.text($.i18n('snac-preferences/close'));
  this._elmts.saveButton.text($.i18n('snac-preferences/save'));

  $(document).on('keypress', function(e) {
    if (e.target.id == "snacKeyDev" || e.target.id == "snacKeyProd") {
      if (e.keyCode === 13) {
        e.preventDefault();
      }
    }
  });

  $(document).on('keyup', function(e) {
    if (e.keyCode === 27) {
      $('.cancel-btn').trigger('click');   // esc
      //console.log("ESCAPED");
    }
  });

  elmts.snacKeyDev.val(data.dev.api_key);
  elmts.snacKeyProd.val(data.prod.api_key);
  elmts.snacMaxPreviewItems.val(data.preview.max_items);
  elmts.snacIncludeAPIResponse.prop("checked", data.upload.api_response);

  this._level = DialogSystem.showDialog(frame);

  $('input[name="snacenv"]').on('change', function() {
    $('.snac-preference-row').removeClass('highlighted');
    $('.snac-preference-row#snacrow' + $(this).val()).addClass('highlighted');
  }); 
  $(`#snacenv${data.env}`).prop("checked", true);
  $(`.snac-preference-row#snacrow${data.env}`).addClass('highlighted');

  var dismiss = function() {
    DialogSystem.dismissUntil(_this._level - 1);
  };

  frame.find('.cancel-btn').on('click', function() {
     dismiss();
     callback(null);
  });

  elmts.saveButton.on('click', function() {
    frame.hide();
    Refine.postCSRF(
      "command/snac/preferences",
      elmts.snacPreferencesForm.serialize(),
      function(data) {
        if (data) {
          if ("code" in data && data.code === "error") {
            alert(`${$.i18n('snac-preferences/error-saving')}: ${data.message}`);
          }
          dismiss();
          callback(data);
        } else {
          dismiss();
          callback(null);
        }
      },
      "json"
    );
  });

};
