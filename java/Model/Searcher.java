package Model;

import javafx.util.Pair;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class Searcher {

    private Parse parserOfSearcher; //parser object to parse queries
    private Indexer indexerForSearch; // indexer object to index the terms of query
    private Ranker ranker; //which ranks the documents
    public static String pathToWriteRead; //path for read and write files
    private Indexer indexerOfProgram; // indexer object that holds info about the corpus

    public Searcher(Indexer indexerOfProgram, Ranker ranker, boolean userChoseStemming) {
        this.parserOfSearcher = new Parse();
        this.indexerForSearch = new Indexer(userChoseStemming);
        parserOfSearcher.setIndexer(indexerForSearch);
        parserOfSearcher.setUserChoseStemming(userChoseStemming);
        parserOfSearcher.setPathToWrite(pathToWriteRead);
        this.indexerOfProgram = indexerOfProgram;
        this.ranker = ranker;
        pathToWriteRead = null;
    }

    /**
     * get a query from user and find the relevant documents for it
     *
     * @param query             from user
     * @param userChoseStemming to stem or not
     * @return 50 relevant docs
     */
    public TreeMap<String, Double> getRelevantDocs(String query, boolean userChoseStemming) {
        TreeMap<String, Double> res = null;
        Doc queryDoc = new Doc();
        parserOfSearcher.importStopWords(pathToWriteRead);
        parserOfSearcher.parse(query, queryDoc); // parse the query
        ArrayList<Pair<String, String>> postingDataOfAllTermsInQuery = new ArrayList<>();
        for (Map.Entry<String, Term> entries : indexerForSearch.getTermsDictionary().entrySet()) {
            String tfInQuery = String.valueOf(entries.getValue().getTermFreqInCorpus());
            if (indexerOfProgram.getTermsDictionary().containsKey(entries.getKey())) {
                String term = entries.getKey();
                long pointer = indexerOfProgram.getTermsDictionary().get(term).getPtrPosting(); // find pointer to the relevant index file
                String postingDataOfTerm = getPostingDataFromDisk(term, pointer, userChoseStemming); // extract posting data from index file
                Pair<String, String> termAndData = null;
                if (postingDataOfTerm != null) {
                    termAndData = new Pair<>(term + "|" + postingDataOfTerm, tfInQuery); // term|FBIS3-67:po1,po2...#freq in doc , tfInQuery
                    postingDataOfAllTermsInQuery.add(termAndData);
                }
            } else if (indexerOfProgram.getTermsDictionary().containsKey(entries.getKey().toLowerCase())) {
                String term = entries.getKey();
                long pointer = indexerOfProgram.getTermsDictionary().get(term.toLowerCase()).getPtrPosting(); // find pointer to the relevant index file
                String postingDataOfTerm = getPostingDataFromDisk(term, pointer, userChoseStemming); // extract posting data from index file
                Pair<String, String> termAndData = null;
                if (postingDataOfTerm != null) {
                    termAndData = new Pair<>(term + "|" + postingDataOfTerm, tfInQuery); // term|FBIS3-67:po1,po2...#freq in doc , tfInQuery
                    postingDataOfAllTermsInQuery.add(termAndData);
                }
            } else if (indexerOfProgram.getTermsDictionary().containsKey(entries.getKey().toUpperCase())) {
                String term = entries.getKey();
                long pointer = indexerOfProgram.getTermsDictionary().get(term.toUpperCase()).getPtrPosting(); // find pointer to the relevant index file
                String postingDataOfTerm = getPostingDataFromDisk(term, pointer, userChoseStemming); // extract posting data from index file
                Pair<String, String> termAndData = null;
                if (postingDataOfTerm != null) {
                    termAndData = new Pair<>(term + "|" + postingDataOfTerm, tfInQuery); // term|FBIS3-67:po1,po2...#freq in doc , tfInQuery
                    postingDataOfAllTermsInQuery.add(termAndData);
                }
            }
        }
        res = ranker.rankDocuments(parserOfSearcher, postingDataOfAllTermsInQuery); //rank the documents
        indexerForSearch.clearDictAndEntities();


        if (!res.isEmpty() && res.size() > 50) {
            return ranker.getTopEntries(50, res);
        }

        return res;
    }

    /**
     * getting posting data from the disk with the pointer we have inserted in the indexing phase
     *
     * @param term
     * @param pointer
     * @param userChoseStemming
     * @return
     */
    private String getPostingDataFromDisk(String term, long pointer, boolean userChoseStemming) {
        String postingDataOfTerm = null;
        String path = null;
        int lineIndex = 1;
        char firstLetter = Character.toUpperCase(term.charAt(0));
        if (Character.isDigit(firstLetter)) {
            path =pathToWriteRead + "\\finalPostingNumber" + firstLetter;
        } else if (!Character.isDigit(firstLetter) && !Character.isLetter(firstLetter)) {
            path = pathToWriteRead+"\\finalPostingSpecialChars";
        } else
            path = pathToWriteRead+"\\finalPosting" + firstLetter;
        try {
            if (userChoseStemming) {
                path += "_stemmed.txt";
            } else {
                path += ".txt";
            }

            try (Stream<String> all_lines = Files.lines(Paths.get(path))) {
                postingDataOfTerm = all_lines.skip(pointer-1).findFirst().get();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return postingDataOfTerm;

    }


    public void setPathToWriteRead(String pathToWriteRead) {
        this.pathToWriteRead = pathToWriteRead;
    }

    public void reset() {
        parserOfSearcher = new Parse();
        indexerForSearch = new Indexer(false);
        ranker = new Ranker();
        pathToWriteRead = "";
        indexerOfProgram = new Indexer(false);
    }

    public void setStmmingInParse(boolean userChoseStemming) {
        parserOfSearcher.setUserChoseStemming(userChoseStemming);
        indexerForSearch.setUserChoseStemming(userChoseStemming);
    }
}
