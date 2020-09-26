package Model;

import java.util.HashMap;

public class EntityInfoInDoc {

    private HashMap<String , Double> entitiesAndFreqInDoc; // <entity , rank>


    public EntityInfoInDoc() {
        this.entitiesAndFreqInDoc = new HashMap<>();
    }

    public HashMap<String, Double> getEntitiesAndFreqInDoc() {
        return entitiesAndFreqInDoc;
    }

    public void setEntitiesAndFreqInDoc(HashMap<String, Double> entitiesAndFreqInDoc) {
        this.entitiesAndFreqInDoc = entitiesAndFreqInDoc;
    }

    public void insert(String ent , double num){
        entitiesAndFreqInDoc.put(ent , num);
    }
}
