import com.fasterxml.jackson.databind.deser.DataFormatReaders;
import javafx.util.Pair;
import org.openrdf.model.impl.IntegerLiteralImpl;
import org.openrdf.model.vocabulary.OWL;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.metrics.DoubleValuedMetric;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.HashCode;

import java.io.File;
import java.util.*;

/**
 * Created by Sharthak Ghosh on 7/1/2016.
 */
public class OwlApiUtils {
    private OWLOntologyManager mgr =
            OWLManager.createOWLOntologyManager();
    private HashMap<String, Double> global_tf_idf_values = new HashMap<String, Double>();
    private int noOfOntologies = 0;             //Increments everytime when an ontology is loaded in memory

    //Loads an ontology from file name given and returns an ontology

    public OWLOntology loadFromFile(File file) throws Exception
    {
        noOfOntologies++;
        return (mgr.loadOntologyFromOntologyDocument(file));
    }

    //List of string of all classes present in the ontology

    public Set<String> getClassesInOntology(OWLOntology ontology)
    {
        Set<String> stringList = new HashSet<String>();

        for(OWLClass cls : ontology.getClassesInSignature())
        {
            String clsString = cls
                    .toString()
                    .substring(1, cls.toString().length() - 1);

            String[] clsSplitString =
                    clsString.split("#");

            stringList.add(clsSplitString[1]);
        }
        return stringList;
    }

    //Gets all list of strings of instances of the classes in an ontology

    public Set<String> getOWLNamedIndividuals(OWLOntology ontology)
    {
        //Setting reasoner to fetch all instances of a class

        OWLReasonerFactory owlReasonerFactory =
                new StructuralReasonerFactory();
        OWLReasoner owlReasoner = owlReasonerFactory
                .createReasoner(ontology);
        owlReasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        Set<String> setOfIndividuals = new HashSet<String>();
        for(OWLClass cls : ontology.getClassesInSignature())
        {
            Set<OWLNamedIndividual> individuals = owlReasoner
                    .getInstances(cls, true)
                    .getFlattened();

            for(OWLNamedIndividual individual : individuals)
                setOfIndividuals.add(individual.getIRI().getFragment());
        }
        return setOfIndividuals;
    }

    public void displayClassesInOntology(OWLOntology ontology)
    {
        Set<String> classesList = new HashSet<String>();
        /*Get all the class names*/
        classesList = getClassesInOntology(ontology);
        for(String i : classesList)
            System.out.println(i);
    }

    /*Displays all the individuals or the instances from the ontology specified*/
    public void displayNamedIndividuals(OWLOntology ontology)
    {
        Set<String> individuals = getOWLNamedIndividuals(ontology);
        for(String i : individuals)
            System.out.println(i);
    }

    /*Returns a set containg the number of distinct words from all the ontologies
    * passed in the parameter*/
    public Set<String> getBagOfWordsFromClasses(OWLOntology... ontologies)
    {
        Set<String> allClasses = new HashSet<String>();
        for(int i = 0; i < ontologies.length; i++)
        {
            Set<String> classes = getClassesInOntology(ontologies[i]);
            for(String j : classes)
                allClasses.add(j);
        }
        return allClasses;
    }

    /*A utility method to convert from a set to list*/
    public List<String> convertToList(Set<String> bagOfWords)
    {
        List<String> listBagOfWords = new ArrayList(bagOfWords);
        Collections.sort(listBagOfWords);
        return listBagOfWords;
    }

    /*Get any number of ontologies as parameters
    * Loop through all the ontologies and add to the hashmap
    * The HashMap contains the word and the ontology id, which specifies
    * the occurrence of each word in the particular ontology specified by the integer*/
    public HashMap<String, List<Integer>> createIncidenceMatrix(OWLOntology... ontologies)
    {
        HashMap<String, List<Integer>> allWords = new HashMap<String, List<Integer>>();
        for(int i = 0; i < ontologies.length; i++)
        {
            Set<String> classesInOntology = getClassesInOntology(ontologies[i]);
            for(String j : classesInOntology)
            {
                if(allWords.get(j) == null)
                    allWords.put(j, new ArrayList<Integer>());
                allWords.get(j).add(i);
            }
        }
        return allWords;
    }

    /*Generate tf_idf values, using the incidence matrix
    * Returns distinct words and idf values for each word*/
    public HashMap<String, Double> create_TF_IDF_fromOntologies(OWLOntology... ontologies)
    {
        HashMap<String, List<Integer>> allWords =
                createIncidenceMatrix(ontologies);
        HashMap<String, Double> tf_idf =
                new HashMap<String, Double>();

        for(String i : allWords.keySet())
        {
            int count = allWords.get(i).size();
            if(tf_idf.get(i) == null)
                tf_idf.put(i, Math.log((new Double(ontologies.length)) / count)/ Math.log(2));
        }

        global_tf_idf_values = tf_idf;
        return tf_idf;
    }

    /*Returns list of strings with pairs containing
    * integer as ontology id and double as score*/
    public HashMap<String, List<Pair<Integer, Double>>> getTermDocumentMatrix
            (HashMap<String, Double> tf_idf_values, HashMap<String, List<Integer>> allWords)
    {
        HashMap<String, List<Pair<Integer, Double>>> matrix =
                new HashMap<String, List<Pair<Integer, Double>>>();

        for(String key : tf_idf_values.keySet())
        {
            List<Integer> documents = allWords.get(key);
            for(Integer docID : documents)
            {
                if(matrix.get(key) == null)
                    matrix.put(key, new ArrayList<Pair<Integer, Double>>());
                matrix.get(key).add(new Pair<Integer, Double>(docID, tf_idf_values.get(key)));
            }
        }
        return matrix;
    }

    /*Calculating the length from the ontology given
    * using the global tf_idf values whcih contains all the values*/
    public double calcLengthOfOntology(OWLOntology ontology)
    {
        double length = 0;
        Set<String> classes = getClassesInOntology(ontology);
        for(String cls : classes)
        {
            if(global_tf_idf_values.get(cls) != null)
            {
                length = length + Math.pow(global_tf_idf_values.get(cls), 2);
            }
        }
        return Math.sqrt(length);
    }

    /*Using the formula of cosine similarity, calculating the score
    * Returns the final score of the comparison as a list
    * Ontology to be compared always contains ID = 0
    * Rest of the ontologies start from 1*/
    public List<Double> calculateCosineSimilarity(HashMap<String, List<Pair<Integer, Double>>> matrix)
    {
        double[][] vectorAddition = new double[noOfOntologies][];
        for(int i = 0; i < noOfOntologies; i++)
        {
            vectorAddition[i] = new double[matrix.keySet().size()];
        }

        for(int i = 0; i < noOfOntologies; i++)
            for(int j = 0; j < matrix.keySet().size(); j++)
                vectorAddition[i][j] = 0;

        int colIdxCalc = 0;
        for(String entity : matrix.keySet())
        {
            System.out.print(entity + " ");
        }
        System.out.println();
        for(String entity : matrix.keySet())
        {
            //System.out.print(entity + " ");
            List<Pair<Integer, Double>> keyVals = matrix.get(entity);
            for(int j = 0; j < keyVals.size(); j++)
            {
                Pair<Integer, Double> pairs = keyVals.get(j);
                vectorAddition[pairs.getKey()][colIdxCalc] = pairs.getValue();
            }
            colIdxCalc = ++colIdxCalc % matrix.keySet().size();
            //System.out.println(keyVals.size());
        }

        //double[] result = new double[noOfOntologies];
        List<Double> result = new ArrayList<Double>();

        int ctr = noOfOntologies - 1;

        for(int i = 1; i < noOfOntologies; i++)
        {
            double sum = 0;
            for(int j = 0; j < matrix.keySet().size(); j++)
            {
                sum = sum + vectorAddition[0][j] * vectorAddition[i][j];
            }
            result.add(sum);
        }

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        //Printing the matrix containing the values
        for(int i = 0; i < noOfOntologies; i++)
        {
            for(int j = 0; j < matrix.keySet().size(); j++)
            {
                System.out.print(vectorAddition[i][j] + " ");
            }
            System.out.println();
        }

        return result;
    }
}
