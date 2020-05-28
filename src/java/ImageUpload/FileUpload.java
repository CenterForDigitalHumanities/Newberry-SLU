/*
 * Copyright 2011-2014 Saint Louis University. Licensed under the
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
 * 
 * This servlet is modified from source code at http://www.javabeat.net/articles/262-asynchronous-file-upload-using-ajax-jquery-progress-ba-1.html
 */
package ImageUpload;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import java.io.*;
import java.sql.Connection;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import static edu.slu.util.ServletUtils.getDBConnection;
import static edu.slu.util.ServletUtils.getUID;
import static edu.slu.util.ServletUtils.reportInternalError;
import textdisplay.Folio;
import textdisplay.Manuscript;
import textdisplay.Project;
import tokens.TokenManager;
import user.Group;
import user.User;

/**
 *
 * @author obi1one
 */
public class FileUpload extends HttpServlet implements Servlet {

   /**
    *
    * @param req
    * @param resp
    * @throws ServletException
    * @throws IOException
    */
   @Override
   protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
         throws ServletException, IOException {
      try (PrintWriter out = resp.getWriter()) {
         final HttpSession session = req.getSession();

         if (session != null) {
            final FileUploadListener listener = (FileUploadListener) session.getAttribute("LISTENER");

            if (listener == null) {
               out.print("No active listener");
               return;
            }

            final long bytesRead = listener.getBytesRead();
            final long contentLength = listener.getContentLength();

            resp.setContentType("text/xml");
            final StringBuilder buffy = new StringBuilder();
            buffy.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
            buffy.append("<response>\n");
            buffy.append("\t<bytes_read>").append(bytesRead).append("</bytes_read>\n");
            buffy.append("\t<content_length>").append(contentLength).append("</content_length>\n");

            if (bytesRead == contentLength) {
               buffy.append("\t<finished />\n");
               // session.setAttribute("LISTENER", null);
            } else {
               final long percentComplete = ((100 * bytesRead) / contentLength);
               buffy.append("\t<percent_complete>").append(percentComplete).append("</percent_complete>\n");
            }
            buffy.append("</response>\n");
            out.println(buffy.toString());
            out.flush();
         }
      }
   }

   /**
    *
    * @param req
    * @param resp
    * @throws ServletException
    * @throws IOException
    */
   @Override
   protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
         throws ServletException, IOException {
      final HttpSession session = req.getSession();
      final int uid = getUID(req, resp);
      final TokenManager man = new TokenManager();
      if (uid > 0) {
         final ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
         final FileUploadListener listener = new FileUploadListener();
         upload.setProgressListener(listener);

         session.setAttribute("LISTENER", listener);

         try (Connection conn = getDBConnection()) {
            conn.setAutoCommit(false);
            final User thisUser = new User(uid);
            final String city = req.getParameter("city");
            final String collection = req.getParameter("collection");
            final String repository = req.getParameter("repository");
            final String archive = "private";

            final long maxSize = Integer.parseInt(man.getProperties().getProperty("maxUploadSize")); // 200 megs
            final List uploadedItems = upload.parseRequest(req);
            final Iterator i = uploadedItems.iterator();
            while (i.hasNext()) {
               final FileItem fileItem = (FileItem) i.next();
               if (fileItem.isFormField() == false) {
                  if (fileItem.getSize() > 0 && fileItem.getSize() < maxSize
                        && fileItem.getName().toLowerCase().endsWith("zip")) {
                     final File f = new File(man.getProperties().getProperty("uploadLocation") + "/"
                           + thisUser.getLname() + thisUser.getUID() + ".zip");
                     fileItem.write(f);
                     final Manuscript ms = new Manuscript(repository, archive, collection, city, -999);
                     UserImageCollection.create(conn, f, thisUser, ms);
                     final Group g = new Group(conn, ms.getShelfMark(), thisUser.getUID());
                     final Project p = new Project(conn, ms.getShelfMark(), g.getGroupID());
                     p.setFolios(conn, ms.getFolios());
                  }
               }
            }
            conn.commit();
         } catch (final Exception ex) {
            reportInternalError(resp, ex);
         } finally {
            session.setAttribute("LISTENER", null);
         }
      } else {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}
   }
}
