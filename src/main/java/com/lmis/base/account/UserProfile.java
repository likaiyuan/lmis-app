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
package com.lmis.base.account;

import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import com.lmis.orm.LmisHelper;
import com.lmis.R;
import com.lmis.auth.LmisAccountManager;
import com.lmis.support.AppScope;
import com.lmis.support.BaseFragment;
import com.lmis.support.LmisUser;
import com.lmis.util.Base64Helper;
import com.lmis.util.controls.LmisTextView;
import com.lmis.util.drawer.DrawerItem;

public class UserProfile extends BaseFragment {

  public static final String TAG = "UserProfile";

	View rootView = null;
	EditText password = null;
	LmisTextView txvUserLoginName, txvUsername, txvServerUrl, txvTimeZone,
			txvDatabase;
	ImageView imgUserPic;
	AlertDialog.Builder builder = null;
	Dialog dialog = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		rootView = inflater.inflate(R.layout.fragment_account_user_profile,
				container, false);
		scope = new AppScope(this);
		scope.main().setTitle("Lmis User Profile");

		setupView();
		return rootView;
	}

	private void setupView() {

		imgUserPic = null;
		imgUserPic = (ImageView) rootView.findViewById(R.id.imgUserProfilePic);
    String avatar = scope.User().getAvatar();
    if(!avatar.equals("false"))
    {
      Log.d(TAG,"user avata : " + avatar);
      imgUserPic.setImageBitmap(Base64Helper.getBitmapImage(scope.context(),avatar));
    }
		txvUserLoginName = (LmisTextView) rootView
				.findViewById(R.id.txvUserLoginName);
		txvUserLoginName.setText(scope.User().getAndroidName());

		txvUsername = (LmisTextView) rootView.findViewById(R.id.txvUserName);
		txvUsername.setText(scope.User().getUsername());

		txvServerUrl = (LmisTextView) rootView.findViewById(R.id.txvServerUrl);
		txvServerUrl.setText(scope.User().getHost());

		txvDatabase = (LmisTextView) rootView.findViewById(R.id.txvDatabase);
		txvDatabase.setText(scope.User().getDatabase());

		txvTimeZone = (LmisTextView) rootView.findViewById(R.id.txvTimeZone);
		String timezone = scope.User().getTimezone();
		txvTimeZone.setText((timezone.equals("false")) ? "GMT" : timezone);

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_fragment_account_user_profile, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_account_user_profile_sync:
			dialog = inputPasswordDialog();
			dialog.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	private Dialog inputPasswordDialog() {
		builder = new Builder(scope.context());
		password = new EditText(scope.context());
		password.setTransformationMethod(PasswordTransformationMethod
				.getInstance());
		builder.setTitle("Enter Password").setMessage("Provide your password")
				.setView(password);
		builder.setPositiveButton("Update Info", new OnClickListener() {
			public void onClick(DialogInterface di, int i) {
				LmisUser userData = null;
				try {
					LmisHelper openerp = new LmisHelper(scope.context(), scope
							.User().getHost());

					userData = openerp.login(scope.User().getUsername(),
							password.getText().toString(), scope.User()
									.getDatabase(), scope.User().getHost());
				} catch (Exception e) {
				}
				if (userData != null) {
					if (LmisAccountManager.updateAccountDetails(
                            scope.context(), userData)) {
						Toast.makeText(getActivity(), "Infomation Updated.",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(getActivity(), "Invalid Password !",
							Toast.LENGTH_LONG).show();
				}
				setupView();
				dialog.cancel();
				dialog = null;
			}
		});
		builder.setNegativeButton("Cancel", new OnClickListener() {
			public void onClick(DialogInterface di, int i) {
				dialog.cancel();
				dialog = null;
			}
		});
		return builder.create();

	}

	@Override
	public Object databaseHelper(Context context) {
		return null;
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		return null;
	}
}
