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

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import textdisplay.Project;
import user.Group;
import user.User;

/**
 * Add user to project and add this user to project group. This is a transformation of tpen function to web service.  
 * It's using tpen MySQL database. 
 * @author hanyan
 */
public class AddUserToProjectServlet extends HttpServlet {

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final HttpSession session = request.getSession();
        // System.out.println("Add user to project
        // ID:"+request.getParameter("projectID"));
        if (session.getAttribute("UID") != null) {
            final int UID = Integer.parseInt(session.getAttribute("UID").toString());
            try {
                final User thisUser = new user.User(UID);
                if (null != request.getParameter("uname") && null != request.getParameter("projectID")) {
                    final Project thisProject = new Project(Integer.parseInt(request.getParameter("projectID")));
                    final int result = thisUser.invite(request.getParameter("uname"), request.getParameter("fname"),
                            request.getParameter("lname"));
                    if (result == 0) {
                        // successfully send out email to user
                        final Group g = new Group(thisProject.getGroupID());
                        if (g.isAdmin(thisUser.getUID())) {
                            final User newUser = new User(request.getParameter("uname"));
                            g.addMember(newUser.getUID());
                            response.getWriter().print(newUser.getUID());
                        } else {
                            // if user is not admin, return unauthorized.
                            response.getWriter().print(response.SC_UNAUTHORIZED);
                        }
                    } else if (result == 2) {
                        // account created but email issue occured, usually happens in dev environments
                        // with no email server.
                        final user.Group g = new user.Group(thisProject.getGroupID());
                        if (g.isAdmin(thisUser.getUID())) {
                            final User newUser = new User(request.getParameter("uname"));
                            g.addMember(newUser.getUID());
                            response.getWriter().print(newUser.getUID());
                        } else {
                            // if user is not admin, return unauthorized.
                            response.getWriter().print(response.SC_UNAUTHORIZED);
                        }
                    } else if (result == 1) {
                        // user exits
                        final user.Group g = new user.Group(thisProject.getGroupID());
                        if (g.isAdmin(thisUser.getUID())) {
                            final User newUser = new User(request.getParameter("uname"));
                            g.addMember(newUser.getUID());
                            response.getWriter().print(newUser.getUID());
                        } else {
                            // if user is not admin, return unauthorized.
                            System.out.println("user not admin error");
                            response.getWriter().print(response.SC_UNAUTHORIZED);
                        }
                    } else {
                        // user doesn't exist
                        System.out.println("user doesnt exist error");
                        response.getWriter().print(response.SC_NOT_ACCEPTABLE);
                    }
                } else {
                    // if there is no uname
                    System.out.println("There is no uname error.");
                    response.getWriter().print(response.SC_NOT_ACCEPTABLE);
                }
            } catch (final SQLException ex) {
                System.out.println("SQL exception");
                Logger.getLogger(AddUserToProjectServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            // if user doesn't log in, return unauthorized.
            System.out.println("No user logged in.");
            response.getWriter().print(response.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        this.doPost(req, resp);
    }
    
}
