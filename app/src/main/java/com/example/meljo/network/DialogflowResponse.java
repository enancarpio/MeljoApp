package com.example.meljo.network;

public class DialogflowResponse {

    public QueryResult queryResult;

    public static class QueryResult {
        public String queryText;
        public Intent intent;
        public String fulfillmentText;

        public static class Intent {
            public String displayName;
        }
    }
}

