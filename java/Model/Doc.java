package Model;


import java.util.Map;
import java.util.TreeMap;

public class Doc {

    private String name;
    private String title;
    private String date;
    private String city;
    private String language;
    private int length;
    private int maxFrequency;
    private int numOfUniqueTerms;
    private TreeMap<String, Double> topFiveEntities;

    public Doc(){
        name = "none";
        title = "none";
        date = "none";
        city = "none";
        language ="none";
        length = 0;
        maxFrequency = 0;
        numOfUniqueTerms = 0;
        topFiveEntities = new TreeMap<>();

    }

    public Doc(Doc document){
        name = document.getName();
        title = document.getTitle();
        date = document.getDate();
        city = document.getCity();
        language =document.getLanguage();
        topFiveEntities =document.topFiveEntities;
        length = document.getLength();
        maxFrequency = document.maxFrequency;
        numOfUniqueTerms = document.getNumOfUniqueTerms();

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() { return title; }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getMaxFrequency() {
        return maxFrequency;
    }

    public void setMaxFrequency(int maxFrequency) {
        this.maxFrequency = maxFrequency;
    }

    public int getNumOfUniqueTerms() {
        return numOfUniqueTerms;
    }

    public void setNumOfUniqueTerms(int numOfUniqueTerms) {
        this.numOfUniqueTerms = numOfUniqueTerms;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, Double> getTopFiveEntities() { return topFiveEntities; }

    public void setTopFiveEntities(Map<String, Double> topFiveEntities) { this.topFiveEntities = new TreeMap<>(topFiveEntities); }


    public String getTopFiveEntitiesString() {
        if (topFiveEntities.isEmpty() || topFiveEntities == null) {
            return "none";
        }
        StringBuilder ans = new StringBuilder();
        int i=0;
        for (Map.Entry<String, Double> entry : topFiveEntities.entrySet()) {
            if(i==4){
                ans.append(entry.getKey()).append(",").append(entry.getValue());
                break;
            }
            ans.append(entry.getKey()).append(",").append(entry.getValue()).append(",");
            i++;
        }
        return ans.toString();
    }

    public void insertToTop5(String ent , double rank){
        topFiveEntities.put(ent, rank);
        if(topFiveEntities.size() == 5){
            topFiveEntities =(TreeMap<String, Double>) Indexer.sortByValues(topFiveEntities);
        }
    }

}
