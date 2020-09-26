package Model;

public class Term {
    private String termName; // Term's Name
    private int termFreqInCorpus; // total times of the Term in corpus
    private int docFreq; // num of the documents the Term was in
    private long ptrPosting; // pointer to relevant row in file

    public Term(String termName) {
        this.termName = termName;
        this.ptrPosting = -1;
    }

    public Term(String name, int termFreq, int docFreq, long lineNumber) {
        this.termName = name;
        this.termFreqInCorpus = termFreq;
        this.docFreq = docFreq;
        this.ptrPosting = lineNumber;
    }

    public String getTermName() {
        return termName;
    }

    public void setTermName(String termName) {
        this.termName = termName;
    }

    public int getTermFreqInCorpus() {
        return termFreqInCorpus;
    }

    public void setTermFreqInCorpus(int termFreqInCorpus) {
        this.termFreqInCorpus = termFreqInCorpus;
    }

    public int getDocFreq() {
        return docFreq;
    }

    public void setDocFreq(int docFreq) {
        this.docFreq = docFreq;
    }

    public long getPtrPosting() {
        return ptrPosting;
    }

    public void setPtrPosting(long ptrPosting) {
        this.ptrPosting = ptrPosting;
    }
}
