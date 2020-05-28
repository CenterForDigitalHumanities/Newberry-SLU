
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

import java.io.File;


public class PaleoRunner
{
    public static void main(final String[] args) {
        // process a folder full of images, generating data files in the specified data
        // folder, using the same file structure as we found in the images folder
        if (args[0].compareTo("processImages") == 0) {
            final String imagePath = args[1];
            final File imageFolder = new File(imagePath);
            final File[] dirs = imageFolder.listFiles();
            for (final File dir : dirs)
            {
                //process this dir
                
            }
        }
        if(args[0].compareTo("compare")==0)
        {
            
        }
        if(args[0].compareTo("crosscompare")==0)
        {
            
        }
    }
}
