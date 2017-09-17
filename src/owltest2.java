import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.WS4J;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import javafx.util.Pair;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import edu.smu.tspell.wordnet.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by Sharthak Ghosh on 6/14/2016.
 */
public class owltest2 {
    private static WordNetUtils wordnetUtils = new WordNetUtils();                          //Helper class with all wordnet and WS4J utils
    private static OwlApiUtils owlApiUtils = new OwlApiUtils();

    public static void main(String[] args) throws Exception {
        System.setProperty("wordnet.database.dir", "C:\\Program Files (x86)\\WordNet\\2.1\\dict");

        //Setting up ontology to be compared
        OWLOntology ontologySample = owlApiUtils.loadFromFile(new File("C:\\OntologyCompare.owl"));
        OWLDataFactory dfSample = OWLManager.getOWLDataFactory();
        OWLOntologyManager mgrSample = OWLManager.createOWLOntologyManager();

        //Setting up the first ontology
        OWLOntology ontologySample1 = owlApiUtils.loadFromFile(new File("C:\\Ontology.owl"));
        OWLDataFactory df1 = OWLManager.getOWLDataFactory();
        OWLOntologyManager mgr1 = OWLManager.createOWLOntologyManager();
        IRI iri1 = IRI.create(new File("C:\\Onotlogy.owl"));

        //Setting up the second ontology
        OWLOntology ontologySample2 = owlApiUtils.loadFromFile(new File("C:\\Ontology-2.owl"));
        OWLDataFactory df2 = OWLManager.getOWLDataFactory();
        OWLOntologyManager mgr2 = OWLManager.createOWLOntologyManager();
        IRI iri2 = IRI.create(new File("C:\\Ontology-2.owl"));

        //Setting up reasoner for first ontology
        OWLReasonerFactory owlReasonerFactory1 = new StructuralReasonerFactory();
        OWLReasoner owlReasoner1 = owlReasonerFactory1.createReasoner(ontologySample1);
        owlReasoner1.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        //Setting up reasoner for second ontology
        OWLReasonerFactory owlReasonerFactory2 = new StructuralReasonerFactory();
        OWLReasoner owlReasoner2 = owlReasonerFactory2.createReasoner(ontologySample2);
        owlReasoner2.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        /*Getting all class names from both the ontologies
          Using a helper method present in class OwlApiUtils to get all class names
        */
        System.out.println("Classes in Ontology to be compared are");
        owlApiUtils.displayClassesInOntology(ontologySample);
        System.out.println();

        /*Using helper method from OwlApiUtils*/
        System.out.println("Classes in ontology 1");
        owlApiUtils.displayClassesInOntology(ontologySample1);
        System.out.println();

        System.out.println("Classes in ontology 2");
        owlApiUtils.displayClassesInOntology(ontologySample2);
        System.out.println();

        /*Displaying individuals or objects using helper method*/
        System.out.println("Individuals in ontology 1");
        owlApiUtils.displayNamedIndividuals(ontologySample1);
        System.out.println();

        System.out.println("Individuals in ontology 2");
        owlApiUtils.displayNamedIndividuals(ontologySample2);

        System.out.println();

        /*Displaying the word and the number of occurrences in the
        * ontologies specified as parameters*/
        HashMap<String, List<Integer>> matrix = owlApiUtils.createIncidenceMatrix(ontologySample1, ontologySample2);
        for(String i : matrix.keySet())
        {
            System.out.print(i + " ");
            for(Integer j : matrix.get(i))
                System.out.print(j + " ");
            System.out.println();
        }
        System.out.println();

        /*Creating incidence matrix for all the ontologies loaded
        * using the utility method present in the class OwlApiUtils*/
        HashMap<String, List<Integer>> allWords = owlApiUtils
                .createIncidenceMatrix(ontologySample, ontologySample1, ontologySample2);

        /*Calculating the tf_idf matrix using the above
        * using the method present in the class OwlApiUtils*/
        HashMap<String, Double> tf_idf = owlApiUtils
                .create_TF_IDF_fromOntologies(ontologySample, ontologySample1, ontologySample2);

        /*Gets the unique word with a list containing key value pairs
        * denoting the integer as the ontology id and double as the score*/
        HashMap<String, List<Pair<Integer, Double>>> termDocMat = owlApiUtils.getTermDocumentMatrix(tf_idf, allWords);


        for(String i : termDocMat.keySet())
        {
            System.out.print(i + " ");
            List<Pair<Integer, Double>> key = termDocMat.get(i);
            for(Pair<Integer, Double> keyVals : key)
            {
                System.out.print(keyVals.getKey() + " " + keyVals.getValue() + ",");
            }
            System.out.println();
        }

        double ontologySampleLength = owlApiUtils.calcLengthOfOntology(ontologySample);
        double ontologyLength1 = owlApiUtils.calcLengthOfOntology(ontologySample1);
        double ontologyLength2 = owlApiUtils.calcLengthOfOntology(ontologySample2);

        System.out.println("Length of sample ontology " + ontologySampleLength);
        System.out.println("Length of ontology 1 " + ontologyLength1);
        System.out.println("Length of ontology 2 " + ontologyLength2);

        List<Double> similarityIndex = owlApiUtils.calculateCosineSimilarity(termDocMat);

        double[] values = new double[similarityIndex.size()];
        int ctr = 0;
        for(Double value : similarityIndex)
        {
            values[ctr++] = value;
        }
        System.out.println("Ranking " + (double)(values[0] / (ontologySampleLength * ontologyLength1)));
        System.out.println("Ranking " + (double)(values[1] / (ontologySampleLength * ontologyLength2)));

        wordnetUtils.classSimilarity(ontologySample, ontologySample1);
        wordnetUtils.classSimilarity(ontologySample, ontologySample2);
    }
}
