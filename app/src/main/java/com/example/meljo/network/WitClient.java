package com.example.meljo.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WitClient {
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder().baseUrl("https://api.wit.ai/").addConverterFactory(GsonConverterFactory.create()).build();
        }
        return retrofit;
    }
}
