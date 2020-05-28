/*
 * Copyright 2014- Saint Louis University. Licensed under the
 *	Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package edu.slu.tpen.servlet;

/**
 * Class to hold all static values that will be reused in project. 
 * @author Bryan Haberberger
 */
public class Constant {
    public static String OLD_ANNOTATION_SERVER_ADDR = "http://165.134.105.29/annotationstore";
    public static String ANNOTATION_SERVER_ADDR = "http://devstore.rerum.io/v1/api";
    public static String TPEN_CANVAS_PREFIX = "http://t-pen.org/TPEN";
    public static String RERUM_ID_PATTERN = "http://devstore.rerum.io/v1/id";
    public static String RERUM_ACCESS_TOKEN_URL = "http://devstore.rerum.io/v1/api/accessToken.action";
    public static String RERUM_REFRESH_TOKEN_URL = "http://devstore.rerum.io/v1/api/refreshToken.action";
    public static String RERUM_CONTEXT = "http://devstore.rerum.io/v1/context.json";
    public static String PROPERTIES_FILE_NAME = "version.properties";
}
