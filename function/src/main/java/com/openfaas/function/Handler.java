package com.openfaas.function;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
            JsonObject fixture = JsonParser.parseString(fixtureJson).getAsJsonObject();
            System.out.println("Fixture deserialised: " + fixture.get("externalFixtureId").getAsInt());

            int teamId = fixture.get("homeTeamId").getAsInt();
            String clubJson = fetchClub(teamId);
            JsonObject  club = JsonParser.parseString(clubJson).getAsJsonObject();
            System.out.println("Deserialised club: "+club.get("clubName").getAsString());

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
        catch(Throwable t) {
            t.printStackTrace(System.out);
            System.out.println("Error");
        }

	    return res;
    }

    private void checkRequest(Map<String, String> query) throws BadRequestException {
        if( ! query.containsKey(FIXTURE)) {
            throw new BadRequestException("Missing fixture ID");
        }
    }

    private String fetchClub(int teamId) throws IOException {
        Request request = new Request.Builder().url(
            "http://rdomloge.entrydns.org:81/clubs/search/findClubByTeamId?teamId="+teamId).build();
        
        Call call = client.newCall(request);
        okhttp3.Response response = call.execute();

        if( ! response.isSuccessful()) throw new IOException("Request to fixture failed("+response.code()+"): "+response.body().string());
        return response.body().string();
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
