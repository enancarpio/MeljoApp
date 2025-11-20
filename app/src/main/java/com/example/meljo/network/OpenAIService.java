package com.example.meljo.network;

import com.google.common.net.HttpHeaders;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenAIService {
    @POST("chat/completions")
    Call<OpenAIResponse> getChatCompletion(@Header(HttpHeaders.AUTHORIZATION) String str, @Body OpenAIRequest openAIRequest);
}
