/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.slu.tpen.servlet.util;

import edu.slu.tpen.servlet.Constant;
import java.io.UnsupportedEncodingException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author hanyan
 */
public class CreateAnnoListUtil {

    public static JSONObject createEmptyAnnoList(Integer projectID, String canvasID, String testingFlag, JSONArray resource, int uID, String localName) {
        JSONObject canvasList = new JSONObject();
        canvasList.element("@type", "sc:AnnotationList");
        canvasList.element("on", canvasID);
        canvasList.element("resources", resource);
        canvasList.element("isPartOf", "" + projectID);
        canvasList.element("@context", Constant.RERUM_CONTEXT);
        canvasList.element("oa:createdBy", localName + "/" + uID);
        canvasList.element("TPEN_NL_TESTING", testingFlag);
        return canvasList;
    }

    /**
     *
     * @author hanyan
     */
    public static JSONObject createEmptyAnnoList(Integer projectID, String canvasID, JSONArray resource) throws UnsupportedEncodingException {
        JSONObject canvasList = new JSONObject();
        canvasList.element("@type", "sc:AnnotationList");
        canvasList.element("on", canvasID);
        canvasList.element("originalAnnoID", "");
        canvasList.element("version", 1);
        canvasList.element("permission", 0);
        canvasList.element("forkFromID", "");
        canvasList.element("resources", resource);
        canvasList.element("proj", projectID);
        canvasList.element("@context", "http://iiif.io/api/presentation/2/context.json");
        //canvasList.element("testing", "msid_creation"); 
        return canvasList;
    }
}
