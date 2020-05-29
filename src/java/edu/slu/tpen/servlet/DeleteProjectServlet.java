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

import edu.slu.util.ServletUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import javax.servlet.http.HttpSession;
import textdisplay.Project;
import user.Group;
import user.User;

/**
 * Delete project and project related attachments from tpen. This is a
 * transformation of tpen function to web service. It's using tpen MySQL
 * database.
 * 
 * @author hanyan
 */
public class DeleteProjectServlet extends HttpServlet {
    private int projectID;

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final int projectNumToDelete = Integer.parseInt(request.getParameter("projectID"));
        int UID = 0;
        UID = ServletUtils.getUID(request, response);

        // FIXME: unused?
        final HttpSession session = request.getSession();
        final Object role = "unknown";

        if (UID != -1) {
            // UID = Integer.parseInt(request.getSession().getAttribute("UID").toString());
            try {
                final Project todel = new Project(projectNumToDelete);
                final Group projectGroup = new Group(todel.getGroupID());
                final boolean isTPENAdmin = (new User(UID)).isAdmin();

                if (isTPENAdmin || projectGroup.isAdmin(UID)) {
                    todel.delete();
                } else {
                    response.setStatus(SC_UNAUTHORIZED);
                    final PrintWriter out = response.getWriter();
                    out.print(SC_UNAUTHORIZED);
                }
            } catch (final SQLException ex) {
                Logger.getLogger(DeleteProjectServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            // user doesn't log in
            final PrintWriter out = response.getWriter();
            out.print("User doesn't log in.");
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }

    /**
     * @return the projectID
     */
    public int getProjectID() {
        return projectID;
    }

    /**
     * @param projectID the projectID to set
     */
    public void setProjectID(final int projectID) {
        this.projectID = projectID;
    }
}
