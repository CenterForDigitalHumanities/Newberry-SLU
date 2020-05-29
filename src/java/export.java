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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.lowagie.text.DocumentException;
import static edu.slu.util.ServletUtils.reportInternalError;
import static java.lang.Integer.parseInt;
import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;
import static textdisplay.Manuscript.getFullDocument;
import static textdisplay.Manuscript.getPartialDocument;
import textdisplay.Metadata;
import textdisplay.Project;
import textdisplay.TagFilter;
import static textdisplay.TagFilter.noteStyles.endnote;
import static textdisplay.TagFilter.noteStyles.footnote;
import static textdisplay.TagFilter.noteStyles.inline;
import static textdisplay.TagFilter.noteStyles.remove;
import static textdisplay.TagFilter.noteStyles.sidebyside;
import static textdisplay.TagFilter.styles.bold;
import static textdisplay.TagFilter.styles.italic;
import static textdisplay.TagFilter.styles.none;
import static textdisplay.TagFilter.styles.paragraph;
import static textdisplay.TagFilter.styles.superscript;
import static textdisplay.TagFilter.styles.underlined;

/**
 *
 * @author jim
 */
public class export extends HttpServlet {

   /**
    * Processes requests for the HTTP <code>GET</code> method.
    *
    * @param req servlet request
    * @param resp servlet response
    * @throws ServletException if a servlet-specific error occurs
    * @throws IOException if an I/O error occurs
    */
   @Override
   protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
         throws ServletException, IOException {

      final Enumeration paramNames = req.getParameterNames();
      String paramString = "";
      while (paramNames.hasMoreElements()) {
         final String name = (String) paramNames.nextElement();
         paramString += name + "=" + req.getParameter(name) + "&";
      }
      LOG.log(INFO, "Export request params: {0}", paramString);

      try {
         int projectID = 0;
         if (req.getParameter("projectID") != null) {
            projectID = parseInt(req.getParameter("projectID"));
         }
         final Project p = new Project(projectID);

         switch (req.getParameter("type")) {
            case "pdf":
               exportPDF(req, resp, p);
               break;
            case "rtf":
               exportRTF(req, resp, p);
               break;
            case "xml":
               exportXML(req, resp, p);
               break;
         }
      } catch (SQLException | DocumentException ex) {
            reportInternalError(resp, ex);
      }
   }

   private void exportPDF(final HttpServletRequest req, final HttpServletResponse resp, final Project p)
         throws IOException, SQLException, DocumentException {
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
         resp.setContentType("application/pdf");
            //response.setHeader( "Content-Disposition", "filename="+p.getProjectName()+".pdf");

         String text = getMetadataString(req, p) + "\n";
         text += getDocumentText(req, p, false);
         final TagFilter f = new TagFilter(text);
         final String[] tagArray = getTags(req);
         final TagFilter.styles[] styleArray = getStyles(req, tagArray);
         f.replaceTagsWithPDFEncoding(tagArray, styleArray, bos);
         resp.getOutputStream().write(bos.toByteArray());
      }
   }

   private void exportRTF(final HttpServletRequest req, final HttpServletResponse resp, final Project p)
         throws IOException, SQLException {
      resp.setContentType("application/rtf");
      resp.setHeader("Content-Disposition",
            " filename=\"" + p.getProjectName().replace(",", "").replace(".", "") + ".rtf\"");
      final PrintWriter out = resp.getWriter();
      String text = getMetadataString(req, p);
      final TagFilter.noteStyles noteStyle = getNoteStyle(req);
      if (req.getParameter("beginFolio") != null && req.getParameter("endFolio") != null) {
         final int endFolio = parseInt(req.getParameter("endFolio"));
         final int beginFolio = parseInt(req.getParameter("beginFolio"));
         text += getPartialDocument(p, noteStyle, hasLineBreak(req), hasPageLabels(req), beginFolio,
               endFolio);
      } else {
         text += getFullDocument(p, noteStyle, hasLineBreak(req), hasPageLabels(req), false);
      }
      final String[] tagArray = getTags(req);
      final TagFilter.styles[] styleArray = getStyles(req, tagArray);
      final TagFilter f = new TagFilter(text);
      f.replaceTagsWithRTFEncoding(tagArray, styleArray, out);
   }

   private void exportXML(final HttpServletRequest req, final HttpServletResponse resp, final Project p)
         throws SQLException, IOException {
      resp.setContentType("application/txt");
      resp.setCharacterEncoding("UTF-8");
      resp.setHeader("Content-Disposition", "filename=\"" + p.getProjectName() + ".txt");
      String text = "";

      if (req.getParameter("beginFolio") != null && req.getParameter("endFolio") != null) {
         Boolean imageWrap = false;
         if (req.getParameter("imageWrap") != null) {
            imageWrap = true;
         }
         final int endFolio = parseInt(req.getParameter("endFolio"));
         final int beginFolio = parseInt(req.getParameter("beginFolio"));
         text += getPartialDocument(p, getNoteStyle(req), hasLineBreak(req), hasPageLabels(req), beginFolio,
               endFolio, imageWrap);
      } else {
         Boolean imageWrap = false;
         if (req.getParameter("imageWrap") != null) {
            imageWrap = true;
         }
         text += getFullDocument(p, getNoteStyle(req), hasLineBreak(req), hasPageLabels(req), imageWrap);
      }
      if (req.getParameter("tei") != null) {
         // Stick the header after the <TEI> tag if it exists.
         if (text.contains("<TEI>")) {
            final String tmp = text;
            text = text.substring(0, text.indexOf("<TEI>") + 4) + p.getMetadata().getTEI()
                  + tmp.substring(tmp.indexOf("<TEI>") + 4);
         } // if there was no <TEI>, prepend the header. Wdont ahve to wrorry about
           // breaking the document, it is already not valid tei
         else {
            text = p.getMetadata().getTEI() + text;
         }
      }
      TagFilter f = new TagFilter(text);
      final String[] tagArray = getTags(req);

      // Note that XML styling only cares about style=none and style=remove; other
      // styles have no impact.
      final String[] tagArrayKeepContent = new String[tagArray.length];
      for (int i = 0; i < tagArray.length; i++) {
         switch (req.getParameter(STYLE_PARAM_PREFIX + (i + 1))) {
            case "remove":
               break;
            case "checked":
               tagArrayKeepContent[i] = tagArray[i];
               break;
            default:
               tagArray[i] = "";
               break;
         }
      }

      final PrintWriter out = resp.getWriter();
      final String result = f.removeTagsAndContents(tagArray);
      f = new TagFilter(result);
      out.append(f.stripTags(tagArrayKeepContent));
   }

   private boolean hasLineBreak(final HttpServletRequest req) {
      return "newline".equals(req.getParameter("linebreak"));
   }

   private boolean hasPageLabels(final HttpServletRequest req) {
      return req.getParameter("pageLabels") != null;
   }

   private TagFilter.noteStyles getNoteStyle(final HttpServletRequest req) {
      final String param = req.getParameter("notes");
      if (param != null) {
         switch (param) {
            case "sideBySide":
               return sidebyside;
            case "line":
               return inline;
            case "endnote":
               return endnote;
            case "footnote":
               return footnote;
            case "remove":
            default:
               break;
         }
      }
      return remove;
   }

   private String getMetadataString(final HttpServletRequest req, final Project p) throws SQLException {
      String metadataString = "";
      if (req.getParameter("metadata") != null) {
         final Metadata m = p.getMetadata();
         metadataString += "Title:" + m.getTitle() + "\n";
         metadataString += "Subtitle:" + m.getSubtitle() + "\n";
         metadataString += "MS identifier:" + m.getMsIdentifier() + "\n";
         metadataString += "MS settlement:" + m.getMsSettlement() + "\n";
         metadataString += "MS Repository:" + m.getMsRepository() + "\n";
         metadataString += "MS Collection:" + m.getMsCollection() + "\n";
         metadataString += "MS id number:" + m.getMsIdNumber() + "\n";
         metadataString += "Subject:" + m.getSubject() + "\n";
         metadataString += "Author:" + m.getAuthor() + "\n";
         metadataString += "Date:" + m.getDate() + "\n";
         metadataString += "Location:" + m.getLocation() + "\n";
         metadataString += "Language:" + m.getLanguage() + "\n";
         metadataString += "Description:" + m.getDescription() + "\n";
      }
      return metadataString;
   }

   private String getDocumentText(final HttpServletRequest req, final Project p, final boolean imageWrap)
         throws SQLException {
      final TagFilter.noteStyles noteStyle = getNoteStyle(req);
      if (req.getParameter("beginFolio") != null && req.getParameter("endFolio") != null) {
         final int endFolio = parseInt(req.getParameter("endFolio"));
         final int beginFolio = parseInt(req.getParameter("beginFolio"));
         return getPartialDocument(p, noteStyle, hasLineBreak(req), hasPageLabels(req), beginFolio, endFolio,
               imageWrap);
      } else {
         return getFullDocument(p, noteStyle, hasLineBreak(req), hasPageLabels(req), imageWrap);
      }
   }

   private String[] getTags(final HttpServletRequest req) {
      final List<String> tagList = new ArrayList<>();
      String tag;
      int i = 1;
      while ((tag = req.getParameter(TAG_PARAM_PREFIX + i)) != null) {
         tagList.add(tag);
         i++;
      }
      return tagList.toArray(new String[0]);
   }

   /**
    * Build an array of styles for the tags.
    * 
    * @param req      servlet request
    * @param tagArray array of tags already extracted
    * @return an array of styles corresponding to the tags
    */
   private TagFilter.styles[] getStyles(final HttpServletRequest req, final String[] tagArray) {
      final TagFilter.styles[] styleArray = new TagFilter.styles[tagArray.length];
      final TagFilter.noteStyles noteStyle = getNoteStyle(req);
      for (int i = 0; i < tagArray.length; i++) {
         if (tagArray[i].equals("tpen_note") && noteStyle == endnote) {
            styleArray[i] = superscript;
         }
         switch (req.getParameter(STYLE_PARAM_PREFIX + (i + 1))) {
            case "italic":
               styleArray[i] = italic;
               break;
            case "bold":
               styleArray[i] = bold;
               break;
            case "underlined":
               styleArray[i] = underlined;
               break;
            case "none":
               styleArray[i] = none;
               break;
            case "paragraph":
               styleArray[i] = paragraph;
               break;
            default:
               styleArray[i] = TagFilter.styles.remove;
               break;
         }
         if (tagArray[i].equals("tpen_note") && noteStyle == endnote) {
            styleArray[i] = superscript;
         }
         LOG.log(INFO, "Tag {0}: {1} {2}", new Object[] { i + 1, tagArray[i], styleArray[i] });
      }
      return styleArray;
   }

   /**
    * Returns a short description of the servlet.
    *
    * @return a String containing servlet description
    */
   @Override
   public String getServletInfo() {
      return "Piece-of-shit export servlet";
   }
   
   private static final String TAG_PARAM_PREFIX = "tag";
   private static final String STYLE_PARAM_PREFIX = "style";
   
   private static final Logger LOG = getLogger(export.class.getName());
}
