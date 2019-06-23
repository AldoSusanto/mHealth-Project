package com.mhealth.application;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;


public class AuthActivity extends AppCompatActivity {

    private final String TAG = "AuthActivity";

    private final String USED_INTENT = "USED_INTENT";
    static final String AUTH_STATE = "AUTH_STATE";
    static final String SHARED_PREFERENCES_NAME = "AuthStatePreference";

    AuthorizationService authorizationService;

    static AuthState appAuthState;
    private static Boolean online = false;

    public static Boolean isOnline() {
        return online;
    }

    private void getAuthorization() {
        AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://www.googleapis.com/oauth2/v4/token")
        );

        authorizationService = new AuthorizationService(this);
        String clientId = "162505448695-6lkgbpruem91s19745j0v8sodifg6jb4.apps.googleusercontent.com";
        Uri redirectUri = Uri.parse("com.mhealth.application:/oauth2callback");
        final AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                serviceConfiguration,
                clientId,
                AuthorizationRequest.RESPONSE_TYPE_CODE,
                redirectUri
        );
        builder.setScopes("https://www.googleapis.com/auth/drive.file");


        AuthorizationRequest request = builder.build();
        String action = "com.mhealth.application.HANDLE_AUTHORIZATION_RESPONSE";
        Intent postAuthorizationIntent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, request.hashCode(), postAuthorizationIntent, 0);
        authorizationService.performAuthorizationRequest(request, pendingIntent);
    }


    @Override
    protected void onStart() {
        super.onStart();

        checkIntent(getIntent());
    }

    // check incoming Intent
    // usually we're just handling authorization
    private void checkIntent(Intent intent) {
        Log.i(TAG, intent.toString());
        if (intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                // got a response from the authorization browser screen
                case "com.mhealth.application.HANDLE_AUTHORIZATION_RESPONSE":
                    Log.i(TAG, "handle auth");
                    if (!intent.hasExtra(USED_INTENT)) {
                        Log.i(TAG, "used intent");
                        handleAuthorizationResponse(intent);
                        intent.putExtra(USED_INTENT, true);
                        // exit to AdminActivity; finish() would go back to the browser
                        startActivity(new Intent(this, AdminActivity.class));
                    } else
                        Log.i(TAG, "not used intent");
                    break;
                default:
                    // do nothing
            }
        } else {

            // this is to allow for users to back out of the authorization browser screen
            if (intent.getBooleanExtra("initialRequest", false)) {
                Log.i(TAG, "initial request");
                intent.putExtra("initialRequest", false);
                getAuthorization();
            } else {
                // if we've already been here then just finish() and return to AdminActivity
                Log.i(TAG, "not initial request");
                finish();
            }
        }
    }

    // handling the response from the authorization browser screen
    private void handleAuthorizationResponse(@NonNull Intent intent) {
        Log.i(TAG, "We're handling the response");
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);
        if (response != null) {
            Log.i(TAG, String.format("Handled Authorization Response %s ", authState.toJsonString()));
            AuthorizationService service = new AuthorizationService(this);

            // exchange for a refresh token for later requests
            service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                @Override
                public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                    if (exception != null) {
                        Log.w(TAG, "Token exchange failed", exception);
                    } else {
                        if (tokenResponse != null) {
                            authState.update(tokenResponse, exception);
                            persistAuthState(AuthActivity.this, authState);
                            Log.i(TAG, String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                        } else {
                            Log.i(TAG, "Token response was null");
                        }
                    }
                }
            });
        } else {
            Log.i(TAG, "Response was null");
        }
    }

    // saved AuthState to preferences
    public static void persistAuthState(Context context, @NonNull AuthState authState) {
        Log.i("AuthActivity", "Persisting auth state");
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putString(AUTH_STATE, authState.toJsonString())
                .commit();
        restoreAuthState(context);
    }

    // restores AuthState and checks Drive connection
    // updates online variable which must be true for any Drive requests to be made
    public static void restoreAuthState(final Context context) {

        String jsonString = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(AUTH_STATE, null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                // restore AuthState instance for UserInfoRequest
                appAuthState = AuthState.fromJson(jsonString);
                Log.i("Auth", "Getting user info");

                // check connection
                Toast.makeText(context, "Connecting to Drive", Toast.LENGTH_SHORT).show();
                UserInfoRequest.getInfo(context, new UserInfoRequest.UserInfoListener() {
                    @Override
                    public void onUserInfoReceived(JSONObject user) {
                        if (user == null) {
                            online = false;
                            Toast.makeText(context, "Failed to connect to Drive", Toast.LENGTH_SHORT).show();
                        } else {
                            online = true;
                            Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return;
            } catch (JSONException jsonException) {
                // should never happen
            }
        }

        // we've never saved an AuthState
        appAuthState = null;
        Toast.makeText(context, "App not authorized", Toast.LENGTH_SHORT).show();
    }

    // deletes AuthState; essentially signs us out of Drive
    public static void clearAuthState(Context context) {
        Log.i("AuthActivity", "Signing out");
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(AUTH_STATE)
                .commit();
        restoreAuthState(context);
    }
}
