package Controller;

import Model.ReadFile;
import Model.myModel;

import java.util.*;

public class Controller  extends Observable implements Observer {

    private myModel model;
    private boolean userChoseStemming;

    public void setModel(myModel model) {
        this.model = model;
    }

    public boolean isUserChoseStemming() {
        return userChoseStemming;
    }

    public void setStemming(boolean b) {
        userChoseStemming = b;
    }

    public void updateUserSettings(String corpus, String pathToWrite) {
        model.setUserSettings(corpus,pathToWrite ,userChoseStemming );
    }

    public void setPathOfIndexFiles(String pathToWrite){
        model.setPathOfIndexFiles(pathToWrite);
    }

    public boolean resetSettings() {
        return model.resetSettings();
    }

    public ArrayList<String> displayDictionary() {
        return model.getDictTermsToDisplay();
    }

    public boolean loadDictionaryToMemo(boolean selected) {
        return model.loadDictionariesToMemo(selected);
    }

    public double[] createInvIdx(boolean userChoseStemming) {
        return model.createIndex(userChoseStemming);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o == model){
            setChanged();
            notifyObservers(arg);
        }
    }

    public void processSingleUserQuery(String text, boolean online, boolean semantics) {
        model.processSingleUserQuery(text,online,semantics,userChoseStemming);
    }

    public void processFileOfQueries(String text, boolean online, boolean semantics) {
        model.processFileOfQueries(text,online,semantics,userChoseStemming);
    }

    public TreeMap<Integer, Vector<String>> getResults() {
        return model.getResultsOfQueries();
    }

    public Set<String> getEntitiesOfDoc(String doc){
        Set<String> ans = new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String[] s1 = ReadFile.splitByDelimiter(o1 , " ,-");
                String[] s2 = ReadFile.splitByDelimiter(o2 , " ,-");
                String[] s11 = ReadFile.splitByDelimiter(s1[s1.length-1] , ".");
                String[] s22 = ReadFile.splitByDelimiter(s2[s2.length-1] , ".");
                if (s22[1].compareTo(s11[1]) == 0) {
                    return o1.compareTo(o2);
                }
                return s22[1].compareTo(s11[1]);
            }
        });
        Map<String, String> docsDict = model.getDocumentsDict();
        String docDetails = docsDict.get(doc);
        String[] details = ReadFile.splitByDelimiter(docDetails , "|");
        String[] entitiesSeperated = ReadFile.splitByDelimiter(details[details.length-1] , ",");
        for (int i = 0; i+1 < entitiesSeperated.length; i=i+2) {
            ans.add(entitiesSeperated[i]+" - "+entitiesSeperated[i+1]);
        }
        return ans;
    }

    public void saveResultsToFile(String path) {
        model.writeResultsToFile(path,isUserChoseStemming());
    }
}
