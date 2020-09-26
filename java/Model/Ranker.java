package Model;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.*;

import static Model.ReadFile.splitByDelimiter;

public class Ranker {

    private Map<String, String> documentsDictionary;
    private double k;
    private double b;
    private int avgDocLength;

    public Ranker() {
        k = 1.6;
        b = 0.75;
        this.documentsDictionary = new HashMap<>();
        avgDocLength = 0;
    }

    /**
     * ranking the documents that are related to the terms from query
     * the rank of document considers the BM25 calc, if the term appears in title, if its in the top 10% of doc, if its related to entity
     * @param parser
     * @param postingDataOfAllTermsInQuery
     * @return
     */
    public TreeMap<String, Double> rankDocuments(Parse parser, ArrayList<Pair<String, String>> postingDataOfAllTermsInQuery) {
        Map<String, Double> docsRank = new HashMap<>(); // doc , rank of doc
        StringBuilder detailDataDocFromIndexFile = new StringBuilder();
        StringBuilder docName = new StringBuilder();
        int tfInQuery = 0;
        String[] optionalDocsToRetrieve;
        Pair<String, String> postingAndTf;
        DecimalFormat df = new DecimalFormat("#.###");
        for (int i = 0; i < postingDataOfAllTermsInQuery.size(); i++) {
            postingAndTf = postingDataOfAllTermsInQuery.get(i); // term|FBIS3-84:51#1|FBIS3-85:41,71#2|FBIS3-92:21#1|FBIS3-93:12,238#2
            tfInQuery = Integer.parseInt(postingAndTf.getValue());// 55
            optionalDocsToRetrieve = ReadFile.splitByDelimiter(postingAndTf.getKey(), "|");
            for (int j = 1; j < optionalDocsToRetrieve.length; j++) {
                detailDataDocFromIndexFile.append(optionalDocsToRetrieve[j]); //FBIS3-84:51#1
                docName.append(detailDataDocFromIndexFile.substring(0, detailDataDocFromIndexFile.indexOf(":")));
                int docLen;
                String entities;
                if (!documentsDictionary.isEmpty()) {
                    String docInformation = documentsDictionary.get(docName.toString());// FBIS3-944|South African Press Review for 16 Mar|16 Mar 1994|none|none|177|6|139|ARTICLE TYPE BFN,858,TYPE BFN,858,SOUTH AFRICA,230,UNITED STATES,202,NATIONAL CONGRESS,199
//                    System.out.println(docName.toString());
                    entities = ReadFile.splitByDelimiter(docInformation, "|")[7]; // ARTICLE TYPE BFN,858,TYPE BFN,858,SOUTH AFRICA,230,UNITED STATES,202,NATIONAL CONGRESS,199
                    docLen = Integer.parseInt(ReadFile.splitByDelimiter(docInformation, "|")[4]);// 177
                    int queryTermInHeadDoc = checkQueryTermInHeadOfDoc(docLen, detailDataDocFromIndexFile.toString());
                    Parse.tmpPostingTerms.clear();
                    int queryTermInTitle = checkQueryTermInTitle(parser, docName.toString(), postingAndTf.getKey());
                    int queryTermRelatedToEntity = 0;
                    if(!entities.equals("none,0.0,")){ //LA010189-0175
                        queryTermRelatedToEntity = checkQueryTermRelatedToTopEntity(entities, postingAndTf.getKey());
                    }

                    int tfInDoc = Integer.parseInt(detailDataDocFromIndexFile.substring(detailDataDocFromIndexFile.indexOf("#") + 1));
                    double finalRank = getFinalRank(tfInQuery, optionalDocsToRetrieve.length, docLen, queryTermInHeadDoc, queryTermInTitle, tfInDoc, queryTermRelatedToEntity);
                    if (docsRank.containsKey(docName.toString())) { //if doc showed in more than 1 term in query as relevant
                        finalRank += docsRank.get(docName.toString());
                    }
                    finalRank = Double.parseDouble(df.format(finalRank));
                    docsRank.put(docName.toString(), finalRank);
                    detailDataDocFromIndexFile.setLength(0);
                    docName.setLength(0);
                }
            }
        }
        return (TreeMap<String, Double>) Indexer.sortByValues(docsRank);

    }

    /**
     * computes rank of doc by using the BM25 formula.
     * @param tfInQuery - num of appearances of term in query
     * @param docFreq - in how many docs the term appeared
     * @param docLen - length of doc
     * @param queryTermInHeadDoc - is the term in top 10% of the doc - 1/0
     * @param queryTermInTitle - is the term in title of doc - 1/0
     * @param tf - total term freq in corpus
     * @param queryTermRelatedToEntity - is term related to an entity - 1/0
     * @return final rank of the doc
     */
    private double getFinalRank(int tfInQuery, int docFreq, int docLen, int queryTermInHeadDoc, int queryTermInTitle, int tfInDoc, int queryTermRelatedToEntity) {
        return 0.5 * (tfInQuery * (k + 1) * tfInDoc / (tfInDoc + k * (1 - b + b * docLen / avgDocLength)))
                * Math.log((documentsDictionary.size()+1) / docFreq) +
                0.2 * queryTermInTitle + 0.2 * queryTermInHeadDoc + 0.1 * queryTermRelatedToEntity;
    }

    /**
     * check if the term is part of entity
     * @param entities in doc
     * @param postingOfWordFromQuery extract term from it
     * @return
     */
    private int checkQueryTermRelatedToTopEntity(String entities, String postingOfWordFromQuery) {
        String termInQuery = postingOfWordFromQuery.substring(0, postingOfWordFromQuery.indexOf("|")); //term in query
        String[] entitiesWithoutFreq = ReadFile.splitByDelimiter(entities, ","); //ARTICLE TYPE BFN,858,TYPE BFN,858,SOUTH AFRICA,230,UNITED STATES,202,NATIONAL CONGRESS,199
        for (int i = 0; i < entitiesWithoutFreq.length; i = i + 2) {
            if (entitiesWithoutFreq[i].toUpperCase().contains(termInQuery.toUpperCase())) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * check if term appears in doc's title.
     * @param parser - meant to parse the title to terms
     * @param docName - getting the docs info
     * @param postingOfWordFromQuery - extract term
     * @return
     */
    private int checkQueryTermInTitle(Parse parser, String docName, String postingOfWordFromQuery) {
        String docFullPosting = documentsDictionary.get(docName); // South African Press Review for 16 Mar|16 Mar 1994|none|none|177|6|139|entities
        String titleOfDoc = ReadFile.splitByDelimiter(docFullPosting, "|")[0];//South African Press Review for 16 Mar
        Doc title = new Doc();
        parser.parse(titleOfDoc, title);
        HashSet<String> termsInTitle = new HashSet<>(Parse.tmpPostingTerms.keySet());// get parsed terms in title
        String termInQuery = postingOfWordFromQuery.substring(0, postingOfWordFromQuery.indexOf("|")); //term in query
        for (String termInTitle : termsInTitle) {
            if (termInQuery.toUpperCase().equals(termInTitle.toUpperCase())) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * check if term appears in top 10% of words of the doc by getting the length of doc and first position of doc and divide firstPositio/length and see if its <=> 0.1
     * @param docLen - docs length
     * @param detailDataDoc - details of doc, posting file data
     * @return
     */
    private int checkQueryTermInHeadOfDoc(int docLen, String detailDataDoc) {
        String[] tmp = ReadFile.splitByDelimiter(detailDataDoc, ":"); //FBIS3-85:41,71#2 -> {FBIS3-85},{41,71#2}
        String[] tmp1 = ReadFile.splitByDelimiter(tmp[1], "#"); // {41,71},{2}
        String firstPositionOnIndex = ReadFile.splitByDelimiter(tmp1[0], ",")[0]; //{41},{71}
        if (Double.parseDouble(firstPositionOnIndex) / docLen <= 0.1) {
            return 1;
        }
        return 0;

    }

    /**
     * get 50 top entries from a TreeMap
     * @param numOfEntries
     * @param from
     * @return
     */
    public TreeMap<String, Double> getTopEntries(int numOfEntries, TreeMap<String, Double> from) {
        int countToNumOfEntries = 0;
        TreeMap<String, Double> topN = new TreeMap<>();
        for (Map.Entry<String, Double> entry : from.entrySet()) {
            if (countToNumOfEntries >= numOfEntries) break;
            topN.put(entry.getKey(), entry.getValue());
            countToNumOfEntries++;
        }

        return (TreeMap<String, Double>) Indexer.sortByValues(topN);

    }

    /**
     * loading the Documents Information from a file we wrote in indexing phase
     * @param selected
     * @return
     */
    public boolean loadDocDetailsToMemo(boolean selected) {
        String line = null;
        BufferedReader reader = null;
        int sum = 0;
        int docs = 0;
        try {
            if (selected) {
                reader = new BufferedReader(new FileReader(Searcher.pathToWriteRead + "\\DocumentsDetails_stemmed.txt"));
            } else {
                reader = new BufferedReader(new FileReader(Searcher.pathToWriteRead + "\\DocumentsDetails.txt"));
            }
            line = reader.readLine();
            while (line != null) {
                docs++;
                String[] details = splitByDelimiter(line, "|");
                documentsDictionary.put(details[0] , line.substring(line.indexOf("|")+1)); // docID , rest of details
                sum += Integer.parseInt(details[5]);
                line = reader.readLine();
            }
            reader.close();
            avgDocLength = sum/docs;
            return true;

        }catch (Exception e){e.printStackTrace();}
        return false;
    }

    public void reset() {
        documentsDictionary.clear();
        k = 0;
        b = 0;
        avgDocLength = 0;
    }

    public Map<String, String> getDocumentsDictionary() {
        return documentsDictionary;
    }
}
