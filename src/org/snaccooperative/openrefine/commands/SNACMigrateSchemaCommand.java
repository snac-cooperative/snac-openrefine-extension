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

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snaccooperative.openrefine.model.SNACAbstractModel;
import org.snaccooperative.openrefine.model.SNACAbstractModel.ModelType;
import org.snaccooperative.openrefine.model.SNACConstellationModel;
import org.snaccooperative.openrefine.model.SNACModelField;
import org.snaccooperative.openrefine.model.SNACRelationModel;
import org.snaccooperative.openrefine.model.SNACResourceModel;
import org.snaccooperative.openrefine.operations.SNACSaveSchemaOperation;
import org.snaccooperative.openrefine.schema.SNACSchema;

public class SNACMigrateSchemaCommand extends Command {

  static final Logger logger = LoggerFactory.getLogger(SNACMigrateSchemaCommand.class);

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    logger.info("migrating SNAC schema...");

    if (!hasValidCSRFToken(request)) {
      logger.error("SNAC schema migration: invalid CSRF token");
      respondCSRFError(response);
      return;
    }

    try {
      Project project = getProject(request);

      response.setCharacterEncoding("UTF-8");
      response.setHeader("Content-Type", "application/json");

      SNACSchema schema = (SNACSchema) project.overlayModels.get("snacSchema");

      if (schema == null) {
        logger.info("no schema to migrate");
        respondStatusOk(response, "No SNAC schema to migrate");
        return;
      }

      // this could be cleaned up, but left as-is for now because
      // of the various ways errors can be returned

      // check the current schema for SNAC field name changes:

      // first, get the SNAC model from the schema type

      SNACAbstractModel model;
      ModelType modelType = ModelType.fromString(schema.getSchemaType());

      switch (modelType) {
        case CONSTELLATION:
          model = new SNACConstellationModel();
          break;

        case RELATION:
          model = new SNACRelationModel();
          break;

        case RESOURCE:
          model = new SNACResourceModel();
          break;

        default:
          logger.error("unrecognized schema type: [" + schema.getSchemaType() + "]");
          respondStatusError(
              response,
              "Cannot migrate SNAC schema due to unrecognized SNAC schema type: ["
                  + schema.getSchemaType()
                  + "]");
          return;
      }

      // now, iterate over the mapped SNAC fields in the current schema:

      Boolean migrate = false;
      HashMap<String, String> columns = schema.getColumnMappings();

      for (String key : columns.keySet()) {
        // get the model field for this SNAC field (past and current names are recognized)
        String snacField = columns.get(key);
        SNACModelField modelField = model.getModelField(snacField);

        // if no model field could be found, we can't migrate this SNAC field. this can happen
        // if the model field class was not properly updated to account for previous name(s).
        if (modelField == null) {
          logger.error("unrecognized field: [" + snacField + "]");
          respondStatusError(
              response,
              "Cannot migrate SNAC schema due to unrecognized SNAC model field: ["
                  + snacField
                  + "]");
          continue;
        }

        // if the SNAC field does not match the model field's current name,
        // it must be a previous name.  update it in the column map.
        if (!modelField.isCurrentName(snacField)) {
          logger.info("migrating field: [" + snacField + "]  =>  [" + modelField.getName() + "]");
          migrate = true;
          columns.put(key, modelField.getName());
          continue;
        }
      }

      // if we updated the schema column map, save the new schema
      if (migrate) {
        AbstractOperation op = new SNACSaveSchemaOperation(schema, "Migrate");
        Process process = op.createProcess(project, new Properties());

        logger.info("SNAC schema migration initiated");

        performProcessAndRespond(request, response, project, process);

        logger.info("SNAC schema migration completed");
        return;
      }

      // looks good, no changes necessary
      logger.info("SNAC schema is up to date");
      respondStatusOk(response, "SNAC schema is up to date");
    } catch (Exception e) {
      logger.error("SNAC schema migration: exception: [" + e + "]");
      respondException(response, e);
    }
  }
}
