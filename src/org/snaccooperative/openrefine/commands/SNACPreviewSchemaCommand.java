/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2018 Antonin Delpeuch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.snaccooperative.openrefine.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.exporters.SNACAbstractItem;
import org.snaccooperative.openrefine.preferences.SNACPreferencesManager;
import org.snaccooperative.openrefine.schema.SNACSchema;

public class SNACPreviewSchemaCommand extends Command {

  static final Logger logger = LoggerFactory.getLogger("SNACPreviewSchemaCommand");

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    logger.info("generating SNAC preview...");

    try {
      Project project = getProject(request);
      Engine engine = getEngine(request, project);

      response.setCharacterEncoding("UTF-8");
      response.setHeader("Content-Type", "application/json");

      String schemaJSON = request.getParameter("schema");

      SNACSchema schema = null;
      if (schemaJSON != null) {
        try {
          schema = SNACSchema.reconstruct(schemaJSON);
        } catch (IOException e) {
          logger.error("SNAC preview generation: could not reconstruct schema: [" + e + "]");
          respondStatusError(response, "SNAC schema could not be parsed.");
          return;
        }
      } else {
        schema = (SNACSchema) project.overlayModels.get("snacSchema");
      }
      if (schema == null) {
        logger.error("SNAC preview generation: missing schema");
        respondStatusError(response, "No SNAC schema provided.");
        return;
      }

      SNACPreferencesManager prefsManager = SNACPreferencesManager.getInstance();

      int recordCount = project.recordModel.getRecordCount();

      List<SNACAbstractItem> items =
          schema.evaluateRecords(project, engine, prefsManager.getMaxPreviewItems());

      logger.info(
          "generated "
              + items.size()
              + " (out of "
              + project.recordModel.getRecordCount()
              + ") preview items");

      Writer w = response.getWriter();
      JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

      writer.writeStartObject();

      writer.writeFieldName("preview");
      writer.writeStartArray();
      for (int i = 0; i < items.size(); i++) {
        writer.writeString(items.get(i).getPreviewText());
      }
      writer.writeEndArray();

      writer.writeNumberField("total", project.recordModel.getRecordCount());

      writer.writeEndObject();

      writer.flush();
      writer.close();
      w.flush();
      w.close();

      logger.info("SNAC preview generation succeeded");
    } catch (Exception e) {
      logger.error("SNAC preview generation: exception: [" + e + "]");
      respondException(response, e);
    }
  }
}
