package org.snaccooperative.openrefine.commands;

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
import org.snaccooperative.openrefine.preferences.SNACPreferencesManager;

public class SNACPreferencesCommand extends Command {

  static final Logger logger = LoggerFactory.getLogger("SNACPreferencesCommand");

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    SNACPreferencesManager prefsManager = SNACPreferencesManager.getInstance();

    if (!hasValidCSRFToken(request)) {
      logger.error("SNAC preferences: invalid CSRF token");
      respondCSRFError(response);
      return;
    }

    // save preferences if provided
    if (request.getParameter("snacenv") != null) {
      logger.info("saving SNAC preferences...");
      prefsManager.savePreferences(
          request.getParameter("snacenv"),
          request.getParameter("snackeydev"),
          request.getParameter("snackeyprod"),
          request.getParameter("snacmaxpreviewitems"),
          request.getParameter("snacincludeapiresponse"));
      logger.info("SNAC preferences saved successfully");
    }

    logger.info("loading SNAC preferences...");

    // return current preference in all cases
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Content-Type", "application/json");

    Writer w = response.getWriter();
    JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

    writer.writeStartObject();

    writer.writeStringField("env", prefsManager.getID());

    String[] ids = prefsManager.getIDs();
    for (int i = 0; i < ids.length; i++) {
      String id = ids[i];
      writer.writeObjectFieldStart(prefsManager.getID(id));
      writer.writeStringField("name", prefsManager.getName(id));
      writer.writeStringField("web_url", prefsManager.getWebURL(id));
      writer.writeStringField("api_key", prefsManager.getAPIKey(id));
      writer.writeStringField("api_url", prefsManager.getAPIURL(id));
      writer.writeEndObject();
    }

    writer.writeObjectFieldStart("preview");
    writer.writeNumberField("max_items", prefsManager.getMaxPreviewItems());
    writer.writeEndObject();

    writer.writeObjectFieldStart("upload");
    writer.writeBooleanField("api_response", prefsManager.includeAPIResponse());
    writer.writeEndObject();

    writer.writeEndObject();

    writer.flush();
    writer.close();
    w.flush();
    w.close();

    logger.info("SNAC preferences loaded successfully");
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }
}
