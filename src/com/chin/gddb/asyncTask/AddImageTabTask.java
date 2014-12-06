package com.chin.gddb.asyncTask;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.chin.common.Util;
import com.chin.gddb.CardStore;
import com.chin.gddb.R;
import com.chin.gddb.activity.CardDetailActivity;
import com.nostra13.universalimageloader.core.ImageLoader;
import android.graphics.Point;
import android.os.AsyncTask;
import android.view.Display;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * The async task that populate the information in the Image tab
 * It is put into a separate file since it is too long
 */
public class AddImageTabTask extends AsyncTask<String, Void, Void> {

    CardDetailActivity activity;
    String cardName;
    CardStore cardStore;

    public AddImageTabTask(CardDetailActivity activity) {
        this.activity = activity;
        this.cardStore = CardStore.getInstance(activity);
    }

    @Override
    protected Void doInBackground(String... params) {
        cardName = params[0];

        try { cardStore.getCardDomReady(cardName);     } catch (Exception e) {e.printStackTrace();}
        if (isCancelled()) {return null; }; // attempt to return early
        return null;
    }

    @Override
    protected void onPostExecute(Void params) {
        // all of these should be fast
        try { addCardImage();               } catch (Exception e) {e.printStackTrace();}
    }

    public void addCardImage() throws Exception {
        // remove the spinner
        ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.fragmentCardInfo_progressBar1);
        LinearLayout layout = (LinearLayout) activity.findViewById(R.id.fragmentCardInfo_mainLinearLayout);
        layout.removeView(pgrBar);

        // calculate the width of the images to be displayed
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int scaleWidth = (int) (screenWidth * 0.8);

        Element dom = cardStore.getCardDom(cardName);
        Elements tabs = dom.select(".tabbertab");

        if (tabs.isEmpty()) {
            Element infoBox = dom.select(".infobox").first();

            // try to get the image first
            String imgSrc = infoBox.select("img").first().attr("src");

            // if the src is a data link, try to use the a element
            if (imgSrc.startsWith("data")) {
                imgSrc = infoBox.select("a").first().attr("href");
            }

            ImageView imgView = new ImageView(activity);
            imgView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            imgView.setScaleType(ScaleType.CENTER_CROP);
            imgView.setAdjustViewBounds(true);
            layout.addView(imgView);

            ImageLoader.getInstance().displayImage(Util.getScaledWikiaImageLink(imgSrc, scaleWidth), imgView);
        }
        else {
            for (Element tab : tabs) {
                String title = tab.attr("title").trim();
                if (title.startsWith("Video")) continue;

                ImageView imgView = new ImageView(activity);
                imgView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

                // make the imgView have a width of fill_parent and a height scaled proportionately
                imgView.setScaleType(ScaleType.CENTER_CROP);
                imgView.setAdjustViewBounds(true);
                layout.addView(imgView);

                // get image link
                String originalLink = tab.getElementsByTag("img").first().attr("src");
                if (originalLink.startsWith("data")) {
                    originalLink = tab.getElementsByTag("a").first().attr("href");
                }

                ImageLoader.getInstance().displayImage(Util.getScaledWikiaImageLink(originalLink, scaleWidth), imgView);

                // label
                TextView tv = new TextView(activity);
                tv.setGravity(Gravity.CENTER);
                tv.setText(title);
                layout.addView(tv);

                // dummy row for separator
                tv = new TextView(activity);
                tv.setGravity(Gravity.CENTER);
                layout.addView(tv);
            }
        }
    }
}