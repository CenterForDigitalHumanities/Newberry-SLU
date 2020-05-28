/*
 * @author Jon Deering
Copyright 2011 Saint Louis University. Licensed under the Educational Community License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License.

You may obtain a copy of the License at http://www.osedu.org/licenses/ECL-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
and limitations under the License.
 */

/**Class to represent 1 of any number of available images which are part of a single image annotation.*/
package dmstech;
public class ImageChoice {
    
    public int getHeight() {
        return height;
    }
    
    public String getImageURL() {
        return imageURL;
    }
    
    public int getWidth() {
        return width;
    }
    
    
    private final int height;
    private final int width;
    private final String imageURL;

    public ImageChoice(final String imageURL, final int w, final int h)
    {
        this.height=h;
        this.width=w;
        this.imageURL=imageURL;
        
    }
    
}
