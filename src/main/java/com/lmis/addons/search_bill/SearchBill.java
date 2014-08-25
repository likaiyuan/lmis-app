package com.lmis.addons.search_bill;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import com.lmis.Lmis;
import com.lmis.R;
import com.lmis.addons.carrying_bill.CarryingBillDB;
import com.lmis.support.BaseFragment;
import com.lmis.support.LmisDialog;
import com.lmis.util.drawer.DrawerItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

/**
 * Created by chengdh on 14-8-25.
 */
public class SearchBill extends BaseFragment {

    public final static String TAG = "SearchBill";


    @InjectView(R.id.txv_from_org)
    TextView mTxvFromOrg;

    @InjectView(R.id.txv_to_org)
    TextView mTxvToOrg;

    @InjectView(R.id.txv_bill_no)
    TextView mTxvBillNo;

    @InjectView(R.id.txv_goods_no)
    TextView mTxvGoodsNo;

    @InjectView(R.id.txv_bill_date)
    TextView mTxvBillDate;

    @InjectView(R.id.txv_pay_type)
    TextView mTxvPayType;

    @InjectView(R.id.txv_carrying_fee)
    TextView mTxvCarryingFee;

    @InjectView(R.id.txv_goods_fee)
    TextView mTxvGoodsFee;

    @InjectView(R.id.txv_insured_fee)
    TextView mTxvInsuredFee;

    @InjectView(R.id.txv_from_customer_name)
    TextView mTxvFromCustomerName;

    @InjectView(R.id.txv_from_customer_mobile)
    TextView mTxvFromCustomerMobile;

    @InjectView(R.id.txv_to_customer_name)
    TextView mTxvToCustomerName;

    @InjectView(R.id.txv_to_customer_mobile)
    TextView mTxvToCustomerMobile;

    @InjectView(R.id.txv_from_short_carrying_fee)
    TextView mTxvFromShortCarryingFee;

    @InjectView(R.id.txv_to_short_carrying_fee)
    TextView mTxvToShortCarryingFee;

    @InjectView(R.id.txv_goods_info)
    TextView mTxvGoodsInfo;

    @InjectView(R.id.txv_goods_num)
    TextView mTxvGoodsNum;

    @InjectView(R.id.txv_note)
    TextView mTxvNote;

    View mView = null;

    SearchView mSearchView = null;
    MenuItem mMenuSearch = null;
    MyQueryTextLisener mQueryTextListener = null;

    BillSearcher mSearcher = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mView = inflater.inflate(R.layout.fragment_bill_search, container, false);
        ButterKnife.inject(this, mView);
        init();
        return mView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_bill_search, menu);
        mMenuSearch = menu.findItem(R.id.menu_bill_search_search);
        mSearchView = (SearchView) mMenuSearch.getActionView();
        mSearchView.setQueryHint("输入运单号或扫描条码");
        mQueryTextListener = new MyQueryTextLisener();
        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mMenuSearch.expandActionView();
    }


    private void init() {
    }

    @Override
    public Object databaseHelper(Context context) {
        return new CarryingBillDB(context);
    }

    @Override
    public List<DrawerItem> drawerMenus(Context context) {
        List<DrawerItem> drawerItems = new ArrayList<DrawerItem>();

        Bundle args = new Bundle();
        args.putInt("no_use", 1);
        SearchBill fragment = new SearchBill();
        fragment.setArguments(args);
        drawerItems.add(new DrawerItem(TAG, "运单查询", 0, R.drawable.ic_action_archive, fragment));
        return drawerItems;
    }

    private class MyQueryTextLisener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String s) {
            if(s != null && s.length() > 0) {
                mSearcher = new BillSearcher(s);
                mSearcher.execute((Void) null);
                return true;
            }
            else
                return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            return false;
        }
    }

    private class BillSearcher extends AsyncTask<Void, Void, Boolean> {

        LmisDialog pdialog;
        String mQueryText = "";
        JSONObject ret = null;

        public BillSearcher(String queryText) {
            mQueryText = queryText;
        }

        @Override
        protected void onPreExecute() {
            pdialog = new LmisDialog(getActivity(), false, "正在查找...");
            pdialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            JSONArray args = new JSONArray();
            args.put(mQueryText);
            Lmis instance = ((CarryingBillDB) databaseHelper(scope.context())).getLmisInstance();
            try {
                ret = instance.callMethod("carrying_bill", "find_by_bill_no", args, null);
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }


        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                mSearcher.cancel(true);
            }
            pdialog.dismiss();
        }
    }
}