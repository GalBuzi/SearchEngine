package View;

import Controller.Controller;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class IndexCreater extends MainWindow {

    @FXML
    private Controller controller;
    private Stage stage;
    public javafx.scene.control.Button btn_browseCorpusAndSW;
    public javafx.scene.control.TextField txtfld_corpusDir;
    public javafx.scene.control.Button btn_browseSavingFiles;
    public javafx.scene.control.TextField txtfld_writingDir;
    public javafx.scene.control.Button btn_resetSettings;
    public javafx.scene.control.Button btn_saveSettings;
    public javafx.scene.control.Button btn_displayDictionary;
    public javafx.scene.control.Button btn_loadDictionary;
    public javafx.scene.control.Button btn_createInvertedIndex;
    public javafx.scene.control.CheckBox cbox_stemming;


    public void setController(Controller controller,Stage stage) {
        this.controller = controller;
        this.stage = stage;
        btn_browseCorpusAndSW.setDisable(false);
        btn_browseSavingFiles.setDisable(false);
        btn_resetSettings.setDisable(true);
        btn_displayDictionary.setDisable(true);
        btn_loadDictionary.setDisable(false);
        btn_createInvertedIndex.setDisable(true);
        btn_saveSettings.setDisable(true);
    }

    private void showAlert(String alertMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(alertMessage);
        alert.show();
    }

    public void browseBtnToChooseCorpusAndStopWords(javafx.event.ActionEvent ae) {
        DirectoryChooser browse = new DirectoryChooser();
        browse.setInitialDirectory(new File(System.getProperty("user.dir")));
        browse.setTitle("Set Your Corpus And StopWords Directory");
        File chosenDir = browse.showDialog(null);
        if (chosenDir != null) {
            txtfld_corpusDir.setText(chosenDir.getAbsolutePath());
        }
    }

    public void browseBtnToChooseWhereToSaveFiles(javafx.event.ActionEvent ae) {
        DirectoryChooser browse1 = new DirectoryChooser();
        browse1.setInitialDirectory(new File(System.getProperty("user.dir")));
        browse1.setTitle("Set Your Directory To Save Files");
        File chosenDir = browse1.showDialog(null);
        if (chosenDir != null) {
            txtfld_writingDir.setText(chosenDir.getAbsolutePath());
        }
        btn_saveSettings.setDisable(false);
    }

    public void getStemmingOptionFromCheckbox(javafx.event.ActionEvent ae) {
        boolean userChoseStem = cbox_stemming.isSelected();
        if (userChoseStem) {
            controller.setStemming(true);
        } else {
            controller.setStemming(false);
        }
    }

    public void saveSettings(javafx.event.ActionEvent ae) {
        File corpus = new File(txtfld_corpusDir.getText());
        File write = new File(txtfld_writingDir.getText());
        if (txtfld_corpusDir.getText().isEmpty() || !corpus.isDirectory() || !write.isDirectory() || txtfld_writingDir.getText().isEmpty()) {
            showAlert("Please Enter Valid Paths To Directories");
        } else {
            controller.updateUserSettings(txtfld_corpusDir.getText(), txtfld_writingDir.getText());
            btn_resetSettings.setDisable(false);
            btn_createInvertedIndex.setDisable(false);
        }
    }

    public void startCreatingInvIdx(javafx.event.ActionEvent ae) {
        double[] resultsAfterBuild = controller.createInvIdx(cbox_stemming.isSelected());
        showAlert("total time for creating index :" + resultsAfterBuild[0] + " minutes\n" + "number of total documents indexed: " + resultsAfterBuild[1]
                + "\n" + "number of distinct terms in corpus: " + resultsAfterBuild[2]);
        btn_resetSettings.setDisable(false);
    }

    public void resetSettings(javafx.event.ActionEvent ae) {
        boolean succeeded = controller.resetSettings();
        if (succeeded) {
            showAlert("The system has restarted");
            btn_resetSettings.setDisable(true);
            btn_createInvertedIndex.setDisable(true);
            txtfld_corpusDir.clear();
            txtfld_writingDir.clear();

        } else {
            showAlert("The system has not managed to restart, Try Again");
            btn_resetSettings.setDisable(false);
        }

    }

    public void displayDictionary(javafx.event.ActionEvent ae) {
        if(txtfld_writingDir.getText() == null || txtfld_writingDir.getText().isEmpty() ){
            showAlert("Please insert path for writing posting files and click Save Settings ");
        }else{
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = null;
            try{
                root = fxmlLoader.load(getClass().getResource("/dictionaryDisplay.fxml").openStream());
            }catch (Exception e){e.printStackTrace();}
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes
            stage.setTitle("Dictionary");
            Scene scene = new Scene(root, 384, 575);
            stage.setScene(scene);
            stage.show();
            Dictionary dic = new Dictionary();
            dic = fxmlLoader.getController();
            dic.setController(controller, stage);
        }

    }

    public void loadDictionary(javafx.event.ActionEvent ae) {
        if(txtfld_writingDir.getText() == null || txtfld_writingDir.getText().isEmpty() ){
            showAlert("Please insert path for writing posting files and click Save Settings ");
        }
        else {
            if (controller.loadDictionaryToMemo(cbox_stemming.isSelected())) {
                showAlert("Dictionary loaded successfully!");
                btn_displayDictionary.setDisable(false);
            } else {
                showAlert("Something went wrong, please try again!");
            }
        }


    }

}
