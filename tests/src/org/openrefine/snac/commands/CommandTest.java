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
import org.snaccooperative.connection.SNACConnector;
import org.snaccooperative.exporters.SNACConstellationItem;
import org.snaccooperative.data.EntityId;
import org.snaccooperative.data.Resource;

public class CommandTest extends RefineTest{

    protected Project project = null;
    protected Project project2 = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected StringWriter writer = null;
    protected Command command = null;
    protected Command upload = null;
    // protected SNACResourceCreator resourceManager = SNACResourceCreator.getInstance();
    // protected SNACConstellationItem constellationManager = SNACConstellationItem.getInstance();
    protected EntityId entityId = null;
    //protected SNACConnector connector = SNACConnector.getInstance();

    // @BeforeMethod
    // public void SetUp() {
    //     // constellationManager.csv_headers = new LinkedList<String>(){{add("subject"); add("place"); add("occupation");}};
    //     HashMap<String, String> hash_map2 = new HashMap<String, String>();
    //     hash_map2.put("subject", "subject");
    //     hash_map2.put("place", "place");
    //     hash_map2.put("occupation", "occupation");
    //     //
    //     // resourceManager.match_attributes = hash_map;
    //     // constellationManager.match_attributes = hash_map2;

    //     project = createCSVProject(TestingData2.resourceCsv);
    //     project2 = createCSVProject(TestingData2.constellationCsv);

    //     //connector.saveKey("");
    //     // command = new SNACResourceCommand();
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
    // public void testEntityIdURI() throws Exception{
    //   EntityId testEntity = new EntityId();
    //   testEntity.setURI("12345");
    //   Assert.assertEquals(testEntity.getURI(), "12345");
    // }

    // @Test
    // public void testEntityIdID() throws Exception{
    //   EntityId testEntity = new EntityId();
    //   testEntity.setID(123);
    //   Assert.assertEquals(testEntity.getID(), 123);
    // }

    // @Test
    // public void testEntityIdText() throws Exception{
    //   EntityId testEntity = new EntityId();
    //   testEntity.setText("I like pizza");
    //   Assert.assertEquals(testEntity.getText(), "I like pizza");
    // }

    // @Test
    // public void testEntityIdEquals() throws Exception{
    //   EntityId testEntity1 = new EntityId();
    //   testEntity1.setID(123);
    //   EntityId testEntity2 = new EntityId();
    //   testEntity2.setID(456);
    //   Assert.assertFalse(testEntity1.equals(testEntity2));
    // }

    // @Test
    // public void testEntityToString() throws Exception{
    //   EntityId testEntity = new EntityId();
    //   testEntity.setText("123");
    //   Assert.assertEquals(testEntity.toString(), "EntityID: 123");
    // }

    // @Test
    // public void testResourceEmpty() throws Exception{
    //   // String response_str = resourceManager.obtainPreview();
    //   Assert.assertTrue(response_str.contains("no Resources"));
    // }

    // @Test
    // public void testResourceUpdate() throws Exception{
    //   // constellationManager.updateColumnMatches("");
    //   // String response_str = resourceManager.getColumnMatchesJSONString();
    //   Assert.assertFalse(response_str.contains("subject"));
    // }

    // @Test
    // public void testConstellationOne() throws Exception{
    //   // String response_str = constellationManager.getColumnMatchesJSONString();
    //   Assert.assertTrue(response_str.contains("subject"));
    // }

    // @Test
    // public void testConstellationFalse() throws Exception{
    //   // String response_str = constellationManager.getColumnMatchesJSONString();
    //   Assert.assertFalse(response_str.contains("title"));
    // }

    // @Test
    // public void testConstellationEmpty() throws Exception{
    //   // String response_str = constellationManager.obtainPreview();
    //   Assert.assertTrue(response_str.contains("no Constellations"));
    // }

    // /*
    // * Test API calls for recently published
    // */
    // @Test
    // public void testRecentlyPublished() throws Exception{
    //   DefaultHttpClient client = new DefaultHttpClient();
    //   HttpPost post = new HttpPost("http://api.snaccooperative.org");
    //   post.setEntity(new StringEntity("{\"command\":\"recently_published\"}", "UTF-8"));
    //   HttpResponse response = client.execute(post);
    //   String result = EntityUtils.toString(response.getEntity());
    //   Assert.assertTrue(result.contains("success"));
    // }

    // @Test
    // public void testProjectColumns() throws Exception{
    //   project = createCSVProject(TestingData2.inceptionWithNewCsv);
    //   //project = createProjectWithColumns("test_columns", TestingData2.column_values);
    //   Assert.assertEquals(project.columnModel.getColumnNames().size(), 3);
    // }

    // /*
    // * Test the converting of a string into a JSOn for download
    // */
    // @Test
    // public void testStringToJSONDownload1() throws Exception{
    //     DefaultHttpClient client = new DefaultHttpClient();
    //     HttpPost post = new HttpPost("http://api.snaccooperative.org");
    //     post.setEntity(new StringEntity("{\"command\": \"search\", \"term\": \"Washington\",\"count\": 10,\"start\": 0,\"entity_type\": \"person\"}","UTF-8"));
    //     HttpResponse response = client.execute(post);
    //     String result = EntityUtils.toString(response.getEntity());
    //     Assert.assertFalse(result.contains("2164"));
    // }

    //  /*
    //  * Test the converting of a string into a JSON for download
    //  */
    // @Test
    // public void testStringToJSONDownload2() throws Exception{
    //     DefaultHttpClient client = new DefaultHttpClient();
    //     HttpPost post = new HttpPost("http://api.snaccooperative.org");
    //     post.setEntity(new StringEntity("","UTF-8"));
    //     HttpResponse response = client.execute(post);
    //     String result = EntityUtils.toString(response.getEntity());
    //     Assert.assertFalse(result.contains("success"));
    // }
}
