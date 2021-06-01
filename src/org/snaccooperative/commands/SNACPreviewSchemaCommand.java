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

package org.snaccooperative.commands;

import static org.snaccooperative.commands.SNACCommandUtilities.respondError;

import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.exporters.SNACUploadItem;
import org.snaccooperative.schema.SNACSchema;

public class SNACPreviewSchemaCommand extends Command {

  static final Logger logger = LoggerFactory.getLogger("SNACPreviewSchemaCommand");

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

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
          respondError(response, "SNAC schema could not be parsed.");
          return;
        }
      } else {
        schema = (SNACSchema) project.overlayModels.get("snacSchema");
      }
      if (schema == null) {
        respondError(response, "No SNAC schema provided.");
        return;
      }

      List<SNACUploadItem> items = schema.evaluateRecords(project, engine);

      SNACPreviewItems previewItems = new SNACPreviewItems(items.size());

      int maxPreviewItems = Math.min(items.size(), 10);

      for (int i = 0; i < maxPreviewItems; i++) {
        SNACUploadItem item = items.get(i);

        previewItems.addPreviewItem(item.getPreviewText());
      }

      respondJSON(response, previewItems);
    } catch (Exception e) {
      respondException(response, e);
    }
  }
}
