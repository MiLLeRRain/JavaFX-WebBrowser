import LovelyUtils.UrlUtils;
import LovelyUtils.WebAddress;
import LovelyUtils.HistoryBookMarkUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.*;
import java.text.ParseException;
import java.util.Scanner;
import java.util.regex.Matcher;

import static LovelyUtils.UrlUtils.*;

public class WBDemoVer0 extends Application {

    private String defaultURL = "https://cdpn.io/Ma5a/fullpage/Xempjq";
    final TextField urlField = new TextField(defaultURL);

    /**
     * WebView and WebEngine
     */
    WebView webTab;
    WebEngine engine;

    /**
     * Web History
     */
    private WebHistory history;
    private ObservableList<WebHistory.Entry> historyEntries;

    /**
     * Favorite menu
     */
    private MenuButton favMenu;
    private Button addFav;

    /**
     * File directory of bookmarks
     */
    File bookmarkDir = new File("./src/favPage/");

    /**
     * Ctrl key boolean for Zoom
     */
    private boolean ctrlDown = false;

    /**
     * Reload button
     */
    Button reload = new Button("\u21BB");

    /**
     * BorderPane root
     */
    BorderPane root;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        root = new BorderPane();

        // TODO not necessary
        HBox tabs = new HBox();
        tabs.setPrefSize(2560, 30);
        tabs.prefWidthProperty().bind(root.widthProperty());
        tabs.setStyle("-fx-background-color: rgba(0, 130, 130, 0.5);");

        VBox underTab = new VBox();

        /* This header is the HBox contains all buttons and url field */
        HBox header = setHeader();
        header.prefWidthProperty().bind(underTab.widthProperty());
        header.setStyle("-fx-background-color: rgba(31, 31, 60, 1);");

        /*
         * Initial the WebView engine
         */
        webTab = initial();
        webTab.prefHeightProperty().bind(underTab.heightProperty()); //TODO 对吗？？？？？？？？

        eventsHandlerSetup(primaryStage);

        underTab.getChildren().addAll(header, webTab);

//        root.setTop(tabs);
        root.setCenter(underTab);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setWidth(1300);
        primaryStage.setHeight(800);
        primaryStage.setAlwaysOnTop(false);
        primaryStage.show();

    }

    private void eventsHandlerSetup(Stage primaryStage) {
        /*
          Url field update to the current displaying site url.
         */
        engine.locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldV, String newV) {
                urlField.setText(newV);
            }
        });

        /*
          Update stage Title to current site Title.
         */
        engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State oldV, Worker.State newV) {
                if (newV == Worker.State.SUCCEEDED) {
                    primaryStage.setTitle(engine.getTitle()); //TODO
                    reload.setText("\u21BB");
                    updateFavMenuIcon();
                }
                else if (newV == Worker.State.RUNNING) reload.setText("\u2715");
            }
        });

        /*
          Process the url entered at textField
         */
        urlField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                loadUrl();
            }
        });

        webTab.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.isControlDown()) {
                    ctrlDown = true;
                }
            }
        });

        webTab.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                ctrlDown = false;
            }
        });

        webTab.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent scrollEvent) {
                if (ctrlDown) {
                    double dY = scrollEvent.getDeltaY();
                    if (dY > 0) webTab.setZoom(webTab.getZoom() * 1.05);
                    else if (dY < 0) webTab.setZoom(webTab.getZoom() / 1.05);
                }
            }
        });


    }

    /**
     * A listener helper to update bookmark icon
     */
    private void updateFavMenuIcon() {
        String titleToCheck = fixFileName(engine.getTitle());
        File newFav = new File("./src/favPage/" + titleToCheck);
        if (newFav.exists()) addFav.setText("\u2605");
        else addFav.setText("\u2606");
    }

    /*
      Used com.android.browser.UrlUtils library
     */
    private void loadUrl() {
        String input = urlField.getText();
        if (isValid(input)) {
            if (headerLegal(input)) engine.load(input);
            else engine.load(UrlUtils.guessUrl(input));
        }
        // Google search
        else engine.load(UrlUtils.smartUrlFilter(input));
    }

    private boolean headerLegal(String url) {
        return STRIP_URL_PATTERN.matcher(url).matches();
    }

    private boolean isValid(String url) {
        return WEB_URL.matcher(url).matches();
    }


    /**
     * Set up browser header
     * @return
     */
    private HBox setHeader() throws ParseException, FileNotFoundException {
        HBox toReturn = new HBox(10);
        toReturn.setAlignment(Pos.CENTER_LEFT);

        // Buttons
        ButtonBar naviBar = new ButtonBar();
        Button backward = new Button("\u21E0");
        Button forward = new Button("\u21E2");

        forward.setOnAction(a -> goForward());
        backward.setOnAction(a -> goBackward());
        reload.setOnAction(a -> {
            if (engine.getLoadWorker().stateProperty().equals(Worker.State.RUNNING)) {
                Worker<Void> loadWorker = engine.getLoadWorker();
                if (loadWorker != null) {
                    Platform.runLater(() -> loadWorker.cancel());
                    engine.load(null);
                }
            }
            else loadUrl();
        });
        ButtonBar.setButtonData(backward, ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonBar.setButtonData(forward, ButtonBar.ButtonData.NEXT_FORWARD);
        ButtonBar.setButtonData(reload, ButtonBar.ButtonData.APPLY);
        naviBar.getButtons().addAll(backward, forward);
        naviBar.getButtons().add(reload);
        naviBar.setPadding(new Insets(8, 8, 8, 8));
        naviBar.setButtonMinWidth(50);
        naviBar.setPrefSize(200, 30);
        
        // Url field
        HBox urlFieldHolder = new HBox();
        urlFieldHolder.setPadding(new Insets(8, 12, 8, 12));
        urlFieldHolder.setAlignment(Pos.CENTER);

        urlField.setPrefSize(2560, 30);
        urlField.setPromptText("Search Google or type a URL");
        urlFieldHolder.getChildren().add(urlField);

        HBox featureBox = new HBox(10);

        featureBox.setPadding(new Insets(8, 8, 8, 2));
        featureBox.setAlignment(Pos.CENTER_LEFT);
        featureBox.setPrefSize(150, 30);
        Button history = new Button("\u231A");
        history.setTooltip(new Tooltip("Show history"));
        history.setOnAction(a -> historyPage());

        /* Button for add to favorite */
        addFav = new Button("\u2606"); // Add the current page to fav file and update the fav menu
        addFav.setTooltip(new Tooltip("Add to bookmark"));
        addFav.setOnAction(a -> {
            try {
                if (addToFav()) {
                    updateFavMenu(); // write to dir then call updateFavMenu TODO if return false, delete from BM
                    addFav.setText("\u2605");
                    popUpAlert();
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        });

        /* MenuBar for bookmarks */
        favMenu = new MenuButton("", addFav);
        favMenu.setStyle("-fx-background-color: rgba(255, 255, 255, 0);" + "-fx-selection-bar: #ffffff00;");
        favMenu.setTooltip(new Tooltip("Bookmarks"));
        updateFavMenu(); // load up bookmarks
//        favBar.getMenus().add(favMenu);
        
        /* Print */
        Button print = new Button("\u2399");
        print.setOnAction(a -> printPage());
        print.setTooltip(new Tooltip("Print page"));

        featureBox.getChildren().addAll(favMenu, history, print);

        toReturn.getChildren().addAll(naviBar, urlFieldHolder, featureBox);
        return toReturn;
    }

    private void popUpAlert() {
        Dialog<ButtonType> bookmarkNotice = new Dialog<>();
        bookmarkNotice.getDialogPane().getButtonTypes().add(ButtonType.OK);

        bookmarkNotice.setContentText("Bookmarked this web site!");

        Button ok = (Button) bookmarkNotice.getDialogPane().lookupButton(ButtonType.OK);
        ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                bookmarkNotice.close();
            }
        });
        bookmarkNotice.show();
    }

    /**
     * Add the current page to the data file.
     */
    private boolean addToFav() throws IOException {
        String title = engine.getTitle();
        if (title.equals("$HISTORY$")) return false; // Do not store history page

        title = fixFileName(title);

        File newFav = new File("./src/favPage/" + title);
        if (newFav.exists()) {return false;}
        newFav.createNewFile();

        String[] list = bookmarkDir.list();
        int ID = list.length;

        StringBuilder bookMarkData = new StringBuilder();
        bookMarkData.append(ID);
        bookMarkData.append("\n");
        bookMarkData.append(engine.getTitle());
        bookMarkData.append("\n");
        bookMarkData.append(engine.getLocation());

        FileWriter bookMarkWriter;
        try {
            bookMarkWriter = new FileWriter(newFav, false);
            bookMarkWriter.write(bookMarkData.toString());
        } catch (IOException e) {
            System.out.println("Unable to write bookmark to disk" + e);
            return false;
        }

        bookMarkWriter.close();

        return true;
    }

    /** Do a filter on forbidden characters for file name */
    private String fixFileName(String title) {
        Matcher fileNameMatcher = fileNamePattern.matcher(title);
        title = fileNameMatcher.replaceAll("");
        return title;
    }

    /**
     * Update the fav menu, everytime the addToFav is clicked. Or load up once the program is started.
     */
    private void updateFavMenu() throws ParseException, FileNotFoundException {
        String[] list = bookmarkDir.list();
        int ID = list.length;

        for (String s : list) {
            File favToAdd = new File("./src/favPage/" + s);
            MenuItem mi = makeMenuItem(favToAdd);
            if (!favMenu.getItems().contains(mi)) favMenu.getItems().add(mi);
        }
    }

    /**
     * Make a WebAddress extends MenuItem and return.
     * @param fav
     * @return
     * @throws ParseException
     * @throws FileNotFoundException
     */
    private MenuItem makeMenuItem(File fav) throws ParseException, FileNotFoundException {
        Scanner sc = new Scanner(fav);
        sc.nextLine();
        String title = sc.nextLine();
        String url = sc.nextLine();
        sc.close();

        MenuItem waToReturn = new WebAddress(url);
        ((WebAddress) waToReturn).setPath(url);
        waToReturn.setText(title);

        /* Setup action on click */
        waToReturn.setOnAction(a -> engine.load(url));

        return waToReturn;
    }

    private void printPage() {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null) {
                engine.print(job);
                job.endJob();
            }
    }

    private void historyPage() {
        try {
            //  File f = new File("/media/liheng/M2.1/Users/Liam Han/IdeaProjects/SWEN502/WebBrowser/hist.html");
            File f = new File("hist.html");
            /*
              Helper class to write all history entries into a html file.
             */
            if (HistoryBookMarkUtils.helper(historyEntries)) engine.load(f.toURI().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void goBackward() {
        if (history.getCurrentIndex() != 0) getHistory().go(-1);
    }

    private void goForward() {
        if (history.getCurrentIndex() != historyEntries.size() - 1) getHistory().go(1);
    }

    private WebHistory getHistory() {
        return history;
    }

    /**
     * Setup browser tab
     */
    private WebView initial() {
        WebView EngToReturn = new WebView();
        engine = EngToReturn.getEngine();
        engine.load(defaultURL);
        history = engine.getHistory();
        historyEntries = history.getEntries();

        return EngToReturn;
    }



}
