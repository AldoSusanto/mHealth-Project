package com.mhealth.application;


import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


class HttpRequestTask extends AsyncTask<String, Void, JSONObject> implements AuthState.AuthStateAction {

    private final String TAG = "HttpRequestTask";

    private String url;
    private RequestBody body;

    HttpRequestTask() {
        this.url = null;
        this.body = null;
        this.listener = null;
    }

    interface HttpResponseListener {
        // handle HTTP response
        void onResponseReceived(JSONObject response);
    }

    private HttpResponseListener listener;


    // used to build HTTP request
    HttpRequestTask setUrl(String url) {
        this.url = url;
        return this;
    }
    HttpRequestTask setBody(RequestBody body) {
        this.body = body;
        return this;
    }
    HttpRequestTask setHttpResponseListener(HttpResponseListener listener) {
        this.listener = listener;
        return this;
    }

    // for appAuthState.performActionWithFreshTokens
    @Override
    public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException authException) {
        Log.i(TAG,"Making request");
        execute(accessToken);
    }

    @Override
    protected JSONObject doInBackground(String... tokens) {

        OkHttpClient client = new OkHttpClient();

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Authorization", String.format("Bearer %s", tokens[0]));
        if (body != null) {
            Log.i(TAG, "Making POST request...");
            builder.post(body);
        }

        Request request = builder.build();

        try {
            Log.i(TAG, "Waiting for response...");
            Response response = client.newCall(request).execute();
            String jsonBody = response.body().string();

            Log.i(TAG, "Response: " + jsonBody);

            return new JSONObject(jsonBody);

        } catch (Exception exception) {
            Log.w(TAG, exception);
        }

        return null;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        super.onPostExecute(jsonObject);
        if (listener != null) {
            listener.onResponseReceived(jsonObject);
        }
    }
}
