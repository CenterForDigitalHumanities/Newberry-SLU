/*
 * @author Jon Deering
Copyright 2011 Saint Louis University. Licensed under the Educational Community License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License.

You may obtain a copy of the License at http://www.osedu.org/licenses/ECL-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
and limitations under the License.
 */
package dmstech;

/**
 *
 * Class to represent a dmstech canvas, which can have some images associated with it
 */
public class canvas {
    
    public String getCanvas() {
        return canvas;
    }
    
    public ImageChoice[] getImageURL() {
        return imageURL;
    }
    
    public int getPosition() {
        return position;
    }
    
    public String getTitle() {
        return title;
    }
    
    private final String canvas;
    private final String title;
    private ImageChoice[] imageURL;
    private int position;
    private final int height;
    private final int width;

    public void setImageURL(final ImageChoice[] c) {
        this.imageURL = c;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public canvas(final String canvas, final String title, final ImageChoice[] img, final int position) {
        this.canvas = canvas;
        this.title = title;
        this.imageURL = img;
        this.position = position;
        height = 0;
        width = 0;
    }

    public canvas(final String canvas, final String title, final ImageChoice[] img, final int position, final int width,
            final int height) {
        this.canvas = canvas;
        this.title = title;
        this.imageURL = img;
        this.position = position;
        this.width = width;
        this.height = height;
    }

    public void setPosition(final int position) {
        this.position = position;
    }
    
}
