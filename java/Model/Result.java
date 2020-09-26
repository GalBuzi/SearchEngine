package Model;

public class Result {

    private String queryId;
    private String docName;
    private String queryTitle;
    private double rank;


    //constructor
    public Result(String queryId,String queryText,String DocName, double rank){
        this.queryTitle = queryText;
        this.queryId = queryId;
        this.docName = DocName;
        this.rank = rank;
    }

    /**Getters and Setters */
    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    public String getQueryTitle() { return queryTitle; }

    public void setQueryTitle(String queryTitle) { this.queryTitle = queryTitle; }

}
