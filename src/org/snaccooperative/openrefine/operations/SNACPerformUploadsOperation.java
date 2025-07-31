package org.snaccooperative.openrefine.operations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.changes.CellAtRow;
import com.google.refine.model.changes.ColumnAdditionChange;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.api.SNACAPIResponse;
import org.snaccooperative.openrefine.exporters.SNACAbstractItem;
import org.snaccooperative.openrefine.preferences.SNACPreferencesManager;
import org.snaccooperative.openrefine.schema.SNACSchema;

public class SNACPerformUploadsOperation extends EngineDependentOperation {

  static final Logger logger = LoggerFactory.getLogger(SNACPerformUploadsOperation.class);

  protected SNACPreferencesManager _prefsManager;

  @JsonCreator
  public SNACPerformUploadsOperation(@JsonProperty("engineConfig") EngineConfig engineConfig) {
    super(engineConfig);

    _prefsManager = SNACPreferencesManager.getInstance();
  }

  @Override
  protected String getBriefDescription(Project project) {
    return "Upload data to SNAC " + _prefsManager.getName();
  }

  @Override
  public Process createProcess(Project project, Properties options) throws Exception {
    return new SNACPerformUploadsProcess(
        project, createEngine(project), getBriefDescription(project));
  }

  public class SNACPerformUploadsProcess extends LongRunningProcess implements Runnable {

    final Logger logger = LoggerFactory.getLogger(SNACPerformUploadsProcess.class);

    private Project _project;
    private Engine _engine;
    private SNACSchema _schema;

    public SNACPerformUploadsProcess(Project project, Engine engine, String description) {
      super(description);
      this._project = project;
      this._engine = engine;
      this._schema = (SNACSchema) project.overlayModels.get("snacSchema");
    }

    @Override
    protected Runnable getRunnable() {
      return this;
    }

    @Override
    public void run() {
      List<SNACAbstractItem> items = _schema.evaluateRecords(_project, _engine);

      List<CellAtRow> results = new ArrayList<CellAtRow>(items.size());
      List<CellAtRow> messages = new ArrayList<CellAtRow>(items.size());
      List<CellAtRow> ids = new ArrayList<CellAtRow>(items.size());
      List<CellAtRow> links = new ArrayList<CellAtRow>(items.size());
      List<CellAtRow> responses = new ArrayList<CellAtRow>(items.size());

      SNACPreferencesManager prefsManager = SNACPreferencesManager.getInstance();

      Boolean includeAPIResponseColumn = prefsManager.includeAPIResponse();

      for (int i = 0; i < items.size(); i++) {
        SNACAbstractItem item = items.get(i);
        int row = item.rowIndex();

        SNACAPIResponse uploadResponse = item.performUpload();
        if (uploadResponse == null) {
          uploadResponse = new SNACAPIResponse("unknown");
        }

        logger.info(
            "["
                + (i + 1)
                + "/"
                + items.size()
                + "] upload result: ["
                + uploadResponse.getResult()
                + "]");

        results.add(new CellAtRow(row, new Cell(uploadResponse.getResult(), null)));
        messages.add(new CellAtRow(row, new Cell(uploadResponse.getMessage(), null)));
        ids.add(new CellAtRow(row, new Cell(uploadResponse.getIDString(), null)));
        links.add(new CellAtRow(row, new Cell(uploadResponse.getURI(), null)));
        if (includeAPIResponseColumn) {
          String apiResponse = uploadResponse.getAPIResponse();
          // attempt to filter out mocked API responses
          if (apiResponse.equals(uploadResponse.getMessage())) {
            apiResponse = "";
          }
          responses.add(new CellAtRow(row, new Cell(apiResponse, null)));
        }

        _progress = i * 100 / items.size();

        if (_canceled) {
          break;
        }
      }

      _progress = 100;

      if (!_canceled) {
        String snacPrefix = "*SNAC " + prefsManager.getName() + "*: ";
        String resultColumn = snacPrefix + "Result";
        String messageColumn = snacPrefix + "Message";
        String idColumn = snacPrefix + "ID";
        String uriColumn = snacPrefix + "Link";
        String responseColumn = snacPrefix + "API Response";

        int i = 0;
        boolean found = false;
        String postfix = "";

        while (!found) {
          i++;
          if (i == 1) {
            postfix = "";
          } else {
            postfix = " " + i;
          }

          if ((_project.columnModel.getColumnByName(resultColumn + postfix) == null)
              && (_project.columnModel.getColumnByName(messageColumn + postfix) == null)
              && (_project.columnModel.getColumnByName(idColumn + postfix) == null)
              && (_project.columnModel.getColumnByName(uriColumn + postfix) == null)
              && (_project.columnModel.getColumnByName(responseColumn + postfix) == null)) {
            found = true;
            resultColumn = resultColumn + postfix;
            messageColumn = messageColumn + postfix;
            idColumn = idColumn + postfix;
            uriColumn = uriColumn + postfix;
            responseColumn = responseColumn + postfix;
          }
        }

        addHistoryEntry(resultColumn, results);
        addHistoryEntry(messageColumn, messages);
        addHistoryEntry(idColumn, ids);
        addHistoryEntry(uriColumn, links);
        if (includeAPIResponseColumn) {
          addHistoryEntry(responseColumn, responses);
        }

        _project.processManager.onDoneProcess(this);
      }
    }

    private void addHistoryEntry(String columnName, List<CellAtRow> cells) {
      int columnIndex = _project.columnModel.columns.size();

      _project.history.addEntry(
          new HistoryEntry(
              HistoryEntry.allocateID(),
              _project,
              "SNAC Upload: Add Column \"" + columnName + "\"",
              SNACPerformUploadsOperation.this,
              new ColumnAdditionChange(columnName, columnIndex, cells)));
    }
  }
}
