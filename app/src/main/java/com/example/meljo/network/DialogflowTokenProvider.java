package com.example.meljo.network;

import android.util.Base64;

import com.example.meljo.BuildConfig;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;

public class DialogflowTokenProvider {

    // 🔐 Tu JSON en BASE64 (encriptado)
    private static final String SERVICE_ACCOUNT_BASE64 = BuildConfig.DIALOGFLOW_API_KEY;

    public static String getAccessToken() throws IOException {

        byte[] decoded = Base64.decode(SERVICE_ACCOUNT_BASE64, Base64.DEFAULT);

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(decoded))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/dialogflow"));

        credentials.refreshIfExpired();

        return credentials.getAccessToken().getTokenValue();
    }
}

