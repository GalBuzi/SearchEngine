package Model;


import com.google.code.externalsorting.ExternalSort;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static Model.Parse.*;
import static Model.ReadFile.allDocsInCorpus;
import static Model.ReadFile.splitByDelimiter;

public class Indexer {

    private TreeMap<String, Term> termsDictionary;
    private HashMap<String, ArrayList<String>> postingToWrite;
    private boolean userChoseStemming;
    private String pathToWrite;
    private Vector<File> numbersPostingFiles;
    private Vector<File> upperCasePostingFiles;
    private Vector<File> lowerCasePostingFiles;
    private int PostingFilesChunkCount;
    ThreadPoolExecutor threadPoolExecutor;
    int termsDictFinalSize;
    private HashMap<String, EntityInfoInCorpus> entitiesDictTemp; // <entity , <docID,freqInDoc> >


    public Indexer(boolean useChoseStemming) {
        termsDictionary = new TreeMap<>(ALPHABETICAL_ORDER);
        postingToWrite = new HashMap<>();
        this.userChoseStemming = useChoseStemming;
        PostingFilesChunkCount = 0;
        numbersPostingFiles = new Vector<>();
        upperCasePostingFiles = new Vector<>();
        lowerCasePostingFiles = new Vector<>();
        int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        threadPoolExecutor.setCorePoolSize(threadPoolSize);
        termsDictFinalSize = 0;
        entitiesDictTemp = new HashMap<>();
    }

    public TreeMap<String, Term> getTermsDictionary() {
        return termsDictionary;
    }

    public void setTermsDictionary(TreeMap<String, Term> termsDictionary) {
        this.termsDictionary = termsDictionary;
    }

    public boolean isUserChoseStemming() {
        return userChoseStemming;
    }

    public void setUserChoseStemming(boolean userChoseStemming) {
        this.userChoseStemming = userChoseStemming;
    }

    /**
     * the main function that merges files and creating the dictionary and index files
     */
    public void generateInvertedIndex() {
        mergeSameFiles();

        Thread uniteUpperCaseToRightFile = new Thread(new Runnable() {
            @Override
            public void run() {
                uniteUpperCase();
            }
        });

        uniteUpperCaseToRightFile.start();

        Thread divideNumbers = new Thread(new Runnable() {
            @Override
            public void run() {
                divideNumbers0To9();
            }
        });

        divideNumbers.start();
        try {
            uniteUpperCaseToRightFile.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread divideToLetters = new Thread(new Runnable() {
            @Override
            public void run() {
                divideAtoZ();
            }
        });

        divideToLetters.start();
        try {
            divideNumbers.join();
            divideToLetters.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread generateDict = new Thread(new Runnable() {
            @Override
            public void run() {
                writeDictToDisk();
            }
        });

        generateDict.start();
        try {
            generateDict.join();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * writing the final dictionary to disk
     */
    private void writeDictToDisk() {
        try {
            File dictionaryFile = null;
            if (userChoseStemming) {
                dictionaryFile = new File(pathToWrite + "\\Dictionary_stemmed.txt");
            } else {
                dictionaryFile = new File(pathToWrite + "\\Dictionary.txt");
            }

            FileWriter writerDict = new FileWriter(dictionaryFile);
            dictionaryToDisk(writerDict);
            writerDict.flush();
            writerDict.close();
            termsDictFinalSize = termsDictionary.size();
            termsDictionary.clear();
        } catch (Exception e) {
        }


    }

    /**
     * write the dictionary to disk after processing the whole corpus
     *
     * @param writerDict
     */
    private void dictionaryToDisk(FileWriter writerDict) {
        File file = null;
        if (userChoseStemming) {
            file = new File(pathToWrite + "\\dictionaryToGUI_stemmed.txt");
        } else {
            file = new File(pathToWrite + "\\dictionaryToGUI.txt");
        }

        FileWriter writerDisplay = null;
        try {
            writerDisplay = new FileWriter(file);
            TreeMap<String, Term> map = new TreeMap<>(termsDictionary);
            for (Map.Entry<String, Term> entry : map.entrySet()) {
                if (entry.getValue().getPtrPosting() == -1) {
                    synchronized (termsDictionary) {
                        termsDictionary.remove(entry.getKey());
                    }
                    continue;
                }
                // term,number of appearance,line number
                writerDict.write(entry.getKey() + "|" + entry.getValue().getTermFreqInCorpus() + "|" + entry.getValue().getDocFreq() + "|" + entry.getValue().getPtrPosting() + "\n");
                writerDisplay.write(entry.getKey() + "|" + entry.getValue().getTermFreqInCorpus() + "\n");
            }
            writerDisplay.flush();
            writerDisplay.close();
        } catch (Exception e) {
        }

    }



    /**
     * dividing the final upper and loser files into new A-Z files which contain the relevant terms by first letter
     */
    private void divideAtoZ() {
        try {
            char c = 'A';
            File finalPostingLetter = null;
            FileWriter writer = null;
            long lineNumber = 1;

            File upperFile = new File(pathToWrite + "\\finalUnitedUpper.txt");
            File lowerFile = new File(pathToWrite + "\\finalUnitedLower.txt");
            BufferedReader upperReader = new BufferedReader(new FileReader((upperFile)));
            BufferedReader lowerReader = new BufferedReader(new FileReader((lowerFile)));

            if (!userChoseStemming) {
                finalPostingLetter = new File(pathToWrite + "\\finalPostingA.txt");
            } else {
                finalPostingLetter = new File(pathToWrite + "\\finalPostingA_stemmed.txt");
            }
            writer = new FileWriter(finalPostingLetter);

            String lineUpper = upperReader.readLine();
            String lineLower = lowerReader.readLine();

            while (true) {

                while (lineUpper != null && lineUpper.charAt(0) == Character.toUpperCase(c)) {
                    String key = lineUpper.substring(0, lineUpper.indexOf("|"));
                    synchronized (termsDictionary) {
                        if (!termsDictionary.containsKey(key)) {
                            lineUpper = upperReader.readLine();
                            continue;
                        }
                    }
                    //write the term to the relevant file and update its pointer
                    writer.write(lineUpper.substring(lineUpper.indexOf("|") + 1) + "\n");
                    synchronized (termsDictionary) {
                        if (termsDictionary.get(key) != null)
                            termsDictionary.get(key).setPtrPosting(lineNumber);
                    }
                    lineNumber++;
                    lineUpper = upperReader.readLine();
                }
                while (lineLower != null && Character.toUpperCase(lineLower.charAt(0)) == c) {
                    String key = lineLower.substring(0, lineLower.indexOf("|"));
                    synchronized (termsDictionary) {
                        if (!termsDictionary.containsKey(key)) {
                            lineLower = lowerReader.readLine();
                            continue;
                        }
                    }
                    //write the term to the relevant file and update its pointer
                    writer.write(lineLower.substring(lineLower.indexOf("|") + 1) + "\n");
                    synchronized (termsDictionary) {
                        if (termsDictionary.get(key) != null)
                            termsDictionary.get(key).setPtrPosting(lineNumber);
                    }
                    lineNumber++;
                    lineLower = lowerReader.readLine();
                }
                c++;
                lineNumber = 1;

                if (c != '[') { //end of abc
                    if (!userChoseStemming) {
                        finalPostingLetter = new File(pathToWrite + "\\finalPosting" + c + ".txt");
                    } else {
                        finalPostingLetter = new File(pathToWrite + "\\finalPosting" + c + "_stemmed.txt");
                    }

                    writer.flush();
                    writer.close();
                    writer = new FileWriter(finalPostingLetter);
                }

                if (c == '[')
                    break;
            }
            writer.flush();
            writer.close();
            lowerReader.close();
            upperReader.close();
            upperFile.delete();
            lowerFile.delete();


        } catch (Exception e) {

        }


    }

    /**
     * takes the united file and split it to files of 0-9 with relevant terms.
     */
    private void divideNumbers0To9() {
        try {
            char c = '0';
            FileWriter specialCharsWriter = null;
            FileWriter numbersWriter = null;
            File finalPostingNumber = null;
            File finalPostingSpecialChars = null;
            long lineNumber = 1;

            File allNumsFile = new File(pathToWrite + "\\unitedPostingNumbers.txt");
            BufferedReader unitedReader = new BufferedReader(new FileReader(allNumsFile));

            if (!userChoseStemming) {
                finalPostingNumber = new File(pathToWrite + "\\finalPostingNumber0.txt");
                finalPostingSpecialChars = new File(pathToWrite + "\\finalPostingSpecialChars.txt");
            } else {
                finalPostingNumber = new File(pathToWrite + "\\finalPostingNumber0_stemmed.txt");
                finalPostingSpecialChars = new File(pathToWrite + "\\finalPostingSpecialChars_stemmed.txt");
            }

            specialCharsWriter = new FileWriter(finalPostingSpecialChars);
            numbersWriter = new FileWriter(finalPostingNumber);


            String line = unitedReader.readLine();

            while (true) {
                //write the term to the relevant file and update its pointer
                while (line != null && !Character.isDigit(line.charAt(0))) {
                    specialCharsWriter.write(line.substring(line.indexOf("|") + 1) + "\n");
                    String key = line.substring(0, line.indexOf("|"));
                    synchronized (termsDictionary) {
                        if (termsDictionary.get(key) != null) {
                            termsDictionary.get(key).setPtrPosting(lineNumber);
                        }
                    }
                    lineNumber++;
                    line = unitedReader.readLine();
                }
                if (line != null && Character.isDigit(line.charAt(0))) {
                    break;
                }
            }
            specialCharsWriter.flush();
            specialCharsWriter.close();


            while (true) {
                //write the term to the relevant file and update its pointer
                while (line != null && line.charAt(0) == c) {
                    numbersWriter.write(line.substring(line.indexOf("|") + 1) + "\n");
                    String key = line.substring(0, line.indexOf("|"));
                    synchronized (termsDictionary) {
                        if (termsDictionary.get(key) != null) {
                            termsDictionary.get(key).setPtrPosting(lineNumber);
                        }
                    }
                    lineNumber++;
                    line = unitedReader.readLine();
                }
                c++;
                lineNumber = 1;
                if (c != ':') { //end of 0-9
                    if (!userChoseStemming) {
                        finalPostingNumber = new File(pathToWrite + "\\finalPostingNumber" + c + ".txt");
                    } else {
                        finalPostingNumber = new File(pathToWrite + "\\finalPostingNumber" + c + "_stemmed.txt");
                    }
                    numbersWriter.flush();
                    numbersWriter.close();
                    numbersWriter = new FileWriter(finalPostingNumber);
                }


                if (c == ':') {
                    break;
                }
            }
            numbersWriter.close();
            specialCharsWriter.close();
            unitedReader.close();
            allNumsFile.delete();
        } catch (Exception e) {
        }

    }




    /**
     * read the files, if we found a term which written in both upper and lower files, merge its data to the line in lower file
     * if not, just copy the line to final valid file
     */
    private void uniteUpperCase() {
        try {
            File finalUnitedLower = new File(pathToWrite + "\\finalUnitedLower.txt");
            File finalUnitedUpper = new File(pathToWrite + "\\finalUnitedUpper.txt");
            FileWriter writerLower = new FileWriter(finalUnitedLower);
            FileWriter writerUpper = new FileWriter(finalUnitedUpper);

            BufferedReader bfUpper = new BufferedReader(new FileReader(pathToWrite + "\\unitedPostingUpperCase.txt"));
            BufferedReader bfLower = new BufferedReader(new FileReader(pathToWrite + "\\unitedPostingLowerCase.txt"));
            String lineInUpper = bfUpper.readLine();
            String lineInLower = bfLower.readLine();

            while (lineInLower != null && lineInUpper != null) {
                String[] termAndPostingDataUpper = ReadFile.splitByDelimiter(lineInUpper, "|");
                String[] termAndPostingDataLower = ReadFile.splitByDelimiter(lineInLower, "|");
                //turn the term in upper to lower to check if its same. if not, write it by order in right file
                // if line in upperCase file is bigger than line in lowerCase file, read the next upper line
                if (termAndPostingDataUpper[0].toLowerCase().compareTo(termAndPostingDataLower[0]) < 0) {
                    writerUpper.write(lineInUpper + "\n");
                    lineInUpper = bfUpper.readLine();
                    continue;
                }
                // if line in upperCase file is smaller than line in lowerCase file, read the next lower line
                if (termAndPostingDataUpper[0].toLowerCase().compareTo(termAndPostingDataLower[0]) > 0) {
                    writerLower.write(lineInLower + "\n");
                    lineInLower = bfLower.readLine();
                    continue;
                }
                // if the term exist also in upper case, copy the content of the line to the new lower words posting file
                if (termAndPostingDataUpper[0].toLowerCase().equals(termAndPostingDataLower[0])) {
                    lineInUpper = lineInUpper.substring(termAndPostingDataUpper[0].indexOf("|") + 1);
                    writerLower.write(lineInLower + "|");
                    writerLower.write(lineInUpper.substring(lineInUpper.indexOf("|") + 1) + "\n");
                    synchronized (termsDictionary) {
                        if (termsDictionary.containsKey(termAndPostingDataUpper[0])) {
                            Term termUpper = termsDictionary.remove(termAndPostingDataUpper[0]);
                            Term termLower = termsDictionary.remove(termAndPostingDataLower[0]);
                            //update details for lower term
                            termsDictionary.put(termAndPostingDataLower[0], new Term(termAndPostingDataLower[0], termLower.getTermFreqInCorpus() + termUpper.getTermFreqInCorpus(),
                                    termLower.getDocFreq() + termUpper.getDocFreq(), -1));

                        }
                    }
                }
                lineInUpper = bfUpper.readLine();
                lineInLower = bfLower.readLine();
            }

            if (lineInLower == null && lineInUpper != null) {
                while (lineInUpper != null) {
                    writerUpper.write(lineInUpper + "\n");
                    lineInUpper = bfUpper.readLine();
                }
            }

            if (lineInLower != null && lineInUpper == null) {
                while (lineInLower != null) {
                    writerLower.write(lineInLower + "\n");
                    lineInLower = bfUpper.readLine();
                }
            }

            bfLower.close();
            bfUpper.close();
            writerLower.flush();
            writerUpper.flush();
            writerLower.close();
            writerUpper.close();
            File file = new File(pathToWrite + "\\unitedPostingUpperCase.txt");
            file.delete();
            file = new File(pathToWrite + "\\unitedPostingLowerCase.txt");
            file.delete();

        } catch (Exception e) {
        }


    }

    /**
     * as we wrote every amount of docs to disk, we have numbered the files by number of the chunk
     * this func will unite all files with same category(numbers,lower,upper) and different chunk number to single file
     */
    private void mergeSameFiles() {
        try {
            //unite same file with different chunk count
            File postingNumbersFile = new File(pathToWrite + "\\unitedPostingNumbersTemp.txt");
            File postingLowerFile = new File(pathToWrite + "\\unitedPostingLowerCaseTemp.txt");
            File postingUpperFile = new File(pathToWrite + "\\unitedPostingUpperCaseTemp.txt");

            Comparator<String> cmp = new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    String s1 = o1.substring(0, o1.indexOf("|"));
                    String s2 = o2.substring(0, o2.indexOf("|"));
                    if (s1.compareTo(s2) == 0) {
                        return o1.compareTo(o2);
                    }
                    return s1.compareTo(s2);
                }
            };
            //merge files by category (numbers, lower , upper) and do it simultaneously
            Thread t1 = new Thread(() -> {
                try {
                    ExternalSort.mergeSortedFiles(numbersPostingFiles, postingNumbersFile, cmp, Charset.defaultCharset(), false);
                } catch (Exception e) {
                }
            });
            Thread t2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ExternalSort.mergeSortedFiles(lowerCasePostingFiles, postingLowerFile, cmp, Charset.defaultCharset(), false);
                    } catch (Exception e) {
                    }
                }
            });
            Thread t3 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ExternalSort.mergeSortedFiles(upperCasePostingFiles, postingUpperFile, cmp, Charset.defaultCharset(), false);
                    } catch (Exception e) {
                    }
                }
            });
            t1.start();
            t2.start();
            t3.start();
            t1.join();
            t2.join();
            t3.join();

            //merge duplicate terms simultaneously and update several terms to one posting data
            threadPoolExecutor.execute(new concurrentMerging(postingNumbersFile, pathToWrite + "\\unitedPostingNumbers.txt"));
            threadPoolExecutor.execute(new concurrentMerging(postingLowerFile, pathToWrite + "\\unitedPostingLowerCase.txt"));
            threadPoolExecutor.execute(new concurrentMerging(postingUpperFile, pathToWrite + "\\unitedPostingUpperCase.txt"));
            threadPoolExecutor.shutdown();
            while (threadPoolExecutor.getActiveCount() != 0) ;
            int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
            this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            threadPoolExecutor.setCorePoolSize(threadPoolSize);

            postingNumbersFile.delete();
            postingLowerFile.delete();
            postingUpperFile.delete();
        } catch (Exception e) {
        }


    }


    /**
     * this class represents an object which is runnable
     * and for that we can run several objects in the same time
     * so we will fix duplicates lines and merge them to one posting data of the duplicated term
     */
    private class concurrentMerging implements Runnable {

        File file;
        String fileName;

        public concurrentMerging(File file, String fileName) {
            this.file = file;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            mergeDuplicateLinesInFile(file, fileName);
        }
    }


    /**
     * this func will go over a file and unite duplicate lines and write them to new file
     *
     * @param postingFileWithDuplicates
     * @param fileNameWithoutDuplicates
     */
    private void mergeDuplicateLinesInFile(File postingFileWithDuplicates, String fileNameWithoutDuplicates) {
        try {
            FileWriter writer = new FileWriter(new File(fileNameWithoutDuplicates));
            BufferedReader bf = new BufferedReader(new FileReader(postingFileWithDuplicates));
            String firstLine = bf.readLine(); // ACADEMY|FBIS3-14:1783#1|FBIS3-17:122,501#2|FBIS3-18:656,738,1060,1103#4
            String secondLine = bf.readLine();// ACADEMY|FBIS3-35:103,201,847,2181#4
            while (firstLine != null) {
                writer.append(firstLine);
                String[] firstLineSplit = ReadFile.splitByDelimiter(firstLine, "|");
                if (secondLine == null)
                    break;
                String[] secondLineSplit = ReadFile.splitByDelimiter(secondLine, "|");
                while (secondLine != null && firstLineSplit[0].equals(secondLineSplit[0])) {
                    writer.append("|" + secondLine.substring(secondLine.indexOf("|") + 1));
                    secondLine = bf.readLine();
                    if (secondLine != null)
                        secondLineSplit = ReadFile.splitByDelimiter(secondLine, "|");
                }
                writer.append("\n");
                firstLine = secondLine;
                secondLine = bf.readLine();
            }
            bf.close();
            writer.flush();
            writer.close();
        } catch (Exception e) {
        }

    }

    /**
     * comparator of terms, compare by Term Freq In Corpus
     */
    public static Comparator<Term> TermComparator = new Comparator<Term>() {
        public int compare(Term t1, Term t2) {
            int res = Integer.compare(t1.getTermFreqInCorpus(), t2.getTermFreqInCorpus());
            if (res == 0) {
                res = t1.getTermName().compareTo(t2.getTermName());
            }
            return res;
        }
    };

    public String getPathToWrite() {
        return pathToWrite;
    }

    public void setPathToWrite(String pathToWrite) {
        this.pathToWrite = pathToWrite;
    }

    public void clearDictAndEntities(){
        termsDictionary.clear();
        entitiesDictTemp.clear();
    }

    public boolean reset() {
        termsDictionary.clear();
        postingToWrite.clear();
        entitiesDictTemp.clear();
        userChoseStemming = false;
        numbersPostingFiles = new Vector<>();
        upperCasePostingFiles = new Vector<>();
        lowerCasePostingFiles = new Vector<>();
        PostingFilesChunkCount = 0;
        int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        threadPoolExecutor.setCorePoolSize(threadPoolSize);
        termsDictFinalSize = 0;
        return cleanPathToWriteDir();
    }

    /**
     * clean path of writing from existing files
     *
     * @return
     */
    private boolean cleanPathToWriteDir() {
        try {
            File folder = new File(pathToWrite);
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
                pathToWrite = null;
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * this func sends an ArrayList<String> of terms so it could been display in the GUI
     *
     * @return
     */
    public ArrayList<String> getTermsInDict() {
        ArrayList<String> terms = new ArrayList<>();
        File pathToWriteDir = null;
        if (userChoseStemming) {
            pathToWriteDir = new File(pathToWrite + "\\Dictionary_stemmed.txt");
        } else {
            pathToWriteDir = new File(pathToWrite + "\\Dictionary.txt");
        }

        if (termsDictionary.isEmpty()) {
            loadDictToMemo(userChoseStemming);
        }
        if (!termsDictionary.isEmpty()) {
            for (Map.Entry<String, Term> entry : termsDictionary.entrySet()) {
                terms.add(entry.getKey() + ", " + entry.getValue().getTermFreqInCorpus());
            }
        }


        return terms;
    }

    /**
     * loading existing file of dictionary to program
     *
     * @param selected
     * @return
     */
    public boolean loadDictToMemo(boolean selected) {
        userChoseStemming = selected;
        termsDictionary.clear();
        String line = null;
        BufferedReader reader = null;
        try {
            if (userChoseStemming) {
                reader = new BufferedReader(new FileReader(pathToWrite + "\\Dictionary_stemmed.txt"));
            } else {
                reader = new BufferedReader(new FileReader(pathToWrite + "\\Dictionary.txt"));
            }
            line = reader.readLine();
            while (line != null) {
                String[] details = splitByDelimiter(line, "|");
                termsDictionary.put(details[0]
                        , new Term(details[0], Integer.parseInt(details[1]), Integer.parseInt(details[2]), Integer.parseInt(details[3])));
                line = reader.readLine();
            }
            reader.close();
//            showTopBottomTen();
            return true;
        } catch (Exception e) {  }

        return false;

    }



    /**
     * print top and bottom terms in dictionary
     */
    private void showTopBottomTen() {
        int freq = 0;
        Pair<Integer, String> freqTerm = null;
        String term = "";
        ArrayList<Term> list = new ArrayList<Term>();

//        TreeMap<Integer, String> termsDict = new TreeMap<>(Collections.reverseOrder());
        File termsFile = new File(pathToWrite + "\\Dictionary.txt");

        Scanner scanner = null;
        try {
            scanner = new Scanner(termsFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (scanner.hasNextLine()) {
            String text = scanner.nextLine();
            String[] tokens = splitByDelimiter(text, "|");
            freq = Integer.parseInt(tokens[1]);
            term = tokens[0];
            list.add(new Term(term, freq, 0, 0));
//            termsDict.put(freq, term);
        }

        Collections.sort(list, TermComparator);

//        int i = 1;
//        for (Map.Entry<Integer, String> entry : termsDict.entrySet()) {
//            System.out.println(entry.getValue() + ", " + entry.getKey());
//            i++;
//            if (i == 20) {
//                break;
//            }
//        }

        for (int i = 0; i < 10; i++) {
            System.out.println(list.get(i).getTermName() + ", " + list.get(i).getTermFreqInCorpus());
        }

        File terms = null;
        File freqs = null;
        FileWriter termsWriter = null;
        FileWriter freqWriter = null;
        pathToWrite = System.getProperty("user.dir") + "\\posting";
        try {
            terms = new File(pathToWrite + "\\termsToExcel.txt");
            freqs = new File(pathToWrite + "\\freqsToExcel.txt");

            termsWriter = new FileWriter(terms);
            freqWriter = new FileWriter(freqs);

//            for (Map.Entry<Integer, String> entry : termsDict.entrySet()) {
//                termsWriter.write(entry.getKey()+"\n");
//                freqWriter.write(entry.getValue()+"\n");
//            }

            for (int i = 0; i < list.size(); i++) {
                termsWriter.write(list.get(i).getTermName() + "\n");
                freqWriter.write(list.get(i).getTermFreqInCorpus() + "\n");
            }

            termsWriter.flush();
            freqWriter.flush();
            termsWriter.close();
            freqWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    /**
     * every amount of docs that have been parsed we will update the dictionary with new terms and the postingToWrite
     * that holds extra data for the terms that eventually will be written to disk
     *
     * @param tmpPosting
     * @param docInParse
     * @param entitiesAndFreqInDoc
     */
    public void setStructures(Map<String, ArrayList<String>> tmpPosting, Doc docInParse, Map<String, Integer> entitiesAndFreqInDoc) {
        if(docInParse != null) {
            for (Map.Entry<String, ArrayList<String>> entry : tmpPosting.entrySet()) {
                //if term isnt in dict
                if (!stopWords.contains(entry.getKey())) {
                    if (!termsDictionary.containsKey(entry.getKey())) {
                        String term = entry.getKey();
                        int termFreq = 0;//
                        for (int i = 0; i < entry.getValue().size(); i++) {
                            termFreq += Integer.parseInt(entry.getValue().get(i).substring(entry.getValue().get(i).indexOf("#") + 1));
                        }
                        int docFreq = 1;
                        long pointer = -1;
                        termsDictionary.put(term, new Term(term, termFreq, docFreq, pointer));
                    } else { //if its in dict
                        int currTf = termsDictionary.get(entry.getKey()).getTermFreqInCorpus();
                        int add = 0;
                        for (int i = 0; i < entry.getValue().size(); i++) {
                            add += Integer.parseInt(entry.getValue().get(i).substring(entry.getValue().get(i).indexOf("#") + 1));
                        }
                        termsDictionary.get(entry.getKey()).setTermFreqInCorpus(currTf + add);
                        int currDf = termsDictionary.get(entry.getKey()).getDocFreq();
                        termsDictionary.get(entry.getKey()).setDocFreq(currDf + 1);
                    }
                }

                //if term isnt in postingToWrite
                if (!stopWords.contains(entry.getKey()) && !docInParse.getName().equals("none")) {
                    if (!postingToWrite.containsKey(entry.getKey())) {
                        postingToWrite.put(entry.getKey(), entry.getValue());
                    } else if (postingToWrite.containsKey(entry.getKey())) { // term is in postingToWrite
                        ArrayList<String> tmp = null;
                        tmp = postingToWrite.get(entry.getKey());
                        tmp.addAll(entry.getValue());
                        postingToWrite.replace(entry.getKey(), tmp);
                    }
                }
            }
            if (countParsedDocs == 2000 && !docInParse.getName().equals("none")) {
                writePostingDataToDisk();
                postingToWrite = new HashMap<>();
                countParsedDocs = 0;
            }

//            if (docInParse.getName().equals("FBIS3-3366")) {
//                TreeMap<String, ArrayList<String>> docTerms = new TreeMap<>(ALPHABETICAL_ORDER);
//                docTerms.putAll(tmpPosting);
//                for (Map.Entry<String, ArrayList<String>> entry : docTerms.entrySet()) {
//                    System.out.println(entry.getKey() + ", " + entry.getValue().get(0).substring(entry.getValue().get(0).indexOf("#") + 1));
//                }
//            }

///check doc is valid
            for (Map.Entry<String, Integer> entity : entitiesAndFreqInDoc.entrySet()) {
                if (!entitiesDictTemp.containsKey(entity.getKey())) {
                    EntityInfoInCorpus ent = new EntityInfoInCorpus();
                    ent.getDocList().put(docInParse, entity.getValue());
                    ent.getRankInDoc().put(docInParse , ((double)entity.getValue()) / ((double)docInParse.getLength() ) );
                    ent.setNumOfDiffDocs(1);
                    entitiesDictTemp.put(entity.getKey(), ent);
                } else {
                    entitiesDictTemp.get(entity.getKey()).getDocList().put(docInParse, entity.getValue());
                    entitiesDictTemp.get(entity.getKey()).setNumOfDiffDocs(entitiesDictTemp.get(entity.getKey()).getDocList().size());
                    entitiesDictTemp.get(entity.getKey()).getRankInDoc().put(docInParse , ((double)entity.getValue()) / ((double)docInParse.getLength() ) ); //insert tf and normalize by doc length
                }
            }

            tmpPostingTerms.clear();
        }

    }

    public void clearEntitiesDictionary() {
        for (Map.Entry<String, EntityInfoInCorpus> entity : entitiesDictTemp.entrySet()) {
            if (entity.getValue().getNumOfDiffDocs() != 1) {
                Set<Doc> tmp = entity.getValue().getDocList().keySet();
                for (Doc d : tmp) {
                    if (allDocsInCorpus.get(d) == null) {
                        String ent = entity.getKey();
                        double count = entity.getValue().getDocList().get(d);
                        allDocsInCorpus.put(d, new EntityInfoInDoc());
                        allDocsInCorpus.get(d).insert(ent, entity.getValue().getRankInDoc().get(d)*(Math.log((((double)468370)/entity.getValue().getDocList().size()))));
                    } else {
                        String ent = entity.getKey();
                        double count = entity.getValue().getDocList().get(d);
                        allDocsInCorpus.get(d).insert(ent, (double)entity.getValue().getRankInDoc().get(d)*(Math.log((((double)468370)/entity.getValue().getDocList().size())))); //tf*idf
                    }
                }
            }
        }
        DecimalFormat df = new DecimalFormat("#.###");
        Map<String , Double> top5 = new TreeMap<>();
        for(Map.Entry< Doc, EntityInfoInDoc> doc : allDocsInCorpus.entrySet()){
            if(doc.getValue()!=null){
                top5 = doc.getValue().getEntitiesAndFreqInDoc();
                top5 = sortByValues(top5);
                int i=0;
                for (Map.Entry< String, Double> entity : top5.entrySet()){
                    doc.getKey().insertToTop5(entity.getKey(), Double.parseDouble(df.format(entity.getValue())));
                    i++;
                    if(i==5) break;
                }
            }else{
                doc.getKey().insertToTop5("none",0);
            }

        }


        entitiesDictTemp.clear();
    }

    public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
        Comparator<K> valueComparator =  new Comparator<K>() {
            public int compare(K k1, K k2) {
                int compare = map.get(k2).compareTo(map.get(k1));
                if (compare == 0) return 1;
                else return compare;
            }
        };
        Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
        sortedByValues.putAll(map);
        return sortedByValues;
    }

    /**
     * writing data to disk every 2000 docs
     */
    public void writePostingDataToDisk() {
        TreeMap<String, ArrayList<String>> sortedPosting = new TreeMap<>(postingToWrite);
        try {

            File numbersFile = new File(pathToWrite + "\\termPosting_" + PostingFilesChunkCount + "_numbers.txt");
            File upperFile = new File(pathToWrite + "\\termPosting_" + PostingFilesChunkCount + "_upper.txt");
            File lowerFile = new File(pathToWrite + "\\termPosting_" + PostingFilesChunkCount + "_lower.txt");
            FileWriter writerToNumbers = new FileWriter(numbersFile);
            FileWriter writerToUpper = new FileWriter(upperFile);
            FileWriter writerToLower = new FileWriter(lowerFile);
            numbersPostingFiles.add(numbersFile);
            upperCasePostingFiles.add(upperFile);
            lowerCasePostingFiles.add(lowerFile);

            for (Map.Entry<String, ArrayList<String>> entry : sortedPosting.entrySet()) {
                if (Character.isUpperCase(entry.getKey().charAt(0))) {
                    writerToUpper.write(entry.getKey() + "|");
                    for (int i = 0; i < entry.getValue().size(); i++) {
                        if (i == entry.getValue().size() - 1) {
                            writerToUpper.write(entry.getValue().get(i) + "\n");
                            break;
                        }
                        writerToUpper.write(entry.getValue().get(i) + "|");
                    }
                } else if (Character.isLowerCase(entry.getKey().charAt(0))) {
                    writerToLower.write(entry.getKey() + "|");
                    for (int i = 0; i < entry.getValue().size(); i++) {
                        if (i == entry.getValue().size() - 1) {
                            writerToLower.write(entry.getValue().get(i) + "\n");
                            break;
                        }
                        writerToLower.write(entry.getValue().get(i) + "|");
                    }
                } else {
                    writerToNumbers.write(entry.getKey() + "|");
                    for (int i = 0; i < entry.getValue().size(); i++) {
                        if (i == entry.getValue().size() - 1) {
                            writerToNumbers.write(entry.getValue().get(i) + "\n");
                            break;
                        }
                        writerToNumbers.write(entry.getValue().get(i) + "|");
                    }
                }
            }

            writerToNumbers.flush();
            writerToUpper.flush();
            writerToLower.flush();
            writerToNumbers.close();
            writerToUpper.close();
            writerToLower.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        PostingFilesChunkCount++;
    }

    public void writeDocumentsInfoToDisk() {
        File docFile = null;
        try{
            if(!userChoseStemming){
                docFile = new File(pathToWrite + "\\DocumentsDetails.txt");
            }
            else
                docFile = new File(pathToWrite + "\\DocumentsDetails_stemmed.txt");

            FileWriter docWriter = new FileWriter(docFile);
            StringBuilder str = new StringBuilder();
            for (Map.Entry<Doc, EntityInfoInDoc> entry : allDocsInCorpus.entrySet()) {
                str.setLength(0);
                str.append(entry.getKey().getName()).append("|").append(entry.getKey().getTitle()).append("|").append(entry.getKey().getDate()).append("|")
                        .append(entry.getKey().getCity()).append("|").append(entry.getKey().getLanguage()).append("|").append(entry.getKey().getLength()).append("|")
                        .append(entry.getKey().getMaxFrequency()).append("|").append(entry.getKey().getNumOfUniqueTerms()).append("|")
                        .append(entry.getKey().getTopFiveEntitiesString());
                docWriter.write(str.toString()+"\n");
            }

            docWriter.flush();
            docWriter.close();

        }catch (Exception e ){e.printStackTrace();}

        allDocsInCorpus.clear();

    }

    public int getTermsDictFinalSize() {
        return termsDictFinalSize;
    }

    public void setTermsDictFinalSize(int termsDictFinalSize) {
        this.termsDictFinalSize = termsDictFinalSize;
    }

    public void setPostingFilesChunkCount(int postingFilesChunkCount) {
        PostingFilesChunkCount = postingFilesChunkCount;
    }

    public void clearLeftOvers() {
        postingToWrite.clear();
    }
}
