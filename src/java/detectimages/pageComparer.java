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
 */package detectimages;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jdeerin1
 */
public class pageComparer implements Callable
{
    Vector <blob>blobs;
    Vector <blob>blobs2;
    String [] assignment;
    String file2;
    blobManager manager;
    private String outputLocation="results/overload/";
    public pageComparer(final Vector<blob> blobs, final String file2, final String[] assignment,
            final blobManager manager) {
        this.file2 = file2;
        this.blobs = blobs; // rememeber the name
        this.manager = manager;
        this.assignment = assignment;

    }

    public void setOutputLocation(final String n) {
        this.outputLocation = n;
    }

    public Object call() {

        if (file2 != null)
            try {
                blobs2 = manager.get(file2);
                if (blobs2 == null) {
                    manager.add(blob.getBlobs(file2), file2);
                    blobs2 = manager.get(file2);

                }

            } catch (final IOException ex) {
            }
        final blob[] b1 = new blob[blobs.size()];

        for (int i = 0; i < b1.length; i++) {
            b1[i] = blobs.get(i);

        }
        final blob[] b2 = new blob[blobs2.size()];

        for (int i = 0; i < b2.length; i++) {
            b2[i] = blobs2.get(i);

        }
        try (final FileWriter w = (new FileWriter(this.outputLocation + assignment[0] + " " + ".txt", true))) {
            int matches = 0;
            final int matches2 = 0;
            final StringBuilder res = new StringBuilder("");
            for (blob b11 : b1) {
                for (blob b21 : b2) {
                    int biggest;
                    if (b11.size > b21.size) {
                        biggest = b11.size;
                    } else {
                        biggest = b21.size;
                    }
                    if (b11.size > 25 && b21.size > 25 && (Math.abs(b11.size - b21.size) < biggest * .3)) {
                        // blobComparer b = new blobComparer(b1[i], b2[j]);
                        // double tmp = b.run();
                        // altBlob a=b1[i].altVersion;
                        // altBlob b=b2[j].altVersion;

                        // double tmp=b1[i].altVersion.run(b2[j].altVersion);
                        double tmp;
                        final double tmp2;
                        if (true || (b11.matrixVersion.matrix.length - b21.matrixVersion.matrix.length > 3 || b21.matrixVersion.matrix.length - b11.matrixVersion.matrix.length > 3)) {
                            if (b11.matrixVersion.matrix.length < b21.matrixVersion.matrix.length) {
                                // tmp=b1[i].matrixVersion.compareWithScaling(b2[j].matrixVersion);
                                // tmp=b1[i].matrixVersion.compareToWithAdjust(b2[j].matrixVersion.scaleMatrixBlob(b2[j],b1[i].matrixVersion.matrix.length));
                                tmp = b11.matrixVersion.compareToWithAdjust(b21.matrixVersion);
                            } else {
                                // tmp=b1[i].matrixVersion.compareWithScaling(b2[j].matrixVersion);
                                // tmp=b1[i].matrixVersion.scaleMatrixBlob(b1[i],b2[j].matrixVersion.matrix.length).compareToWithAdjust(b2[j].matrixVersion);
                                tmp = b11.matrixVersion.compareToWithAdjust(b21.matrixVersion);
                            }
                            {
                            }
                        }
                        // tmp=0.0;
                        // tmp=b1[i].matrixVersion.compareWithScaling(b2[j].matrixVersion);
                        // tmp=b1[i].matrixVersion.compareToWithAdjust(b2[j].matrixVersion); //
                        // tmp2=b1[i].altVersion.run(b2[j].altVersion);
                        // tmp2=(double) tmp2 /biggest;
                        // tmp=0.0;
                        // tmp=b1[i].matrixVersion.compareWithScaling(b2[j].matrixVersion);
                        // tmp=b1[i].matrixVersion.compareToWithAdjust(b2[j].matrixVersion); //
                        // tmp2=b1[i].altVersion.run(b2[j].altVersion);
                        // tmp2=(double) tmp2 /biggest;
                        // else
                        {
                        // tmp=0.0;
                        // tmp=b1[i].matrixVersion.compareWithScaling(b2[j].matrixVersion);
                        // tmp=b1[i].matrixVersion.compareToWithAdjust(b2[j].matrixVersion); //
                        // tmp2=b1[i].altVersion.run(b2[j].altVersion);
                        
                        // tmp2=(double) tmp2 /biggest;
                        
                    }   // tmp=(double) tmp / biggest;
                        // tmp=tmp2;
                        // tmp=tmp2;
                        if (tmp > 0.7) {
                            matches++;
                            try {
                                // b1[i].matrixVersion.writeGlyph(assignment[0]+assignment[1]+i+j+"a",
                                // b2[j].matrixVersion);
                                // System.out.print("tmp:"+tmp+"\n");
                            } catch (final Exception e) {
                            }
                            // imageHelpers.writeImage(toret,
                            // "/usr/glyphs/"+assignment[0]+assignment[1]+i+j+".jpg");
                            // blob.writeMatchResults(b1[i].id, b2[j].id, assignment, w);
                            w.flush();
                            // String a="\"insert into blobs(img1, blob1,img2,blob2) values
                            // ('"+assignment[0] + "','" + b1[i].id + "','" + assignment[1] + "','" +
                            // b2[j].id + "');\n";
                            // String a="\""+assignment[0] + "\",\"" + b1[i].id + "\",\"" + assignment[1] +
                            // "\",\"" + b2[j].id + "\"\n";
                            final int tmpInt = (int) (tmp * 100);
                            final String a = assignment[0] + ":" + b11.id + ";" + assignment[1] + ":" + b21.id + "/" + tmpInt + "\n";
                            res.append(a);
                            // blob.writeMatchResults(i, j, assignment, w);
                        } else {
                            // System.out.print("tmp:"+tmp+"\n");
                        }
                        /*
                         * if(tmp>0.7 && tmp2<0.7)
                         * System.out.print("old val "+tmp+" new val "+tmp2+"\n"); if(tmp<=0.7 &&
                         * tmp2>0.7) System.out.print("foundnew old val "+tmp+" new val "+tmp2+"\n");
                         */
                    }
                }
                // if the overlap of the 2 images is more than 70%, it is a good match
                // if the overlap of the 2 images is more than 70%, it is a good match
            }
            blob.writeMatchResults(res.toString(), w);
            w.flush();
            System.out.print(assignment[0] + ":" + assignment[1] + ":" + matches + "\n");
            return assignment[0] + ":" + assignment[1] + ":" + matches + "\n";
        } catch (final IOException ex) {
            System.out.print("caught error\n");
            Logger.getLogger(pageComparer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
