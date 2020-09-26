package View;

import Controller.Controller;
import Model.myModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private myModel model;
    private MainWindow view;

    public static void main(String[] args) {
        launch(args);

//        myModel prog = myModel.getInstance();
//        prog.setUserSettings("C:\\Users\\gal\\IdeaProjects\\EnginFiles\\corpus" , "C:\\Users\\gal\\Desktop\\לימודים\\סמסטר ז\\אחזור\\מנוע\\engin\\posting",false);
////        prog.setUserSettings("C:\\Users\\gal\\Desktop\\לימודים\\סמסטר ז\\אחזור\\מנוע\\engin\\miniCorpus" , "C:\\Users\\gal\\Desktop\\לימודים\\סמסטר ז\\אחזור\\מנוע\\engin\\posting",false);
////        prog.createIndex(false);
//        prog.loadDictionariesToMemo(false);
////        prog.processSingleUserQuery("United States is Kiro-Gligorov" , false, false);
//        prog.processFileOfQueries("C:\\Users\\gal\\IdeaProjects\\EnginFiles\\queries.txt" , true,false);
//        System.out.println("done");

    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        model = myModel.getInstance();
        Controller controller = new Controller();
        controller.setModel(model);
        model.addObserver(controller);
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResource("/MainWindow.fxml").openStream());
        primaryStage.setTitle("Information Retrieval Course - Search Engine");
        Scene scene = new Scene(root, 577, 458);
        primaryStage.setScene(scene);
        view = loader.getController();
        view.setController(controller,primaryStage);
        controller.addObserver(view);
        primaryStage.show();
    }

}
