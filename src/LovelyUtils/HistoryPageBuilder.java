package LovelyUtils;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.web.WebHistory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ListIterator;

/**
 * Steal from www.javatips.net.
 *
 * https://www.javatips.net/api/Lightning-Browser-master/app/src/main/java/acr/browser/lightning/constant/HistoryPage.java
 */
public class HistoryPageBuilder {

    public static final String FILENAME = "hist.html";

    private static final String HEADING_1 = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta content=\"en-us\" http-equiv=\"Content-Language\" /><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\"><title>";

    private static final String HEADING_2 = "</title></head><style>body { background: #f5f5f5;}.box { vertical-align:middle;position:relative; display: block; margin: 10px;padding-left:10px;padding-right:10px;padding-top:5px;padding-bottom:5px; background-color:#fff;box-shadow: 0px 2px 3px rgba( 0, 0, 0, 0.25 );font-family: Arial;color: #444;font-size: 12px;-moz-border-radius: 2px;-webkit-border-radius: 2px;border-radius: 2px;}.box a { width: 100%; height: 100%; position: absolute; left: 0; top: 0;}.black {color: black;font-size: 15px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}.font {color: gray;font-size: 10px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}</style><body><div id=\"content\">";

    private static final String PART1 = "<div class=\"box\"><a href=\"";

    private static final String PART2 = "\"></a><p class=\"black\">";

    private static final String PART3 = "</p><p class=\"font\">";

    private static final String PART4 = "</p></div></div>";

    private static final String END = "</div></body></html>";

    private final static String mTitle = "$HISTORY$";

    private String mHistoryUrl = null;

    private HistoryPageBuilder() {
    }

    /**
     * Helper method to create a history.html file, from latest to beginning of this engine
     * @param historyEntries is history entries
     * @return is a boolean if the file is created
     * @throws IOException
     */
    public static boolean helper(ObservableList<WebHistory.Entry> historyEntries) throws IOException {

        StringBuilder historyBuilder = new StringBuilder(HEADING_1 + mTitle + HEADING_2);

        // From latest to ancient
        ListIterator<WebHistory.Entry> it = historyEntries.listIterator(historyEntries.size());

        WebHistory.Entry entry;
        while (it.hasPrevious()) {
            entry = it.previous();
            if (!entry.getTitle().equals("$HISTORY$") && !entry.getTitle().equals("New Tab")) { // Do not show history of HISTORY page
                historyBuilder.append(PART1);
                historyBuilder.append(entry.getUrl());
                historyBuilder.append(PART2);
                historyBuilder.append(entry.getTitle()).append(" | " + entry.getLastVisitedDate());
                historyBuilder.append(PART3);
                historyBuilder.append(entry.getUrl());
                historyBuilder.append(PART4);
            }
        }

        historyBuilder.append(END);

        File historyWebPage = new File(FILENAME);
        FileWriter historyWriter = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            historyWriter = new FileWriter(historyWebPage, false);
            historyWriter.write(historyBuilder.toString());
        } catch (IOException e) {
            System.out.println("Unable to write history page to disk" + e);
            return false;
        }
        historyWriter.close();
        return true;
    }

    /**
     * Use this method to immediately delete the history
     * page on the current thread. This will clear the
     * cached history page that was stored on file.
     *
     * @param application the application object needed to get the file.
     */
    public static void deleteHistoryPage(Application application) {
        File historyWebPage = new File(FILENAME);
        if (historyWebPage.exists()) {
            historyWebPage.delete();
        }
    }

    /**
     * Append writing the history html file, from furthest to latest.
     * @param entry is the current WebHistory.Entry which triggered this call.
     * @throws IOException
     */
    public static void newHelper(WebHistory.Entry entry) throws IOException {

        StringBuilder historyBuilder = new StringBuilder(HEADING_1 + mTitle + HEADING_2);

        if (!entry.getTitle().equals("$HISTORY$") && !entry.getTitle().equals("New Tab")) {
            historyBuilder.append(PART1);
            historyBuilder.append(entry.getUrl());
            historyBuilder.append(PART2);
            historyBuilder.append(entry.getTitle()).append(" | " + entry.getLastVisitedDate());
            historyBuilder.append(PART3);
            historyBuilder.append(entry.getUrl());
            historyBuilder.append(PART4);
        }

        historyBuilder.append(END);

        File historyWebPage = new File(FILENAME);
        FileWriter historyWriter = null;

        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            historyWriter = new FileWriter(historyWebPage, true);
            historyWriter.write(historyBuilder.toString());
        } catch (IOException e) {
            System.out.println("Unable to write history page to disk" + e);
        }
        historyWriter.close();
    }

}
