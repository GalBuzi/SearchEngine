package View;

import java.util.List;

import Controller.Controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

public class Dictionary extends IndexCreater {

    private Controller controller;
    private Stage stage;

    @FXML
    public ListView<String> dicVals;

    public void setController(Controller controller, Stage stage) {
        this.controller = controller;
        this.stage = stage;
        setText();
    }

    public void setText(){
        List<String> dictionary = controller.displayDictionary();
        ObservableList<String> dictionaryObservable = FXCollections.observableArrayList(dictionary);
        for(String str : dictionaryObservable )
            dicVals.getItems().add(str);
    }

}
