package org.snaccooperative.commands;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.operations.SNACPerformValidationOperation;

public class SNACPerformValidationCommand extends EngineDependentCommand {

  static final Logger logger = LoggerFactory.getLogger("SNACPerformValidationCommand");

  @Override
  protected AbstractOperation createOperation(
      Project project, HttpServletRequest request, EngineConfig engineConfig) throws Exception {
    return new SNACPerformValidationOperation(engineConfig);
  }
}
