import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.impl.Vector;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import javafx.geometry.Pos;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.List;
import java.util.Set;

/**
 * Created by Sharthak Ghosh on 6/30/2016.
 */
public class WordNetUtils {
    private static ILexicalDatabase db = new NictWordNet();
    private static OwlApiUtils owlApiUtils = new OwlApiUtils();

    public double computeWup(String w1, String w2) {
        WS4JConfiguration.getInstance().setMFS(true);
        RelatednessCalculator rcWuPalmer = new WuPalmer(db);
        double maxScore = -1;

        //Getting all pos values like 'n' and 'v'
        List<POS[]> posPairs = rcWuPalmer.getPOSPairs();
        for(POS[] posPair : posPairs)
        {
            List<Concept> synsets1 = (List<Concept>)db.getAllConcepts(w1, posPair[0].toString());
            List<Concept> synsets2 = (List<Concept>)db.getAllConcepts(w2, posPair[1].toString());

            //Computing the maximum score out of all the synsets
            for(Concept synset1 : synsets1)
            {
                for(Concept synset2 : synsets2)
                {
                    Relatedness relatedness = rcWuPalmer.calcRelatednessOfSynset(synset1, synset2);
                    double score = relatedness.getScore();
                    if(score > maxScore)
                        maxScore = score;
                }
            }
        }
        return maxScore;
    }

    public double computePath(String w1, String w2) {
        WS4JConfiguration.getInstance().setMFS(true);

        RelatednessCalculator rcPath = new Path(db);
        double maxScore = -1;

        List<POS[]> posPairs = rcPath.getPOSPairs();
        for(POS[] posPair : posPairs)
        {
            List<Concept> synsets1 = (List<Concept>)db.getAllConcepts(w1, posPair[0].toString());
            List<Concept> synsets2 = (List<Concept>)db.getAllConcepts(w2, posPair[1].toString());

            for(Concept synset1 : synsets1)
            {
                for(Concept synset2 : synsets2)
                {
                    Relatedness relatedness = rcPath.calcRelatednessOfSynset(synset1, synset2);
                    double score = relatedness.getScore();
                    if(score > maxScore)
                        maxScore = score;
                }
            }
        }
        return maxScore;
    }

    public double computeResnik(String w1, String w2) {
        WS4JConfiguration.getInstance().setMFS(true);
        return (new Resnik(db).calcRelatednessOfWords(w1, w2));
    }

    public void classSimilarity(OWLOntology ontology1, OWLOntology ontology2)
    {
        Set<String> classes1 = owlApiUtils.getClassesInOntology(ontology1);
        Set<String> classes2 = owlApiUtils.getClassesInOntology(ontology2);

        for(String i : classes1)
        {
            for(String j : classes2)
            {
                System.out.println(i + " and " + j + " similarity " + computeWup(i, j));
            }
        }
    }
}
