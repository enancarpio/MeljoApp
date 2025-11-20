package com.example.meljo.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OpenAIClient {
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder().baseUrl("https://api.groq.com/openai/v1/").addConverterFactory(GsonConverterFactory.create()).build();
        }
        return retrofit;
    }
}
