package com.chin.gddb.activity;

import com.chin.gddb.PagerSlidingTabStrip;
import com.chin.gddb.R;
import com.chin.gddb.asyncTask.AddImageTabTask;
import com.chin.gddb.asyncTask.AddOverviewDetailTabsTask;

import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

/**
 * Activity to show all details about a card
 */
public class CardDetailActivity extends BaseFragmentActivity {

    public String cardName = null;

    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private MyPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            cardName = savedInstanceState.getString("CARDNAME");
        }
        else {
            Intent intent = getIntent(); // careful, this intent may not be the intent from MainActivity...
            String tmpName = intent.getStringExtra(MainActivity.CARD_NAME);
            if (tmpName != null) {
                cardName = tmpName; // needed since we may come back from other activity, not just the main one
            }
        }

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new MyPagerAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);

        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setViewPager(pager);
        tabs.setIndicatorColor(getResources().getColor(R.color.bluegrey));

        getActionBar().setTitle(cardName);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("CARDNAME", cardName);
    }

    /**
     * Fragment for the card info view
     */
    public static class CardInfoFragment extends Fragment {

        AsyncTask<?, ?, ?> myTask = null;
        static String cardName;

        public CardInfoFragment(String cardName) {
            CardInfoFragment.cardName = cardName;
        }

        public CardInfoFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View view = inflater.inflate(R.layout.fragment_card_info, container, false);
            myTask = new AddImageTabTask((CardDetailActivity) getActivity()).execute(cardName);
            return view;
        }

        @Override
        public void onPause() {
            super.onPause();
            if (myTask != null) {
                myTask.cancel(true);
                myTask = null;
            }
        }
    }

    public static class CardGenericDetailFragment extends Fragment {
        AddOverviewDetailTabsTask myTask;

        private static final String TYPE = "TYPE";
        private static final String CARD_NAME = "CARD_NAME";

        String type;
        String cardName;

        public static CardGenericDetailFragment newInstance(String type, String cardName) {
            CardGenericDetailFragment f = new CardGenericDetailFragment();
            Bundle b = new Bundle();
            b.putSerializable(TYPE, type);
            b.putString(CARD_NAME, cardName);
            f.setArguments(b);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            cardName = getArguments().getString(CARD_NAME);
            type     = getArguments().getString(TYPE);
            setRetainInstance(true);
        }

        @SuppressLint("RtlHardcoded")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_general_linear, container, false);
            LinearLayout layout = (LinearLayout) view.findViewById(R.id.fragment_layout);
            layout.setGravity(Gravity.RIGHT);

            myTask = (AddOverviewDetailTabsTask) new AddOverviewDetailTabsTask(layout, (CardDetailActivity) getActivity(), type, cardName).execute();

            return view;
        }

        @Override
        public void onPause() {
            super.onPause();
            if (myTask != null) {
                myTask.cancel(true);
                myTask = null;
            }
        }
    }

    public class MyPagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = { "Overview", "Image", "Detail"};

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return CardGenericDetailFragment.newInstance("Overview", cardName);
            }
            else if (position == 1){
                return new CardInfoFragment(cardName);
            }
            else {//if (position == 1){
                return CardGenericDetailFragment.newInstance("Detail", cardName);
            }
        }
    }
}
