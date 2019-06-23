package com.mhealth.application;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import net.openid.appauth.AuthorizationService;

import org.json.JSONException;
import org.json.JSONObject;


import static com.mhealth.application.AuthActivity.appAuthState;


class UserInfoRequest {

    private final String TAG = "UserInfoRequest";

    // for handling whether we received a user object
    interface UserInfoListener {
        void onUserInfoReceived(JSONObject user);
    }

    private UserInfoListener listener;
    private AuthorizationService authorizationService;

    private UserInfoRequest(Context context, UserInfoListener listener) {
        this.listener = listener;
        this.authorizationService = new AuthorizationService(context);
    }

    // make the request for Google user info
    private void requestInfo() {

        // we ask for just the user object
        HttpRequestTask httpRequest = new HttpRequestTask()
                .setUrl("https://www.googleapis.com/drive/v3/about?fields=user")
                .setHttpResponseListener(new HttpRequestTask.HttpResponseListener() {
                    @Override
                    public void onResponseReceived(JSONObject response) {
                        if (response == null) {
                            Log.i(TAG, "We didn't get a response for user info");
                            listener.onUserInfoReceived(null);
                            return;
                        }

                        try {
                            // pass on the user object
                            listener.onUserInfoReceived(response.getJSONObject("user"));
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listener.onUserInfoReceived(null);
                    }
                });

        appAuthState.performActionWithFreshTokens(authorizationService, httpRequest);
    }

    // called via UserInfoRequest.getInfo(<args>)
    // listener should be implemented to handle the returned user object
    static void getInfo(Context context, UserInfoListener listener) {
        // we only check for a null AuthState since UserInfoRequest is used to check the Drive connection
        if (appAuthState == null) {
            Toast.makeText(context, "App not authorized", Toast.LENGTH_SHORT).show();
            listener.onUserInfoReceived(null);
            return;
        }
        new UserInfoRequest(context, listener).requestInfo();
    }
}
