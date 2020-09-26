package View;

import Controller.Controller;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class SearchResults extends MainWindow {

    private Controller controller;
    private Stage stage;
    TreeMap<Integer, Vector<String>> result;
    @FXML
    public ListView<String> resultList;
    public ListView<String> entites;
    public ChoiceBox queryId;
    public TextField path;
    public Button btn_browseDir;
    public Button btn_save;

    public void setController(Controller controller,Stage stage) {
        this.controller = controller;
        this.stage = stage;
        this.result = MainWindow.resultsOfSearches;
        setQueryId();
    }

    private void setQueryId() {
        for(Map.Entry<Integer,Vector<String>> entry: MainWindow.resultsOfSearches.entrySet()){
            queryId.getItems().add(entry.getKey());
        }
        // if a query is selected, show all the relevant document
        queryId.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                resultList.getItems().clear();
                Integer key = (Integer) queryId.getItems().get((Integer) number2);
                Vector <String> result = MainWindow.resultsOfSearches.get(key);
                ObservableList<String> dictionaryObservable = FXCollections.observableArrayList(result);
                for(String str : dictionaryObservable) {
                    resultList.getItems().add(str);
                }
            }
        });

        // if a document is selected, show all it's entities
        resultList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                this.entites.getItems().clear();
                ObservableList<String> entites = FXCollections.observableArrayList(controller.getEntitiesOfDoc(newSelection));
                for(String str : entites) {
                    this.entites.getItems().add(str);
                }
            }
        });
    }

    public void loadPath(){
        try {
            DirectoryChooser fileChooser = new DirectoryChooser();
            fileChooser.setTitle("Open Resource File");
            File selectedFile = fileChooser.showDialog(new Stage());
            if (selectedFile != null) {
                path.setText(selectedFile.getAbsolutePath());
            }
        }catch (Exception e){ }
    }

    /**
     * This function save the result of the query to a chosen file by the user
     */
    public void saveQueryResultToFile(){
        if (path.getText() == null || path.getText().trim().isEmpty())
            alert("You did not enter any path", Alert.AlertType.ERROR);
        else {
            controller.saveResultsToFile(path.getText());
            alert("Result saved to file", Alert.AlertType.INFORMATION);
        }
    }
}
