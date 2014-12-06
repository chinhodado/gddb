package com.chin.gddb.asyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import com.chin.common.MyTagHandler;
import com.chin.common.Util;
import com.chin.gddb.CardStore;
import com.chin.gddb.R;
import com.chin.gddb.activity.CardDetailActivity;
import com.nostra13.universalimageloader.core.ImageLoader;

public class AddOverviewDetailTabsTask extends AsyncTask<String, Void, Element> {
    LinearLayout layout;
    CardDetailActivity activity;
    String type;
    String cardName;
    boolean exceptionOccurred = false;

    public AddOverviewDetailTabsTask(LinearLayout layout, CardDetailActivity activity, String type, String cardName) {
        this.layout = layout;
        this.activity = activity;
        this.type = type;
        this.cardName = cardName;
    }

    @Override
    protected Element doInBackground(String... params) {
        try {
            return CardStore.getInstance(activity).getCardDom(cardName);
        } catch (Exception e) {
            e.printStackTrace();

            // set the flag so we can do something about this in onPostExecute()
            exceptionOccurred = true;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Element param) {

        if (exceptionOccurred) {
            TextView tv = new TextView(activity);
            layout.addView(tv);
            tv.setText("Not available");
        }
        else {
            if (type.equals("Overview")) {
                addOverview(param);
            }
            else if (type.equals("Detail")){
                addDetail(param);
            }
        }

        // remove the spinner
        ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.progressBar_fragment_general);
        layout.removeView(pgrBar);
    }

    /**
     * Basically put the content of the wiki article into a TextView
     * @param param
     */
    private void addDetail(Element param) {
        TextView tv = new TextView(activity);

        // clone to remove the infobox
        param = Jsoup.parse(param.html());
        param.select(".infobox").remove();

        // turn <span> to <a>, <dt> to <b>
        String html = param.html();
        html = html.replace("<a", "<span").replace("/a>", "/span>").replace("<dt", "<b").replace("/dt>", "/b>");

        tv.setText(Html.fromHtml(html, null, new MyTagHandler()));
        layout.addView(tv);
    }

    /***
     * Parse and display the wiki infobox of the article
     * @param param
     */
    private void addOverview(Element param) {
        Element infobox = param.select(".infobox").first();

        // add a small image at the top of the info table
        // calculate the width of the images to be displayed
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int scaleWidth = (int) (screenWidth * 0.5);
        ImageView imgView = new ImageView(activity);
        imgView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        imgView.getLayoutParams().width = scaleWidth;
        imgView.requestLayout();

        // make the imgView have a width of fill_parent and a height scaled proportionately
        imgView.setScaleType(ScaleType.CENTER_CROP);
        imgView.setAdjustViewBounds(true);
        layout.addView(imgView);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        try {
            String originalLink = infobox.getElementsByTag("img").first().attr("src");
            if (originalLink.startsWith("data")) {
                originalLink = infobox.getElementsByTag("a").first().attr("href");
            }
            ImageLoader.getInstance().displayImage(Util.getScaledWikiaImageLink(originalLink, scaleWidth), imgView);
        }
        catch (Exception e) {
            Log.i("gddb", "Image not found for " + cardName);
        }

        TableLayout table = null;

        // get all rows that are the (first) children of the infobox's body
        String domTxt = param.html();
        domTxt = domTxt.replace("</ul>", "chinho</ul>"); //  hackish to preserve newline, replace "chinho" with "\n" later
        param = Jsoup.parse(domTxt);
        infobox = infobox.select("tbody").first();
        Elements rows = infobox.select(":root > tr");

        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements rowChild = row.children();
            try {
                Elements nestedRows = rowChild.select("tr");
                if (nestedRows.isEmpty()) { // no nested row, aka "real" row
                    Elements headers = row.select(".mainheader");
                    if (headers.isEmpty()) { // key/value row
                        boolean addSeparator = i == rows.size() - 1? false : !isHeaderRow(rows.get(i + 1));
                        processSimpleInfoRow(table, row, addSeparator);
                    }
                    else { // header row
                        table = new TableLayout(activity);
                        processSimpleHeaderRow(table, headers.first().text());
                    }
                }
                else { // row with nested rows
                    processComplexRow(row);
                }
            } catch (Exception e) {
                //e.printStackTrace();
                Log.i("gddb", e.getClass().getName() + " when processing info table, row " + i);
            }
        }
    }

    /**
     * Process a simple key-value info row
     * @param table The table to add this row to
     * @param row The row to process
     * @param addSeparator Whether or not to add a line separator after this row
     */
    private void processSimpleInfoRow(TableLayout table, Element row, boolean addSeparator) {
        String headerTxt = row.select("th").first().text();
        String value = row.select("td").first().text().replace("chinho", "\n");
        Util.addRowWithTwoTextView(activity, table, headerTxt + "  ", value, addSeparator);
    }

    /**
     * Process a simple header row
     * @param table The table that this row will be a header of
     * @param headerText The header text of this row
     */
    private void processSimpleHeaderRow(TableLayout table, String headerText) {
        String headerTxt = headerText;
        TextView tv = new TextView(activity);
        tv.setText(headerTxt);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(activity.getResources().getColor(R.color.bluegrey));
        tv.setTextColor(Color.WHITE);
        layout.addView(tv);

        table.setLayoutParams(new LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
        table.setColumnShrinkable(1, true);
        layout.addView(table);
    }

    /**
     * Process a complex row, aka row with nested rows
     * @param row
     */
    private void processComplexRow(Element row) {
        Element tbody = row.select("tbody").first();
        Elements innerRows = tbody.select("tr");

        TextView tv = new TextView(activity);
        tv.setText(innerRows.get(0).text());
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(activity.getResources().getColor(R.color.bluegrey));
        tv.setTextColor(Color.WHITE);
        layout.addView(tv);

        tv = new TextView(activity);
        tv.setText(innerRows.get(1).text().replace("chinho", "\n"));
        tv.setGravity(Gravity.CENTER);
        layout.addView(tv);
    }

    /**
     * Check if a row in the infobox is a header row
     * @param row The row to check
     * @return true if it is a header row
     */
    private boolean isHeaderRow(Element row) {
        if (row == null) return false;
        Elements rowChild = row.children();
        Elements nestedRows = rowChild.select("tr");
        if (nestedRows.isEmpty()) { // no nested row, aka "real" row
            Elements headers = row.select(".mainheader");
            if (headers.isEmpty()) {
                return false;
            }
            else {
                return true;
            }
        }
        else { // "combined" row, can be considered header
            return true;
        }
    }
}