/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.slu.tpen.servlet;

import edu.slu.tpen.entity.Image.Canvas;
import edu.slu.tpen.servlet.util.CreateAnnoListUtil;
import edu.slu.util.ServletUtils;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import textdisplay.Folio;
import textdisplay.Project;
import tokens.TokenManager;

/**
 *
 * @author bhaberbe
 *
 * Copy all project metadata and the annotation list with its annotations for
 * each canvas (here named folio). Makes use of Mongo Bulk operation
 * capabilities to limit the amount of necessary http connections which greatly
 * improved speed.
 */
public class CopyProjectAndAnnos extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        String result = "";
        final int uID = ServletUtils.getUID(request, response);
        int codeOverwrite = 500;
        boolean er = false;
        response.setHeader("Content-Type", "application/json; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        final Date date = new Date();
        // System.out.println("Copying at "+dateFormat.format(date));
        if (null != request.getParameter("projectID") && uID != -1) {
            final Integer projectID = Integer.parseInt(request.getParameter("projectID"));
            System.out.println("Copy project and annos for " + projectID);
            try {
                // find original project and copy to a new project.
                final Project templateProject = new Project(projectID);
                final Connection conn = ServletUtils.getDBConnection();
                conn.setAutoCommit(false);
                // in this method, it copies everything about the project.
                if (null != templateProject.getProjectName()) {
                    final Project thisProject = new Project(templateProject.copyProjectWithoutTranscription(conn, uID));
                    final TokenManager man = new TokenManager();
                    // set partener project. It is to make a connection on switch board.
                    thisProject.setAssociatedPartnerProject(projectID);
                    // PartnerProject theTemplate = new PartnerProject(projectID);
                    // thisProject.copyButtonsFromProject(conn, theTemplate.getTemplateProject());
                    // thisProject.copyHotkeysFromProject(conn, theTemplate.getTemplateProject());
                    // ^^copyProjectWithoutTranscription already does this, don't do it again here.
                    conn.commit();
                    final Folio[] folios = thisProject.getFolios();
                    // System.out.println("Created a new project template. What was the ID assigned
                    // to it: "+thisProject.getProjectID());
                    if (null != folios && folios.length > 0) {
                        for (Folio folio : folios) {
                            // System.out.println("Starting copy for canvas");
                            // Parse folio.getImageURL() to retrieve paleography pid, and then generate new
                            // canvas id
                            final String imageURL = folio.getImageURL();
                            // use regex to extract paleography pid
                            // THIS MUST MATCH THE NAMING CONVENTION IN JSONLDEXporter
                            final String canvasID = man.getProperties().getProperty("PALEO_CANVAS_ID_PREFIX")
                                    + imageURL.replaceAll("^.*(paleography[^/]+).*$", "$1"); // for paleo
                            // String canvasID = man.getProperties().getProperty("SERVERURL") +
                            // templateProject.getProjectName() + "/canvas/" +
                            // URLEncoder.encode(folio.getPageName(), "UTF-8"); // for slu testing
                            final JSONArray ja_allAnnoLists = Canvas.getAnnotationListsForProject(projectID, canvasID,
                                    uID, man);
                            JSONObject jo_annotationList = new JSONObject();
                            // ^^ this does all the filtering and will either have 0 or 1 lists for this
                            // particular version of TPEN
                            if (!ja_allAnnoLists.isEmpty()) {
                                jo_annotationList = ja_allAnnoLists.getJSONObject(0);
                            }
                            JSONArray new_resources = new JSONArray();
                            JSONArray resources = new JSONArray();
                            String parseThis;
                            String pubTok = man.getAccessToken();
                            final boolean expired = man.checkTokenExpiry();
                            if (expired) {
                                System.out.println(
                                        "TPEN_NL Token Manager detected an expired token, auto getting and setting a new one...");
                                pubTok = man.generateNewAccessToken();
                            }
                            if (!jo_annotationList.isEmpty() && (null != jo_annotationList.get("resources")
                                    && !jo_annotationList.get("resources").toString().equals("[]"))) {
                                try {
                                    resources = (JSONArray) jo_annotationList.get("resources");
                                } catch (final JSONException e) {
                                    System.out.println(
                                            "List we found could not be parsed, so we are defaulting with an empty list.");
                                    // If this list can't be parsed, the copied list will have errors. Just define
                                    // it as empty as the fail.
                                }
                                // add testing flag before passing off
                                for (int h = 0; h < resources.size(); h++) {
                                    resources.getJSONObject(h).element("TPEN_NL_TESTING",
                                            man.getProperties().getProperty("TESTING"));
                                    resources.getJSONObject(h).element("oa:createdBy",
                                            request.getLocalName() + "/" + uID);
                                    resources.getJSONObject(h).remove("_id");
                                    resources.getJSONObject(h).remove("@id");
                                    resources.getJSONObject(h).remove("addedTime");
                                    resources.getJSONObject(h).remove("originalAnnoID");
                                    resources.getJSONObject(h).remove("version");
                                    resources.getJSONObject(h).remove("permission");
                                    resources.getJSONObject(h).remove("forkFromID"); // retained for legacy v0 objects
                                    resources.getJSONObject(h).remove("serverName");
                                    resources.getJSONObject(h).remove("serverIP");
                                }

                                final URL postUrlCopyAnno = new URL(
                                        Constant.ANNOTATION_SERVER_ADDR + "/batch_create.action");
                                final HttpURLConnection ucCopyAnno = (HttpURLConnection) postUrlCopyAnno
                                        .openConnection();
                                ucCopyAnno.setDoInput(true);
                                ucCopyAnno.setDoOutput(true);
                                ucCopyAnno.setRequestMethod("POST");
                                ucCopyAnno.setUseCaches(false);
                                ucCopyAnno.setInstanceFollowRedirects(true);
                                ucCopyAnno.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                                ucCopyAnno.setRequestProperty("Authorization", "Bearer " + pubTok);
                                ucCopyAnno.connect();
                                try (DataOutputStream dataOutCopyAnno = new DataOutputStream(
                                        ucCopyAnno.getOutputStream())) {
                                    String str_resources;
                                    if (resources.size() > 0) {
                                        str_resources = resources.toString();
                                    } else {
                                        str_resources = "[]";
                                    }
                                    // System.out.println("Batch create these");
                                    // System.out.println(str_resources);
                                    final byte[] toWrite = str_resources.getBytes("UTF-8");
                                    dataOutCopyAnno.write(toWrite);
                                    dataOutCopyAnno.flush();
                                }
                                codeOverwrite = ucCopyAnno.getResponseCode();
                                response.setStatus(codeOverwrite);
                                String lines;
                                final StringBuilder sbAnnoLines = new StringBuilder();
                                try (BufferedReader returnedAnnoList = new BufferedReader(
                                        new InputStreamReader(ucCopyAnno.getInputStream(), "utf-8"))) {
                                    while ((lines = returnedAnnoList.readLine()) != null) {
                                        // System.out.println(lineAnnoLs);
                                        sbAnnoLines.append(lines);
                                    }
                                } catch (final IOException ex) {
                                    final StringBuilder sb;
                                    try ( // Forward error response from RERUM
                                            BufferedReader error = new BufferedReader(
                                                    new InputStreamReader(ucCopyAnno.getErrorStream(), "utf-8"))) {
                                        String errorLine;
                                        sb = new StringBuilder();
                                        while ((errorLine = error.readLine()) != null) {
                                            sb.append(errorLine);
                                        }
                                    }
                                    result = sb.toString();
                                    er = true;
                                    break;
                                }
                                ucCopyAnno.disconnect();
                                parseThis = sbAnnoLines.toString();
                                final JSONObject batchSaveResponse = JSONObject.fromObject(parseThis);
                                try {
                                    new_resources = (JSONArray) batchSaveResponse.get("new_resources");

                                } catch (final JSONException e) {
                                    System.out
                                            .println("Batch save response does not contain JSONARRAY in new_resouces.");
                                    result = "Batch save response does not contain JSONARRAY in new_resouces.";
                                    er = true;
                                    break;
                                }
                            } else {
                                // System.out.println("No annotation list for this canvas. do not call batch
                                // save. just save empty list.");
                            }
                            final JSONObject canvasList = CreateAnnoListUtil.createEmptyAnnoList(
                                    thisProject.getProjectID(), canvasID, man.getProperties().getProperty("TESTING"),
                                    new_resources, uID, request.getLocalName());
                            canvasList.element("copiedFrom", request.getParameter("projectID"));
                            final URL postUrl = new URL(Constant.ANNOTATION_SERVER_ADDR + "/create.action");
                            final HttpURLConnection uc = (HttpURLConnection) postUrl.openConnection();
                            uc.setDoInput(true);
                            uc.setDoOutput(true);
                            uc.setRequestMethod("POST");
                            uc.setUseCaches(false);
                            uc.setInstanceFollowRedirects(true);
                            uc.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                            uc.setRequestProperty("Authorization", "Bearer " + pubTok);
                            uc.connect();
                            try (DataOutputStream dataOut = new DataOutputStream(uc.getOutputStream())) {
                                final byte[] toWrite2 = canvasList.toString().getBytes("UTF-8");
                                dataOut.write(toWrite2);
                                dataOut.flush();
                            }
                            codeOverwrite = uc.getResponseCode();
                            response.setStatus(codeOverwrite);
                            try {
                                final BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(uc.getInputStream(), "utf-8"));
                                reader.close();
                                uc.disconnect();
                            } catch (final IOException ex) {
                                final StringBuilder sb;
                                try ( // forward error response from ererum
                                        BufferedReader error = new BufferedReader(
                                                new InputStreamReader(uc.getErrorStream(), "utf-8"))) {
                                    String errorLine;
                                    sb = new StringBuilder();
                                    while ((errorLine = error.readLine()) != null) {
                                        sb.append(errorLine);
                                    }
                                }
                                result = sb.toString();
                                er = true;
                                break;
                            }

                            // System.out.println("Finished this canvas.");
                        }
                    }
                    // System.out.println("Copy isPartOf and annos finished. Whats the ID to return:
                    // "+thisProject.getProjectID());
                    if (!er) {
                        final String propVal = man.getProperties().getProperty("CREATE_PROJECT_RETURN_DOMAIN");
                        result = propVal + "project/" + thisProject.getProjectID();
                    }
                } else {
                    System.out.println("Could not get a project name, this is an error.");
                    response.setStatus(codeOverwrite);
                    result = "Could not find a project name";
                }
            } catch (final Exception e) {
                response.setStatus(codeOverwrite);
                result = e.getMessage();
                response.getWriter().print(e);
            }
        } else {
            response.setStatus(SC_FORBIDDEN);
            result = "Unauthorized or invalid project speficied.";
        }
        final Date date2 = new Date();
        // .out.println();
        // System.out.println("Copying done at "+dateFormat.format(date2));
        response.getWriter().print(result);
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    public static JSONArray getFromV0(final JSONObject params) throws MalformedURLException, IOException {
        final URL postUrl = new URL(Constant.OLD_ANNOTATION_SERVER_ADDR + "/anno/getAnnotationByProperties.action");
        JSONArray v0Objs;
        final HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.connect();
        try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
            // value to save
            out.writeBytes("content=" + URLEncoder.encode(params.toString(), "utf-8"));
            out.flush();
        }
        final StringBuilder sb;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String line;
            sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                line = new String(line.getBytes(), "utf-8");
                sb.append(line);
            }
        }
        connection.disconnect();
        //FIXME: Every now and then, this line throws an error: A JSONArray text must start with '[' at character 1 of &lt
        String jarray = sb.toString();
        jarray = jarray.trim();
        v0Objs = JSONArray.fromObject(jarray);
        return v0Objs;
    }

}
