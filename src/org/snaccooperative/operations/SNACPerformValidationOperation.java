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
import org.snaccooperative.exporters.SNACUploadItem;
import org.snaccooperative.schema.SNACSchema;

public class SNACPerformValidationOperation extends EngineDependentOperation {

  static final Logger logger = LoggerFactory.getLogger("SNACPerformValidationOperation");

  @JsonCreator
  public SNACPerformValidationOperation(
      @JsonProperty("engineConfig") EngineConfig engineConfig) {
    super(engineConfig);
  }

  @Override
  protected String getBriefDescription(Project project) {
    return "Validate data against SNAC";
  }

  @Override
  public Process createProcess(Project project, Properties options) throws Exception {
    return new SNACPerformValidationProcess(
        project, createEngine(project), getBriefDescription(project));
  }

  public class SNACPerformValidationProcess extends LongRunningProcess implements Runnable {

    final Logger logger = LoggerFactory.getLogger("SNACPerformValidationProcess");

    protected Project _project;
    protected Engine _engine;
    protected SNACSchema _schema;

    public SNACPerformValidationProcess(Project project, Engine engine, String description) {
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
      List<SNACUploadItem> items = _schema.evaluateRecords(_project, _engine);

      List<CellAtRow> results = new ArrayList<CellAtRow>(items.size());
      List<CellAtRow> messages = new ArrayList<CellAtRow>(items.size());

      for (int i = 0; i < items.size(); i++) {
        SNACUploadItem item = items.get(i);
        int row = item.rowIndex();

        SNACAPIResponse validationResponse = item.performValidation();

        logger.info(
            "["
                + (i + 1)
                + "/"
                + items.size()
                + "] validation result: ["
                + validationResponse.getResult()
                + "]");

        results.add(new CellAtRow(row, new Cell(validationResponse.getResult(), null)));
        messages.add(new CellAtRow(row, new Cell(validationResponse.getMessage(), null)));

        _progress = i * 100 / items.size();

        if (_canceled) {
          break;
        }
      }

      _progress = 100;

      if (!_canceled) {
        String resultColumn = "*SNAC*: Validation Result";
        String messageColumn = "*SNAC*: Validation Message";

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
              && (_project.columnModel.getColumnByName(messageColumn + postfix) == null)) {
            found = true;
            resultColumn = resultColumn + postfix;
            messageColumn = messageColumn + postfix;
          }
        }

        addHistoryEntry(resultColumn, results);
        addHistoryEntry(messageColumn, messages);

        _project.processManager.onDoneProcess(this);
      }
    }

    private void addHistoryEntry(String columnName, List<CellAtRow> cells) {
      int columnIndex = _project.columnModel.columns.size();

      _project.history.addEntry(
          new HistoryEntry(
              HistoryEntry.allocateID(),
              _project,
              "SNAC Validation: Add Column \"" + columnName + "\"",
              SNACPerformValidationOperation.this,
              new ColumnAdditionChange(columnName, columnIndex, cells)));
    }
  }
}
