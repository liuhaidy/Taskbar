/* Copyright 2016 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.WindowManager;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.U;

public class ContextMenuActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    String packageName;
    String componentName;
    String appName;

    boolean showStartMenu = false;
    boolean shouldHideTaskbar = false;

    @SuppressLint("RtlHardcoded")
    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        showStartMenu = getIntent().getBooleanExtra("launched_from_start_menu", false);

        // Determine where to position the dialog on screen
        WindowManager.LayoutParams params = getWindow().getAttributes();
        SharedPreferences pref = U.getSharedPreferences(this);
        switch(pref.getString("position", "bottom_left")) {
            case "bottom_left":
                params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                params.y = getResources().getDimensionPixelSize(R.dimen.icon_size);
                break;
            case "bottom_vertical_left":
                params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                params.x = getResources().getDimensionPixelSize(R.dimen.icon_size);
                break;
            case "bottom_right":
                params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                params.y = getResources().getDimensionPixelSize(R.dimen.icon_size);
                break;
            case "bottom_vertical_right":
                params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                params.x = getResources().getDimensionPixelSize(R.dimen.icon_size);
                break;
        }

        params.width = getResources().getDimensionPixelSize(R.dimen.context_menu_width);
        params.dimAmount = 0;

        getWindow().setAttributes(params);

        // Generate options to show on the menu, depending on which icon was clicked
        if(getIntent().hasExtra("package_name") && getIntent().hasExtra("app_name")) {
            appName = getIntent().getStringExtra("app_name");
            setTitle(appName);

            final PackageManager pm = getPackageManager();
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo defaultLauncher = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            packageName = getIntent().getStringExtra("package_name");

            if(!packageName.equals(BuildConfig.APPLICATION_ID)
                    && !packageName.equals(defaultLauncher.activityInfo.packageName)) {
                PinnedBlockedApps pba = PinnedBlockedApps.getInstance(this);
                componentName = getIntent().getStringExtra("component_name");

                if(pba.isPinned(componentName)) {
                    addPreferencesFromResource(R.xml.pref_context_menu_pin);
                    findPreference("pin_app").setOnPreferenceClickListener(this);
                    findPreference("pin_app").setTitle(R.string.unpin_app);
                } else if(pba.isBlocked(componentName)) {
                    addPreferencesFromResource(R.xml.pref_context_menu_block);
                    findPreference("block_app").setOnPreferenceClickListener(this);
                    findPreference("block_app").setTitle(R.string.unblock_app);
                } else {
                    final int MAX_NUM_OF_COLUMNS =
                            pref.getString("position", "bottom_left").contains("vertical")
                                    ? getResources().getInteger(R.integer.num_of_columns_vertical)
                                    : getResources().getInteger(R.integer.num_of_columns);

                    if(pba.getPinnedApps().size() < MAX_NUM_OF_COLUMNS) {
                        addPreferencesFromResource(R.xml.pref_context_menu_pin);
                        findPreference("pin_app").setOnPreferenceClickListener(this);
                        findPreference("pin_app").setTitle(R.string.pin_app);
                    }

                    addPreferencesFromResource(R.xml.pref_context_menu_block);
                    findPreference("block_app").setOnPreferenceClickListener(this);
                    findPreference("block_app").setTitle(R.string.block_app);
                }
            }

            addPreferencesFromResource(R.xml.pref_context_menu);

            findPreference("app_info").setOnPreferenceClickListener(this);
            findPreference("uninstall").setOnPreferenceClickListener(this);
        } else {
            addPreferencesFromResource(R.xml.pref_context_menu_open_settings);
            findPreference("open_taskbar_settings").setOnPreferenceClickListener(this);

            if(!getIntent().getBooleanExtra("dont_show_quit", false)) {
                addPreferencesFromResource(R.xml.pref_context_menu_quit);
                findPreference("quit_taskbar").setOnPreferenceClickListener(this);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference p) {
        switch(p.getKey()) {
            case "app_info":
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getIntent().getStringExtra("package_name")));
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode())
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "uninstall":
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
                    Intent intent2 = new Intent(ContextMenuActivity.this, UninstallActivity.class);
                    intent2.putExtra("uninstall", getIntent().getStringExtra("package_name"));
                    startActivity(intent2);
                } else
                    startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getIntent().getStringExtra("package_name"))));

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "open_taskbar_settings":
                startActivity(new Intent(this, MainActivity.class));

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "quit_taskbar":
                sendBroadcast(new Intent("com.farmerbb.taskbar.QUIT"));

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "pin_app":
                PinnedBlockedApps pba = PinnedBlockedApps.getInstance(this);
                if(pba.isPinned(componentName))
                    pba.removePinnedApp(this, componentName);
                else {
                    Intent throwaway = new Intent();
                    throwaway.setComponent(ComponentName.unflattenFromString(componentName));

                    pba.addPinnedApp(this, new AppEntry(
                            packageName,
                            componentName,
                            appName,
                            throwaway.resolveActivityInfo(getPackageManager(), 0).loadIcon(getPackageManager()),
                            true));
                }
                break;
            case "block_app":
                PinnedBlockedApps pba2 = PinnedBlockedApps.getInstance(this);
                if(pba2.isBlocked(componentName))
                    pba2.removeBlockedApp(this, componentName);
                else {
                    Intent throwaway = new Intent();
                    throwaway.setComponent(ComponentName.unflattenFromString(componentName));

                    pba2.addBlockedApp(this, new AppEntry(
                            packageName,
                            componentName,
                            appName,
                            null,
                            false));
                }
                break;
        }

        finish();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!isFinishing()) finish();
    }

    @Override
    public void finish() {
        if(showStartMenu)
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TOGGLE_START_MENU"));
        else if(shouldHideTaskbar) {
            SharedPreferences pref = U.getSharedPreferences(this);
            if(pref.getBoolean("hide_taskbar", true))
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_TASKBAR"));
        }

        super.finish();
    }
}