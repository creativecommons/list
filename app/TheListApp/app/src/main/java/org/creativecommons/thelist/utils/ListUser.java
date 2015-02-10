/* The List powered by Creative Commons

   Copyright (C) 2014 Creative Commons

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

package org.creativecommons.thelist.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.creativecommons.thelist.R;
import org.creativecommons.thelist.StartActivity;
import org.creativecommons.thelist.authentication.AccountGeneral;
import org.creativecommons.thelist.authentication.ServerAuthenticate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ListUser implements ServerAuthenticate {
    public static final String TAG = ListUser.class.getSimpleName();
    public static final String TEMP_USER = "temp";


    private RequestMethods requestMethods;
    private SharedPreferencesMethods sharedPreferencesMethods;
    //private AccountManager accountManager;

    private boolean tempUser;
    private  String userName;
    private String userID;
    private String sessionToken;
    private Context mContext;

    public ListUser(Context mc) {
        mContext = mc;
        requestMethods = new RequestMethods(mContext);
        sharedPreferencesMethods = new SharedPreferencesMethods(mContext);
        //accountManager = AccountManager.get(mContext);
    }

    public ListUser(String name, String id) {
        this.userName = name;
        this.userID = id;
    }

    public boolean isTempUser() {
        //TODO: Check if User account exists in AccountManager
        SharedPreferences sharedPref = mContext.getSharedPreferences
                (SharedPreferencesMethods.APP_PREFERENCES_KEY, Context.MODE_PRIVATE);

        if(!(sharedPref.contains(SharedPreferencesMethods.USER_ID_PREFERENCE_KEY)) ||
                sharedPreferencesMethods.getUserId() == null) {
            tempUser = true;
        } else {
            tempUser = false;
        }
        return tempUser;
    } //isTempUser

    public String getAuthed(Activity a) {

        //IF TEMP USER
        if(isTempUser()){
           return TEMP_USER;
        } else {
            //ELSE
            AccountManager am = AccountManager.get(mContext);
            Account availableAccounts[] = am.getAccounts();
            Account account;

            if (availableAccounts.length > 0) {
                account = availableAccounts[0];

                AccountManagerFuture<Bundle> future = am.getAuthToken(account, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, null, a, null, null);

                try {
                    Bundle bundle = future.getResult();
                    String auth = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    return auth;
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                }

            }
        }
        return null;
    }

    @Deprecated
    public boolean isLoggedIn() {
        SharedPreferences sharedPref = mContext.getSharedPreferences
                (SharedPreferencesMethods.APP_PREFERENCES_KEY, Context.MODE_PRIVATE);

        //TODO: what if this fail?
        tempUser = sharedPref.contains(SharedPreferencesMethods.USER_ID_PREFERENCE_KEY)
                && sharedPreferencesMethods.getUserId() != null;

        return tempUser;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        //this.userName = name;
    }

    //TODO: get rid of this eventually
    public void setTempUser(boolean bol) {
        tempUser = bol;
    }

    //TODO: move to sharedPreferenceMethods
    public String getUserID() {
        userID = sharedPreferencesMethods.getUserId();
        //See if sharedPreference methods contains userID
        //If yes: get and return userID; else: return null
        if (userID == null) {
            Log.v(TAG, "You don’t got no userID, man");
            return null;
        } else {
            return userID;
        }
    }

    //TODO: might not need with sharedPreferences
    public void setUserID(String id) {
        this.userID = id;
    }

//    public String getSessionToken(){
//        //TODO: actually get sessionToken
//        //sessionToken = //TODO: GET SESION TOKEN;
//
//        //TODO: IF THERE IS NO SESSION TOKEN? WILL THE AUTHENTICATOR DO THIS?
//        return sessionToken;
//    }

    public void setSessionToken(String token){
        sessionToken = token;
    }

    public void logOut() {
        //TODO; invalidateSessionToken?
        userID = null;
        tempUser = false;

        //Clear all sharedPreferences
        sharedPreferencesMethods.ClearAllSharedPreferences();

        //TODO: take you back to startActivity?
        Intent intent = new Intent(mContext, StartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(intent);
    }

    public interface VolleyCallback{
        void onSuccess(String authtoken);
    }

    @Override
    public void userSignIn(final String email, final String pass, String authType, final VolleyCallback callback){
        RequestQueue queue = Volley.newRequestQueue(mContext);
        String url = ApiConstants.LOGIN_USER;

        StringRequest userSignInRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Get Response
                        if(response == null || response.equals("null")) {
                            Log.v("RESPONSE IS NULL IF YOU ARE HERE", response);
                            requestMethods.showErrorDialog(mContext, "YOU SHALL NOT PASS",
                                    "Sure you got your email/password combo right?");
                        } else {
                            Log.v("THIS IS THE RESPONSE FOR LOGIN: ", response);
                            try {
                                JSONObject res = new JSONObject(response);
                                //TODO: remove when endpoints work without ID
                                userID = res.getString(ApiConstants.USER_ID);
                                sessionToken = res.getString(ApiConstants.USER_TOKEN);

                                //Save userID in sharedPreferences
                                sharedPreferencesMethods.SaveSharedPreference
                                        (SharedPreferencesMethods.USER_ID_PREFERENCE_KEY, userID);

                                //Add items chosen before login to userlist
                                //TODO: also add category preferences
                                addSavedItemsToUserList();

                                //pass authtoken back to activity
                                callback.onSuccess(sessionToken);

                            } catch (JSONException e) {
                                Log.v(TAG,e.getMessage());
                                //TODO: add proper error message
                                requestMethods.showErrorDialog(mContext, mContext.getString
                                                (R.string.login_error_exception_title),
                                        mContext.getString(R.string.login_error_exception_message));
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                requestMethods.showErrorDialog(mContext,
                        mContext.getString(R.string.login_error_title),
                        mContext.getString(R.string.login_error_message));
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", email);
                params.put(ApiConstants.USER_PASSWORD, pass);
                return params;
            }
        };
        queue.add(userSignInRequest);
    } //userSignIn

    @Override
    public void userSignUp(String email, String pass, String authType, final VolleyCallback callback) throws Exception {
        //TODO: actually register user
    }

    //Add all list items to userlist
    public void addSavedItemsToUserList(){
        JSONArray listItemPref;
        listItemPref = sharedPreferencesMethods.RetrieveUserItemPreference();

        try{
            if (listItemPref != null && listItemPref.length() > 0) {
                Log.v("HEY THERE LIST ITEM PREF: ", listItemPref.toString());
                for (int i = 0; i < listItemPref.length(); i++) {
                    Log.v("ITEMS", "ARE BEING ADDED");
                    addItemToUserList(listItemPref.getString(i));
                }
            }
        } catch(JSONException e){
            Log.d(TAG, e.getMessage());
        }
    } //addSavedItemsToUserList

    //Add all categories to userlist
    public void addSavedCategoriesToUserAccount(){

    }

    //Add SINGLE random item to user list
    public void addItemToUserList(final String itemID) {
        RequestQueue queue = Volley.newRequestQueue(mContext);

        //TODO: session token will know which user this is?
        String url = ApiConstants.ADD_ITEM + getUserID() + "/" + itemID;
        int mStatusCode;

        //Add single item to user list
        StringRequest postItemRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        //get Response
                        Log.v("Response: ", response);
                        Log.v(TAG,"AN ITEM IS BEING ADDED");
                        //TODO: on success remove the item from the sharedPreferences
                        sharedPreferencesMethods.RemoveUserItemPreference(itemID);

                        //Toast: Confirm List Item has been added
                        final Toast toast = Toast.makeText(mContext,
                                "Added to Your List", Toast.LENGTH_SHORT);
                        toast.show();
                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                toast.cancel();
                            }
                        }, 1000);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse (VolleyError error){
                //TODO: Add “not successful“ toast
                requestMethods.showErrorDialog(mContext,
                        mContext.getString(R.string.error_title),
                        mContext.getString(R.string.error_message));
                Log.v("ERROR ADDING AN ITEM: ", "THIS IS THE ERROR BEING DISPLAYED");
            }
        });
        queue.add(postItemRequest);
    } //addItemToUserList

    //REMOVE SINGLE item from user list
    //TODO: FILL IN WITH REAL API INFO
    public void removeItemFromUserList(final String itemID){
        RequestQueue queue = Volley.newRequestQueue(mContext);

        if(!tempUser){
            //If not logged in, remove item from sharedPreferences
            sharedPreferencesMethods.RemoveUserItemPreference(itemID);

        } else { //If logged in, remove from DB
            String url = ApiConstants.REMOVE_ITEM + getUserID() + "/" + itemID;
            final String skey = sharedPreferencesMethods.getUserToken();

            StringRequest deleteItemRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            //get Response
                            Log.v("Response: ", response);
                            Log.v(TAG, "AN ITEM IS BEING REMOVED");
                            //TODO: do something with response?
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //TODO: Add “not successful“ toast
                    requestMethods.showErrorDialog(mContext,
                            mContext.getString(R.string.error_title),
                            mContext.getString(R.string.error_message));
                    Log.v("ERROR DELETING AN ITEM: ", "THIS IS THE ERROR BEING DISPLAYED");
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    //TODO: get sessionToken from AccountManager
                    params.put(ApiConstants.USER_TOKEN, sessionToken);

                    return params;
                }
            };
            queue.add(deleteItemRequest);
        }
    } //removeItemFromUserList

} //ListUser
