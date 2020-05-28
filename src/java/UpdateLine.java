/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.owasp.esapi.ESAPI;
import textdisplay.Project;
import textdisplay.Transcription;
import user.Group;

/**
 *
 * @author jdeerin1
 */
public class UpdateLine extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     * 
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    protected void processRequest(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {

            if (request.getParameter("text") == null) {

                response.sendError(response.SC_BAD_REQUEST);
                return;
            }
            final String text = request.getParameter("text");
            String comment = "";
            if (request.getParameter("comment") != null) {
                comment = request.getParameter("comment");
            }
            final HttpSession session = request.getSession();

            final int FORBIDDEN = SC_FORBIDDEN;
            if (session.getAttribute("UID") == null || request.getParameter("projectID") == null) {
                response.sendError(response.scForbidden);
                return;
            }
            final int uid = Integer.parseInt(session.getAttribute("UID").toString());
            if (request.getParameter("line") == null) {

                if (request.getParameter("projectID") != null) {
                    final int projectID = Integer.parseInt(request.getParameter("projectID"));
                    try {
                        final Project thisProject = new Project(projectID);
                        if (new Group(thisProject.getGroupID()).isMember(uid)) {
                            thisProject.setLinebreakText(text);
                        }
                    } catch (final Exception e) {
                    }
                }
            }

            if (request.getParameter("projectID") != null) {
                final int projectID = Integer.parseInt(request.getParameter("projectID"));
                final int line = Integer.parseInt(request.getParameter("line"));
                try {
                    final Project thisProject = new Project(projectID);
                    if (new Group(thisProject.getGroupID()).isMember(uid)) {
                        final Transcription t = new Transcription(line);
                        t.archive(); // create an archived version before making changes
                        t.setText(text);
                        t.setComment(comment);
                        t.setCreator(uid);

                        out.print(ESAPI.encoder().decodeForHTML(new Transcription(line).getText()));
                        return;
                    } else {
                        response.sendError(FORBIDDEN);
                        return;
                    }
                } catch (final SQLException ex) {
                    // Logger.getLogger(UpdateLine.class.getName()).log(Level.SEVERE, null, ex);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                final int line = Integer.parseInt(request.getParameter("line"));

                Transcription t;
                try {
                    t = new Transcription(line);

                    t.setText(text);
                    t.setComment(comment);
                    out.print("success");
                    return;
                } catch (final SQLException ex) {
                    Logger.getLogger(UpdateLine.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            out.print("failure");

        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the
    // + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     * 
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * 
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
