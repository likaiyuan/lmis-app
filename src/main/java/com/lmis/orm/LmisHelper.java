/*
 * OpenERP, Open Source Management Solution
 * Copyright (C) 2012-today OpenERP SA (<http:www.openerp.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * 
 */
package com.lmis.orm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.lmis.Lmis;
import com.lmis.LmisArguments;
import com.lmis.LmisDomain;
import com.lmis.LmisVersionException;
import com.lmis.base.ir.Ir_model;
import com.lmis.orm.LmisFieldsHelper.OERelationData;
import com.lmis.support.LmisUser;
import com.lmis.util.LmisDate;
import com.lmis.util.PreferenceManager;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LmisHelper extends Lmis {
    public static final String TAG = "LmisHelper";
    Context mContext = null;
    LmisDatabase mDatabase = null;
    LmisUser mUser = null;
    PreferenceManager mPref = null;
    int mAffectedRows = 0;
    List<Long> mResultIds = new ArrayList<Long>();
    List<LmisDataRow> mRemovedRecordss = new ArrayList<LmisDataRow>();

    public LmisHelper(SharedPreferences pref) {
        super(pref);
        init();
    }

    public LmisHelper(Context context, String host)
            throws ClientProtocolException, JSONException, IOException,
            LmisVersionException {
        super(host);
        mContext = context;
        init();
    }

    public LmisHelper(Context context, LmisUser data, LmisDatabase lmisDatabase)
            throws ClientProtocolException, JSONException, IOException,
            LmisVersionException {
        super(data.getHost());
        Log.d(TAG, "LmisHelper->LmisHelper(Context, LmisUser, LmisDatabase)");
        Log.d(TAG, "Called from LmisDatabase->getOEInstance()");
        mContext = context;
        mDatabase = lmisDatabase;
        mUser = data;
        init();
        /*
         * Required to login with server.
		 */
        login(mUser.getUsername(), mUser.getPassword(), mUser.getDatabase(),
                mUser.getHost());
    }

    private void init() {
        Log.d(TAG, "LmisHelper->init()");
        mPref = new PreferenceManager(mContext);
    }

    public LmisUser login(String username, String password, String database,
                          String serverURL) {
        LmisUser userObj = null;
        try {
            JSONObject response = this.authenticate(username, password,
                    database);
            int userId = 0;
            if (response.get("uid") instanceof Integer) {
                userId = response.getInt("uid");

                LmisFieldsHelper fields = new LmisFieldsHelper(new String[]{
                        "partner_id", "tz", "image", "company_id"});
                LmisDomain domain = new LmisDomain();
                domain.add("id", "=", userId);
                JSONObject res = search_read("res.users", fields.get(),
                        domain.get()).getJSONArray("records").getJSONObject(0);

                userObj = new LmisUser();
                userObj.setAvatar(res.getString("image"));

                userObj.setDatabase(database);
                userObj.setHost(serverURL);
                userObj.setIsactive(true);
                userObj.setAndroidName(androidName(username, database));
                userObj.setPartner_id(res.getJSONArray("partner_id").getInt(0));
                userObj.setTimezone(res.getString("tz"));
                userObj.setUser_id(userId);
                userObj.setUsername(username);
                userObj.setPassword(password);
                String company_id = new JSONArray(res.getString("company_id"))
                        .getString(0);
                userObj.setCompany_id(company_id);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userObj;
    }

    public LmisUser getUser() {
        return mUser;
    }

    private String androidName(String username, String database) {
        StringBuffer android_name = new StringBuffer();
        android_name.append(username);
        android_name.append("[");
        android_name.append(database);
        android_name.append("]");
        return android_name.toString();
    }

    public boolean syncWithServer() {
        return syncWithServer(false, null, null, false, -1, false);
    }

    public boolean syncWithServer(boolean removeLocalIfNotExists) {
        return syncWithServer(false, null, null, false, -1,
                removeLocalIfNotExists);
    }

    public boolean syncWithServer(LmisDomain domain,
                                  boolean removeLocalIfNotExists) {
        return syncWithServer(false, domain, null, false, -1,
                removeLocalIfNotExists);
    }

    public boolean syncWithServer(LmisDomain domain) {
        return syncWithServer(false, domain, null, false, -1, false);
    }

    public boolean syncWithServer(boolean twoWay, LmisDomain domain,
                                  List<Object> ids) {
        return syncWithServer(twoWay, domain, ids, false, -1, false);
    }

    public int getAffectedRows() {
        return mAffectedRows;
    }

    public List<LmisDataRow> getRemovedRecords() {
        return mRemovedRecordss;
    }

    public List<Integer> getAffectedIds() {
        List<Integer> ids = new ArrayList<Integer>();
        for (Long id : mResultIds) {
            ids.add(Integer.parseInt(id.toString()));
        }
        return ids;
    }

    public boolean syncWithMethod(String method, LmisArguments args) {
        return syncWithMethod(method, args, false);
    }

    public boolean syncWithMethod(String method, LmisArguments args,
                                  boolean removeLocalIfNotExists) {
        Log.d(TAG, "LmisHelper->syncWithMethod()");
        Log.d(TAG, "Model: " + mDatabase.getModelName());
        Log.d(TAG, "User: " + mUser.getAndroidName());
        Log.d(TAG, "Method: " + method);
        boolean synced = false;
        LmisFieldsHelper fields = new LmisFieldsHelper(
                mDatabase.getDatabaseColumns());
        try {
            JSONObject result = call_kw(mDatabase.getModelName(), method,
                    args.getArray());

            Log.d(TAG, "result: " + result);
            if (result.getJSONArray("result").length() > 0)
                mAffectedRows = result.getJSONArray("result").length();

            synced = handleResultArray(fields, result.getJSONArray("result"), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return synced;
    }

    public boolean syncWithServer(boolean twoWay, LmisDomain domain,
                                  List<Object> ids, boolean limitedData, int limits,
                                  boolean removeLocalIfNotExists) {
        boolean synced = false;
        Log.d(TAG, "LmisHelper->syncWithServer()");
        Log.d(TAG, "Model: " + mDatabase.getModelName());
        Log.d(TAG, "User: " + mUser.getAndroidName());
        LmisFieldsHelper fields = new LmisFieldsHelper(
                mDatabase.getDatabaseColumns());
        try {
            if (domain == null) {
                domain = new LmisDomain();
            }
            if (ids != null) {
                domain.add("id", "in", ids);
            }
            if (limitedData) {
                int data_limit = mPref.getInt("sync_data_limit", 60);
                domain.add("create_date", ">=",
                        LmisDate.getDateBefore(data_limit));
            }

            if (limits == -1) {
                limits = 50;
            }
            JSONObject result = search_read(mDatabase.getModelName(),
                    fields.get(), domain.get(), 0, limits, null, null);
            mAffectedRows = result.getJSONArray("records").length();
            synced = handleResultArray(fields, result.getJSONArray("records"), removeLocalIfNotExists);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, mDatabase.getModelName() + " synced");
        return synced;
    }

    private boolean handleResultArray(LmisFieldsHelper fields, JSONArray results,
                                      boolean removeLocalIfNotExists) {
        boolean flag = false;
        try {
            fields.addAll(results);
            // Handling many2many and many2one records
            // 通过id从服务器端获取many2many many2one one2many表的相关值
            List<OERelationData> rel_models = fields.getRelationData();
            for (OERelationData rel : rel_models) {
                LmisHelper oe = rel.getDb().getOEInstance();
                oe.syncWithServer(false, null, rel.getIds(), false, 0, false);
            }
            List<Long> result_ids = mDatabase.createORReplace(fields.getValues(), removeLocalIfNotExists);
            mResultIds.addAll(result_ids);
            mRemovedRecordss.addAll(mDatabase.getRemovedRecords());
            if (result_ids.size() > 0) {
                flag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public boolean isModelInstalled(String model) {
        boolean installed = false;
        Ir_model ir_model = new Ir_model(mContext);
        try {
            LmisFieldsHelper fields = new LmisFieldsHelper(new String[]{"model"});
            LmisDomain domain = new LmisDomain();
            domain.add("model", "=", model);
            JSONObject result = search_read(ir_model.getModelName(),
                    fields.get(), domain.get());
            if (result.getInt("length") > 0) {
                installed = true;
                JSONObject record = result.getJSONArray("records")
                        .getJSONObject(0);
                LmisValues values = new LmisValues();
                values.put("id", record.getInt("id"));
                values.put("model", record.getString("model"));
                values.put("is_installed", installed);
                int count = ir_model.count("model = ?", new String[]{model});
                if (count > 0)
                    ir_model.update(values, "model = ?", new String[]{model});
                else
                    ir_model.create(values);
            }

        } catch (Exception e) {
            Log.d(TAG, "LmisHelper->isModuleInstalled()");
            Log.e(TAG, e.getMessage() + ". No connection with Lmis server");
        }
        return installed;
    }

    public List<LmisDataRow> search_read_remain() {
        Log.d(TAG, "LmisHelper->search_read_remain()");
        return search_read(true);
    }

    private LmisDomain getLocalIdsDomain(String operator) {
        LmisDomain domain = new LmisDomain();
        JSONArray ids = new JSONArray();
        for (LmisDataRow row : mDatabase.select()) {
            ids.put(row.getInt("id"));
        }
        domain.add("id", operator, ids);
        return domain;
    }

    private List<LmisDataRow> search_read(boolean getRemain) {
        List<LmisDataRow> rows = new ArrayList<LmisDataRow>();
        try {
            LmisFieldsHelper fields = new LmisFieldsHelper(
                    mDatabase.getDatabaseServerColumns());
            JSONObject domain = null;
            if (getRemain)
                domain = getLocalIdsDomain("not in").get();
            JSONObject result = search_read(mDatabase.getModelName(),
                    fields.get(), domain, 0, 100, null, null);
            for (int i = 0; i < result.getJSONArray("records").length(); i++) {
                JSONObject record = result.getJSONArray("records")
                        .getJSONObject(i);
                LmisDataRow row = new LmisDataRow();
                row.put("id", record.getInt("id"));
                for (LmisColumn col : mDatabase.getDatabaseServerColumns()) {
                    row.put(col.getName(), record.get(col.getName()));
                }
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    public List<LmisDataRow> search_read() {
        Log.d(TAG, "LmisHelper->search_read()");
        return search_read(false);
    }

    public void delete(int id) {
        Log.d(TAG, "LmisHelper->delete()");
        try {
            unlink(mDatabase.getModelName(), id);
            mDatabase.delete(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object call_kw(String method, LmisArguments arguments) {
        return call_kw(method, arguments, new JSONObject());
    }

    public Object call_kw(String method, LmisArguments arguments,
                          JSONObject context) {
        return call_kw(null, method, arguments, context);
    }

    public Object call_kw(String model, String method, LmisArguments arguments,
                          JSONObject context) {
        Log.d(TAG, "LmisHelper->call_kw()");
        JSONObject result = null;
        if (model == null) {
            model = mDatabase.getModelName();
        }
        try {
            if (context != null) {
                arguments.add(updateContext(context));
            }
            result = call_kw(model, method, arguments.getArray());
            return result.get("result");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer create(LmisValues values) {
        Log.d(TAG, "LmisHelper->create()");
        Integer newId = null;
        try {
            JSONObject result = createNew(mDatabase.getModelName(),
                    generateArguments(values));
            newId = result.getInt("result");
            values.put("id", newId);
            mDatabase.create(values);
            return newId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newId;
    }

    public Boolean update(LmisValues values, Integer id) {
        Log.d(TAG, "LmisHelper->update()");
        Boolean flag = false;
        try {
            flag = updateValues(mDatabase.getModelName(),
                    generateArguments(values), id);
            if (flag)
                mDatabase.update(values, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    private JSONObject generateArguments(LmisValues values) {
        Log.d(TAG, "LmisHelper->generateArguments()");
        JSONObject arguments = new JSONObject();
        try {
            for (String key : values.keys()) {
                if (values.get(key) instanceof LmisM2MIds) {
                    LmisM2MIds m2mIds = (LmisM2MIds) values.get(key);
                    JSONArray m2mArray = new JSONArray();
                    m2mArray.put(6);
                    m2mArray.put(false);
                    m2mArray.put(m2mIds.getJSONIds());
                    arguments.put(key, new JSONArray("[" + m2mArray.toString()
                            + "]"));
                } else {
                    arguments.put(key, values.get(key));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arguments;
    }

    public boolean moduleExists(String name) {
        Log.d(TAG, "LmisHelper->moduleExists()");
        boolean flag = false;
        try {
            LmisDomain domain = new LmisDomain();
            domain.add("name", "ilike", name);
            LmisFieldsHelper fields = new LmisFieldsHelper(new String[]{"state"});
            JSONObject result = search_read("ir.module.module", fields.get(),
                    domain.get());
            JSONArray records = result.getJSONArray("records");
            if (records.length() > 0
                    && records.getJSONObject(0).getString("state")
                    .equalsIgnoreCase("installed")) {
                flag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }
}
