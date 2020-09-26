package Model;


import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

public class Parse {


    public static Set<String> stopWords; // holds the stop words that we won't parse in parse func
    private Doc docInParse; // current doc in parsing process
    private int positionOnIndex; //counts the index only on tokens and not all over the text
    private Stemmer stemmer; //in case we need to use it
    private Map<String, String> calender; //contains all months with matching numbers for parse
    public static Map<String, ArrayList<String>> tmpPostingTerms; //mapping lowerCase to lowerCase/upperCase
    private boolean userChoseStemming; //indicates if we need to use stemming
    public String pathToWrite; //path from user where to store posting files
    private String pathOfCorpusAndStopWords; // path to corpus and stop words to read from and write to
    public static int countParsedDocs; //count parsed docs so we would know when to write to disk
    private Indexer indexer;
    public Map<String, Integer> entitiesAndFreqInDoc = new HashMap<>(); //String for Term, Double for importance

    public static Comparator<String> ALPHABETICAL_ORDER = new Comparator<String>() {
        public int compare(String str1, String str2) {
            int res = String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
            if (res == 0) {
                res = str1.compareTo(str2);
            }
            return res;
        }
    };

    public Parse() {
        stopWords = new HashSet<>();
        tmpPostingTerms = new HashMap<>();
        stemmer = new Stemmer();
        userChoseStemming = false;
        countParsedDocs = 0;
        calender = new HashMap<String, String>() {{
            put("Jan", "01");
            put("Feb", "02");
            put("Mar", "03");
            put("Apr", "04");
            put("May", "05");
            put("Jun", "06");
            put("Jul", "07");
            put("Aug", "08");
            put("Sep", "09");
            put("Oct", "10");
            put("Nov", "11");
            put("Dec", "12");
            put("January", "01");
            put("February", "02");
            put("March", "03");
            put("April", "04");
            put("June", "06");
            put("July", "07");
            put("August", "08");
            put("September", "09");
            put("October", "10");
            put("November", "11");
            put("December", "12");
            put("JANUARY", "01");
            put("FEBRUARY", "02");
            put("MARCH", "03");
            put("APRIL", "04");
            put("MAY", "05");
            put("JUNE", "06");
            put("JULY", "07");
            put("AUGUST", "08");
            put("SEPTEMBER", "09");
            put("OCTOBER", "10");
            put("NOVEMBER", "11");
            put("DECEMBER", "12");
        }};
    }

    /**
     * get a path from user and import the stop words list.
     *
     * @param path
     */
    public void importStopWords(String path) {
        File f = new File(path + "\\stop_words.txt");
        Scanner scanner = null;
        try {
            scanner = new Scanner(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (scanner != null)
            stopWords = new HashSet<>();
        while (scanner.hasNextLine())
            // add each stop words into the hash set
            stopWords.add(scanner.nextLine());
        scanner.close();
    }

    public Indexer getIndexer() {
        return indexer;
    }

    public void setIndexer(Indexer indexer) {
        this.indexer = indexer;
    }

    /**
     * get a text from the readFile class to parse and create termPosting Files to help construct the TermDictionary
     * parsing execute by the rules that were given and few extra rules we added
     *
     * @param text
     * @param docInParse
     */
    public void parse(String text, Doc docInParse) {
        String[] tokens = text.split("\\?|\\||]|\\[|- |\"|''|--|\\*|=|:|\\+| |\\|\n|#|@|\t|;|\\(|\\{|}|\\)");
        int indexInText = 0;
        positionOnIndex = 0;
        String cleanToken = "";
        String currToken = "";
        String followToken = "";
        String secondFollowToken = "";
        String thirdFollowToken = "";
        DecimalFormat df = new DecimalFormat("#.###");
        String currTokenEnt = "";
        String followTokenEnt = "";
        String secondFollowTokenEnt = "";
        String thirdFollowTokenEnt = "";


        //loop on tokens and parse each one
        while (indexInText < tokens.length) {
            currToken = cutDelimiters(tokens[indexInText]);
            boolean btwAnd = false;
            if(indexInText+2 < tokens.length) btwAnd = ((tokens[indexInText].equals("between") || tokens[indexInText].equals("Between")) && (tokens[indexInText + 2].equals("and") || tokens[indexInText + 2].equals("And"))); //let between __ and __ to be parsed
            if (isVerifiedToken(currToken)) { //check for valid token
                if (stopWords.contains(currToken.toLowerCase()) && !btwAnd) { //check if token is stop_word
                    positionOnIndex++;
                    indexInText++;
                    continue;
                } else { //not stop_word

                    if (currToken.equals("cvj") || currToken.equals("chj") || currToken.startsWith(">") || currToken.startsWith("<") || currToken.endsWith("<") || currToken.endsWith(">") || (currToken.length() <= 1 && !Character.isDigit(currToken.charAt(0)))) {
                        indexInText++;
                        continue;
                    }

                    //look for the next 3 tokens to figure how to classify this token
                    if (indexInText + 1 < tokens.length && isVerifiedToken(cutDelimiters(tokens[indexInText + 1]))) {
                        followToken = cutDelimiters(tokens[indexInText + 1]);
                    }
                    if (indexInText + 2 < tokens.length && isVerifiedToken(cutDelimiters(tokens[indexInText + 2]))) {
                        secondFollowToken = cutDelimiters(tokens[indexInText + 2]);
                    }
                    if (indexInText + 3 < tokens.length && isVerifiedToken(cutDelimiters(tokens[indexInText + 3]))) {
                        thirdFollowToken = cutDelimiters(tokens[indexInText + 3]);
                    }

                    //now that we have 4 tokens we can interpret the relevant rule

                    // 1. word-word , 2.word-word-word, 3. number-word or word-number , 4. number-number
                    if (currToken.contains("-")) {
                        cleanToken = currToken;
                        if (Character.isLetter(currToken.charAt(0)) && Character.isUpperCase(currToken.charAt(0))) {
                            cleanToken = currToken.toUpperCase(); // as mentioned in Upper/Lower letters part
                        } else {
                            cleanToken = currToken.toLowerCase();
                        }
                    }
                    // ~Percentages
                    // 1. Number%
                    else if (currToken.endsWith("%") && isNumber(currToken.substring(0, currToken.length() - 1))) {
                        cleanToken = makeNumberWithNoComma(currToken.substring(0, currToken.length() - 1)) + '%';
                    }
                    // 2.Number percent || Number percentage
                    else if (isNumber(currToken) && isWrittenWithPercentage(followToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + "%";
                        indexInText++;
                    }
                    // optional - Number fracture percentage
                    else if (isNumber(currToken) && isFracture(followToken) && isWrittenWithPercentage(secondFollowToken)) {
                        currToken = makeNumberWithNoComma(currToken) + " " + followToken + " " + secondFollowToken;
                        indexInText += 2;
                    }

                    // ~Prices

                    //smaller than 1 Million Dollar
                    // 1.Price Dollars
                    else if (isNumber(currToken) && isSmallerThanMillion(currToken) && isWrittenWithDollar(followToken)) {
                        cleanToken = currToken + " Dollars";
                        indexInText++;
                    }
                    // 2.Price fraction Dollars
                    else if (isNumber(currToken) && isSmallerThanMillion(currToken) && isFracture(followToken) && isWrittenWithDollar(secondFollowToken)) {
                        cleanToken = currToken + " " + followToken + " Dollars";
                        indexInText += 2;
                    }

                    // bigger than 1 Million Dollar

                    // 1.Price Dollars
                    else if (isNumber(currToken) && !isSmallerThanMillion(currToken) && isWrittenWithDollar(followToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken));
                        if (number % 1000000 == 0)
                            cleanToken = String.valueOf((int) (number / 1000000)) + " M Dollars";
                        else
                            cleanToken = String.valueOf(df.format(number / 1000000)) + " M Dollars";
                        indexInText++;
                    }
                    // 3. $price million
                    else if (currToken.startsWith("$") && isNumber(currToken.substring(1)) && isWrittenWithMillion(followToken)) {
                        cleanToken = makeNumberWithNoComma(currToken.substring(1)) + " M Dollars";
                        indexInText++;
                    }
                    // 4. $price billion
                    else if (currToken.startsWith("$") && isNumber(currToken.substring(1)) && isWrittenWithBillion(followToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken.substring(1)));
                        number = number * 1000;
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            cleanToken = String.valueOf(df.format(number)) + " M Dollars";
                        indexInText++;
                    }
                    // optional - $price trillion
                    else if (currToken.startsWith("$") && isNumber(currToken.substring(1)) && isWrittenWithTrillion(followToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken.substring(1)));
                        number = number * 1000000;
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            cleanToken = String.valueOf(df.format(number)) + " M Dollars";
                        indexInText++;
                    }

                    // 2. $price
                    else if (currToken.startsWith("$") && isNumber(currToken.substring(1)) && !isSmallerThanMillion(currToken.substring(1))
                            && !isWrittenWithMillion(followToken) && !isWrittenWithBillion(followToken) && !isWrittenWithTrillion(followToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken.substring(1)));
                        if (number % 1000000 == 0)
                            cleanToken = String.valueOf((int) (number / 1000000)) + " M Dollars";
                        else
                            cleanToken = String.valueOf(df.format(number / 1000000)) + " M Dollars";
                    } else if (currToken.startsWith("$") && isNumber(currToken.substring(1)) && isSmallerThanMillion(currToken.substring(1))
                            && !isWrittenWithMillion(followToken) && !isWrittenWithBillion(followToken) && !isWrittenWithTrillion(followToken)) {
                        cleanToken = currToken.substring(1) + " Dollars";
                    }

                    //numberM Dollars
                    else if (currToken.contains("m") && isNumber(currToken.substring(0, currToken.indexOf("m"))) && currToken.substring(currToken.length() - 1).equals("m") && isWrittenWithDollar(followToken)) {
                        cleanToken = makeNumberWithNoComma(currToken.substring(0, currToken.indexOf("m"))) + " M Dollars";
                        indexInText += 1;
                    }
                    //numberBN Dollars
                    else if (currToken.contains("bn") && isNumber(currToken.substring(0, currToken.indexOf("bn"))) && currToken.substring(currToken.length() - 2).equals("bn") && isWrittenWithDollar(followToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken.substring(0, currToken.indexOf("bn"))));
                        number = number * 1000;
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            cleanToken = String.valueOf(df.format(number)) + " M Dollars";
                        indexInText += 1;
                    }

                    // 5. Price M Dollars
                    else if (isNumber(currToken) && isWrittenWithMillion(followToken) && isWrittenWithDollar(secondFollowToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + " M Dollars";
                        indexInText += 2;
                    }
                    // 6. Price B Dollars
                    else if (isNumber(currToken) && isWrittenWithBillion(followToken) && isWrittenWithDollar(secondFollowToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken));
                        number = number * 1000;
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            cleanToken = String.valueOf(df.format(number)) + " M Dollars";
                        indexInText += 2;
                    }
                    // optional - Price trillion Dollars
                    else if (isNumber(currToken) && isWrittenWithTrillion(followToken) && isWrittenWithDollar(secondFollowToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken));
                        number = number * 1000000;
                        //(number-(int)number)- get the decimal part
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            cleanToken = String.valueOf(df.format(number)) + " M Dollars";
                        indexInText += 2;

                    }
                    // 7. Price million U.S. Dollars
                    else if (isNumber(currToken) && isWrittenWithMillion(followToken) && isWrittenWithDollar(secondFollowToken + " " + thirdFollowToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + " M Dollars";
                        indexInText += 3;
                    }
                    // 8. Price billion U.S. Dollars
                    else if (isNumber(currToken) && isWrittenWithBillion(followToken) && isWrittenWithDollar(secondFollowToken + " " + thirdFollowToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken));
                        number = number * 1000;
                        //(number-(int)number)- get the decimal part
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            cleanToken = String.valueOf(df.format(number)) + " M Dollars";
                        indexInText += 3;
                    }
                    // 9. Price trillion U.S. Dollars
                    else if (isNumber(currToken) && isWrittenWithTrillion(followToken) && isWrittenWithDollar(secondFollowToken + " " + thirdFollowToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken));
                        number = number * 1000000;
                        //(number-(int)number)- get the decimal part
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            cleanToken = String.valueOf(df.format(number)) + " M Dollars";
                        indexInText += 3;
                    }

                    // ~Dates
                    // MM-DD
                    // 1. DD Month
                    else if (!currToken.contains("-") && calender.containsKey(followToken) && isNumber(currToken) && !currToken.contains(".") && !currToken.contains(",") && Integer.parseInt(currToken) >= 1 && Integer.parseInt(currToken) <= 31) {
                        if (currToken.length() == 1)
                            currToken = "0" + currToken;
                        cleanToken = calender.get(followToken) + "-" + currToken;
                        indexInText++;
                    }
                    // 2. Month DD
                    else if (!followToken.contains("-") && calender.containsKey(currToken) && isNumber(followToken) && !followToken.contains(".") && !followToken.contains(",") && Integer.parseInt(followToken) >= 1 && Integer.parseInt(followToken) <= 31) {
                        if (followToken.length() == 1)
                            followToken = "0" + followToken;
                        cleanToken = calender.get(currToken) + "-" + followToken;
                        indexInText++;
                    }

                    // YYYY-MM
                    // 3. yyyy month
                    else if (calender.containsKey(followToken) && isNumber(currToken) && !currToken.contains(".") && !currToken.contains(",") && currToken.length() == 4) {
                        cleanToken = currToken + "-" + calender.get(followToken);
                        indexInText++;
                    }
                    // 4. month yyyy
                    else if (calender.containsKey(currToken) && isNumber(followToken) && !followToken.contains(".") && !followToken.contains(",") && followToken.length() == 4) {
                        cleanToken = followToken + "-" + calender.get(currToken);
                        indexInText++;
                    }

                    // ~Ranges
                    // 5.between number and number
                    else if ((currToken.equals("between") || (currToken.equals("Between"))) && isNumber(followToken)
                            && ((secondFollowToken.equals("And") || secondFollowToken.equals("and")) && isNumber(thirdFollowToken))) {
                        cleanToken = followToken + "-" + thirdFollowToken;
                        indexInText += 3;
                    }

                    // rules of our own:

                    //1.Kilograms

                    //Number kilogram
                    else if (isNumber(currToken) && isWrittenWithKG(followToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + " kg";
                        indexInText++;
                    }
                    //Number million kilogram
                    else if (isNumber(currToken) && isWrittenWithMillion(followToken) && isWrittenWithKG(secondFollowToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + "M kg";
                        indexInText += 2;
                    }
                    //Number billion kilogram
                    else if (isNumber(currToken) && isWrittenWithBillion(followToken) && isWrittenWithKG(secondFollowToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + "B kg";
                        indexInText += 2;
                    }
                    //Number trillion kilogram
                    else if (isNumber(currToken) && isWrittenWithTrillion(followToken) && isWrittenWithKG(secondFollowToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken)) * 1000;
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + "B kg";
                        else
                            cleanToken = String.valueOf(number) + "B kg";
                        indexInText += 2;
                    }

                    //2.Pounds

                    //Number pound
                    else if (isNumber(currToken) && isWrittenWithPounds(followToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + " lbs";
                        indexInText++;
                    }
                    //Number million pound
                    else if (isNumber(currToken) && isWrittenWithMillion(followToken) && isWrittenWithPounds(secondFollowToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + "M lbs";
                        indexInText += 2;
                    }
                    //Number billion pound
                    else if (isNumber(currToken) && isWrittenWithBillion(followToken) && isWrittenWithPounds(secondFollowToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + "B lbs";
                        indexInText += 2;
                    }
                    //Number trillion pound
                    else if (isNumber(currToken) && isWrittenWithTrillion(followToken) && isWrittenWithPounds(secondFollowToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken)) * 1000;
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + "B lbs";
                        else
                            cleanToken = String.valueOf(number) + "B lbs";
                        indexInText += 2;
                    }

                    //3.Kilometers

                    //Number Kilometers
                    else if (isNumber(currToken) && isWrittenWithPounds(followToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + " lbs";
                        indexInText++;
                    }
                    //Number million Kilometers
                    else if (isNumber(currToken) && isWrittenWithMillion(followToken) && isWrittenWithKM(secondFollowToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + "M km";
                        indexInText += 2;
                    }
                    //Number billion Kilometers
                    else if (isNumber(currToken) && isWrittenWithBillion(followToken) && isWrittenWithKM(secondFollowToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + "B km";
                        indexInText += 2;
                    }
                    //Number trillion Kilometers
                    else if (isNumber(currToken) && isWrittenWithTrillion(followToken) && isWrittenWithKM(secondFollowToken)) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken)) * 1000;
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + "B km";
                        else
                            cleanToken = String.valueOf(number) + "B km";
                        indexInText += 2;
                    }
                    // ~Numbers
                    // 1.Numbers above 1,000

                    // a.Numbers between 1,000 to 1,000,000

                    //Number in form of xx,xxx or xx,xxx.xx or xxxx or xxxx.xx

                    else if (isNumber(currToken) && Float.parseFloat(makeNumberWithNoComma(currToken)) >= 1000 && Float.parseFloat(makeNumberWithNoComma(currToken)) < 1000000) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken));
                        number = number / 1000;
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + "K";
                        else
                            cleanToken = String.valueOf(df.format(number)) + "K";
                    }

                    // Number in form of xxx thousand or xxx.xx thousand

                    else if (isNumber(currToken) && isWrittenWithThousand(followToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + "K";
                        indexInText++;
                    }

                    // b.Numbers between 1,000,000 to 1,000,000,000

                    //Number in form of xx,xxx,xxx or xx,xxx,xxx.xx or xxxxxxx or xxxxxxx.xx

                    else if (isNumber(currToken) && Float.parseFloat(makeNumberWithNoComma(currToken)) >= 1000000 && Float.parseFloat(makeNumberWithNoComma(currToken)) < 1000000000) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken));
                        number = number / 1000000;
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + "M";
                        else
                            cleanToken = String.valueOf(df.format(number)) + "M";
                    }

                    // Number in form of xxx million or xxx.xx million

                    else if (isNumber(currToken) && isWrittenWithMillion(followToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + "M";
                        indexInText++;
                    }

                    // c. Numbers bigger than 1,000,000,000

                    //Number in form of xx,xxx,xxx,xxx or xx,xxx,xxx,xxx.xx or xxxxxxxxxx or xxxxxxxxxx.xx

                    else if (isNumber(currToken) && Float.parseFloat(makeNumberWithNoComma(currToken)) >= 1000000000) {
                        float number = Float.parseFloat(makeNumberWithNoComma(currToken));
                        number = number / 1000000000;
                        if ((number - (int) number) == 0)
                            cleanToken = String.valueOf(((int) number)) + "B";
                        else
                            cleanToken = String.valueOf(df.format(number)) + "B";
                    }

                    // Number in form of xxx billion or xxx.xx billion

                    else if (isNumber(currToken) && isWrittenWithBillion(followToken)) {
                        cleanToken = makeNumberWithNoComma(currToken) + "B";
                        indexInText++;
                    }

                    // 2.Numbers under 1000

                    //Number with fracture

                    else if (isNumber(currToken) && Float.parseFloat(makeNumberWithNoComma(currToken)) >= 0 && Float.parseFloat(makeNumberWithNoComma(currToken)) < 1000 && isFracture(followToken)
                            && !isWrittenWithPounds(secondFollowToken) && !isWrittenWithDollar(secondFollowToken) && !isWrittenWithPercentage(secondFollowToken) &&
                            !isWrittenWithKG(secondFollowToken) && !isWrittenWithKM(secondFollowToken)) {
                        cleanToken = currToken + " " + followToken;
                        indexInText++;
                    }

                    //Number in regular form

                    else if (isNumber(currToken) && Float.parseFloat(makeNumberWithNoComma(currToken)) >= 0 && Float.parseFloat(makeNumberWithNoComma(currToken)) < 1000 && !calender.containsKey(followToken)
                            && !isWrittenWithPounds(followToken) && !isWrittenWithDollar(followToken) && !isWrittenWithPercentage(followToken) &&
                            !isWrittenWithKG(followToken) && !isWrittenWithKM(followToken)
                            && !isWrittenWithPounds(secondFollowToken) && !isWrittenWithDollar(secondFollowToken) && !isWrittenWithPercentage(secondFollowToken) &&
                            !isWrittenWithKG(secondFollowToken) && !isWrittenWithKM(secondFollowToken)) {
                        cleanToken = currToken;
                    } else {
                        //stem if asked for
                        if (userChoseStemming) {
                            stemmer.setTerm(currToken);
                            stemmer.stem();
                            currToken = stemmer.getTerm();
                        }
                        cleanToken = setLowerOrUpperPosting(currToken);//set the token according to rules of Upper/Lower Case
                    }

                }
//                if (indexInText + 3 == tokens.length) {
//                    cleanToken = cutDelimiters(tokens[indexInText]);
//                }

                if (isVerifiedToken(cleanToken) && !stopWords.contains(cleanToken)) {
                    if (!followToken.isEmpty() && stopWords.contains(cleanToken.toLowerCase()) && Character.isLowerCase(followToken.charAt(0))) {
                        indexInText++;
                        continue;
                    }

                    addTermToTmpPosting(cleanToken, docInParse); //add term to posting file
                    positionOnIndex++; //increase position on valid tokens only

                    if (isAllLetters(currToken) && isAllLetters(followToken) && isAllLetters(secondFollowToken) && isAllLetters(thirdFollowToken)) {
                        currTokenEnt = tokens[indexInText];
                        //look for the next 3 tokens to figure how to classify this token
                            if (indexInText + 1 < tokens.length && isVerifiedToken(tokens[indexInText + 1])) {
                                followTokenEnt = tokens[indexInText + 1];
                            }
                            if (indexInText + 2 < tokens.length && isVerifiedToken(tokens[indexInText + 2])) {
                                secondFollowTokenEnt = tokens[indexInText + 2];
                            }
                            if (indexInText + 3 < tokens.length && isVerifiedToken(tokens[indexInText + 3])) {
                                thirdFollowTokenEnt = tokens[indexInText + 3];
                            }

                            checkIfEntityAndUpdateFreq(currTokenEnt, followTokenEnt, secondFollowTokenEnt, thirdFollowTokenEnt, entitiesAndFreqInDoc);

                            currTokenEnt = "";
                            followTokenEnt = "";
                            secondFollowTokenEnt = "";
                            thirdFollowTokenEnt = "";
                    } else if (isAllLetters(currToken) && isAllLetters(followToken) && isAllLetters(secondFollowToken)) {
                        currTokenEnt = tokens[indexInText];
                        //look for the next 3 tokens to figure how to classify this token
                            if (indexInText + 1 < tokens.length && isVerifiedToken(tokens[indexInText + 1])) {
                                followTokenEnt = tokens[indexInText + 1];
                            }
                            if (indexInText + 2 < tokens.length && isVerifiedToken(tokens[indexInText + 2])) {
                                secondFollowTokenEnt = tokens[indexInText + 2];
                            }

                            checkIfEntityAndUpdateFreq(currTokenEnt, followTokenEnt, secondFollowTokenEnt, "", entitiesAndFreqInDoc);

                            currTokenEnt = "";
                            followTokenEnt = "";
                            secondFollowTokenEnt = "";
                            thirdFollowTokenEnt = "";

                    } else if (isAllLetters(currToken) && isAllLetters(followToken)) {
                        currTokenEnt = tokens[indexInText];
                        //look for the next 3 tokens to figure how to classify this token
                            if (indexInText + 1 < tokens.length &&  isVerifiedToken(tokens[indexInText + 1])) {
                                followTokenEnt = tokens[indexInText + 1];
                            }

                            checkIfEntityAndUpdateFreq(currTokenEnt, followTokenEnt, "", "", entitiesAndFreqInDoc);

                            currTokenEnt = "";
                            followTokenEnt = "";
                            secondFollowTokenEnt = "";
                            thirdFollowTokenEnt = "";

                    }
                }
                cleanToken = "";
                currToken = "";
                followToken = "";
                secondFollowToken = "";
                thirdFollowToken = "";
            }
            indexInText++;

        }
        countParsedDocs++;
        indexer.setStructures(tmpPostingTerms, docInParse, entitiesAndFreqInDoc);
        entitiesAndFreqInDoc.clear();


//        docInParse.setEntitiesList(entitiesAndFreqInDoc); //update entities list for the doc that done with parsing
    }

    public static boolean isAllLetters(String name) {
        char[] chars = name.toCharArray();
        for (char c : chars) {
            if (!(c >= 65 && c <= 90) && !(c >= 97 && c <= 122)) {
                return false;
            }
        }
        return true;
    }

    private void checkIfEntityAndUpdateFreq(String currTokenEnt, String followTokenEnt, String secondFollowTokenEnt, String thirdFollowTokenEnt, Map<String, Integer> entitiesInDoc) {
        StringBuilder entity = new StringBuilder();
        if (isVerifiedToken(currTokenEnt) && Character.isUpperCase(currTokenEnt.charAt(0)) &&
                !(currTokenEnt.endsWith(".") || currTokenEnt.endsWith(","))) {

            if (isVerifiedToken(followTokenEnt) && Character.isUpperCase(followTokenEnt.charAt(0))) {
                if (followTokenEnt.endsWith(".") || followTokenEnt.endsWith(",")) {
                    entity.append(currTokenEnt.toUpperCase()).append(" ").append(cutDelimiters(followTokenEnt).toUpperCase());
                    if (entitiesInDoc.containsKey(entity.toString())) {
                        int frq = entitiesInDoc.get(entity.toString());
                        entitiesInDoc.replace(entity.toString(), frq + 1);
                    } else
                        entitiesInDoc.put(entity.toString(), 1);
                } else if (!isVerifiedToken(followTokenEnt) || stopWords.contains(followTokenEnt.toLowerCase())) {
                    return;
                } else {
                    if (isVerifiedToken(secondFollowTokenEnt) && Character.isUpperCase(secondFollowTokenEnt.charAt(0))) {
                        if (secondFollowTokenEnt.endsWith(".") || secondFollowTokenEnt.endsWith(",")) {
                            entity.append(currTokenEnt.toUpperCase()).append(" ").append(followTokenEnt.toUpperCase()).append(" ").append(cutDelimiters(secondFollowTokenEnt).toUpperCase());
                            if (entitiesInDoc.containsKey(entity.toString())) {
                                int frq = entitiesInDoc.get(entity.toString());
                                entitiesInDoc.replace(entity.toString(), frq + 1);
                            } else
                                entitiesInDoc.put(entity.toString(), 1);
                        } else if (!isVerifiedToken(secondFollowTokenEnt) || stopWords.contains(followTokenEnt.toLowerCase())) {
                            entity.append(currTokenEnt.toUpperCase()).append(" ").append(followTokenEnt.toUpperCase());
                            if (entitiesInDoc.containsKey(entity.toString())) {
                                int frq = entitiesInDoc.get(entity.toString());
                                entitiesInDoc.replace(entity.toString(), frq + 1);
                            } else
                                entitiesInDoc.put(entity.toString(), 1);
                        } else {
                            if (isVerifiedToken(thirdFollowTokenEnt) && Character.isUpperCase(thirdFollowTokenEnt.charAt(0))) {
                                if (thirdFollowTokenEnt.endsWith(".") || thirdFollowTokenEnt.endsWith(",")) {
                                    entity.append(currTokenEnt.toUpperCase()).append(" ").append(followTokenEnt.toUpperCase()).append(" ").append(secondFollowTokenEnt.toUpperCase()).append(" ").append(cutDelimiters(thirdFollowTokenEnt).toUpperCase());
                                    if (entitiesInDoc.containsKey(entity.toString())) {
                                        int frq = entitiesInDoc.get(entity.toString());
                                        entitiesInDoc.replace(entity.toString(), frq + 1);
                                    } else
                                        entitiesInDoc.put(entity.toString(), 1);
                                }
                            } else if (!isVerifiedToken(thirdFollowTokenEnt) || stopWords.contains(thirdFollowTokenEnt.toLowerCase())) {
                                entity.append(currTokenEnt.toUpperCase()).append(" ").append(followTokenEnt.toUpperCase()).append(" ").append(secondFollowTokenEnt.toUpperCase());
                                if (entitiesInDoc.containsKey(entity.toString())) {
                                    int frq = entitiesInDoc.get(entity.toString());
                                    entitiesInDoc.replace(entity.toString(), frq + 1);
                                } else
                                    entitiesInDoc.put(entity.toString(), 1);
                            }
                        }
                    } else {
                        entity.append(currTokenEnt.toUpperCase()).append(" ").append(followTokenEnt.toUpperCase());
                        if (entitiesInDoc.containsKey(entity.toString())) {
                            int frq = entitiesInDoc.get(entity.toString());
                            entitiesInDoc.replace(entity.toString(), frq + 1);
                        } else
                            entitiesInDoc.put(entity.toString(), 1);
                    }
                }
            }

        }
    }


    /**
     * for each term that was done with parsing we need to add to posting file with its position in text,tf in doc  and in which docs have we seen it.
     * we will create the data posting for each term and update doc's attributes in the meanwhile
     *
     * @param cleanToken
     */
    private void addTermToTmpPosting(String cleanToken, Doc docInParse) {
        //adding the token in case its not in posting
        // term will add in the form of
        // term=docName:positionInText,positionInText,positionInText..#3|docName:positionInText,positionInText,positionInText..#2
        String dataInfoTerm = "";
        if (!tmpPostingTerms.containsKey(cleanToken)) {
            if (docInParse != null) {
                dataInfoTerm = docInParse.getName() + ":" + positionOnIndex + "#1";// # is for counting the times term appeared in doc
                docInParse.setNumOfUniqueTerms(docInParse.getNumOfUniqueTerms() + 1); //every time we see a new term we will add one
                //update max freq. for the most frequent term in doc
                if (docInParse.getMaxFrequency() < 1) {
                    docInParse.setMaxFrequency(1);
                }
                docInParse.setLength(docInParse.getLength() + 1);
                ArrayList<String> list = new ArrayList<>();
                list.add(dataInfoTerm);
                tmpPostingTerms.put(cleanToken, list);
            }else{
                if (tmpPostingTerms.containsKey(cleanToken)) {
                    tmpPostingTerms.get(cleanToken).get(0).concat("#");
                } else {
                    String s = "#";
                    ArrayList<String> list = new ArrayList<>();
                    list.add(s);
                    tmpPostingTerms.put(cleanToken, list);
                }
            }
        } else { //we have seen this term before so we will update its dataInfoTerm
            String currDataInfoTerm = tmpPostingTerms.get(cleanToken).get(0);
            StringBuilder sb = new StringBuilder();
            if (docInParse != null) {
                if (currDataInfoTerm.contains(docInParse.getName())) { //counting for same doc
                    int indexCount = currDataInfoTerm.lastIndexOf('#');//find index of the counting sign
                    int termFreq = Integer.parseInt(currDataInfoTerm.substring(indexCount + 1));
                    sb.append(currDataInfoTerm.substring(0, indexCount));
                    sb.append(",").append(positionOnIndex).append("#").append(String.valueOf((termFreq + 1)));
                    if (docInParse.getMaxFrequency() < termFreq + 1) {
                        docInParse.setMaxFrequency(termFreq + 1); //update max freq in doc
                    }
                } else { //counting for new doc
                    sb.append(currDataInfoTerm).append("|").append(docInParse.getName()).append(":").append(positionOnIndex).append("#1");
                    docInParse.setNumOfUniqueTerms(docInParse.getNumOfUniqueTerms() + 1);
                    if (docInParse.getMaxFrequency() < 1) {
                        docInParse.setMaxFrequency(1);
                    }
                }
                docInParse.setLength(docInParse.getLength() + 1);
                ArrayList<String> listToInsert = new ArrayList<>();
                listToInsert.add(sb.toString());
                tmpPostingTerms.put(cleanToken, listToInsert);
            } else {
                if (tmpPostingTerms.containsKey(cleanToken)) {
                    tmpPostingTerms.get(cleanToken).get(0).concat("#");
                } else {
                    String s = "#";
                    ArrayList<String> list = new ArrayList<>();
                    list.add(s);
                    tmpPostingTerms.put(cleanToken, list);
                }
            }
        }
    }

    // ~Upper/Lower Case terms
    // if first letter is capital save the term as capital
    // if we find  same term as lower case, we change it in posting
    private String setLowerOrUpperPosting(String currToken) {
        String ans = "";
        String upperToken = currToken.toUpperCase();
        String lowerToken = currToken.toLowerCase();
        boolean containUpper = tmpPostingTerms.containsKey(upperToken);
        boolean containLower = tmpPostingTerms.containsKey(lowerToken);
        //first letter is capital
        if (Character.isUpperCase(currToken.charAt(0))) {
            ans = upperToken;
            if (containLower) {
                ans = lowerToken;
            }
        } else if (containUpper) { //first letter is not capital
            //there is already capital word
            ans = lowerToken;
            String currDataOfTermInPosting = tmpPostingTerms.remove(upperToken).get(0); //remove the term with upper and save its data
            ArrayList<String> listToInsert = new ArrayList<>();
            listToInsert.add(currDataOfTermInPosting);
            tmpPostingTerms.put(lowerToken, listToInsert); //insert the term as lower with previous data
        } else {
            ans = currToken;
        }
        return ans;

    }

    /**
     * check if number is smaller than 1M
     *
     * @param token
     * @return
     */
    private boolean isSmallerThanMillion(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        float number = Float.parseFloat(makeNumberWithNoComma(token));
        if (number < 1000000)
            return true;
        return false;
    }

    /**
     * check if token is a number
     *
     * @param token to check
     * @return true if the token is a number
     */
    public boolean isNumber(String token) {
        int countDots = 0;
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        for (char c : token.toCharArray()) {
            if (c == ',' || c == '-')
                continue;
            if (c == '.') {
                countDots++;
                continue;
            }
            if (!Character.isDigit(c))
                return false;
        }
        return countDots <= 1;
    }

    /**
     * check if token in smaller than 0
     *
     * @param token to check
     * @return true if the token represent a fracture
     */
    private boolean isFracture(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        int countSlash = 0;
        for (char c : token.toCharArray()) {
            if (c == '/')
                countSlash++;
            if (!Character.isDigit(c) && c != '/' && countSlash < 2)
                return false;
        }
        if (countSlash == 1)
            return true;
        return false;
    }

    /**
     * take a number with one or more commas and strip it from commas
     *
     * @param token like 1,000,000
     * @return 1000000
     */
    public String makeNumberWithNoComma(String token) {
        StringBuilder ans = new StringBuilder();
        if (token == null || token.equals("") || token.isEmpty())
            return ans.toString();
        for (char c : token.toCharArray()) {
            if (c != ',')
                ans.append(c);
        }
        return ans.toString();
    }

    /**
     * check case of number that written with the word Thousand to describe a number bigger than 1,000
     *
     * @param token to check
     * @return true if the number is bigger that 1,000 and written with "Thousand"
     */
    private boolean isWrittenWithThousand(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Thousand") || token.equals("thousand") || token.equals("Thousands") ||
                token.equals("thousands") || token.equals("THOUSAND") || token.equals("THOUSANDS"))
            return true;
        return false;
    }

    /**
     * check if token is written with dollars token to figure if this token is connected to money
     *
     * @param token
     * @return
     */
    private boolean isWrittenWithDollar(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Dollars") || token.equals("dollars") ||
                token.equals("dollar") || token.equals("Dollar") ||
                token.equals("U.S dollars") || token.equals("U.S Dollars") ||
                token.equals("U.S dollar") || token.equals("U.S Dollar"))
            return true;
        return false;
    }

    /**
     * check if token is written with kg token to figure if this token is connected to weight
     *
     * @param token
     * @return
     */
    private boolean isWrittenWithKG(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Kilograms") || token.equals("kilograms") ||
                token.equals("kilogram") || token.equals("Kilogram") ||
                token.equals("KG") || token.equals("K.G.") ||
                token.equals("K.G") || token.equals("kg") || token.equals("k.g.") || token.equals("k.g"))
            return true;
        return false;
    }

    /**
     * check if token is written with lbs token to figure if this token is connected to weight
     *
     * @param token
     * @return
     */
    private boolean isWrittenWithPounds(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Pounds") || token.equals("pounds") ||
                token.equals("Pound") || token.equals("pound") ||
                token.equals("LBS") || token.equals("lbs") ||
                token.equals("Lbs") || token.equals("lb") ||
                token.equals("Lb") || token.equals("Libra") ||
                token.equals("Libras") || token.equals("libras") ||
                token.equals("libra"))
            return true;
        return false;
    }

    /**
     * check if token is written with lbs token to figure if this token is connected to distance
     *
     * @param token
     * @return
     */
    private boolean isWrittenWithKM(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Kilometers") || token.equals("kilometers") ||
                token.equals("kilometer") || token.equals("Kilometer") ||
                token.equals("KM") || token.equals("K.M.") ||
                token.equals("K.M") || token.equals("km") || token.equals("k.m.") || token.equals("k.m"))
            return true;
        return false;
    }

    /**
     * check if token is related to scale of millions to the number before the token
     *
     * @param token
     * @return
     */
    private boolean isWrittenWithMillion(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Million") || token.equals("million") || token.equals("Millions") ||
                token.equals("millions") || token.equals("MILLIONS") || token.equals("MILLION")
                || token.equals("m") || token.equals("M"))
            return true;
        return false;
    }

    /**
     * check if token is related to scale of billions to the number before the token
     *
     * @param token
     * @return
     */
    private boolean isWrittenWithBillion(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Billion") || token.equals("billion") || token.equals("Billions") ||
                token.equals("billions") || token.equals("BILLIONS") || token.equals("BILLION")
                || token.equals("bn") || token.equals("BN") || token.equals("Bn") || token.equals("B"))

            return true;
        return false;
    }

    /**
     * check if token is related to scale of trillions to the number before the token
     *
     * @param token
     * @return
     */
    private boolean isWrittenWithTrillion(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Trillion") || token.equals("trillion") || token.equals("Trillions") ||
                token.equals("trillions") || token.equals("TRILLIONS") || token.equals("TRILLION"))
            return true;
        return false;
    }

    /**
     * check the token for special chars, we need clean token to work with
     *
     * @param token
     * @return
     */
    private boolean isVerifiedToken(String token) {
        if (token == null || token.equals("")) {
            return false;
        }
        if ((token.isEmpty()) || token.equals("\n") || token.equals("\t") || token.equals("<text>") ||
                token.equals("</text>") || token.equals(" ") || token.contains("'")
                || token.contains(";") || token.contains(":") || token.contains("?")
                || token.contains("!") || token.contains("~") || token.contains("+") || token.contains("^")
                || token.contains("(") || token.contains(")") || token.contains("&") || token.contains("[")
                || token.contains("]") || token.contains("{") || token.contains("}") || token.contains("*")
                || token.contains("@") || token.contains("#") || token.contains("=") || token.contains("/f")
                || token.contains("_")
        )
            return false;
        return true;
    }


    /**
     * cut the token from unnecessary delimiters from front and back.
     *
     * @param token - the token we wish to clean from delimiters.
     * @return a cleaned form of the token.
     */
    public String cutDelimiters(String token) {
        if (token.startsWith(".") || token.startsWith("'") || token.startsWith(",") || token.startsWith(";") || token.startsWith(":")
                || token.startsWith("\"") || token.startsWith("?") || token.startsWith("!") || token.startsWith("(") || token.startsWith(")")
                || token.startsWith("-") || token.startsWith("+") || token.startsWith("`") || token.startsWith("#")
                || token.startsWith("@") || token.startsWith("&") || token.startsWith("{") || token.startsWith("}") || token.startsWith("[")
                || token.startsWith("]") || token.startsWith("/") || token.startsWith("_")) {
            token = token.substring(1);
            token = cutDelimiters(token);
        }
        if (token.endsWith(".") || token.endsWith(",") || token.endsWith("'") || token.endsWith(";") || token.endsWith(":")
                || token.endsWith("(") || token.endsWith(")") || token.endsWith("?") || token.endsWith("!")
                || token.endsWith("-") || token.endsWith("+") || token.endsWith("`") || token.endsWith("\"") || token.endsWith("#")
                || token.endsWith("@") || token.endsWith("&") || token.endsWith("{") || token.endsWith("}") || token.endsWith("[")
                || token.endsWith("]") || token.endsWith("/") || token.endsWith("_")) {
            token = token.substring(0, token.length() - 1);
            token = cutDelimiters(token);
        }
        if (token.endsWith("'s") || token.endsWith("./") || token.endsWith("'S")) {
            token = token.substring(0, token.length() - 2);
            token = cutDelimiters(token);
        }
        return token;
    }

    /**
     * check if token is written with percent or percentage token
     *
     * @param token to check
     * @return true if the number is bigger that 1,000 and written with "Thousand"
     */
    private boolean isWrittenWithPercentage(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("percent") || token.equals("Percent") || token.equals("PERCENT") ||
                token.equals("percentage") || token.equals("Percentage") || token.equals("PERCENTAGE"))
            return true;
        return false;
    }

    public String getPathToWrite() {
        return pathToWrite;
    }

    public void setPathToWrite(String pathToWrite) {
        this.pathToWrite = pathToWrite;
    }

    public boolean isUserChoseStemming() {
        return userChoseStemming;
    }

    public void setUserChoseStemming(boolean userChoseStemming) {
        this.userChoseStemming = userChoseStemming;
//        indexer.setUserChoseStemming(userChoseStemming);
    }

    public String getPathOfCorpusAndStopWords() {
        return pathOfCorpusAndStopWords;
    }

    public void setPathOfCorpusAndStopWords(String pathOfCorpusAndStopWords) {
        importStopWords(pathOfCorpusAndStopWords);
        this.pathOfCorpusAndStopWords = pathOfCorpusAndStopWords;
    }

    public void reset() {
        stopWords.clear(); // holds the stop words that we won't parse in parse func
//        docInParse = null; // current doc in parsing process
        positionOnIndex = 0; //counts the index only on tokens and not all over the text
        stemmer = new Stemmer(); //in case we need to use it
        calender.clear(); //contains all months with matching numbers for parse
        tmpPostingTerms.clear(); //mapping lowerCase to lowerCase/upperCase
        userChoseStemming = false; //indicates if we need to use stemming
        pathToWrite = ""; //path from user where to store posting files
        pathOfCorpusAndStopWords = "";
        countParsedDocs = 0; //count parsed docs so we would know when to write to disk
    }
}
