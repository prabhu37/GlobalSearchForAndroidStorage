/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.filemanager;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.Toolbar;

import org.openintents.distribution.DistributionLibrary;
import org.openintents.distribution.DistributionLibraryActivity;
import org.openintents.filemanager.bookmarks.BookmarkListActivity;
import org.openintents.filemanager.files.FileHolder;
import org.openintents.filemanager.lists.SimpleFileListFragment;
import org.openintents.filemanager.util.FileUtils;
import org.openintents.intents.FileManagerIntents;
import org.openintents.util.MenuIntentOptionsWithIcons;

import java.io.File;

public class FileManagerActivity extends DistributionLibraryActivity {
    @VisibleForTesting
    public static final String FRAGMENT_TAG = "ListFragment";

    protected static final int REQUEST_CODE_BOOKMARKS = 1;

    private SimpleFileListFragment mFragment;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getData() != null)
            mFragment.openInformingPathBar(new FileHolder(FileUtils.getFile(intent.getData())));
    }

    /**
     * Either open the file and finish, or navigate to the designated directory. This gives FileManagerActivity the flexibility to actually handle file scheme data of any type.
     *
     * @return The folder to navigate to, if applicable. Null otherwise.
     */
    private File resolveIntentData() {
        File data = FileUtils.getFile(getIntent().getData());
        if (data == null)
            return null;

        if (data.isFile() && !getIntent().getBooleanExtra(FileManagerIntents.EXTRA_FROM_OI_FILEMANAGER, false)) {
            FileUtils.openFile(new FileHolder(data, false), this);

            finish();
            return null;
        } else
            return FileUtils.getFile(getIntent().getData());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // update theme from preferences
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("usedarktheme", true)) {
            this.setTheme(R.style.Theme_Dark_NoActionBar);
        } else {
            this.setTheme(R.style.Theme_Light_DarkTitle_NoActionBar);
        }

        setContentView(R.layout.activity_filemanager);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Check whether EULA has been accepted
        // or information about new version can be presented.
        if (mDistribution.showEulaOrNewVersion()) {
            return;
        }

        getSupportActionBar().setHomeButtonEnabled(true);


        // Search when the user types.
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        // If not called by name, open on the requested location.
        File data = resolveIntentData();

        // Add fragment only if it hasn't already been added.
        mFragment = (SimpleFileListFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (mFragment == null) {
            mFragment = new SimpleFileListFragment();
            Bundle args = new Bundle();

            if (getIntent().hasExtra(FileManagerIntents.EXTRA_ABSOLUTE_PATH)) {
                String absolutePath = getIntent().getStringExtra(FileManagerIntents.EXTRA_ABSOLUTE_PATH);
                if (hasValidAbsolutePathExtra()) {
                    args.putString(FileManagerIntents.EXTRA_DIR_PATH,
                            absolutePath);
                } else {
                    Toast.makeText(this, "invalid absolute path extra " + absolutePath, Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                if (data == null) {
                    args.putString(FileManagerIntents.EXTRA_DIR_PATH, Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ? Environment.getExternalStorageDirectory().getAbsolutePath() : "/");
                } else {
                    args.putString(FileManagerIntents.EXTRA_DIR_PATH, data.toString());
                }
            }
            mFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction().add(R.id.container_file_list, mFragment, FRAGMENT_TAG).commit();
        } else {
            // If we didn't rotate and data wasn't null.
            if (icicle == null && data != null)
                mFragment.openInformingPathBar(new FileHolder(new File(data.toString())));
        }
    }

    private boolean hasValidAbsolutePathExtra() {
        return getIntent().getStringExtra(FileManagerIntents.EXTRA_ABSOLUTE_PATH) != null &&
                new File(getIntent().getStringExtra(FileManagerIntents.EXTRA_ABSOLUTE_PATH)).isDirectory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.main, menu);

        mDistribution.onCreateOptionsMenu(menu);

        if (FileManagerApplication.hideDonateMenu(this)) {
            menu.findItem(R.id.menu_donate).setVisible(false);
            menu.getItem(DistributionLibrary.OFFSET_SUPPORT).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Generate any additional actions that can be performed on the
        // overall list. This allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        // menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
        // new ComponentName(this, NoteEditor.class), null, intent, 0, null);

        // Workaround to add icons:
        MenuIntentOptionsWithIcons menu2 = new MenuIntentOptionsWithIcons(this,
                menu);
        menu2.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, FileManagerActivity.class), null,
                intent, 0, null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_search:
                onSearchRequested();
                return true;

            case R.id.menu_settings:
                Intent intent = new Intent(this, PreferenceActivity.class);
                startActivity(intent);
                return true;

            case R.id.menu_bookmarks:
                startActivityForResult(new Intent(FileManagerActivity.this, BookmarkListActivity.class), REQUEST_CODE_BOOKMARKS);
                return true;

            case R.id.menu_donate:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://openintents.org/en/contribute"));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // ignore
                }
                return true;

            case android.R.id.home:
                mFragment.browseToHome();
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    // The following methods should properly handle back button presses on every API Level.
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mFragment.pressBack()) {
            return true;
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    /**
     * This is called after the file manager finished.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CODE_BOOKMARKS:
                if (resultCode == RESULT_OK && data != null) {
                    mFragment.openInformingPathBar(new FileHolder(new File(data.getStringExtra(BookmarkListActivity.KEY_RESULT_PATH))));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    /**
     * We override this, so that we get informed about the opening of the search dialog and start scanning silently.
     */
    @Override
    public boolean onSearchRequested() {
        Bundle appData = new Bundle();
        appData.putString(FileManagerIntents.EXTRA_SEARCH_INIT_PATH, mFragment.getPath());
        startSearch(null, false, appData, false);

        return true;
    }
}
