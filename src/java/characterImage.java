/*
 * Copyright 2011-2013 Saint Louis University. Licensed under the
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
 * @author Jon Deering
 */
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static imageLines.ImageCache.getImage;
import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static javax.imageio.ImageIO.write;
import match.blobGetter;
import tokens.TokenManager;

/**
 *
 * @author jdeerin1
 */
public class characterImage extends HttpServlet {
   
    /**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param milliSeconds
     * @param request servlet request
     * @param response servlet response
     * @return 
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
public static String getGMTTimeString(final long milliSeconds) {
        final SimpleDateFormat sdf = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'");
        return sdf.format(new Date(milliSeconds));
    }

    protected void processRequest(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        response.addHeader("Cache-Control", "max-age=3600");
        final long relExpiresInMillis = currentTimeMillis() + (1000 * 2600);
        response.addHeader("Expires", getGMTTimeString(relExpiresInMillis));
        response.setContentType("image/jpeg");
        int width, height, x, y;
        String pageIdentifier;
        int blobIdentifier;

        try {
            blobIdentifier = parseInt(request.getParameter("blob"));
            pageIdentifier = request.getParameter("page");
        } catch (final NumberFormatException | NullPointerException e) {
            return;
        }
        final blobGetter thisBlob = new blobGetter(pageIdentifier, blobIdentifier);
        final TokenManager man = new TokenManager();
        final String s = (man.getProperties().getProperty("SERVERCONTEXT") + "imageResize?folioNum=" + pageIdentifier
                + "&height=2000");
        out.print(s + "\n");
        BufferedImage originalImg = getImage(parseInt(pageIdentifier));// imageHelpers.readAsBufferedImage(new
        width = thisBlob.getHeight();
        height = thisBlob.getWidth();
        x = thisBlob.getX();
        y = thisBlob.getY();
        // scale coordinates based on the fixed 1500 pixel size of the observations
        final double factor = originalImg.getHeight() / (double) 2000;
        // factor=1.0;
        width = (int) (width * factor);
        height = (int) (height * factor);
        x = (int) (x * factor);
        y = (int) (y * factor);
        final OutputStream os = response.getOutputStream();
        write(originalImg.getSubimage(x, y, width, height), "jpg", os);
        originalImg = null; // hack to recover memory
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
            getLogger(characterImage.class.getName()).log(SEVERE, null, ex);
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
            getLogger(characterImage.class.getName()).log(SEVERE, null, ex);
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
