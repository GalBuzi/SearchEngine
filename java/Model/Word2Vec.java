package Model;

import com.medallia.word2vec.Word2VecModel;
import com.medallia.word2vec.Searcher.Match;

import java.io.File;
import java.util.List;

public class Word2Vec {

    public Word2Vec() {
    }

    /**
     * this func will return 3 terms at most that have similar meaning to the term parameter
     * @param term
     * @return
     */
    public String similarTerms(String term) {
        StringBuilder similarTerms = new StringBuilder();
        Word2VecModel model;
        String filename = System.getProperty("user.dir")+ "\\word2vec.c.output.model.txt";
//        "src/main/resources/word2vec.c.output.model.txt";
        File file = new File(filename);
        try {
            model = Word2VecModel.fromTextFile(file);
            com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();
            List<Match> matches = semanticSearcher.getMatches(term, 3);
            for (Match match : matches) {
                String similarWord = match.match(); //matching word
                double similarityScore = match.distance();  // how close the match is to the original word
                if(similarityScore > 0.95){
                    similarTerms.append(similarWord).append(" ");
                }
            }
        }catch(Exception e){   }

        return similarTerms.toString();
    }


}
