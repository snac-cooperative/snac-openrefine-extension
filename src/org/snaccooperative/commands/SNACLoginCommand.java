package org.snaccooperative.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;
import java.io.IOException;
import java.io.Writer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.connection.SNACConnector;

public class SNACLoginCommand extends Command {

  static final Logger logger = LoggerFactory.getLogger("SNACLoginCommand");

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String apikey = request.getParameter("snackey");
    SNACConnector manager = SNACConnector.getInstance();
    if (apikey != null) {
      manager.saveKey(apikey);
    } else if ("true".equals(request.getParameter("logout"))) {
      manager.removeKey();
    }
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Content-Type", "application/json");

    Writer w = response.getWriter();
    JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

    writer.writeStartObject();
    writer.writeStringField("apikey", manager.getKey());
    writer.writeEndObject();
    writer.flush();
    writer.close();
    w.flush();
    w.close();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }
}
