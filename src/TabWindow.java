import LovelyUtils.HistoryBookMarkUtils;
import LovelyUtils.UrlUtils;
import LovelyUtils.WebAddress;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.io.*;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Scanner;
import java.util.regex.Matcher;

import static LovelyUtils.UrlUtils.*;

/**
 * This is a Tab holding a seperated WebView.
 * After each construct, add to the main class TabPane: tabRoot.
 */
public class TabWindow extends Tab {

    /** Start Page of each new tab */
    private String defaultURL;
    private final TextField urlField = new TextField();

    /** Own webview */
    private WebView webView;
    private WebEngine engine;

    /** Web History */
    private WebHistory history;
    private ObservableList<WebHistory.Entry> historyEntries;

    /** Might be useful fields */
    private static String tabTitle;

    /** Containers */
    VBox tabHolder;
    HBox ctrlBar;

    /** Reload button */
    Button reload = new Button("\u21BB");

    /** Bookmark menu */
    private MenuButton favMenu;
    private Button addFav;

    /** File directory of bookmarks */
    private final File BOOKMARK_DIR = new File("./src/favPage/");

    /** Ctrl key boolean for Zoom */
    private boolean ctrlDown = false;

    /** For blank tab */
    public TabWindow(int ID) throws FileNotFoundException, ParseException {
        // 1 Start engine
        File newTab = new File("NewTab.html");
        this.defaultURL = newTab.toURI().toString();
        this.webView = initWebView();
        // 2 Build UI
        initTabUI();
        this.setId(""+ID);
        // 3 Set up the events handlers
        eventsHandlerSetup();
    }

    /**
     * For first load of HomePage
     * @param initUrl is the HomePage url
     * @param ID is the Tab ID, would be useful when dragging tabs around
     * @throws FileNotFoundException
     * @throws ParseException
     */
    public TabWindow(String initUrl, int ID) throws FileNotFoundException, ParseException {
        // 1 Start engine and initialize the WebView
        this.defaultURL = initUrl;
        this.webView = initWebView();
        // 2 Build UI
        initTabUI();
        this.setId(""+ID);
        // 3 Set up the events handlers
        eventsHandlerSetup();
    }

    /**
     * Initialize the WebView and return
     * @return a WebView with defaultURL (can be HomePage(ID 1) or blank(ID > 1))
     */
    private WebView initWebView() {
        WebView toReturn = new WebView();
        engine = toReturn.getEngine();
        engine.load(defaultURL);

        history = engine.getHistory();
        historyEntries = history.getEntries();

        return toReturn;
    }

    /**
     * Only initialize the UI part, tabHolder is the root, contains ctrl bar and WebView
     * @throws FileNotFoundException
     * @throws ParseException
     */
    private void initTabUI() throws FileNotFoundException, ParseException {

        /* Initial the main container of ctrl bar */
        tabHolder = new VBox();
        VBox.setVgrow(webView, Priority.ALWAYS);

        /* Control(HBox) */
        ctrlBar = setCtrlBar();

        webView.prefHeightProperty().bind(tabHolder.prefHeightProperty());

        /* Assemble the ctrl bar and WebView */
        tabHolder.getChildren().addAll(ctrlBar, webView);

        /* Final assembling to this.TabWindow */
        this.setContent(tabHolder);
    }

    private HBox setCtrlBar() throws FileNotFoundException, ParseException {
        HBox toReturn = new HBox(10);
        toReturn.prefWidthProperty().bind(tabHolder.prefWidthProperty());
        toReturn.setStyle("-fx-background-color: rgba(31, 31, 60, 1);"); // TODO can update theme here

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
        Button showHistory = new Button("\u231A");
        showHistory.setTooltip(new Tooltip("Show history"));
        showHistory.setOnAction(a -> showHistory()); // TODO updated but failed by engine loading problem

        /* Button for add to favorite */
        addFav = new Button("\u2606"); // Add the current page to fav file and update the fav menu
        addFav.setTooltip(new Tooltip("Add to bookmark"));
        addFav.setOnAction(a -> {
            try {
                if (addToFav()) {
                    updateFavMenu(); // write to dir then call updateFavMenu TODO if return false, delete from BM
                    addFav.setText("\u2605");
                    popUpAlert("Bookmarked this web site!");
                }
                else {
                    deleteBookMark();
                    addFav.setText("\u2606");
                    popUpAlert("Un-Bookmarked this web site!");
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

        /* Printer Button */
        Button print = new Button("\u2399");
        print.setOnAction(a -> printPage(webView));
        print.setTooltip(new Tooltip("Print page"));

        /* Assemble the left part of ctrl panel (navi buttons) */
        naviBar.getButtons().addAll(backward, forward, reload);

        /* Assemble the right side of ctrl panel (feature buttons) */
        featureBox.getChildren().addAll(favMenu, showHistory, print);

        /* Assemble the whole ctrl panel */
        toReturn.getChildren().addAll(naviBar, urlFieldHolder, featureBox);

        return toReturn;
    }

    private void initTabUrlText() {
        if (!tabTitle.equals("New Tab")) urlField.setText(engine.getLocation());
        this.setText(tabTitle);
        this.setTooltip(new Tooltip(tabTitle));
    }

    /**
     * Setup all events
     */
    private void eventsHandlerSetup() {

        /*
          Update stage Title to current site Title.
         */
        engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State oldV, Worker.State newV) {
                if (newV == Worker.State.SUCCEEDED) {
                    /* For set tab title later */
                    tabTitle = engine.getTitle();
                    initTabUrlText();
                    WBDemoVer1.setupTitle(engine.getTitle()); // TODO check
                    reload.setText("\u21BB");
                    /* For update the bookmark icon */
                    updateFavMenuIcon();
                    /* For update tab text graphic */
                    loadFavicon(engine.getLocation());
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

        webView.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.isControlDown()) {
                    ctrlDown = true;
                }
            }
        });

        /* Following 2 handlers help to Zoom in/out */
        webView.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                ctrlDown = false;
            }
        });

        webView.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent scrollEvent) {
                if (ctrlDown) {
                    double dY = scrollEvent.getDeltaY();
                    if (dY > 0) webView.setZoom(webView.getZoom() * 1.05);
                    else if (dY < 0) webView.setZoom(webView.getZoom() / 1.05);
                }
            }
        });

        /* This is an approach I want, but this listener is not synced with the loading thread of engine */
        historyEntries.addListener(new ListChangeListener<WebHistory.Entry>() {
            @Override
            public void onChanged(Change<? extends WebHistory.Entry> change) {
                try {
                    writeHistory();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

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
     * Write to historyPage file
     */
    private void showHistory() {

        try {
            File f = new File("hist.html");
            /* Helper class to write all history entries into a html file */
            if (HistoryBookMarkUtils.helper(historyEntries)) engine.load(f.toURI().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* Better approach rests in peace here */
//        File f = new File("hist.html");
//        engine.load(f.toURI().toString());
    }

    /**
     * Write to hist.html right away after the change to WebHistory.Entry was detected.
     */
    private void writeHistory() throws IOException {
//        HistoryPageBuilder.newHelper(historyEntries.get(history.getCurrentIndex()));
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

        String[] list = BOOKMARK_DIR.list();
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
        String[] list = BOOKMARK_DIR.list();
        int ID = list.length;

        for (String s : list) {
            File favToAdd = new File("./src/favPage/" + s);
            MenuItem mi = makeMenuItem(favToAdd);
            if (!favMenu.getItems().contains(mi)) favMenu.getItems().add(mi);
        }
    }

    /**
     * Delete the bookmark
     * @throws FileNotFoundException
     * @throws ParseException
     */
    public void deleteBookMark() throws FileNotFoundException, ParseException {
        String[] list = BOOKMARK_DIR.list();
        int ID = list.length;
        String title = engine.getTitle();
        title = fixFileName(title);
        File toDelete = null;

        for (String s : list) {
            File favToAdd = new File("./src/favPage/" + s);
            MenuItem mi = makeMenuItem(favToAdd);
            if (favToAdd.getPath().equals("./src/favPage/" + title)) {
                favMenu.getItems().remove(mi);
                toDelete = favToAdd;
                break;
            }
        }

        if (toDelete != null) HistoryBookMarkUtils.deleteBookMark(toDelete);
    }

    /**
     * Make a WebAddress extends MenuItem and return.
     * @param fav is the File to make add, maybe already exist or new
     * @return a MenuItem to add into the bookmark MenuButton
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

    /**
     * Popup Dialog to notice user if a bookmark has been added/removed.
     */
    private void popUpAlert(String s) {
        Dialog<ButtonType> bookmarkNotice = new Dialog<>();
        bookmarkNotice.getDialogPane().getButtonTypes().add(ButtonType.OK);

        bookmarkNotice.setContentText(s);

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
     * A listener helper to update bookmark icon
     */
    private void updateFavMenuIcon() {
        String titleToCheck = fixFileName(engine.getTitle());
        File newFav = new File("./src/favPage/" + titleToCheck);
        if (newFav.exists()) addFav.setText("\u2605");
        else addFav.setText("\u2606");
    }

    /**
     * Printing method, don't know how to get it work TODO check
     */
    private void printPage(final Node webView) {
//        PageLayout pageLayout = Printer.getDefaultPrinter().createPageLayout(Paper.NA_LETTER, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
//        double scaleX = pageLayout.getPrintableWidth() / webView.getBoundsInParent().getWidth();
//        double scaleY = pageLayout.getPrintableHeight() / webView.getBoundsInParent().getHeight();
//        webView.getTransforms().add(new Scale(scaleX, scaleY));

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            engine.print(job);
            job.endJob();
        }
    }

    /**
     * Steal from stackoverflow:
     * https://stackoverflow.com/questions/27691381/javafx-get-favicon-with-web-browser/35327398
     * @param location is current engine location
     */
    private void loadFavicon(String location) {
        try {
            String faviconUrl = String.format("http://www.google.com/s2/favicons?domain_url=%s", URLEncoder.encode(location, "UTF-8"));
            Image favicon = new Image(faviconUrl, true);
            ImageView iv = new ImageView(favicon);
            this.setGraphic(iv);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex); // not expected
        }
    }

    /**
     * Go back action
     */
    private void goBackward() {
        if (history.getCurrentIndex() != 0) getHistory().go(-1);
    }

    /**
     * Go forward action
     */
    private void goForward() {
        if (history.getCurrentIndex() != historyEntries.size() - 1) getHistory().go(1);
    }

    public WebEngine getEngine() {
        return engine;
    }

    private WebHistory getHistory() {
        return history;
    }

}
