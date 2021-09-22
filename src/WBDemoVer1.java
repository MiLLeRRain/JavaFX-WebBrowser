import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.text.ParseException;

public class WBDemoVer1 extends Application {

    /**
     * Use this to manipulate the Stage title
     */
    private static Stage primaryStage;

    /**
     * Containers for contents holding
     */
    private Pane root = new Pane();
    private TabPane tabRoot;

    /**
     * HOME PAGE
     */
    private static final String HOME_PAGE = "https://cdpn.io/Ma5a/fullpage/Xempjq";

    @Override
    public void start(Stage primaryStage) throws Exception {
        WBDemoVer1.primaryStage = primaryStage;

        /* Create a new Tab Root (TabPane) to hold all tabs */
        tabRoot = initialTabRoot();
        tabRoot.prefWidthProperty().bind(root.widthProperty());
        tabRoot.prefHeightProperty().bind(root.heightProperty());

        root.getChildren().add(tabRoot);

        root.prefWidthProperty().bind(primaryStage.widthProperty());
        root.prefHeightProperty().bind(primaryStage.heightProperty());

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setWidth(1300);
        primaryStage.setHeight(800);
        primaryStage.setAlwaysOnTop(false);
        primaryStage.show();

    }

    /**
     * Build the TabPane for all tabs
     * @return a built TabPane with an initial tab and the tabAdder
     * @throws FileNotFoundException
     * @throws ParseException
     */
    private TabPane initialTabRoot() throws FileNotFoundException, ParseException {
        TabPane init = new TabPane();
        init.setStyle("-fx-tab-min-width: 20; " +
                "-fx-border-width: 0; " +
                "-fx-tab-max-width: 80; " +
                "-fx-open-tab-animation: true;" +
                "-fx-background-color: #1f1f3c;");

        // Necessary for each time create new tab? TODO check
        int ID = init.getTabs().size() + 1;
        TabWindow tab = new TabWindow(HOME_PAGE, ID);

        /* Build adder tab */
        Tab tabAdder = buildAdder();
        init.getTabs().addAll(tab, tabAdder);

        init.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        return init;
    }

    /**
     * Build the adder tab
     * @return a tab with adder button TODO use CSS to get rid of border
     */
    private Tab buildAdder() {
        Button adderBtn = new Button("\u002B");
        adderBtn.setStyle("-fx-background-color: transparent;" +
                "-fx-max-width: 40;");
        adderBtn.setPrefSize(20, 20);

        Tab toReturn = new Tab();
        toReturn.setGraphic(adderBtn);
//        toReturn.setStyle(".tab {-fx-pref-width: 250}"); // TODO ???? css how to change header width
        toReturn.setClosable(false); // Not closable
        toReturn.setDisable(true);

        /* Build the adder function */
        adderBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    addTab();
                } catch (FileNotFoundException | ParseException e) {
                    e.printStackTrace();
                }
            }
        });

        return toReturn;
    }

    /**
     * Add new Tab
     */
    private void addTab() throws FileNotFoundException, ParseException {
        int ID = tabRoot.getTabs().size() + 1;
        TabWindow tab = new TabWindow(ID);
        tab.setText(tab.getEngine().getTitle());
        tabRoot.getTabs().add(ID - 2, tab);
        tabRoot.getSelectionModel().select(ID - 2);
    }

    /**
     * Set up the stage title
     * @param title is stage title passing from my lovely TabWindows
     */
    public static void setupTitle(String title) {
        primaryStage.setTitle(title);
    }

    public static void main(String[] args) {
        launch();
    }
}
