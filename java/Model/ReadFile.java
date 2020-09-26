package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ReadFile {

    /**
     * reads files from certain path
     */
    private File corpus; //path to file which holds the whole corpus
    private int totalDocCount; // counting how many docs are in the corpus
    private Parse parser; // parser that will parse documents
    private int totalDocs;
    public static HashMap<Doc, EntityInfoInDoc> allDocsInCorpus = new HashMap<>(); //< doc , <entity,freqInDoc> >


    /**
     * Reads the documents pool and sends each document to the parser for following processing.
     */
    public ReadFile() {
//        docsWaitingToBeWritten = new LinkedList<>();
        totalDocCount = 0;
        parser = new Parse();
    }

    /**
     * sets the the corpus to the given path by user
     *
     * @param path - the path of the corpus.
     */
    public void setCorpus(String path) {
        this.corpus = new File(path);
    }

    /**
     * set the parser to myModel's parser
     *
     * @param parser
     */
    public void setParser(Parse parser) {
        this.parser = parser;
    }

    public Parse getParser() {
        return parser;
    }

    /**
     * the function goes over the corpus and reads each file and sedns it to parsing
     * every 500 docs we will write the posting files which created so far
     */
    public void readFile() {
        File[] corpusContent = corpus.listFiles(); //gives all files/directories in the corpus directory
        Document fileOfDocs = null;//separated doc in single file
        Elements listOfDocuments = null;

        File[] filesInDir = null;
        for (int i = 0; i < corpusContent.length; i++) { //going over all dirs
            File dirInCorpus = corpusContent[i];
            if (dirInCorpus.isDirectory()) {
                filesInDir = dirInCorpus.listFiles(); //get file in dir in corpus, it will be always first cause there is one file in each dir
                if (filesInDir != null) {//if the dir isn't empty
                    try {
                        fileOfDocs = Jsoup.parse(new String(Files.readAllBytes(Paths.get(filesInDir[0].getAbsolutePath()))));//get the File which contains lots of docs
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    listOfDocuments = fileOfDocs.getElementsByTag("doc");//split the File for docs by <doc></doc> tags
                    for (int j = 0; j < listOfDocuments.size(); j++) {
                        Element doc = listOfDocuments.get(j);
                        if (doc != null) {
                            if (doc.select("text").size() > 0) {
                                Doc currentDocument = new Doc();
                                parseDocImportantTags(currentDocument, doc);
                                parser.parse(doc.getElementsByTag("text").toString(), currentDocument);
                                totalDocCount++;
                                allDocsInCorpus.put(currentDocument , null);
                            }
                        }
                    }
                }
            }
        }
        totalDocs = totalDocCount;
        totalDocCount = 0;

    }

    /**
     * asserting relevant info to Doc by parsing tags of the doc element we got
     *
     * @param currentDocument - doc to fill with info
     * @param docToParse      - doc to parse
     */
    private void parseDocImportantTags(Doc currentDocument, Element docToParse) {
        //set name
        String name = docToParse.getElementsByTag("docno").toString();
        currentDocument.setName(splitByDelimiter(name, " |\n")[1]);

        if (currentDocument.getName().startsWith("FBIS")) {
            //set title
            String titleOption1 = docToParse.getElementsByTag("ti").toString();     //some of the files specifies title like this and other with headlines
            if (titleOption1 != null && !titleOption1.equals("") && !titleOption1.isEmpty() && !titleOption1.equals(" ") && titleOption1.length() >= 2) {
                String[] titleChars = splitByDelimiter(titleOption1, " |\n");
                StringBuilder sbTitle = new StringBuilder();
                restoreSrting(sbTitle, titleChars, 1, titleChars.length - 1);
                if(sbTitle.toString().length()>0){
                    currentDocument.setTitle(sbTitle.toString());
                }
            }
            //set date
            String byDate1 = docToParse.getElementsByTag("date1").toString(); //several options for specifying a date
            String byDate = docToParse.getElementsByTag("date").toString();
            if (byDate1 != null && !byDate1.equals("")) {
                String[] s = splitByDelimiter(docToParse.getElementsByTag("date1").toString(), " \n");
                StringBuilder sbDate = new StringBuilder();
                restoreSrting(sbDate, s, 1, s.length - 1);
                if(sbDate.toString().length()>0){
                    currentDocument.setDate(sbDate.toString());
                }

            } else if (byDate != null && !byDate.equals("")) {
                String[] s = splitByDelimiter(docToParse.getElementsByTag("date").toString(), " \n");
                StringBuilder sbDate = new StringBuilder();
                restoreSrting(sbDate, s, 1, s.length - 1);
                if(sbDate.toString().length()>0){
                    currentDocument.setDate(sbDate.toString());
                }
            }
            //set city
            String cityBtwTags = docToParse.select("f").select("[p=104]").toString(); //take specific tags
            if (cityBtwTags != null && !cityBtwTags.equals("") && !cityBtwTags.isEmpty() && !cityBtwTags.equals(" ") && cityBtwTags.length() >= 2) {
                String[] cityArr = splitByDelimiter(cityBtwTags, " |\n");
                StringBuilder sbTitle = new StringBuilder();
                if (!cityArr[1].equals("p=\"104\"></f>")) { //there is no city btw tags, its empty
                    String cityName = splitByDelimiter(cityBtwTags, " |\n")[2].toUpperCase();
                    if(sbTitle.toString().length()>0){
                        currentDocument.setCity(cityName);
                    }

                }
            }
            //set language
            String langBtwTags = docToParse.select("f").select("[p=105]").toString(); //take specific tags
            if (langBtwTags != null && !langBtwTags.equals("") && !langBtwTags.isEmpty() && !langBtwTags.equals(" ") && langBtwTags.length() >= 2) {
                String[] langArr = splitByDelimiter(cityBtwTags, " |\n");
                StringBuilder sbTitle = new StringBuilder();
                if (!langArr[1].equals("p=\"105\"></f>")) { //there is no lang btw tags, its empty
                    String lang = splitByDelimiter(langBtwTags, " |\n")[2].toUpperCase();
                    if(sbTitle.toString().length()>0){
                        currentDocument.setLanguage(lang);
                    }

                }

            }
        } else if (currentDocument.getName().startsWith("LA")){ //docs with LA start

            //set title
            String titleOption2 = docToParse.getElementsByTag("headline").toString();
            if (titleOption2 != null && titleOption2.contains("<p>")) {
                String[] titleChars = splitByDelimiter(titleOption2, "<p>\n/ ");
                StringBuilder sbTitle = new StringBuilder();
                restoreSrting(sbTitle, titleChars, 1, titleChars.length - 1);
                if(sbTitle.toString().length()>0){
                    currentDocument.setTitle(sbTitle.toString());
                }
            }
            //set date
            String byDate = docToParse.getElementsByTag("DATE").toString();
            if (byDate != null && !byDate.equals("") && byDate.contains("<p>")) {
                String[] dateParsed = splitByDelimiter(byDate, "<p>\n/  ");
                StringBuilder sbDate = new StringBuilder();
                restoreSrting(sbDate , dateParsed , 1 , dateParsed.length-1 );
                if(sbDate.toString().length()>0){
                    currentDocument.setDate(sbDate.toString());
                }
            }
            //set city
            String place = docToParse.getElementsByTag("DATELINE").toString();
            if (place != null && !place.equals("") && place.contains("<p>")) {
                String[] placeParsed = splitByDelimiter(place, "<p>\n/  ");
                StringBuilder sbPlace = new StringBuilder();
                restoreSrting(sbPlace , placeParsed , 1 , placeParsed.length - 1);
                if(sbPlace.toString().length()>0){
                    currentDocument.setCity(sbPlace.toString());
                }
            }

        }else if (currentDocument.getName().startsWith("FT")){
            //set title and date
            String titleOption1 = docToParse.getElementsByTag("headline").toString();     //some of the files specifies title like this and other with headlines
            if (titleOption1 != null && !titleOption1.equals("") && !titleOption1.isEmpty() && !titleOption1.equals(" ") && titleOption1.length() >= 2) {
                String[] titleChars = splitByDelimiter(titleOption1, " |\n");
                StringBuilder sbTitle = new StringBuilder();
                StringBuilder sbDate = new StringBuilder();
                restoreSrting(sbDate, titleChars, 2, 5);
                restoreSrting(sbTitle, titleChars, 6, titleChars.length - 1);
                if(sbTitle.toString().length()>0){
                    currentDocument.setTitle(sbTitle.toString());
                }
                if(sbDate.toString().length()>0){
                    currentDocument.setDate(sbDate.toString());
                }
            }
            //set city
            String place = docToParse.getElementsByTag("DATELINE").toString();
            if (place != null && !place.equals("") ) {
                String[] placeParsed = splitByDelimiter(place, "\n/<>  ");
                StringBuilder sbPlace = new StringBuilder();
                restoreSrting(sbPlace , placeParsed , 1 , placeParsed.length - 1);
                if(sbPlace.toString().length()>0){
                    currentDocument.setCity(sbPlace.toString());
                }
            }

        }

    }

    /**
     * create a string from an array that was slitted by a delimiter
     *
     * @param sbTitle    - inserting the strings to
     * @param titleChars - contains the strings in each cell
     * @param start      - from where to restore
     * @param end        - to where
     */
    private void restoreSrting(StringBuilder sbTitle, String[] titleChars, int start, int end) {
        for (int i = start; i < end; i++) {
            if (i == end - 1) {
                sbTitle.append(titleChars[i]);
                break;
            }
            sbTitle.append(titleChars[i] + " ");
        }
    }

    /**
     * faster implementation of the str.split().
     * improves performances.
     *
     * @param str        - the string we wish to split.
     * @param delimiter- the delimiter we wish to split by.
     * @return a String array contains all the tokens.
     */
    public static String[] splitByDelimiter(String str, String delimiter) {
        if(str.length() > 0 && delimiter.length()>0){
            ArrayList<String> splitData = new ArrayList<>();
            StringTokenizer tokenizer = new StringTokenizer(str, delimiter);
            while (tokenizer.hasMoreTokens()) {
                splitData.add(tokenizer.nextToken());
            }
            String[] splitResult = new String[splitData.size()];
            return splitData.toArray(splitResult);
        }
        return new String[0];
    }

    public int getTotalDocs() {
        return totalDocs;
    }

    public void reset() {
        corpus = null;
        totalDocCount = 0;
        parser = null;
        totalDocs = 0;
    }


    public void setTotalDocs(int totalDocs) {
        this.totalDocs = totalDocs;
    }
}