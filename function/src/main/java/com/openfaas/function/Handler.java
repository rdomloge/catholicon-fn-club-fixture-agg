package com.openfaas.function;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Map.Entry;

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

    public IResponse Handle(IRequest req) {
        Response res = new Response();
        Map<String, String> query = req.getQuery();

        try {
            System.out.println("Handling "+req.getPathRaw()+"::"+req.getQueryRaw());
            checkRequest(query);
            System.out.println("Request OK");
            String fixtureJson = fetchFixture(query);
            System.out.println("Fetched fixture: "+fixtureJson);
            res.setBody(fixtureJson);
        }
        catch(BadRequestException brex) {
            res = new Response();
            res.setBody(brex.getMessage());
            res.setStatusCode(400);
            return res;
        } catch (IOException e) {
            e.printStackTrace();
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
        System.out.println("Fetching fixture "+fixtureId);
        OkHttpClient client = new OkHttpClient.Builder()
            .cache(new Cache(new File("/tmp/okhttpclientcache"), 1000))
            .addInterceptor(new HttpLoggingInterceptor())
            .build();
        System.out.println("Client ready");
        Request request = new Request.Builder().url(
            "http://rdomloge.entrydns.org:81/fixtures/search/findByExternalFixtureId?externalFixtureId="+fixtureId).build();
        
        System.out.println("Request ready");
        Call call = client.newCall(request);
        System.out.println("Call ready - executing");
        okhttp3.Response response = call.execute();
        System.out.println("Response received");

        if( ! response.isSuccessful()) throw new IOException("Request to fixture failed("+response.code()+"): "+response.body().string());
        System.out.println("Response was successful");
        return response.body().string();
    }
}
