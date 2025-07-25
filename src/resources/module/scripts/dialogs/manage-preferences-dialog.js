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

SNACManagePreferencesDialog.resetTestButton = function(button, field) {
  var _button = $(button);
  var _field = $(field);

  _button
    .text($.i18n('snac-preferences/test-key-button-default'))
    .prop('disabled', _field.val() == '')
    .removeClass('snac-api-key-valid')
    .removeClass('snac-api-key-invalid')
    .removeClass('snac-api-key-unknown');
}

SNACManagePreferencesDialog.testAPIKey = function(url, field, button) {
  this.resetTestButton(button, field);

  var _button = $(button);
  _button
    .prop('disabled', true)
    .text($.i18n('snac-preferences/test-key-button-checking'));

  var _field = $(field);

  // field's 'input' event should prevent this from being empty, but check anyway
  // because the snac command we use below will not fail with an empty api key
  if (_field.val() == "") {
    return;
  }

  _field.prop('disabled', true);

  var req = { command: "vocabulary", query_string: "person", type: "entity_type", apikey: _field.val() }

   $.ajax({
      url: url,
      type: 'POST',
      data: JSON.stringify(req),
      success: function(response) {
        _button
          .addClass('snac-api-key-valid')
          .text($.i18n('snac-preferences/test-key-button-valid'));

        _button.prop('disabled', false);
        _field.prop('disabled', false);
      },
      error: function(xhr, status, error) {
        // mold any non-snac response into a snac-like error response
        // (assumes a valid snac error response if parsing succeeds)
        var data;
        try {
          data = JSON.parse(xhr.responseText);
        } catch (error) {
          data = { error: { type: "xhr", message: xhr.responseText ?? "unknown error" } }
        }

        //console.log(`ERROR: data = [${JSON.stringify(data)}]`);

        var invalidKeyRegex = "API Key is not authorized";
        var regex = new RegExp(invalidKeyRegex, "i");

        if (data.error.type != "xhr" && regex.test(data.error.message)) {
          _button
            .addClass('snac-api-key-invalid')
            .text($.i18n('snac-preferences/test-key-button-invalid'));
        } else {
          _button
            .addClass('snac-api-key-unknown')
            .text($.i18n('snac-preferences/test-key-button-unknown'));
        }

        _button.prop('disabled', false);
        _field.prop('disabled', false);
      }
   });
}

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
  this._elmts.snacStatusHeader.html($.i18n('snac-preferences/header-status'));
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

  elmts.snacKeyDev
    .val(data.dev.api_key)
    .on('input', function() { _this.resetTestButton('#snactestkeydev', this); });

  elmts.snacTestKeyDev
    .text($.i18n('snac-preferences/test-key-button-default'))
    .addClass('snac-test-api-key-button')
    .prop('disabled', $('#snackeydev').val() == '')
    .on('click', function() { _this.testAPIKey(data.dev.api_url, '#snackeydev', this); });

  elmts.snacKeyProd
    .val(data.prod.api_key)
    .on('input', function() { _this.resetTestButton('#snactestkeyprod', this); });

  elmts.snacTestKeyProd
    .text($.i18n('snac-preferences/test-key-button-default'))
    .addClass('snac-test-api-key-button')
    .prop('disabled', $('#snackeyprod').val() == '')
    .on('click', function() { _this.testAPIKey(data.prod.api_url, '#snackeyprod', this); });

  elmts.snacMaxPreviewItems.val(data.preview.max_items);

  elmts.snacIncludeAPIResponse.prop('checked', data.upload.api_response);

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
