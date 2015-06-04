/* The List powered by Creative Commons

   Copyright (C) 2014, 2015 Creative Commons

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package org.creativecommons.thelist.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.creativecommons.thelist.R;
import org.creativecommons.thelist.adapters.GalleryItem;
import org.creativecommons.thelist.authentication.AccountGeneral;
import org.creativecommons.thelist.fragments.GalleryFragment;
import org.creativecommons.thelist.fragments.MyListFragment;
import org.creativecommons.thelist.utils.ListApplication;
import org.creativecommons.thelist.utils.ListUser;
import org.creativecommons.thelist.utils.MessageHelper;
import org.creativecommons.thelist.utils.RequestMethods;
import org.creativecommons.thelist.utils.SharedPreferencesMethods;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GalleryFragment.GalleryListener {
    public static final String TAG = MyListFragment.class.getSimpleName();

    private Context mContext;

    private ListUser mCurrentUser;
    private MessageHelper mMessageHelper;
    private RequestMethods mRequestMethods;
    private SharedPreferencesMethods mSharedPref;

    //Navigation Drawer
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private Menu mNavigationMenu;
    private TextView mAccountName;

    // --------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        mCurrentUser = new ListUser(MainActivity.this);
        mMessageHelper = new MessageHelper(mContext);
        mRequestMethods = new RequestMethods(mContext);
        mSharedPref = new SharedPreferencesMethods(mContext);

        //Google Analytics Tracker
        ((ListApplication) getApplication()).getTracker(ListApplication.TrackerName.GLOBAL_TRACKER);

        //Drawer Components
        mNavigationView = (NavigationView) findViewById(R.id.navigation);
        mNavigationMenu = mNavigationView.getMenu();

        //View headerView = (View) mNavigationView.findViewById(R.id.drawer_header);
        mAccountName = (TextView) mNavigationView.findViewById(R.id.drawer_account_name);

        //mAccountName = (TextView) findViewById(R.id.drawer_account_name);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        Toolbar toolbar = (Toolbar)findViewById(R.id.app_bar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);

        }

        //If there is no savedInstanceState, load in default fragment
        if(savedInstanceState == null) {
            MyListFragment listFragment = new MyListFragment();
            //load default view
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.main_content_container, listFragment)
                    .commit();
            getSupportActionBar().setTitle(getString(R.string.title_activity_drawer));

        }

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                Fragment fragment = null;

                Tracker t = ((ListApplication) getApplication()).getTracker(
                        ListApplication.TrackerName.GLOBAL_TRACKER);

                switch(menuItem.getItemId()){
                    case R.id.nav_item_list:
                        fragment = new MyListFragment();

                        // Set screen name.
                        t.setScreenName("My List");
                        // Send a screen view.
                        t.send(new HitBuilders.ScreenViewBuilder().build());

                        break;
                    case R.id.nav_item_photos:
                        if(!mRequestMethods.isNetworkAvailable()){
                            mMessageHelper.toastNeedInternet();
                            return true;
                        }

                        fragment = new GalleryFragment();

                        // Set screen name.
                        t.setScreenName("My Photos");
                        // Send a screen view.
                        t.send(new HitBuilders.ScreenViewBuilder().build());

                        break;
                    case R.id.nav_item_categories:
                        if(!mRequestMethods.isNetworkAvailable()){
                            mMessageHelper.toastNeedInternet();
                            return true;
                        }

                        mDrawerLayout.closeDrawers();

                        Intent catIntent = new Intent(MainActivity.this, CategoryListActivity.class);
                        startActivity(catIntent);

                        break;
                    case R.id.nav_item_requests:
                        if(!mRequestMethods.isNetworkAvailable()){
                            mMessageHelper.toastNeedInternet();
                            return true;
                        }

                        mDrawerLayout.closeDrawers();

                        Intent reqIntent = new Intent(MainActivity.this, AddItemActivity.class);
                        startActivity(reqIntent);
                        break;
                    case R.id.nav_item_about:
                        Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                        startActivity(aboutIntent);

                        break;
                    case R.id.nav_item_feedback:
                        mDrawerLayout.closeDrawers();

                        //Set survey taken
                        mSharedPref.setSurveyTaken(true);

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.dialog_survey_link)));
                        startActivity(browserIntent);

                        break;
                    case R.id.nav_item_account:
                        if(mCurrentUser.isTempUser() || mCurrentUser.getAccount() == null){
                            handleUserAccount();
                        } else {
                            //Log out user
                            mCurrentUser.removeAccounts(new ListUser.AuthCallback() {
                                @Override
                                //TODO: probably should have its own callback w/out returned value (no authtoken anyway)
                                public void onAuthed(String authtoken) {
                                    mSharedPref.ClearAllSharedPreferences();
                                    Intent startIntent = new Intent(MainActivity.this, StartActivity.class);
                                    startActivity(startIntent);
                                }
                            });
                        }

                        break;
                } //switch

                if(fragment != null) {
                    mDrawerLayout.closeDrawers();
                    menuItem.setChecked(true);
                    setTitle(menuItem.getTitle());

                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.main_content_container, fragment)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                }

                return true;
            } //onNavigationItemSelected
        });

    } //onCreate

    @Override
    public void onResume() {
        super.onResume();

        if(mCurrentUser.isTempUser()){ //TEMP USER
            mAccountName.setVisibility(View.GONE);

            MenuItem item = mNavigationMenu.getItem(R.id.nav_item_account);
            item.setTitle("Log In");
            item.setIcon(R.drawable.ic_login_grey600_24dp);

        } else { //LOGGED IN USER
            mAccountName.setVisibility(View.VISIBLE);
            mAccountName.setText(mCurrentUser.getAccountName());

            MenuItem item = mNavigationMenu.findItem(R.id.nav_item_account);
            item.setTitle("Log Out");
            item.setIcon(R.drawable.ic_logout_grey600_24dp);
        }

    } //onResume

    // --------------------------------------------------------
    // Gallery Fragment
    // --------------------------------------------------------

    @Override
    public void viewImage(ArrayList<GalleryItem> photoObjects, int position) {

        Bundle b = new Bundle();
        b.putParcelableArrayList("photos", photoObjects);
        b.putInt("position", position);

        //Start detailed view
        Intent intent = new Intent(MainActivity.this, ImageActivity.class);
        intent.putExtras(b);
        startActivity(intent);
    } //viewImage


    // --------------------------------------------------------
    // Main Menu + Helpers
    // --------------------------------------------------------

    private void handleUserAccount(){
    //TODO: bring up account picker dialog w/ new option
    if(mCurrentUser.getAccountCount() > 0){
        mCurrentUser.showAccountPicker(new ListUser.AuthCallback() {
            @Override
            public void onAuthed(String authtoken) {
                Log.d(TAG, " > switch_accounts MenuItem > showAccountPicker > " +
                        "got authtoken: " + authtoken);
            }
        });
    } else {
        mCurrentUser.addNewAccount(AccountGeneral.ACCOUNT_TYPE,
                AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, new ListUser.AuthCallback() {
                    @Override
                    public void onAuthed(String authtoken) {
                        Log.d(TAG, " > switch_accounts MenuItem > addNewAccount > " +
                                "got authtoken: " + authtoken);

                        mDrawerLayout.closeDrawer(GravityCompat.START);

                        MyListFragment listFragment = new MyListFragment();
                        //load default view
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        fragmentManager.beginTransaction()
                                .replace(R.id.main_content_container, listFragment)
                                .commit();
                    }
                });
        }
    } //handleUserAccount

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

} //MainActivity
