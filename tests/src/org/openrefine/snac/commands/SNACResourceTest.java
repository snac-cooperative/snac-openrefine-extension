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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.google.refine.model.Row;
import com.google.refine.RefineTest;
import com.google.refine.util.ParsingUtilities;

import org.apache.http.*;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.IOException;
import org.powermock.modules.testng.PowerMockTestCase;

import org.snaccooperative.commands.SNACPerformUploadsCommand;
import org.snaccooperative.exporters.SNACResourceItem;
import org.snaccooperative.data.EntityId;
import org.snaccooperative.data.Resource;

public class SNACResourceTest extends RefineTest{

    protected Project project = null;
    protected Project project2 = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected StringWriter writer = null;
    protected Command command = null;
    protected Command upload = null;
    // protected SNACResourceCreator manager = SNACResourceItem.getInstance();
    protected EntityId entityId = null;

    // @BeforeMethod
    // public void SetUp() {
    //     // Setup for Post Request
    //     manager.csv_headers = new LinkedList<String>(){{add("id"); add("type"); add("title"); add("display entry");
    //   add("link"); add("abstract"); add("extent"); add("date"); add("language"); add("script"); add("holding repository snac id");}};
    //     HashMap<String, String> hash_map = new HashMap<String, String>();
    //     hash_map.put("id", "id");
    //     hash_map.put("type", "type");
    //     hash_map.put("title", "title");
    //     hash_map.put("display entry","display entry");
    //     hash_map.put("link", "link");
    //     hash_map.put("abstract", "abstract");
    //     hash_map.put("extent","extent");
    //     hash_map.put("date","date");
    //     hash_map.put("language", "language");
    //     hash_map.put("script","script");
    //     hash_map.put("holding repository snac id","holding repository snac id");

    //     manager.match_attributes = hash_map;

    //     project = createCSVProject(TestingData2.resourceCsv);
    //     project2 = createCSVProject(TestingData2.resourceRecordCsv);

    //     upload = new SNACPerformUploadsCommand();
    //     request = mock(HttpServletRequest.class);
    //     response = mock(HttpServletResponse.class);
    //     writer = new StringWriter();
    //     PrintWriter printWriter = new PrintWriter(writer);

    //     // when(request.getParameter("dict")).thenReturn("{\"col1\":\"snaccol1\", \"col2\":\"snaccol2\", \"col3\":\"snaccol3\"}");
    //     // when(request.getParameter("project")).thenReturn("" + project.id + "");

    //     try {
    //         when(response.getWriter()).thenReturn(printWriter);
    //     } catch (IOException e1) {
    //         Assert.fail();
    //     }
    // }

    // @Test
    // public void testGetResource() throws Exception{
    //   Assert.assertNull(manager.getResource(100));
    // }

    // @Test
    // public void testPreview() throws Exception{
    //   manager.clearResources();
    //   manager.setProject(createCSVProject(TestingData2.resourceRecordCsv2));
    //   manager.rowsToResources();
    //   String result = manager.obtainPreview();
    //   String correct = "Inserting 1 new Resources into SNAC.\n"
    //       + "Extent: extent1\n"
    //       + "Date: 2020\n"
    //       + "Link: http://record1test.com\n"
    //       + "Language(s): English(eng)\n"
    //       + "Repository ID: 12345\n"
    //       + "ID: 1\n"
    //       + "Abstract: abstract_example1\n"
    //       + "Document Type: DigitalArchivalResource (400479)\n"
    //       + "Title: Title1\n"
    //       + "Display Entry: display_entry1\n"
    //       + "Script(s): English, Korean\n";
    //   Assert.assertEquals(result, correct);
    // }

    // @Test
    // public void testUploadResources(){
    //   manager.clearResources();
    //   manager.setProject(createCSVProject(TestingData2.resourceRecordCsv2));
    //   manager.rowsToResources();
    //   manager.uploadResources("fake_api_key", "prod");
    //   manager.clearResources();
    //   String a="";
    //   Assert.assertTrue(a.equals(""));
    // }

    // @Test
    // public void testSetUp(){
    //   String json_source = "{\"id\":\"id\",\"type\":\"type\",\"title\":\"title\""
    //   + ",\"display entry\":\"display entry\",\"link\":\"link\""
    //   + ",\"abstract\":\"abstract\",\"extent\":\"extent\",\"date\":\"date\""
    //   + ",\"language\":\"language\",\"script\":\"script\",\"holding repository snac id\":\"holding repository snac id\"}";
    //   manager.clearResources();
    //   try{
    //     manager.setUp(createCSVProject(TestingData2.simpleCsv), json_source);
    //     Assert.assertTrue(true);
    //   } catch (Exception e){
    //     Assert.assertFalse(true);
    //   }
    // }

    // @Test
    // public void testExportJson() throws Exception{
    //   manager.clearResources();
    //   manager.setProject(createCSVProject(TestingData2.simpleCsv));
    //   manager.rowsToResources();
    //   String result = manager.exportResourcesJSON();
    //   String correct = "{\"resources\":[{\"dataType\":\"Resource\",\"id\":1}]}";
    //   Assert.assertEquals(result, correct);
    // }

    // @Test
    // public void testLanguage1() throws Exception{
    //   Assert.assertNotNull(manager.detectLanguage("eng"));
    //   Assert.assertNotNull(manager.detectLanguage("kor"));
    //   Assert.assertNull(manager.detectLanguage("reeeee"));
    //   Assert.assertNotNull(manager.detectLanguage("jpn"));
    //   Assert.assertNull(manager.detectLanguage("hmm"));
    // }

    // @Test
    // public void testRecordsToResource() throws Exception{
    //   List<Row> record_temp = new LinkedList<Row>();
    //   for(int x = 0; x < project2.rows.size(); x++){
    //     record_temp.add(project2.rows.get(x));
    //   }
    //   Resource fromDataRes = manager.createResourceRecord(record_temp);
    //   String fromData = Resource.toJSON(fromDataRes);
    //   Assert.assertTrue(fromData.contains("eng"));
    //   Assert.assertTrue(fromData.contains("kor"));
    //   Assert.assertTrue(fromData.contains("English"));
    //   Assert.assertFalse(fromData.contains("reeeee"));
    //   Assert.assertFalse(fromData.contains("hmm"));
    // }

    // @Test
    // public void testRecordstoResource2() throws Exception{
    //   manager.clearResources();
    //   manager.setProject(project2);
    //   manager.rowsToResources();
    //   String secondRes = Resource.toJSON(manager.getResource(1));
    //   Assert.assertTrue(secondRes.contains("eng"));
    //   Assert.assertTrue(secondRes.contains("kor"));
    //   Assert.assertTrue(secondRes.contains("English"));
    //   Assert.assertFalse(secondRes.contains("reeeee"));
    //   Assert.assertFalse(secondRes.contains("hmm"));
    // }

    // @Test
    // public void testRecordstoResource3() throws Exception{
    //   manager.clearResources();
    //   manager.csv_headers = new LinkedList<String>(){{add("id"); add("type"); add("title"); add("display entry");
    // add("link"); add("abstract"); add("extent"); add("date"); add("script"); add("language"); add("holding repository snac id");}};
    //   manager.setProject(createCSVProject(TestingData2.resourceRecordCsv2));
    //   manager.rowsToResources();
    //   Resource fromDataRes = manager.getResource(0);
    //   String fromData = Resource.toJSON(fromDataRes);
    //   Assert.assertTrue(fromData.contains("English"));
    //   Assert.assertTrue(fromData.contains("eng"));
    //   Assert.assertTrue(fromData.contains("Korean"));
    //   Assert.assertFalse(fromData.contains("reeeee"));
    //   Assert.assertFalse(fromData.contains("hmm"));
    // }

    // @Test
    // public void testResourceEquivalent1() throws Exception{
    //   Resource fromDataRes = manager.createResourceRow(project.rows.get(0));
    //   Resource fromDataRes2 = manager.createResourceRow(project.rows.get(1));
    //   Resource fromDataRes3 = manager.createResourceRow(project.rows.get(2));
    //   String fromData = Resource.toJSON(fromDataRes);
    //   String fromData2 = Resource.toJSON(fromDataRes2);
    //   String fromData3 = Resource.toJSON(fromDataRes3);
    //   Assert.assertTrue(fromData.contains("Title1"));
    //   Assert.assertTrue(fromData2.contains("Title2"));
    //   Assert.assertTrue(fromData3.contains("Title3"));
    // }

    // @Test
    // public void testResourceGlobalOne() throws Exception{
    //   String response_str = manager.getColumnMatchesJSONString();
    //   Assert.assertTrue(response_str.contains("title"));
    // }

    // @Test
    // public void testResourceGlobalFalseFive() throws Exception{
    //   String response_str = manager.getColumnMatchesJSONString();
    //   Assert.assertFalse(response_str.contains("col5"));
    // }


    // @Test
    // public void testResourceGlobalTwo() throws Exception{
    //   String response_str = manager.getColumnMatchesJSONString();
    //   Assert.assertTrue(response_str.contains("abstract"));
    // }

    // @Test
    // public void testResourceGlobalThree() throws Exception{
    //   String response_str = manager.getColumnMatchesJSONString();
    //   Assert.assertTrue(response_str.contains("link"));
    // }

    // @Test
    // public void testResourceGlobalFalseFour() throws Exception{
    //   String response_str = manager.getColumnMatchesJSONString();
    //   Assert.assertFalse(response_str.contains("col4"));
    // }

    // @Test
    // public void testResourceUpload() throws Exception{
    //   manager.clearResources();
    //   upload.doPost(request, response);
    //   ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
    //   String response_str = response.get("done").textValue();
    //   Assert.assertNotNull(response_str);
    // }

    // @Test
    // public void testResourceUploadGet(){
    //     try{
    //         upload.doGet(request, response);
    //         ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
    //         String response_str = response.get("doneGet").textValue();
    //     }
    //     catch(Exception e){
    //         String a="";
    //         Assert.assertTrue(a.equals(""));
    //     }
    // }

    // @Test
    // public void testInsertIDResource(){
    //   manager.resource_ids.clear();
    //   Resource dummy = new Resource();
    //   String result1 = "{\"resource\":{\"id\":0}}";
    //   String result2 = "{\"resource\":{\"id\":1}}";
    //   String result3 = "wrong parse json";
    //   manager.insertID(result1, dummy);
    //   manager.insertID(result2, dummy);
    //   manager.insertID(result3, dummy);
    //   Assert.assertNull(manager.resource_ids.get(0));
    //   Assert.assertNotNull(manager.resource_ids.get(1));
    //   Assert.assertEquals(manager.resource_ids.size(), 2);
    // }

    // /*WARNING: This test overrides match_attributes*/
    // @Test
    // public void testUpdateColumns() throws Exception{
    //   String new_json = "{\"newcol1\":\"newvalue1\",\"newcol2\":\"newvalue2\"}";
    //   manager.updateColumnMatches(new_json);
    //   Assert.assertEquals(manager.match_attributes.get("newcol1"), "newvalue1");
    // }
}
