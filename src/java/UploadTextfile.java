/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.output.DeferredFileOutputStream;

/**
 *
 * @author jdeerin1
 */
public class UploadTextfile extends HttpServlet {
   DeferredFileOutputStream f;
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, SQLException, FileUploadException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            int projectID = 0;

            textdisplay.Project thisProject = null;
            if (request.getParameter("projectID") != null) {
                String location = "";
                projectID = Integer.parseInt(request.getParameter("projectID"));
                location = (Integer.parseInt(request.getParameter("p")) > 0)
                        ? "?projectID=" + projectID + "&p=" + request.getParameter("p")
                        : "?projectID=" + projectID;
                thisProject = new textdisplay.Project(projectID);
                if (ServletFileUpload.isMultipartContent(request)) {
                    final ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
                    final List fileItemsList = servletFileUpload.parseRequest(request);

                    final String optionalFileName = "";
                    final FileItem fileItem = null;
                    final Iterator it = fileItemsList.iterator();
                    while (it.hasNext()) {
                        final FileItem fileItemTemp = (FileItem) it.next();
                        final String tmp = fileItemTemp.getFieldName();
                        if (fileItemTemp.getFieldName().compareTo("file") == 0
                                && (fileItemTemp.getName().endsWith("txt") || fileItemTemp.getName().endsWith("xml"))) {

                            String textData;// =fileItemTemp.getString();
                            final BufferedReader in = new BufferedReader(
                                    new InputStreamReader(fileItemTemp.getInputStream(), "UTF-8"));
                            final StringBuilder b = new StringBuilder("");
                            while (in.ready()) {
                                b.append(in.readLine());
                            }
                            textData = b.toString();
                            thisProject.setLinebreakText(textData);
                            response.sendRedirect("transcription.jsp" + location);
                            return;

                        } else {
                            out.print(
                                    "You must upload a .txt or .xml file, other formats are not supported at this time.");
                        }
                    }
                }
            }
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
        try {
            processRequest(request, response);
        } catch (final SQLException ex) {
            Logger.getLogger(UploadTextfile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (final FileUploadException ex) {
            Logger.getLogger(UploadTextfile.class.getName()).log(Level.SEVERE, null, ex);
        }
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
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (final SQLException ex) {
            Logger.getLogger(UploadTextfile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (final FileUploadException ex)
            {
            Logger.getLogger(UploadTextfile.class.getName()).log(Level.SEVERE, null, ex);
            }
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
