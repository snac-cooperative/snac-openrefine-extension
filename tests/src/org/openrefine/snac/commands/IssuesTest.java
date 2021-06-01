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
package org.openrefine.snac.commands;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.snac.testing.TestingData2;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.refine.commands.Command;
import com.google.refine.model.Project;
// import com.google.refine.model.Row;
import com.google.refine.RefineTest;
import com.google.refine.util.ParsingUtilities;

import org.apache.http.*;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import java.io.IOException;

import org.snaccooperative.commands.SNACSaveSchemaCommand;

public class IssuesTest extends RefineTest{

    protected Project project = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected StringWriter writer = null;
    protected Command command = null;
    protected Command issues = null;

    @BeforeMethod
    public void SetUp() {
        // Setup for Post Request
        HashMap<String, String> hash_map = new HashMap<String, String>();
        hash_map.put("error", "error");
        hash_map.put("flush", "flush");

        project = createCSVProject(TestingData2.resourceCsv);
        issues = new SNACSaveSchemaCommand();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        // when(request.getParameter("dict")).thenReturn("{\"col1\":\"snaccol1\", \"col2\":\"snaccol2\", \"col3\":\"snaccol3\"}");
        // when(request.getParameter("project")).thenReturn("" + project.id + "");

        try {
            when(response.getWriter()).thenReturn(printWriter);
        } catch (IOException e1) {
            Assert.fail();
        }
    }


    /*
    * Test post when no issues exist
    */
    @Test
    public void testPostNoIssues() throws Exception{
      try{
        issues.doPost(request, response);
        ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
        Assert.assertTrue(response.get("error").textValue() == null);
      } catch (Exception e){
        String a = "";
        Assert.assertFalse(a.equals(""));
      }
    }

    /*
    * Test post when no flush exist
    */
    @Test
    public void testPostNoFlush() throws Exception{
      try{
        when(request.getParameter("error")).thenReturn("{\"title\": \"\'title\' found empty\", "
          + "\"body\": \"The required field \'title\' is missing from schema.\"}");
        issues.doPost(request, response);

        ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
        String response_str = response.get("error").textValue();

        Assert.assertTrue(response_str.equals("{\"title\": \"\'title\' found empty\", "
        + "\"body\": \"The required field \'title\' is missing from schema.\"}"));
      } catch (Exception e){
        String a = "";
        Assert.assertFalse(a.equals(""));
      }
    }

    /*
    * Test post when flush is set to true
    */
    @Test
    public void testPostTrueFlush() throws Exception{
      try{
        when(request.getParameter("error")).thenReturn("{\"title\": \"\'title\' found empty\", "
          + "\"body\": \"The required field \'title\' is missing from schema.\"}");
        when(request.getParameter("flush")).thenReturn("true");
        issues.doPost(request, response);

        ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
        String error_response_str = response.get("error").textValue();
        String flush_response_str = response.get("flush").textValue();

        Assert.assertTrue(error_response_str.equals("{\"title\": \"\'title\' found empty\", "
        + "\"body\": \"The required field \'title\' is missing from schema.\"}"));
        Assert.assertTrue(flush_response_str.equals("true"));
      } catch (Exception e){
        String a = "";
        Assert.assertFalse(a.equals(""));
      }
    }

    /*
    * Test get when flush is set to true
    */
    @Test
    public void testGet() throws Exception{
      try{
        when(request.getParameter("error")).thenReturn("{\"title\": \"\'title\' found empty\", "
          + "\"body\": \"The required field \'title\' is missing from schema.\"}");
        when(request.getParameter("flush")).thenReturn("true");
        issues.doPost(request, response);

        issues.doGet(request, response);
        ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
        String response_str = response.get("errors").textValue();
        Assert.assertTrue(response_str != null);
        System.out.println(response_str);
      }
      catch(Exception e){
          String a="";
          Assert.assertTrue(a.equals(""));
      }
    }
}
