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
package detectimages;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * A blob is a representation of the boolean matrix that describes a single glyph from a binarized image
 */
public class blob implements Serializable {
   //all images are resized to have this height.

   static final int maxHeight = 2000;
   static final int minBlobSize = 15;

   /**
    * Draw a blob on an image starting from the given coordinates.
    */
   public static void drawBlob(final BufferedImage img, final int x, final int y, final blob b) {
      int color = 0xcc0000;
      if (b.size % 3 == 0) {
         color = 0xcc0000;
      }
      if (b.size % 3 == 1) {
         color = 0x000099;
      }
      if (b.size % 3 == 2) {
         color = 0x006600;
      }

      for (int i = 0; i < b.pixels.size(); i++) {
         final pixel p = b.pixels.get(i);
         try {
            img.setRGB(x + p.x, y + p.y, color);
         } catch (final ArrayIndexOutOfBoundsException e) {
            return;
         }
      }
   }

   /**
    * Draw the blob onto a buffere
    */
   public static void drawBlob(final BufferedImage img, final int x, final int y, final blob b, final int color) {
      for (int i = 0; i < b.pixels.size(); i++) {
         final pixel p = b.pixels.get(i);
         try {
            img.setRGB(x + p.x, y + p.y, color);
         } catch (final ArrayIndexOutOfBoundsException e) {
         }
      }
   }

   // coordinates of a pixel in the blob. Any pixel will do.
   static Pattern p = Pattern.compile("\\n");
   static Pattern comma = Pattern.compile(",");
   protected int x;
   protected int y;
   protected int width;
   protected int height;
   public matrixBlob matrixVersion;
   public BlockBlob blockVersion;
   public altBlob altVersion;
   protected int size;
   public blob blob2;
   protected Vector<pixel> pixels = new Vector(25, 25);
   public int color;
   public int id;
   public BufferedImage copy;
   public pixel[] arrayVersion;

   public blob() {
      size = 0;
      x = 0;
      y = 0;
      id = -1;
   }

   public blob(final int x, final int y) {
      this.x = x;
      this.y = y;
      // pixels.push(new pixel(x,y));
      id = -1;
      size = 0;
   }

   /**
    * Service a request for a single blob from a page, intended for drawing the
    * blob. Currently loads the entire page worth of data, so quite inefficient.
    * Returns null if it couldnt service the request.
    */
   public static blob getBlob(final String imageName, final int blobIdentifier) {
      try {
         final Vector<blob> b = blob.getBlobs("data/" + imageName);

         return b.get(blobIdentifier);
      } catch (final FileNotFoundException ex) {
         Logger.getLogger(blob.class.getName()).log(Level.SEVERE, null, ex);
         return null;
      } catch (final IOException ex) {
         Logger.getLogger(blob.class.getName()).log(Level.SEVERE, null, ex);
         return null;
      }
   }

   /**
    * Service a request for a single blob from a page, intended for drawing the
    * blob. Currently loads the entire page worth of data, so quite inefficient.
    * Returns null if it couldnt service the request.
    */
   public static blob getBlob(final String path, final String imageName, final int blobIdentifier) {
      try {
         final Vector<blob> b = blob.getBlobs(path + imageName);

         return b.get(blobIdentifier);
      } catch (final FileNotFoundException ex) {
         Logger.getLogger(blob.class.getName()).log(Level.SEVERE, null, ex);
         return null;
      } catch (final IOException ex) {
         Logger.getLogger(blob.class.getName()).log(Level.SEVERE, null, ex);
         return null;
      }
   }

   /**
    * Draw this blob on the image in the requested color. Color is RGB.
    */
   protected void drawBlob(final BufferedImage img, final int color) {
      final Stack<pixel> pixels2 = new Stack();
      for (int i = 0; i < pixels.size(); i++) {
         final pixel thisOne = pixels.get(i);
         try {
            img.setRGB(thisOne.x, thisOne.y, color);
         } catch (final ArrayIndexOutOfBoundsException ex) {
         }
         pixels2.push(thisOne);
      }
      // displayImage(img);
      pixels = pixels2;
   }

   /**
    * Find the topmost and leftmost points, and subtract their positions from all
    * pixels, the result being a coordinate set that describes this blob from a
    * base of 0,0
    */
   protected void calculateRelativeCoordinates() {

      int top_lowest = 9999999;
      int left_lowest = 9999999;
      for (int i = 0; i < pixels.size(); i++) {
         final pixel thisOne = pixels.get(i);
         if (thisOne.y < left_lowest) {
            left_lowest = thisOne.y;
         }
         if (thisOne.x < top_lowest) {
            top_lowest = thisOne.x;
         }

      }

      // This is the point that the relative coordinates are based on, store its real
      // location within the image so the blob can be
      // redrawn later using the blob data.
      this.x = top_lowest;
      this.y = left_lowest;
      for (int i = 0; i < pixels.size(); i++) {
         final pixel tmp = pixels.get(i);
         tmp.x = (short) (tmp.x - this.x);
         tmp.y = (short) (tmp.y - this.y);
      }
      sort();
   }

   /**
    * Sort blob pixels by x, with y as the secondary sort criteria
    */
   private void sort() {
      final pixel[] pixelArray = pixels.toArray(new pixel[pixels.size()]);
      Arrays.sort(pixelArray);
      pixels = new Vector();
      for (int i = 0; i < pixelArray.length; i++) {
         pixels.add(pixelArray[i]);
      }
   }

   /**
    * count the number of black pixels adjacent to x,y using recursion. this is the
    * character/blob segmentation mechanism. It is primative.
    */
   public void count(final BufferedImage img, final int currentx, final int currenty) {
      // increase size and set this pixel to white so it doesnt get counted again
      // System.out.println(currentx+" "+currenty);

      // if(size>50000)
      // return;

      img.setRGB(currentx, currenty, -1);
      if (size > 1 && size < 4000) {
         copy.setRGB(currentx, currenty, color);
         pixels.add(new pixel(currentx, currenty));

         // imageHelpers.writeImage(copy, "/usr/web/bin"+size+".jpg");
      }
      // prevent stack overflow if the blob is too large, if it is this large it is
      // uninteresting
      if (size > 4000) {
         return;
      }
      size++;
      // check the 8 surrounding pixels, if any of them is black, call this function
      // again.
      try {
         /*
          * if(img.getRGB(currentx-1, currenty)!=-1)
          * 
          * { count(img,currentx-1, currenty); } if(img.getRGB(currentx-1,
          * currenty+1)!=-1) { count(img,currentx-1, currenty+1); }
          */
         if (img.getRGB(currentx - 1, currenty) != -1) {
            count(img, currentx - 1, currenty);
         }
         if (img.getRGB(currentx, currenty + 1) != -1) {
            count(img, currentx, currenty + 1);
         }
         if (img.getRGB(currentx, currenty - 1) != -1) {
            count(img, currentx, currenty - 1);
         }
         if (img.getRGB(currentx + 1, currenty) != -1) {
            count(img, currentx + 1, currenty);
         }
         /*
          * if(img.getRGB(currentx+1, currenty+1)!=-1) { count(img,currentx+1,
          * currenty+1); } if(img.getRGB(currentx+1, currenty-1)!=-1) {
          * count(img,currentx+1, currenty-1); }
          */
      } catch (final ArrayIndexOutOfBoundsException e) {
      }
   }

   /**
    * Given mean height and width for this page, this blob will attpemt to break
    * itself into a reasonable number of smaller pieces.
    */
   /*
    * blob [] breakBlob(int width, int height) {
    * 
    * blob [] toret=new blob[1]; matrixBlob thisOne = new matrixBlob(this); int []
    * [] matrixVersion=thisOne.matrix return toret; }
    */
   /**
    * count the number of black pixels adjacent to x,y No longer used, blob drawing
    * happens after this so overly large and small blobs are excluded from the
    * drawing process, and so the same methodology can be used for the graphical
    * demonstration where blobs are drawn as they are matched.
    */
   public void count(final BufferedImage img, final BufferedImage bin, final int currentx, final int currenty) {
      // increase size and set this pixel to white so it doesnt get counted again
      // System.out.println(currentx+" "+currenty);

      if (size > 5000) {
         return;
      }
      img.setRGB(currentx, currenty, -1);
      if (size > 1) {
         copy.setRGB(currentx, currenty, color);

         // imageHelpers.writeImage(copy, "/usr/web/bin"+size+".jpg");
      }
      size++;
      // check the 8 surrounding pixels, if any of them is black, call this function
      // again.
      try {
         if (img.getRGB(currentx - 1, currenty) != -1) {
            count(img, bin, currentx - 1, currenty);
         }
         /*
          * if(img.getRGB(currentx-1, currenty+1)!=-1) { count(img,bin,currentx-1,
          * currenty+1); } if(img.getRGB(currentx-1, currenty-1)!=-1) {
          * count(img,bin,currentx-1, currenty-1); }
          */
         if (img.getRGB(currentx, currenty + 1) != -1) {
            count(img, bin, currentx, currenty + 1);
         }
         if (img.getRGB(currentx, currenty - 1) != -1) {
            count(img, bin, currentx, currenty - 1);
         }
         if (img.getRGB(currentx + 1, currenty) != -1) {
            count(img, bin, currentx + 1, currenty);
         }
         /*
          * if(img.getRGB(currentx+1, currenty+1)!=-1) { count(img,bin,currentx+1,
          * currenty+1); } if(img.getRGB(currentx+1, currenty-1)!=-1) {
          * count(img,bin,currentx+1, currenty-1); }
          */
      } catch (final ArrayIndexOutOfBoundsException e) {
      }
   }

   public static BufferedImage drawBlob(final blob b, final BufferedImage bin) {

      return bin;
   }

   /**
    * Display the image...like on the screen. Useful for debugging, not so great on
    * a server.
    */
   public static ImagePanel2 displayImage(final BufferedImage img) {
      final JFrame fr = new JFrame();
      fr.setDefaultCloseOperation(fr.EXIT_ON_CLOSE);
      // fr.setTitle(title)
      final ImagePanel2 pan = new ImagePanel2(img);
      final Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
      final Dimension screensize = toolkit.getScreenSize();

      pan.setSize(screensize.width, screensize.height);
      fr.getContentPane().add(pan);
      fr.pack();
      fr.setSize(img.getWidth(), img.getHeight());
      fr.show();
      return pan;
   }

   public static class ImagePanel2 extends JComponent {

      protected BufferedImage image;

      public ImagePanel2() {
      }

      public ImagePanel2(final BufferedImage img) {
         image = img;
      }

      public void setImage(final BufferedImage img) {
         image = img;
      }

      public void paintComponent(final Graphics g) {
         final Rectangle rect = this.getBounds();
         if (image != null) {
            int newWidth, newHeight;
            final double scale = image.getWidth() / 800;
            newHeight = 800;
            newWidth = (int) (image.getWidth() / scale);
            g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), this);
         }
      }
   }

   public int getX() {
      return x;
   }

   public int getY() {
      return y;
   }

   public int getSize() {
      return size;
   }

   /**
    * Returns the percentage of pixels in the relative positioned blobs that are in
    * common
    */
   public static double compare(final blob a, final blob b) {
      if (a.size - b.size > 500 || a.size - b.size < -500) {
         return 0.01;
      }
      int good = 0;
      // first, find the center of mass of each blob, this will be used to best align
      // them. well, maybe not best.
      final int axSum = 0;
      final int aySum = 0;
      final int bxSum = 0;
      final int bySum = 0;
      final pixel[] aArray = a.pixels.toArray(new pixel[a.pixels.size()]);
      final pixel[] bArray = b.pixels.toArray(new pixel[b.pixels.size()]);

      int innercount = 0;
      /*
       * for(int i=0;i<aArray.length;i++) { pixel aCurrent=aArray[i];
       * axSum+=aCurrent.x; aySum+=aCurrent.y; }
       * 
       * for(int i=0;i<bArray.length;i++) { pixel bCurrent=bArray[i];
       * Arrays.sort(bArray); bxSum+=bCurrent.x; bySum+=bCurrent.y; }
       * 
       * bxSum=bxSum/b.size; bySum=bySum/b.size;
       * 
       * aySum=aySum/a.size; axSum=axSum/a.size; int yDiff=aySum-bySum; int
       * xDiff=axSum-bxSum; if(xDiff<0) xDiff=xDiff*-1; if(yDiff<0) yDiff=xDiff*-1;
       * System.out.print("xDiff:"+xDiff+" yDiff:"+yDiff+"\n"); int maxgood=0;
       */

      final Boolean wouldabroke = false;
      for (int i = 0; i < aArray.length; i++) {

         innercount = 0;
         final pixel aCurrent = aArray[i];

         for (int j = 0; j < bArray.length; j++) {
            final pixel bCurrent = bArray[j];
            if ((aCurrent.x == (bCurrent.x) && aCurrent.y == (bCurrent.y))) {
               // System.out.print("offset "+ offset+"\n");

               good++;

            }

            innercount++;
         }
      }

      if (a.size > b.size) {
         return (double) good / a.size;
      } else {
         return (double) good / b.size;
      }

   }

   /**
    * @Depricated we no longer write blob data to a database
    */
   public static void writeBlob(final Connection j, final blob b, final String image) throws SQLException {
      final String query = "Insert into blobs(image, size) values(?,?)";
      final String pixelQuery = "Insert into pixels(x,y,BlobId) values(?,?,?)";

      PreparedStatement stmt = j.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
      stmt.setInt(2, b.size);
      stmt.setString(1, image);
      ResultSet rs;
      stmt.execute();
      rs = stmt.getGeneratedKeys();
      rs.next();
      final int blobID = rs.getInt(1);

      /*
       * stmt=j.
       * prepareStatement("insert into bin_blobs (image, blob, blobID) values(?,?,?)"
       * ); stmt.setString(1, image); stmt.setObject(2, b); stmt.setInt(3, blobID);
       * stmt.execute();
       */

      stmt = j.prepareStatement(pixelQuery);
      stmt.setInt(3, blobID);
      final Enumeration e = b.pixels.elements();
      final String InsertQuery = "insert into pixels(x,y,BlobId) values(";
      final int ctr = 0;
      while (e.hasMoreElements()) {

         final pixel thisOne = (pixel) e.nextElement();

         stmt.setInt(1, thisOne.x);
         stmt.setInt(2, thisOne.y);
         stmt.execute();
      }

   }

   /**
    * Write the coordinate pairs for all black pixels in this blob in an xml format
    * that can be read later
    */
   public static void writeBlob(final BufferedWriter w, final blob b) throws IOException {
      if (b.pixels.size() < 25) {
         return;
      }
      if (b.pixels.size() > 2000) {
         return;
      }
      // save the original location of the blob within its image
      w.append("b<sx>" + b.x + "</sx><sy>" + b.y + "</sy>\n");
      final Enumeration e = b.pixels.elements();
      while (e.hasMoreElements()) {
         final pixel p = (pixel) e.nextElement();
         w.append(+p.x + "," + p.y + "\n");
      }

      w.flush();
   }

   /**
    * Rather than writing coordinate pairs, write the dimensions of the blob canvas
    * and 1 for black 0 for white, this reads in much faster than coordinate pairs
    */
   public static void writeMatrixBlob(final BufferedWriter w, final blob b) throws IOException {
      /**
       * if(b.pixels.size()<25) return; if(b.pixels.size()>4000) return;*
       */
      // save the original location of the blob within its image
      w.append("b<sx>" + b.x + "</sx><sy>" + b.y + "</sy>\n");

      final int[][] matrix = b.matrixVersion.matrix;
      if (matrix.length <= 0) {
         return;
      }
      w.append("<szx>" + matrix.length + "</szx><szy>" + matrix[0].length + "</szy><id>" + b.id + "</id>\n");
      for (int i = 0; i < matrix.length; i++) {
         for (int j = 0; j < matrix[0].length; j++) {
            w.append(matrix[i][j] + ",");
         }
      }

      w.flush();
   }

   /**
    * Read a comparison assignment from an assignment file
    */
   public static String[] getAssignment(final BufferedReader assignmentReader, final File statefile) {
      try {
         final String buff = assignmentReader.readLine();
         final String[] toret = buff.split(" ");
         final FileWriter w = new FileWriter(statefile);
         w.write(buff);
         w.flush();
         w.close();
         return toret;

      } catch (final IOException ex) {

         return null;
      }

   }

   /**
    * Read a comparison assignment from an assignment file
    */
   public static String[] getAssignment(final BufferedReader stateReader, final BufferedReader assignmentReader) {
      try {
         final String buff = stateReader.readLine();
         String assignment = "";
         while (assignment.compareTo(buff) != 0) {
            assignment = assignmentReader.readLine();
         }
         final String[] toret = buff.split(" ");
         return toret;

      } catch (final IOException ex) {

         return null;
      }

   }

   /**
    * @Depricated we no longer save these values to a DB
    */
   public static void writeResults(final Connection j, final int count, final String[] imageNames, final int ms)
         throws SQLException {

      try {
         // Connection j=dbWrapperOld.getConnection();
         // PreparedStatement stmt=j.prepareStatement("insert into comparisons
         // (count,image1,image2,ms) values(? ,? ,?,? )");
         final PreparedStatement stmt = j
               .prepareStatement("update comparisons set count=? where image1=? and image2=?");
         // PreparedStatement stmt=j.prepareStatement("insert into
         // comparisons(count,image1,image2) values(?,?,?)");
         stmt.setInt(1, count);
         stmt.setString(2, imageNames[0]);
         stmt.setString(3, imageNames[1]);
         stmt.setInt(4, ms);
         stmt.execute();
         // j.close();
         return;
      } catch (final Exception e) {
         System.out.print("Encountered error sending results. " + e.toString() + "\n. Retrying in 5 seconds...");

      }
      // failed last time, try again.
      try {
         Thread.sleep(4000);
      } catch (final InterruptedException ex) {
         Logger.getLogger(blob.class.getName()).log(Level.SEVERE, null, ex);
      }
   }

   public static synchronized void writeMatchResults(final String toWrite, final FileWriter w) {

      try {

         w.append(toWrite);
         w.flush();
      } catch (final IOException ex) {
         ex.printStackTrace();
      }
   }

   public static synchronized void writeMatchResults(final int count1, final int count2, final String[] imageNames,
         final FileWriter w) {

      try {

         w.append(imageNames[0] + ":" + count1 + ";" + imageNames[1] + ":" + count2 + "\n");
         w.flush();
      } catch (final IOException ex) {
         ex.printStackTrace();
      }
   }

   public static void writeResults(final int count, final String[] imageNames, final FileWriter w) throws SQLException {
      try {
         w.append(imageNames[0] + ":" + imageNames[1] + ":" + count + "\n");
         System.out.print(imageNames[0] + ":" + imageNames[1] + ":" + count + "\n");
         System.out.flush();
         w.flush();
      } catch (final IOException ex) {
         Logger.getLogger(blob.class.getName()).log(Level.SEVERE, null, ex);
      }
   }

   /**
    * Load the blobs associated with a particular image from the database
    */
   /*
    * public static Vector<blob> getBlobs(String image, Connection j) throws
    * SQLException { Vector <blob> blobs=new Vector(); PreparedStatement
    * stmt=j.prepareStatement("select id from blobs where image=?");
    * stmt.setString(1, image); ResultSet rs=stmt.executeQuery(); while(rs.next())
    * { PreparedStatement
    * stmt2=j.prepareStatement("select x,y from pixels where blobID=?"); ResultSet
    * pixelSet=stmt2.executeQuery(); blob thisOne=new blob(); while(rs.next()) {
    * pixel thisPixel=new pixel(pixelSet.getInt("x"),pixelSet.getInt("y"));
    * thisOne.pixels.push(thisPixel); }
    * 
    * } return blobs; }
    */
   /**
    * Read the blobs stored in a file
    */
   public static Vector<blob> getBlobs(final String filename) throws FileNotFoundException, IOException {
      // do this is the blob file is in matrix format rather than coordinate pair
      // format
      if (true) {
         return (getMatrixBlobs(filename));
      }

      final Vector<blob> blobs = new Vector();
      // if(lastPage!=null)
      // return lastPage;
      blob thisOne = new blob();
      int maxX = 0;
      int maxY = 0;
      String buff;

      int idCounter = 0;

      final String[] stuff = blob.readFileIntoArray(filename); // p.split(readFile(filename.replace(".txt.txt",
                                                               // ".txt")));

      for (int i = 0; i < stuff.length; i++) {
         buff = stuff[i];

         if (buff.contains("b")) {
            if (thisOne != null) {
               thisOne.arrayVersion = thisOne.pixels.toArray(new pixel[thisOne.pixels.size()]);
               thisOne.width = maxX;
               thisOne.height = maxY;
               thisOne.matrixVersion = new matrixBlob(thisOne);
               // thisOne.blockVersion=new BlockBlob(thisOne.matrixVersion);
               thisOne.altVersion = new altBlob(thisOne);
               blobs.add(thisOne);
               thisOne.id = idCounter;
               idCounter++;
               maxX = 0;
               maxY = 0;
            }
            thisOne = new blob();
            try {
               final int startx = buff.indexOf("<sx>") + 4;
               final int endx = buff.indexOf("</sx>");
               final int starty = buff.indexOf("<sy>") + 4;
               final int endy = buff.indexOf("</sy>");
               final int x = Integer.parseInt(buff.substring(startx, endx));

               final int y = Integer.parseInt(buff.substring(starty, endy));
               thisOne.x = x;
               thisOne.y = y;

            } catch (final StringIndexOutOfBoundsException e) {
               final int k = 0;
            }
         } else {
            final String[] parts = comma.split(buff);
            final int x = Integer.parseInt(parts[0]);

            final int y = Integer.parseInt(parts[1]);
            if (x > maxX) {
               maxX = x;
            }
            if (y > maxY) {
               maxY = y;
            }
            thisOne.pixels.add(new pixel(x, y));
            thisOne.size++;
         }

      }
      // System.out.print(blobs.size() + " " + idCounter);
      lastPage = blobs;
      return blobs;
   }

   /**
    * Read all blobs from the file. The file is in matrix format rather than
    * coordinate pair format
    */
   public static Vector<blob> getMatrixBlobs(final String filename) throws FileNotFoundException, IOException {
      try {

         final Vector<blob> blobs = new Vector();
         // if(lastPage!=null)
         // return lastPage;
         blob thisOne = null;
         int maxX = 0;
         int maxY = 0;
         String buff;
         // BufferedImage img=imageHelpers.readAsBufferedImage("/usr/blankimg.jpg");
         final String fileName = filename.split("/")[filename.split("/").length - 1];
         // img=imageHelpers.scale(img,2500);
         int idCounter = 0;

         final String[] stuff = blob.readFileIntoArray(filename); // p.split(readFile(filename.replace(".txt.txt",
                                                                  // ".txt")));
         int[][] matrix = null;

         for (int i = 0; i < stuff.length; i++) {
            buff = stuff[i];

            if (buff.contains("b")) {
               if (thisOne != null) {

                  thisOne.arrayVersion = thisOne.pixels.toArray(new pixel[thisOne.pixels.size()]);
                  thisOne.width = matrix.length;
                  thisOne.height = matrix[0].length;
                  thisOne.matrixVersion = new matrixBlob(matrix, thisOne.size);
                  // thisOne.blockVersion=new BlockBlob(thisOne.matrixVersion);
                  // thisOne.matrixVersion.drawBlob(img, thisOne.x, thisOne.y, 0xff0000);
                  thisOne.altVersion = new altBlob(thisOne);
                  blobs.add(thisOne);
                  thisOne.id = blobs.size() - 1;

                  idCounter++;
                  maxX = 0;
                  maxY = 0;
               }
               thisOne = new blob();
               try {
                  final int startx = buff.indexOf("<sx>") + 4;
                  final int endx = buff.indexOf("</sx>");
                  final int starty = buff.indexOf("<sy>") + 4;
                  final int endy = buff.indexOf("</sy>");
                  final int x = Integer.parseInt(buff.substring(startx, endx));

                  final int y = Integer.parseInt(buff.substring(starty, endy));
                  thisOne.x = x;
                  thisOne.y = y;

               } catch (final StringIndexOutOfBoundsException e) {
                  final int k = 0;
               }
               // now read the next line, which contains the matrix dimensions
               i++;
               buff = stuff[i];
               final int startx = buff.indexOf("<szx>") + 5;
               final int endx = buff.indexOf("</szx>");
               final int starty = buff.indexOf("<szy>") + 5;
               final int endy = buff.indexOf("</szy>");
               final int startid = buff.indexOf("<id>") + 4;
               final int endid = buff.indexOf("</id>");
               final int id = Integer.parseInt(buff.substring(startid, endid));
               final int sizeX = Integer.parseInt(buff.substring(startx, endx));
               thisOne.id = id;
               final int sizeY = Integer.parseInt(buff.substring(starty, endy));
               matrix = new int[sizeY][sizeX];
               i++;
               buff = stuff[i];
               final String[] parts = comma.split(buff);
               for (int ctr = 0; ctr < parts.length; ctr++) {
                  if (parts[ctr].compareTo("1") == 0) {
                     // thisOne.pixels.add(new pixel(ctr/sizeY,ctr-ctr/sizeY));
                     thisOne.size++;
                     matrix[(ctr % sizeY)][ctr / sizeY] = 1;
                  }
               }

            }

         }
         // System.out.print(blobs.size() + " " + idCounter);
         // imageHelpers.writeImage(img, "/usr/blobimgs/"+ fileName +".jpg");
         lastPage = blobs;
         return blobs;
      } catch (final Exception e) {
         e.printStackTrace();
      }
      return null;

   }

   /**
    * Quick method for reading a file in and spliting by line
    */
   public static String[] readFileIntoArray(final String file) {
      String[] toret = null;

      final Vector<String> v = new Vector();
      try {
         final BufferedReader b = new BufferedReader(new FileReader(new File(file)));
         while (b.ready()) {
            v.add(b.readLine());
         }
      } catch (final IOException ex) {
         Logger.getLogger(blob.class.getName()).log(Level.SEVERE, null, ex);
      }
      toret = new String[v.size()];
      for (int i = 0; i < toret.length; i++) {
         toret[i] = v.get(i);
      }
      return toret;
   }

   /**
    * Faster way to read the entire data file in 1 go, courtesy of stackoverflow
    */
   private static String readFile(final String path) throws IOException {
      final FileInputStream stream = new FileInputStream(new File(path));
      try {
         final FileChannel fc = stream.getChannel();
         final MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
         /* Instead of using default, pass in a decoder. */
         return Charset.defaultCharset().decode(bb).toString();
      } finally {
         stream.close();
      }
   }

   /**
    * @Depricated This is left over fromt he time when we ran this as a distributed
    *             computing project
    */
   public static void downloadFile(final String urlString, final String destination)
         throws MalformedURLException, IOException {
      final URL url = new URL(urlString);
      url.openConnection();
      final URLConnection conn = url.openConnection();

        try ( // Read all the text returned by the server
                InputStream in = conn.getInputStream()) {
            final File dest = new File(destination);
      if (dest.exists()) {
         dest.delete();
      }
      final BufferedOutputStream tmpOut = new BufferedOutputStream(new FileOutputStream(destination));
      final int total = conn.getContentLength() / 1000000;
      int done = 0;
      int doneMB = -1;
      final byte[] bytes = new byte[4096];

      while (true) {
         final int len = in.read(bytes);
         if (len == -1) {
            break;
         }
         tmpOut.write(bytes, 0, len);
         done += len;

         if (doneMB < done / 1000000) {
            doneMB = done / 1000000;

            System.out.print("" + doneMB + "mb / " + total + "\n");
         }

         tmpOut.flush();
      }
        }
   }

   /**
    * @Depricated This is left over fromt he time when we ran this as a distributed
    *             computing project
    */
   public static void unzipFile(final String fileLoc, final String dataPath) throws FileNotFoundException, IOException {
      final int BUFFER = 4096;
      BufferedOutputStream dest = null;
      final FileInputStream fis = new FileInputStream(fileLoc);
      final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
         System.out.println("Extracting: " + entry);
         int count;
         final byte data[] = new byte[BUFFER];
         // write the files to the disk
         final FileOutputStream fos = new FileOutputStream(dataPath + entry.getName());
         dest = new BufferedOutputStream(fos, BUFFER);
         while ((count = zis.read(data, 0, BUFFER))
                 != -1) {
            dest.write(data, 0, count);
         }
         dest.flush();
         dest.close();
      }
      zis.close();
   }

   public int getWidth() {

      return width;
   }

   public int getHeight() {

      return height;
   }
   static Vector<blob> lastPage;
}
