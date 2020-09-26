package Model;

import java.util.HashMap;

public class EntityInfoInCorpus {

    private HashMap<Doc,Integer> docList; // < docID , EntityFreqInDoc >
    private int numOfDiffDocs;
    private HashMap<Doc,Double> rankInDoc; // <docID, rank>

    public EntityInfoInCorpus() {
        this.docList = new HashMap<>();
        this.numOfDiffDocs = 0;
        this.rankInDoc = new HashMap<>();
    }

    public HashMap<Doc, Double> getRankInDoc() {
        return rankInDoc;
    }

    public void setRankInDoc(HashMap<Doc, Double> rankInDoc) {
        this.rankInDoc = rankInDoc;
    }

    public HashMap<Doc, Integer> getDocList() {
        return docList;
    }

    public void setDocList(HashMap<Doc, Integer> docList) {
        this.docList = docList;
    }

    public int getNumOfDiffDocs() {
        return numOfDiffDocs;
    }

    public void setNumOfDiffDocs(int numOfDiffDocs) {
        this.numOfDiffDocs = numOfDiffDocs;
    }
}
