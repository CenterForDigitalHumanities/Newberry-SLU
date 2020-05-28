/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package OAC;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 *
 * A canvas from the Stanford opencanvas data model. Represents a single displayable canvas where a page or a bunch of page fragments might reside.
 */
public class view
{
    String[] images;
    Model theGraph;

    public view(final Model m, final String uri) throws MalformedURLException, IOException {
        theGraph = m;
        theGraph.setNsPrefix("dms", "http://dms.stanford.edu/ns/");
        theGraph.setNsPrefix("oac", "http://www.openannotation.org/ns/");
        theGraph.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        theGraph.setNsPrefix("ore", "http://www.openarchives.org/ore/terms/");
        theGraph.setNsPrefix("cnt", "http://www.w3.org/2008/content#");
        theGraph.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
        theGraph.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        new URL(uri);

        // read the RDF/XML file

        // if it were turtle model.read(in,null, "N3");
        final Property transcriptionProperty = theGraph.createProperty("http://www.openannotation.org/ns/",
                "imageAnnotation");
        final ResIterator r = theGraph.listResourcesWithProperty(transcriptionProperty);
        while (r.hasNext()) {
            final Resource theImage = r.next();
            final String imageurl = theImage.getURI();
            final String[] tmp = new String[images.length + 1];
            for(int i=0;i<images.length;i++)
            {
                tmp[i]=images[i];
            }
            tmp[tmp.length-1]=imageurl;
            images=tmp;
        }

        }
    /**Return associated image uris*/
    public String[] getImages()
    {
        return images;
    }


}
