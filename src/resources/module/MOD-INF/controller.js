
importPackage(org.snaccooperative.commands);

/*
 * Function invoked to initialize the extension.
 */
function init() {
    var RefineServlet = Packages.com.google.refine.RefineServlet;

    // these must be defined to allow history entries to be reloaded after openrefine restarts
    RefineServlet.registerClassMapping(
            "org.snaccooperative.operations.SNACSaveSchemaOperation$SNACSchemaChange",
            "org.snaccooperative.operations.SNACSaveSchemaOperation$SNACSchemaChange");

    RefineServlet.cacheClass(Packages.org.snaccooperative.operations.SNACSaveSchemaOperation$SNACSchemaChange);

    /*
     *  Attach a SNAC schema to each project.
     */
    var Project = Packages.com.google.refine.model.Project;

    Project.registerOverlayModel("snacSchema", Packages.org.snaccooperative.schema.SNACSchema);

    /*
     * Operations
     */
    var OperationRegistry = Packages.com.google.refine.operations.OperationRegistry;

    OperationRegistry.registerOperation(module, "save-schema", Packages.org.snaccooperative.operations.SNACSaveSchemaOperation);
    OperationRegistry.registerOperation(module, "perform-uploads", Packages.org.snaccooperative.operations.SNACPerformUploadsOperation);
    OperationRegistry.registerOperation(module, "perform-validation", Packages.org.snaccooperative.operations.SNACPerformValidationOperation);

    /*
     * Commands
     */
    RefineServlet.registerCommand(module, "save-schema", new SNACSaveSchemaCommand());
    RefineServlet.registerCommand(module, "preview-schema", new SNACPreviewSchemaCommand());
    RefineServlet.registerCommand(module, "perform-uploads", new SNACPerformUploadsCommand());
    RefineServlet.registerCommand(module, "perform-validation", new SNACPerformValidationCommand());
    RefineServlet.registerCommand(module, "export-json", new SNACExportJSONCommand());
    RefineServlet.registerCommand(module, "preferences", new SNACPreferencesCommand());
    RefineServlet.registerCommand(module, "get-model", new SNACGetModelCommand());

    /*
     * Resources
     */
    ClientSideResourceManager.addPaths(
      "project/scripts",
      module,
      [
        "scripts/menu-bar-extension.js",
        "scripts/dialogs/manage-preferences-dialog.js",
        "scripts/dialogs/manage-upload-dialog.js",
        "scripts/dialogs/manage-validation-dialog.js",
        "scripts/dialogs/schema-alignment-dialog.js",
      ]);

    ClientSideResourceManager.addPaths(
      "project/styles",
      module,
      [
        "styles/dialogs/manage-preferences-dialog.less",
        "styles/dialogs/manage-upload-dialog.less",
        "styles/dialogs/manage-validation-dialog.less",
        "styles/dialogs/schema-alignment-dialog.css",
      ]);
}
