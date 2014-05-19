/*
 * OpenERP, Open Source Management Solution
 * Copyright (C) 2012-today OpenERP SA (<http://www.openerp.com>)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * 
 */
package com.lmis.services;

import com.lmis.LmisDomain;
import com.lmis.addons.meeting.MeetingDB;
import com.lmis.auth.LmisAccountManager;
import com.lmis.orm.LmisHelper;
import com.lmis.support.calendar.LmisCalendar;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MeetingSyncService extends Service {
	public static final String TAG = "MeetingSyncService";
	private static SyncAdapterImpl sSyncAdapter = null;
	static int i = 0;
	Context mContext = null;

	public MeetingSyncService() {
		mContext = this;
	}

	@Override
	public IBinder onBind(Intent intent) {
		IBinder ret = null;
		ret = getSyncAdapter().getSyncAdapterBinder();
		return ret;
	}

	public SyncAdapterImpl getSyncAdapter() {

		if (sSyncAdapter == null) {
			sSyncAdapter = new SyncAdapterImpl(this);
		}
		return sSyncAdapter;
	}

	public void performSync(Context context, Account account, Bundle extras,
			String authority, ContentProviderClient provider,
			SyncResult syncResult) {

		try {

			MeetingDB meeting = new MeetingDB(context);
			meeting.setAccountUser(LmisAccountManager.getAccountDetail(
                    context, account.name));
			LmisHelper oe = meeting.getOEInstance();
			if (oe != null) {
				LmisDomain domain = new LmisDomain();
				domain.add("user_id", "=", oe.getUser().getUser_id());
				if (oe.syncWithServer(domain, true)) {
					LmisCalendar calendar = new LmisCalendar(context);
					calendar.removeEvents(oe.getRemovedRecords());
					calendar.syncCalendar();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
		private Context mContext;

		public SyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync(Account account, Bundle bundle, String str,
				ContentProviderClient providerClient, SyncResult syncResult) {
			Log.d(TAG, "Meeting sync service started");
			try {
				if (account != null) {
					new MeetingSyncService().performSync(mContext, account,
							bundle, str, providerClient, syncResult);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}
