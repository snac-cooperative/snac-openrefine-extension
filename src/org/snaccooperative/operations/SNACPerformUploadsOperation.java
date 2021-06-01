package org.snaccooperative.operations;

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
import org.snaccooperative.commands.SNACAPIResponse;
import org.snaccooperative.connection.SNACConnector;
import org.snaccooperative.exporters.SNACUploadItem;
import org.snaccooperative.schema.SNACSchema;

public class SNACPerformUploadsOperation extends EngineDependentOperation {

  static final Logger logger = LoggerFactory.getLogger("SNACPerformUploadsOperation");

  @JsonProperty("snacEnv")
  private String _snacEnv;

  @JsonCreator
  public SNACPerformUploadsOperation(
      @JsonProperty("engineConfig") EngineConfig engineConfig,
      @JsonProperty("snacEnv") String snacEnv) {
    super(engineConfig);
    this._snacEnv = snacEnv;
  }

  @Override
  protected String getBriefDescription(Project project) {
    return "Upload data to SNAC";
  }

  @Override
  public Process createProcess(Project project, Properties options) throws Exception {
    return new SNACPerformUploadsProcess(
        project, createEngine(project), getBriefDescription(project));
  }

  public class SNACPerformUploadsProcess extends LongRunningProcess implements Runnable {

    final Logger logger = LoggerFactory.getLogger("SNACPerformUploadsProcess");

    protected Project _project;
    protected Engine _engine;
    protected SNACSchema _schema;

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
      SNACConnector keyManager = SNACConnector.getInstance();
      String apiKey = keyManager.getKey();

      String apiURL = "";

      switch (_snacEnv.toLowerCase()) {
        case "prod":
          apiURL = "https://api.snaccooperative.org/";
          break;

        case "dev":
        default:
          apiURL = "https://snac-dev.iath.virginia.edu/api/";
      }

      List<SNACUploadItem> items = _schema.evaluateRecords(_project, _engine);

      List<CellAtRow> results = new ArrayList<CellAtRow>(items.size());
      List<CellAtRow> messages = new ArrayList<CellAtRow>(items.size());
      List<CellAtRow> ids = new ArrayList<CellAtRow>(items.size());
      List<CellAtRow> responses = new ArrayList<CellAtRow>(items.size());

      for (int i = 0; i < items.size(); i++) {
        SNACUploadItem item = items.get(i);
        int row = item.rowIndex();

        SNACAPIResponse uploadResponse = item.performUpload(apiURL, apiKey);

        results.add(new CellAtRow(row, new Cell(uploadResponse.getResult(), null)));
        messages.add(new CellAtRow(row, new Cell(uploadResponse.getMessage(), null)));
        ids.add(new CellAtRow(row, new Cell(uploadResponse.getIDString(), null)));
        responses.add(new CellAtRow(row, new Cell(uploadResponse.getAPIResponse(), null)));

        _progress = i * 100 / items.size();

        if (_canceled) {
          break;
        }
      }

      _progress = 100;

      if (!_canceled) {
        // FIXME: generate unique column postfixes to avoid potential schema confusion down the road
        // e.g. find lowest number N such that no columns exist with that postfix

        String resultColumn = "SNAC Result";
        String messageColumn = "SNAC Message";
        String idColumn = "SNAC ID";
        String responseColumn = "SNAC API Response";

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
              && (_project.columnModel.getColumnByName(responseColumn + postfix) == null)) {
            found = true;
            resultColumn = resultColumn + postfix;
            messageColumn = messageColumn + postfix;
            idColumn = idColumn + postfix;
            responseColumn = responseColumn + postfix;
          }
        }

        addHistoryEntry(resultColumn, results);
        addHistoryEntry(messageColumn, messages);
        addHistoryEntry(idColumn, ids);
        addHistoryEntry(responseColumn, responses);

        _project.processManager.onDoneProcess(this);
      }
    }

    private void addHistoryEntry(String columnName, List<CellAtRow> cells) {
      int columnIndex = _project.columnModel.getMaxCellIndex() + 1;

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
