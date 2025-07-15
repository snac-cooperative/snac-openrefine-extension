package org.snaccooperative.commands;

import com.google.refine.commands.Command;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.model.SNACModel;

public class SNACGetModelCommand extends Command {

  static final Logger logger = LoggerFactory.getLogger("SNACGetModelCommand");

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Content-Type", "application/json");

    respondJSON(response, new SNACModel());
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }
}
