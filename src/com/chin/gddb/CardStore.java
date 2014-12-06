package com.chin.gddb;

import java.util.ArrayList;
import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import com.chin.gddb.CardStore;

import android.content.Context;
import android.util.Log;

/**
 * A singleton class that acts as a storage for card information. Support lazy loading information.
 *
 * For now:
 * - the cardDOM is saved, so if you want the details from it you need to parse it manually
 *
 * @author Chin
 *
 */
public final class CardStore {

    public static class Pair {
        public String key;
        public String value;
        public Pair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    // a list of all cards available, initialized in MainActivity's onCreate()
    public static ArrayList<String> cardList = null;

    // map a card name to its wiki page url, initialized in MainActivity's onCreate()
    public static Hashtable<String, String[]> cardLinkTable = null;

    // a storage for cards' detail after being fetched online
    private static Hashtable<String, Element> cardDomCache = new Hashtable<String, Element>();

    private static CardStore CARDSTORE;
    private static Context context;

    static boolean initializedOnline = false;
    /**
     * Private constructor. For singleton.
     */
    private CardStore(Context context) {
        if (CARDSTORE != null) {
            throw new IllegalStateException("Already instantiated");
        }
        CardStore.context = context;
    }

    /**
     * Get the only instance of this class. Because of singleton.
     * @return The only instance of this class.
     */
    public static CardStore getInstance(Context context) {
        if (CARDSTORE == null) {
            CARDSTORE = new CardStore(context);
        }
        return CARDSTORE;
    }

    public void initializeCardList() throws Exception {
        if (initializedOnline) return;

        Log.i("gddb", "Initializing online...");
        cardList = new ArrayList<String>(4096);
        cardLinkTable = new Hashtable<String, String[]>(4096);
        initializeCardListOnline(null);
        initializedOnline = true;
        Log.i("gddb", "Done initializing online.");

        Log.i("gddb", "Number of cards: " + cardList.size());
    }

    private void initializeCardListOnline(String offset) throws Exception {
        // this will return up to 5000 articles in the Mobile_Weapons category. Note that this is not always up-to-date,
        // as newly added articles may take a day or two before showing up in here
        String url  = "http://gundam.wikia.com/api/v1/Articles/List?category=Mobile_Weapons&limit=5000&namespaces=0";

        if (offset != null) {
            url = url + "&offset=" + offset;
        }
        String jsonString = Jsoup.connect(url).ignoreContentType(true).execute().body();

        JSONObject myJSON = new JSONObject(jsonString);
        JSONArray myArray = myJSON.getJSONArray("items");
        for (int i = 0; i < myArray.length(); i++) {
            String cardName = myArray.getJSONObject(i).getString("title");
            if (!cardLinkTable.containsKey(cardName) && !cardName.startsWith("List of") && !cardName.contains("Mobile Weapon")) {
                cardList.add(cardName);
                String[] tmp = {myArray.getJSONObject(i).getString("url"),
                                myArray.getJSONObject(i).getString("id")};
                cardLinkTable.put(cardName, tmp);
            }
        }

        if (myJSON.has("offset")) {
            initializeCardListOnline((String) myJSON.get("offset"));
        }
    }

    public void getCardDomReady(String cardName) throws Exception {
        initializeCardList();

        if (cardDomCache.containsKey(cardName)) {
            return; // already cached, just return
        }

        String cardURL = "http://gundam.wikia.com" + CardStore.cardLinkTable.get(cardName)[0];

        String cardHTML = null;
        try {
            cardHTML = Jsoup.connect(cardURL).ignoreContentType(true).execute().body();
        } catch (Exception e) {
            Log.e("CardDetail", "Error fetching the card HTML page");
            e.printStackTrace();
        }
        Element cardDOM = Jsoup.parse(cardHTML).getElementById("mw-content-text");
        cardDOM = HtmlCleaner.getCleanedHtml(cardDOM);

        // save the DOM for later use. Should look into this so that it doesn't cause huge mem usage
        cardDomCache.put(cardName, cardDOM);
    }

    public Element getCardDom(String cardName) throws Exception {
        initializeCardList();
        getCardDomReady(cardName);
        return cardDomCache.get(cardName);
    }
}

