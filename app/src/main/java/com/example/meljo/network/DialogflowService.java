package com.example.meljo.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface DialogflowService {

    @POST("v2/projects/{projectId}/agent/sessions/{sessionId}:detectIntent")
    Call<DialogflowResponse> detectIntent(
            @Header("Authorization") String authHeader,
            @Path("projectId") String projectId,
            @Path("sessionId") String sessionId,
            @Body DialogflowRequest body
    );
}

