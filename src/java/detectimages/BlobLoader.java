/*
 * @author Jon Deering
Copyright 2011 Saint Louis University. Licensed under the Educational Community License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License.

You may obtain a copy of the License at http://www.osedu.org/licenses/ECL-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
and limitations under the License.
 */
package detectimages;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlobLoader implements Callable{
    private final blobManager bm;
    String blobFile;

    public BlobLoader(final blobManager bm, final String blobFile) {
        this.bm = bm;
        this.blobFile = blobFile;

    }

    @Override
    public Object call() {
        try {
            // System.out.print("caching "+this.blobFile+"\n");
            bm.add(blob.getMatrixBlobs(blobFile), blobFile);
        } catch (final FileNotFoundException ex) {
            Logger.getLogger(BlobLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (final Exception ex) {
            Logger.getLogger(BlobLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
}
