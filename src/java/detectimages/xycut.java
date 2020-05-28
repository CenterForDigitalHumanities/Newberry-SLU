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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Vector;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class attempts to implement the xycut line segmentation algorithm. It is currently not used.
 */
public class xycut {

   public xycut(final int[][] imageMap) {
   }

   public int tnx = 78;
   public int tny = 32;
   public int tcx = 35;
   public int tcy = 54;
   public static int zero = 0xff000000;
   public static int one = 0xffffffff;

   /**
    * Compute the integral for this image
    */
   public void buildIntegralImage(final int[][] img, final int[][] int_img_hor, final int[][] int_img_ver) {
      for (int x = 0; x < int_img_ver.length; x++) {
         for (int y = 0; y < int_img_ver[0].length; y++) {
            int_img_ver[x][y] = 0;
            int_img_hor[x][y] = 0;
         }
      }
      for (int x = 0; x < int_img_ver.length; x++) {
         int count = 0;
         for (int y = 0; y < int_img_ver[0].length; y++) {
            if (img[x][y] == 0) {
               count++;
            }
            int_img_ver[x][y] = count;
         }
      }

      for (int y = 0; y < int_img_hor[0].length; y++) {
         int count = 0;
         for (int x = 0; x < int_img_hor.length; x++) {
            if (img[x][y] == 0) {
               count++;
            }
            int_img_hor[x][y] = count;
         }
      }

   }

   public class rectangle {

      public int x;
      public int y;
      // public int h;
      // public int w;
      public int x1;
      public int y1;

      public rectangle() {
         x = -1;
         y = -1;
         x1 = -1;
         y1 = -1;

         // h=-1;
         // w=-1;
      }

      public rectangle(final int x, final int y, final int x1, final int y1) {
         this.x = x;
         this.y = y;
         this.y1 = y1;
         this.x1 = x1;

      }

      public int height() {
         return y1 - y;
      }

      public int width() {
         return x1 - x;
      }
   }

   public void get_projection_profiles(final int[] proj_on_yaxis, final int[] proj_on_xaxis, final rectangle r,
         final int[][] hor, final int[][] ver) {

      Arrays.fill(proj_on_yaxis, 0);
      Arrays.fill(proj_on_xaxis, 0);

      for (int y = (int) r.y; y < (int) r.y1; y++) {
         int m = 0;
         try {
            if (r.x - 1 >= 0) {
               m = hor[(int) r.x - 1][y];
            }
         } catch (final Exception e) {
            e.printStackTrace();
         }
         try {
            proj_on_yaxis[y - (int) r.y] = hor[(int) r.x1 - 1][y] - m;
         } catch (final Exception e) {
            e.printStackTrace();
         }
      }
      for (int x = (int) r.x; x < (int) r.x1; x++) {
         int m = 0;
         if (r.y - 1 >= 0) {
            m = ver[x][(int) r.y - 1];
         }
         try {
            proj_on_xaxis[x - r.x] = ver[x][(int) r.y1 - 1] - m;
         } catch (final Exception e) {
            e.printStackTrace();
         }
      }

   }

   public void shrink_and_clean(final rectangle r, final int[] proj_on_yaxis, final int[] proj_on_xaxis,
         final double f_tnx, final double f_tny) {

      int xbegin = 0, xend = proj_on_xaxis.length - 1;
      while (xbegin < proj_on_xaxis.length && proj_on_xaxis[xbegin] <= 0) {
         xbegin++;
      }
      while (xend >= 0 && proj_on_xaxis[xend] <= 0) {
         xend--;
      }
      int ybegin = 0, yend = proj_on_yaxis.length - 1;
      while (ybegin < proj_on_yaxis.length && proj_on_yaxis[ybegin] <= 0) {
         ybegin++;
      }
      while (yend >= 0 && proj_on_yaxis[yend] <= 0) {
         yend--;
      }

      final int old_x0 = (int) r.x;
      final int old_y0 = (int) r.y;
      r.x = old_x0 + xbegin;
      r.x1 = old_x0 + xend + 1;
      r.y = old_y0 + ybegin;
      r.y1 = old_y0 + yend + 1;

      // Mao. Fig.3. 2.b) & 2.c)
      // cleaning
      for (int i = 0; i < proj_on_yaxis.length; i++) {
         proj_on_yaxis[i] = proj_on_yaxis[i] - (int) (r.width() * f_tny);
      }
      for (int i = 0; i < proj_on_xaxis.length; i++) {
         proj_on_xaxis[i] = proj_on_xaxis[i] - (int) (r.height() * f_tnx);
      }

   }

   public int[] get_widest_gap(int cut_pos_y, int gap_y, int cut_pos_x, int gap_x, final int[] proj_on_yaxis,
         final int[] proj_on_xaxis) {
      int gap_hor = -1, gap_ver = -1, pos_hor = -1, pos_ver = -1;
      int begin = -1;
      int end = -1;
      // find gap in y-axis projection
      for (int i = 1; i < proj_on_yaxis.length; i++) {
         if (begin >= 0 && proj_on_yaxis[i - 1] <= 0 && proj_on_yaxis[i] > 0) {
            end = i;
         }
         if (proj_on_yaxis[i - 1] > 0 && proj_on_yaxis[i] <= 0) {
            begin = i;
         }
         if (begin > 0 && end > 0 && end - begin > gap_hor) {
            gap_hor = end - begin;
            pos_hor = (end + begin) / 2;
            begin = -1;
            end = -1;
         }
      }

      begin = -1;
      end = -1;
      // find gap in x-axis projection
      for (int i = 1; i < proj_on_xaxis.length; i++) {
         if (begin >= 0 && proj_on_xaxis[i - 1] <= 0 && proj_on_xaxis[i] > 0) {
            end = i;
         }
         if (proj_on_xaxis[i - 1] > 0 && proj_on_xaxis[i] <= 0) {
            begin = i;
         }
         if (begin > 0 && end > 0 && end - begin > gap_ver) {
            gap_ver = end - begin;
            pos_ver = (end + begin) / 2;
            begin = -1;
            end = -1;
         }
      }

      cut_pos_y = pos_hor;
      gap_y = gap_hor;

      cut_pos_x = pos_ver;
      gap_x = gap_ver;
      final int[] toret = new int[4];
      toret[0] = gap_x;
      toret[1] = gap_y;
      toret[2] = cut_pos_x;
      toret[3] = cut_pos_y;
      return toret;

   }

   public enum cut_type {

      HORIZONTAL_CUT, VERTICAL_CUT
   };

   void split_rect(final rectangle r1, final rectangle r2, final rectangle r, final cut_type dir, final int pos) {
      if (dir == xycut.cut_type.HORIZONTAL_CUT) { // horizontal cut
         r1.x = r.x;
         r1.y = r.y;
         r1.x1 = r.x1;
         r1.y1 = pos; // lower rectangle
         r2.x = r.x;
         r2.y = pos;
         r2.x1 = r.x1;
         r2.y1 = r.y1; // upper rectangle
      }
      if (dir == xycut.cut_type.VERTICAL_CUT) { // vertical cut
         r1.x = r.x;
         r1.y = r.y;
         r1.x1 = pos;
         r1.y1 = r.y1;
         r2.x = pos;
         r2.y = r.y;
         r2.x1 = r.x1;
         r2.y1 = r.y1; // rigth rectangle
      }

      /*
       * if(dir==cut_type.HORIZONTAL_CUT) {
       * 
       * r1.x=r.x; r1.y=r.y; r1.y1=r.y1; r1.x1=pos-r1.x;
       * 
       * r2.x=r.x; r2.y=pos; r2.y1=r1.y1; r2.x1=r.x1;
       * 
       * } //change from horizontal to vertical if(dir==cut_type.VERTICAL_CUT) {
       * 
       * r1.x=r.x; r1.y=r.y; r1.y1=r.y1; r1.x1=pos;
       * 
       * r2.x=pos; r2.y=r.y; r2.y1=r.y1; r2.x1=r.x1;
       * 
       * }
       */
   }

   /**
    * @Depricated
    */
   void insertRectangleIntoTree(final int index, final rectangle[] tree, final rectangle rR, final rectangle rL) {
      final int left = index * 2 + 1;
      final int right = index * 2 + 2;
      // resize the array if needed
      if ((right + 1) > tree.length) {
         final rectangle[] tmp = new rectangle[right + 1];
         for (int i = 0; i < tree.length; i++) {
            tmp[i] = tree[i];
         }

      }
      tree[left] = rL;
      tree[right] = rR;
   }

   void xycut(final Vector<rectangle> blocks, final Vector<rectangle> tree, final int[][] imageMap, final int tnx,
         final int tny, final int tcx, final int tcy, final int[][] hor, final int[][] ver) {
      int[] proj_on_yaxis = null;
      int[] proj_on_xaxis = null;
      final rectangle page = new rectangle();
      page.x1 = imageMap.length - 1; /// set to image height
      page.y = 0;
      page.y1 = imageMap[0].length - 1; // set to image width
      page.x = 0;
      tree.add(page);

      final double factor_tnx = (double) tnx / (double) imageMap.length;
      final double factor_tny = (double) tnx / (double) imageMap[0].length;
      try {
         int i = 0;
         while (i < tree.size()) {
            final rectangle r = tree.get(i);
            if (tree.get(i).x >= 0 && tree.get(i).y >= 0 && tree.get(i).x1 >= 0 && tree.get(i).y1 >= 0) {
               // compute projection profile
               // System.out.print("getting profiles\n");
               proj_on_yaxis = new int[r.y1 - r.y];
               proj_on_xaxis = new int[r.x1 - r.x];
               this.get_projection_profiles(proj_on_yaxis, proj_on_xaxis, r, hor, ver);
               // save the old coordinates before shrinking
               final int old_x = r.x;
               final int old_y = r.y;
               this.shrink_and_clean(r, proj_on_yaxis, proj_on_xaxis, factor_tnx, factor_tny);
               // r has now been changed
               Integer cut_pos_y = -1;
               Integer cut_pos_x = -1;
               Integer gap_y = -1;
               Integer gap_x = -1;
               // find the widest gap, the middle of the gap, and the cut direction
               final int[] parts = this.get_widest_gap(cut_pos_y, gap_y, cut_pos_x, gap_x, proj_on_yaxis,
                     proj_on_xaxis);
               gap_x = parts[0];
               gap_y = parts[1];
               cut_pos_x = parts[2];
               cut_pos_y = parts[3];

               // perform the split
               if (gap_y >= gap_x && gap_y > tcy) {
                  // System.out.print("meth1\n");
                  final rectangle rBottom = new rectangle();
                  final rectangle rTop = new rectangle();
                  this.split_rect(rBottom, rTop, r, cut_type.HORIZONTAL_CUT, old_y + cut_pos_y);
                  // this.insertRectangleIntoTree(i, tree, rBottom, rTop);
                  tree.add(rBottom);
                  tree.add(rTop);
               } else {
                  if (gap_y >= gap_x && gap_y <= tcy && gap_x > tcx) {
                     // System.out.print("meth2\n");
                     final rectangle rLeft = new rectangle();
                     final rectangle rRight = new rectangle();
                     this.split_rect(rLeft, rRight, r, cut_type.VERTICAL_CUT, old_x + cut_pos_x);
                     // this.insertRectangleIntoTree(i, tree, rLeft, rRight);
                     tree.add(rLeft);
                     tree.add(rRight);
                  } else {
                     if (gap_x > gap_y && gap_x > tcx) {
                        // System.out.print("meth3\n");
                        final rectangle rLeft = new rectangle();
                        final rectangle rRight = new rectangle();
                        this.split_rect(rLeft, rRight, r, cut_type.VERTICAL_CUT, old_x + cut_pos_x);
                        tree.add(rLeft);
                        tree.add(rRight);
                     } else {
                        if (gap_x > gap_y && gap_x <= tcx && gap_y > tcy) {
                           // System.out.print("meth4\n");
                           final rectangle rBottom = new rectangle();
                           final rectangle rTop = new rectangle();
                           this.split_rect(rBottom, rTop, r, cut_type.HORIZONTAL_CUT, old_y + cut_pos_y);
                           // this.insertRectangleIntoTree(i, tree, rBottom, rTop);
                           tree.add(rBottom);
                           tree.add(rTop);
                        } else {
                           // insert dummy rectangles
                           // System.out.print("meth5\n");
                           final rectangle rLeft = new rectangle();
                           final rectangle rRight = new rectangle();
                           // this.insertRectangleIntoTree(i, tree, rLeft, rRight);
                           tree.add(rLeft);
                           tree.add(rRight);
                           final rectangle b = new rectangle();
                           b.x = r.x;
                           b.y = r.y;
                           b.x1 = r.x1;
                           b.y1 = r.y1;
                           blocks.add(b);

                        }
                     }
                  }
               }

            }
            i++;
         }

      } catch (final Exception e) {
         e.printStackTrace();
      }

   }

   public Vector<rectangle> segment(final int[][] imageMap, final BufferedImage img, final String prefix) {
      Boolean toret = false;
      final int[][] hor = new int[imageMap.length][imageMap[0].length];
      final int[][] ver = new int[imageMap.length][imageMap[1].length];
      this.buildIntegralImage(imageMap, hor, ver);
      final Vector<rectangle> blocks = new Vector();

      final Vector<rectangle> tree = new Vector();
      xycut(blocks, tree, imageMap, tnx, tny, tcx, tcy, hor, ver);
      if (tree.size() >= 5) {
         toret = true;
         System.out.print("" + blocks.size() + " blocks and " + tree.size() + " items in tree\n");
         final int cflen = blocks.size();
         for (int i = 0; i < cflen; i++) {

            final int color = 0xff0000;
            final rectangle r = blocks.get(i);
            System.out.print("rectangle x=" + r.x + " y=" + r.y + " x1=" + r.x1 + " y1=" + r.y1 + "\n");
            for (int x = r.x; x < (r.x1); x++) {
               img.setRGB(x, r.y, color);
               img.setRGB(x, r.y1, color);
            }
            for (int y = r.y; y < (r.y1); y++) {
               img.setRGB(r.x, y, color);
               img.setRGB(r.x1, y, color);
            }
            // ImageHelpers.writeImage(img, "/usr/web/" + prefix + "out" + i + ".jpg");
            // else
            //
            // else
            // System.out.print(img.getRGB(x, y)+"\n");
         }
      }

      return blocks;
   }

   public static Vector<rectangle> segment(final BufferedImage binarizedImage) throws IOException {
      final int[][] imageMap = new int[binarizedImage.getWidth()][binarizedImage.getHeight()];
      for (int i = 0; i < binarizedImage.getWidth(); i++) {
         for (int j = 0; j < binarizedImage.getHeight(); j++) {

            // System.out.print((Integer.toHexString(img.getRGB(i, j))+"\n"));

            if (binarizedImage.getRGB(i, j) == zero) {
               imageMap[i][j] = 0;

            } else {

               imageMap[i][j] = 255;
            }
         }
      }

      ImageIO.write(binarizedImage, "jpg", new File("/usr/debugImages/inner" + System.currentTimeMillis() + ".jpg"));
      final xycut x = new xycut(imageMap);
      return (x.segment(imageMap, binarizedImage, "prod"));
   }
}
