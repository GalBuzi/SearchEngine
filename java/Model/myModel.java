package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class myModel extends Observable {

    private static myModel singleton = null;
    private ReadFile fileReader;
    private Parse parser;
    private Indexer indexer;
    private Ranker ranker;
    private Searcher searcher;
    private List<Pattern> repeatedPatterns;
    private Word2Vec word2Vec;
    private TreeMap<Integer , Vector<String>> resultsOfQueries;
    private ArrayList<Result> results;


    //singleton
    public static myModel getInstance() {
        if (singleton == null)
            singleton = new myModel();
        return singleton;
    }

    //constructor
    private myModel(){
        indexer=new Indexer(false);
        parser = new Parse();
        fileReader=new ReadFile();
        ranker = new Ranker();
        searcher = new Searcher(indexer,ranker,parser.isUserChoseStemming());
        repeatedPatterns = new ArrayList<>();
        word2Vec = new Word2Vec();
        resultsOfQueries = new TreeMap<>();
        results = new ArrayList<>();

    }


    /**
     * setting objects with configuration that we get from user
     * @param corpus
     * @param pathToWrite
     * @param stemming
     */
    public void setUserSettings(String corpus, String pathToWrite, boolean stemming) {
        fileReader.setCorpus(corpus);
        parser.setPathToWrite(pathToWrite);
        parser.setPathOfCorpusAndStopWords(corpus);
        parser.setIndexer(indexer);
        parser.setUserChoseStemming(stemming);
        fileReader.setParser(parser);
        indexer.setUserChoseStemming(stemming);
        indexer.setPathToWrite(pathToWrite);
        searcher.setPathToWriteRead(pathToWrite);
    }

    /**
     * setting the path for reading&writing files
     * @param pathToWrite
     */
    public void setPathOfIndexFiles(String pathToWrite){
        indexer.setPathToWrite(pathToWrite);
        searcher.setPathToWriteRead(pathToWrite);
    }

    /**
     * initialize objects and delete files in directory
     * @return
     */
    public boolean resetSettings() {
        results = new ArrayList<>();
        resultsOfQueries = new TreeMap<>();
        word2Vec = new Word2Vec();
        repeatedPatterns = new ArrayList<>();
        fileReader.reset();
        parser.reset();
        searcher.reset();
        ranker.reset();
        return indexer.reset();
    }

    /**
     * reading file from disk that contains the dictionary with terms and term freq for each one
     * to present it to the user in GUI
     * @return
     */
    public ArrayList<String> getDictTermsToDisplay() {
        return indexer.getTermsInDict();
    }

    /**
     * read files of terms' dictionary and documents' dictionary
     * @param selected
     * @return
     */
    public boolean loadDictionariesToMemo(boolean selected) {
        return indexer.loadDictToMemo(selected) && ranker.loadDocDetailsToMemo(selected);
    }

    /**
     * call for reading files, parsing them and creating index files.
     * @param stemming
     * @return time to create , how many docs we parsed , terms' dictionary size
     */
    public double[] createIndex(boolean stemming) {
        double[] details = new double[3];
        long readStart = System.nanoTime();
        fileReader.getParser().setUserChoseStemming(stemming);
        indexer.setUserChoseStemming(stemming);
        fileReader.readFile();
        indexer.writePostingDataToDisk();
        indexer.clearLeftOvers();
        indexer.setPostingFilesChunkCount(0);
        indexer.generateInvertedIndex();
        long indexEnd = System.nanoTime();
        double indexTimer = (indexEnd - readStart) * 1.6667 * Math.pow(10, -11);
        System.out.println(indexTimer + " minutes --- read+write and create index");
        indexer.clearEntitiesDictionary(); //sets top 5 entities in each doc
        indexer.writeDocumentsInfoToDisk(); //write documentDetails file
        details[0] = indexTimer; //turn nanosec to minutes
        details[1] = fileReader.getTotalDocs();
        details[2] = indexer.getTermsDictFinalSize();
        fileReader.setTotalDocs(0);
        return details;
    }


    /**
     * crating ID for user's query and call for processing the query with the new ID
     * @param query
     * @param isSmanticsOnline
     * @param isSemantics
     * @param userChoseStemming
     * @return
     */
    public ArrayList<Result> processSingleUserQuery(String query, boolean isSmanticsOnline, boolean isSemantics, boolean userChoseStemming){
        //single query has no id so we will generate one
        String qID = String.valueOf( 500 + (int) (Math.random() * 500)); //number btw 500-999
        indexer.setUserChoseStemming(userChoseStemming);
        parser.setUserChoseStemming(userChoseStemming);
        searcher.setStmmingInParse(userChoseStemming);
        return processSingleQuery(qID , query , isSmanticsOnline,isSemantics,userChoseStemming);
    }

    /**
     * setting how single query will be processed and call for searcher to get relevant documents
     * @param qID
     * @param query
     * @param isSmanticsOnline
     * @param isSemantics
     * @param userChoseStemming
     * @return
     */
    private ArrayList<Result> processSingleQuery(String qID, String query, boolean isSmanticsOnline, boolean isSemantics, boolean userChoseStemming) {
        String qTitle = "";
        if(Integer.parseInt(qID) < 500 ){
            qTitle = query.substring(0,query.indexOf(";"));
        }else{
            qTitle = "none"; //user enters query with no title
        }

        ArrayList<Result> results = new ArrayList<>(); // final results
        TreeMap<String, Double> qResults; // midterm results
        if(isSemantics){
            if(isSmanticsOnline){
                query += getSemanticTermsToQueryTerms(query); //add synonymous terms to the terms in query
            }
            else{
                String[] queryTerms = ReadFile.splitByDelimiter(query , "; ");
                StringBuilder queryBuilder = new StringBuilder(query);
                for (int i = 0; i < queryTerms.length; i++) {
                    String add = word2Vec.similarTerms(queryTerms[i]);
                    if(add.length()>0){
                        queryBuilder.append(add).append(" ");
                    }

                }
                query += queryBuilder.toString();
            }
        }

        qResults = searcher.getRelevantDocs(query , userChoseStemming); //find relevant documents to the query
        if(qResults != null){
            for (Map.Entry<String,Double> queryRes : qResults.entrySet() ){
                results.add(new Result(qID,qTitle,queryRes.getKey() , queryRes.getValue()));
            }
        }else{
            results.add(new Result(qID,qTitle, "none" , 0));
        }

        resultsOfQueries.put(Integer.parseInt(qID) , new Vector<>());
        for(Result res : results){
            resultsOfQueries.get(Integer.parseInt(res.getQueryId())).add(res.getDocName());//+" "+res.getRank()
        }


        return results;
    }

    /**
     * usuing online API to find similar terms to those in query to enhance the accuracy of the results
     * @param query
     * @return
     */
    private String getSemanticTermsToQueryTerms(String query) {
        StringBuffer synonymousTerms = new StringBuffer();
        String[] wordsInQuery = ReadFile.splitByDelimiter(query, " -()?,.:/{}");
        for (int i = 0; i < wordsInQuery.length; i++) {
            try {
                //working with DataMuse API to get synonymous terms for each term in query
                URL address = new URL("https://api.datamuse.com/words?rel_syn=" + wordsInQuery[i]);
                HttpURLConnection con = (HttpURLConnection) address.openConnection();
                con.setRequestMethod("GET");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String firstLineInAPI;
                StringBuffer synonTermsFromAPI = new StringBuffer();
                if ((firstLineInAPI = bufferedReader.readLine()) != null)
                    synonTermsFromAPI.append(firstLineInAPI);
                bufferedReader.close();
                con.disconnect();
                String[] parsedTermsOfAPI = ReadFile.splitByDelimiter(synonTermsFromAPI.toString(), "{|[|,|\"|:|]|}");
                if(parsedTermsOfAPI.length>0){//there are results
                    synonymousTerms.append(parsedTermsOfAPI[1]).append(" ").append(parsedTermsOfAPI[5]).append(" ");// take 2 synonymous for each term
                }


            } catch (Exception e) { }

        }
        return " "+synonymousTerms.toString();
    }


    /**
     * getting a path to query file, breaking it to single queries and call processSingleQuery
     * returns all results of all queries in file ordered by query ID
     * @param pathToQueryFile
     * @param isSemanticsOnline
     * @param isSemantics
     * @param userChoseStemming
     * @return
     */
    public ArrayList<Result> processFileOfQueries(String pathToQueryFile, boolean isSemanticsOnline, boolean isSemantics, boolean userChoseStemming){
        insertPatterns();
        indexer.setUserChoseStemming(userChoseStemming);
        parser.setUserChoseStemming(userChoseStemming);
        searcher.setStmmingInParse(userChoseStemming);
        try {
            //get file of allQueries
            Document doc = Jsoup.parse(new String(Files.readAllBytes(Paths.get(pathToQueryFile))));
            Elements allQueries = doc.getElementsByTag("top"); // separate btw allQueries
            for (Element queryInFile : allQueries) {
                long start = System.nanoTime();
                //extract queryInFile ID
                String queryId = queryInFile.select("num").text().substring(8,11);
                System.out.println("---------------------------------------Start "+ queryId);
                //extract title
                String title = queryInFile.select("title").text();
                //extract description
                String desc = "";
                String descContent =  queryInFile.select("desc").text();
                if(descContent!= null && descContent.length()>0){
                    desc = descContent.substring(13,descContent.indexOf(":",13)-9);
                }
                //extract narrative
                String narr = "";
                String narrContent =  queryInFile.select("narr").text();
                if(narrContent!= null && narrContent.length()>0){
                    narr = narrContent.substring(11);
                }

                String cleanQuery = title+";"+ desc +";"+ eliminatePatterns(narr);

                ArrayList<Result> rankedResults = processSingleQuery(queryId, cleanQuery,isSemanticsOnline,isSemantics,userChoseStemming);
                results.addAll(rankedResults);
                long indexEnd = System.nanoTime();
                double indexTimer = (indexEnd - start) * Math.pow(10, -9);
                System.out.println(indexTimer + " seconds to run query");
            }

        }catch (Exception e){ }

        Collections.sort(results, new Comparator<Result>() {
            @Override
            public int compare(Result r1, Result r2) {
                return r1.getQueryId().compareTo(r2.getQueryId());
            }
        });


//        writeResultsToFile();

        return results;

    }

    /**
     * writing results when the user decides to save the file to the disk
     * the saved file will be called "results.txt"
     * @param path
     * @param userChoseStemming
     */
    public void writeResultsToFile(String path, boolean userChoseStemming) {
        try {
            File res = null;
            if(!userChoseStemming){
                res = new File(path +"\\results.txt");
            }else{
                res = new File(path +"\\results_stemmed.txt");
            }

            FileWriter resWriter = new FileWriter(res);
            for (int i = 0; i < results.size(); i++) {
                resWriter.write(results.get(i).getQueryId()+" 1 "+results.get(i).getDocName()+" 1 42.38 mt\n");
            }
            resWriter.flush();
            resWriter.close();

        } catch (Exception e) {
        }


    }

    /**
     * go through all patterns in list and eliminate words that in patterns
     * take the relevant info from the narrative part
     * @param content
     * @return
     */
    private String eliminatePatterns(String content){
        Matcher matcher;
        if(content!=null) {
            for (int i = 0; i < repeatedPatterns.size(); i++) {
                matcher = repeatedPatterns.get(i).matcher(content);
                if (matcher.find()) {
                    String relevant = matcher.group(1);
                    return relevant;
                }
            }
            return "";
        }
        return "";
    }

    /**
     * insert repeated patterns that appear in the queries so we can ignore them later on
     */
    private void insertPatterns() {
        Pattern p1 = Pattern.compile(Pattern.quote("document discussing") +
                "(?s)(.*?)" + Pattern.quote("is relevant."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p1);
        Pattern p2 = Pattern.compile(Pattern.quote("documents discussing") +
                "(?s)(.*?)" + Pattern.quote("are relevant."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p2);
        Pattern p3 = Pattern.compile(Pattern.quote("document discussing") +
                "(?s)(.*?)" + Pattern.quote("is considered relevant."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p3);
        Pattern p4 = Pattern.compile(Pattern.quote("documents discussing") +
                "(?s)(.*?)" + Pattern.quote("are considered relevant."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p4);
        Pattern p5 = Pattern.compile(Pattern.quote("relevant documents must contain information on") +
                "(?s)(.*?)" + Pattern.quote("."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p5);
        Pattern p6 = Pattern.compile(Pattern.quote("relevant documents must contain the following information:") +
                "(?s)(.*?)" + Pattern.quote("."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p6);
        Pattern p7 = Pattern.compile(Pattern.quote("documents which refer to") +
                "(?s)(.*?)" + Pattern.quote("are relevant."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p7);
        Pattern p8 = Pattern.compile(Pattern.quote("What information is available") +
                "(?s)(.*?)" + Pattern.quote("?"), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p8);//Documents discussing the following issues are not relevant:
        Pattern p9 = Pattern.compile(Pattern.quote("any document discussing") +
                "(?s)(.*?)" + Pattern.quote("is considered relevant."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p9);
        Pattern p10 = Pattern.compile(Pattern.quote("Documents discussing the following issues are relevant:") +
                "(?s)(.*?)" + Pattern.quote("Documents"), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p10);
        Pattern p11 = Pattern.compile(Pattern.quote("What role does") +
                "(?s)(.*?)" + Pattern.quote("?"), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p11);
        Pattern p12 = Pattern.compile(Pattern.quote("A document must contain at least one factor such as:") +
                "(?s)(.*?)" + Pattern.quote("to be relevant."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p12);
        Pattern p13 = Pattern.compile(Pattern.quote("Documents that discuss") +
                "(?s)(.*?)" + Pattern.quote("are considered relevant."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p13);
        Pattern p14 = Pattern.compile(Pattern.quote("Document that discusses") +
                "(?s)(.*?)" + Pattern.quote("is considered relevant."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p14);
        Pattern p15 = Pattern.compile(Pattern.quote("Identify documents that discuss") +
                "(?s)(.*?)" + Pattern.quote("."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p15);
        Pattern p16 = Pattern.compile(Pattern.quote("A relevant document will discuss") +
                "(?s)(.*?)" + Pattern.quote("."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p16);
        Pattern p17 = Pattern.compile(Pattern.quote("A relevant document should identify") +
                "(?s)(.*?)" + Pattern.quote("."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p17);
        Pattern p18 = Pattern.compile(Pattern.quote("A relevant document may include") +
                "(?s)(.*?)" + Pattern.quote("."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p18);//
        Pattern p19 = Pattern.compile(Pattern.quote("The focus of the topic is") +
                "(?s)(.*?)" + Pattern.quote("."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p19);
        Pattern p20 = Pattern.compile(Pattern.quote("Find documents that discuss") +
                "(?s)(.*?)" + Pattern.quote("."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p20);
        Pattern p21 = Pattern.compile(Pattern.quote("Identify documents that discuss the concerns of") +
                "(?s)(.*?)" + Pattern.quote("."), Pattern.CASE_INSENSITIVE);
        repeatedPatterns.add(p21);//
    }

    public TreeMap<Integer, Vector<String>> getResultsOfQueries() {
        return resultsOfQueries;
    }

    public void setResultsOfQueries(TreeMap<Integer, Vector<String>> resultsOfQueries) {
        this.resultsOfQueries = resultsOfQueries;
    }

    public Map<String, String> getDocumentsDict() {
        return ranker.getDocumentsDictionary();
    }

}
