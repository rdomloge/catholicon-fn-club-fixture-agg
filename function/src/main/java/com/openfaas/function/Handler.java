package com.openfaas.function;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.openfaas.model.IRequest;
import com.openfaas.model.IResponse;
import com.openfaas.model.Response;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

public class Handler extends com.openfaas.model.AbstractHandler {

    private static final String FIXTURE = "fixtureId";

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .addNetworkInterceptor(new HttpLoggingInterceptor())
        .cache(new Cache(new File("/tmp/catholicon-fn-club-fixture-agg-cache"), 1000))
        .build();

    public IResponse Handle(IRequest req) {
        Response res = new Response();
        Map<String, String> query = req.getQuery();

        try {
            System.out.println("Handling "+req.getPathRaw()+"::"+req.getQueryRaw());
            checkRequest(query);
            String fixtureJson = fetchFixture(query);
            Gson gson = new Gson();
            Fixture fixture = gson.fromJson(fixtureJson, Fixture.class);
            System.out.println("Fixture deserialised: "+fixture.getExternalFixtureId());
            res.setBody(fixtureJson);
        }
        catch(BadRequestException brex) {
            res = new Response();
            res.setBody(brex.getMessage());
            res.setStatusCode(400);
            return res;
        } 
        catch (IOException e) {
            res = new Response();
            res.setBody("Could not call downstream service: "+e.getMessage());
            res.setStatusCode(503);
            return res;
        } 

	    return res;
    }

    private void checkRequest(Map<String, String> query) throws BadRequestException {
        if( ! query.containsKey(FIXTURE)) {
            throw new BadRequestException("Missing fixture ID");
        }
    }

    private String fetchFixture(Map<String, String> query) throws IOException {
        int fixtureId = Integer.parseInt(query.get(FIXTURE));
        
        Request request = new Request.Builder().url(
            "http://rdomloge.entrydns.org:81/fixtures/search/findByExternalFixtureId?externalFixtureId="+fixtureId).build();
        
        Call call = client.newCall(request);
        okhttp3.Response response = call.execute();

        if( ! response.isSuccessful()) throw new IOException("Request to fixture failed("+response.code()+"): "+response.body().string());
        return response.body().string();
    }
}
