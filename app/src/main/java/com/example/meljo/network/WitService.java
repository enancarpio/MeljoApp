package com.example.meljo.network;

import com.google.common.net.HttpHeaders;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface WitService {
    @GET("message")
    Call<WitResponse> getMessage(@Header(HttpHeaders.AUTHORIZATION) String str, @Query("q") String str2);
}
