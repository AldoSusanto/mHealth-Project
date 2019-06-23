package com.mhealth.application;


import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import static com.mhealth.application.AuthActivity.appAuthState;


public class RefreshAuthRequest {

    private final String TAG = "RefreshAuth";

    private Context context;

    private RefreshAuthRequest(Context context) {
        this.context = context;
    }

    private void requestAuth() {
        if (appAuthState == null) {
            Toast.makeText(context, "App not authorized", Toast.LENGTH_SHORT).show();
            return;
        }
        final AuthState authState = new AuthState();
        new AuthorizationService(context).performTokenRequest(appAuthState.createTokenRefreshRequest(), new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                if (exception != null) {
                    Log.w(TAG, "Token Exchange failed", exception);
                } else {
                    if (tokenResponse != null) {
                        authState.update(tokenResponse, exception);
                        AuthActivity.persistAuthState(context, authState);
                        Log.i(TAG, String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                    } else {
                        Log.i(TAG, "Token response was null");
                    }
                }
                AuthActivity.restoreAuthState(context);
            }
        });
    }

    static void refreshAuthorization(Context context) {
        if (appAuthState == null) {
            Toast.makeText(context, "App not authorized", Toast.LENGTH_SHORT).show();
            return;
        }
        new RefreshAuthRequest(context).requestAuth();
    }
}
