package View;

import Controller.Controller;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MainWindow implements Observer {

    private Controller controller;
    private Stage primaryStage;
    private SearchResults searchResults;
    private Dictionary displayDictionary;
    private IndexCreater indexCreater;

    @FXML
    public javafx.scene.control.Button btn_createIndex;
    public javafx.scene.control.Button btn_loadDictionaries;
    public javafx.scene.control.Button btn_reset;
    public javafx.scene.control.Button btn_displayDict;
    public javafx.scene.control.Button btn_run;
    public javafx.scene.control.Button btn_browseQureyFile;
    public javafx.scene.control.Button btn_getPath;
    public javafx.scene.control.RadioButton radiobtn_queryUser;
    public javafx.scene.control.RadioButton radiobtn_queryFile;
    public javafx.scene.control.CheckBox checkBox_semantics;
    public javafx.scene.control.CheckBox checkBox_semanticsOnline;
    public javafx.scene.control.CheckBox checkBox_stemming;
    public javafx.scene.control.TextField txtfld_queryUser;
    public javafx.scene.control.TextField txtfld_queryFile;
    public javafx.scene.control.TextField txtfld_path;
    public static TreeMap<Integer, Vector<String>> resultsOfSearches;

    public void getStemmingOptionFromCheckbox(javafx.event.ActionEvent ae) {
        boolean userChoseStem = checkBox_stemming.isSelected();
        if (userChoseStem) {
            controller.setStemming(true);
        } else {
            controller.setStemming(false);
        }
    }

    public void runQueries(javafx.event.ActionEvent ae){

        if(txtfld_queryFile.getText().length()>0){
            controller.processFileOfQueries(txtfld_queryFile.getText(),checkBox_semanticsOnline.isSelected() , checkBox_semantics.isSelected());
            resultsOfSearches = controller.getResults();
            newStage("SearchResults.fxml", "", searchResults, 600, 400, controller);
        }else if(txtfld_queryUser.getText().length()>0){
            controller.processSingleUserQuery(txtfld_queryUser.getText() , checkBox_semanticsOnline.isSelected() , checkBox_semantics.isSelected());
            resultsOfSearches = controller.getResults();
            newStage("SearchResults.fxml", "", searchResults, 600, 400, controller);
        }else{
            alert("Something went wrong, check that you have entered a query or a file", Alert.AlertType.ERROR);
        }

    }


    public void browseQueryFile(javafx.event.ActionEvent ae){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Your Queries File");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(primaryStage);
        if(file!=null){
            txtfld_queryFile.setText(file.getAbsolutePath());
        }
    }


    public void radioBtnQueryFileClicked(javafx.event.ActionEvent ae){
        if(radiobtn_queryFile.isSelected()&& !radiobtn_queryUser.isSelected()){
            txtfld_queryFile.setDisable(false);
            btn_run.setDisable(false);
            btn_browseQureyFile.setDisable(false);
        }
        else {
            txtfld_queryFile.setDisable(true);
            btn_run.setDisable(true);
            btn_browseQureyFile.setDisable(true);
        }
    }

    public void radioBtnQueryUserClicked(javafx.event.ActionEvent ae){
        if(radiobtn_queryUser.isSelected() && !radiobtn_queryFile.isSelected()){
            txtfld_queryUser.setDisable(false);
            btn_run.setDisable(false);
        }
        else {
            txtfld_queryUser.setDisable(true);
            btn_run.setDisable(true);
        }
    }

    public void resetSettings(javafx.event.ActionEvent ae) {
        File f = new File(txtfld_path.getText());
        File[] list = f.listFiles();
        if(list != null && list.length > 0){
            boolean succeeded = controller.resetSettings();
            if (succeeded) {
                alert("The system has restarted" , Alert.AlertType.CONFIRMATION);
                btn_reset.setDisable(true);
                btn_loadDictionaries.setDisable(true);
                btn_displayDict.setDisable(true);
            } else {
                alert("The system has not managed to restart, Try Again", Alert.AlertType.WARNING);

            }
        }
        else alert("This directory is already empty!", Alert.AlertType.ERROR);

    }


    public void displayDictionary(javafx.event.ActionEvent ae) {
        if(txtfld_path.getText() == null || txtfld_path.getText().isEmpty() ){
            alert("Please insert path for files", Alert.AlertType.INFORMATION);
        }else{
            newStage("dictionaryDisplay.fxml", "", displayDictionary, 384, 575, controller);
        }

    }


    public void loadDictionaries(javafx.event.ActionEvent ae) {
        if(txtfld_path.getText() == null || txtfld_path.getText().isEmpty() ){
            alert("Please insert path for writing posting files and click Save Settings" , Alert.AlertType.WARNING);
        }
        else {
            if (controller.loadDictionaryToMemo(checkBox_stemming.isSelected())) {
                alert("Dictionary loaded successfully!", Alert.AlertType.INFORMATION);
                btn_displayDict.setDisable(false);
                radiobtn_queryUser.setDisable(false);
                radiobtn_queryFile.setDisable(false);
            } else {
                alert("Something went wrong, please try again!" , Alert.AlertType.ERROR);
            }
        }
    }

    public void browsePathToFiles(javafx.event.ActionEvent ae) {
        DirectoryChooser browse1 = new DirectoryChooser();
        browse1.setInitialDirectory(new File(System.getProperty("user.dir")));
        browse1.setTitle("Set Your Directory To Save Files");
        File chosenDir = browse1.showDialog(null);
        if (chosenDir != null) {
            txtfld_path.setText(chosenDir.getAbsolutePath());
        }
        controller.setPathOfIndexFiles(txtfld_path.getText() );
        btn_reset.setDisable(false);
        btn_loadDictionaries.setDisable(false);
    }

    public void CreateIndexFromScrach(ActionEvent actionEvent) {
        newStage("IndexCreater.fxml", "", indexCreater, 670, 490, controller);
    }

    @Override
    public void update(Observable o, Object arg) {

    }

    public void setController(Controller controller, Stage primaryStage) {
        this.controller = controller;
        this.primaryStage = primaryStage;
        btn_reset.setDisable(true);
        btn_displayDict.setDisable(true);
        btn_loadDictionaries.setDisable(true);
        txtfld_queryUser.setDisable(true);
        txtfld_queryFile.setDisable(true);
        btn_browseQureyFile.setDisable(true);
        btn_run.setDisable(true);
        radiobtn_queryUser.setDisable(true);
        radiobtn_queryFile.setDisable(true);
    }

    protected void SetStageCloseEvent(Stage primaryStage) {
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent windowEvent) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("Yes");
                ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Back");
                alert.setContentText("Are you sure you want to exit?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    // ... user chose OK
                    // Close program
                    //enable start button when return to primary stage (home window)
//                    if(btn_start!=null)
//                        btn_start.setDisable(false);
                } else {
                    // ... user chose CANCEL or closed the dialog
                    windowEvent.consume();

                }
            }
        });
    }


    protected void newStage(String fxmlName, String title, MainWindow windowName, int width, int height, Controller controller){
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = null;
        try {
            root = fxmlLoader.load(getClass().getResource("/" + fxmlName).openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        Scene scene = new Scene(root, width, height);
        stage.setScene(scene);
        stage.setResizable(false);
        SetStageCloseEvent(stage);
        stage.show();
        windowName = fxmlLoader.getController();
        windowName.setController(controller, stage);
        controller.addObserver(windowName);
    }

    protected void alert(String messageText, Alert.AlertType alertType){
        Alert alert = new Alert(alertType);
        alert.setContentText(messageText);
        alert.showAndWait();
        alert.close();

    }


}
