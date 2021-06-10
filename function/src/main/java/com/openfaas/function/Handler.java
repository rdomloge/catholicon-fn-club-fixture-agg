package com.openfaas.function;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Map.Entry;

import com.openfaas.model.IRequest;
import com.openfaas.model.IResponse;
import com.openfaas.model.Response;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Handler extends com.openfaas.model.AbstractHandler {

    private static final String FIXTURE = "fixtureId";

    public IResponse Handle(IRequest req) {
        Response res = new Response();
        Map<String, String> query = req.getQuery();

        try {

            checkRequest(query);
            String fixtureJson = fetchFixture(query);
            res.setBody(fixtureJson);
        }
        catch(BadRequestException brex) {
            res = new Response();
            res.setBody(brex.getMessage());
            res.setStatusCode(400);
            return res;
        } catch (IOException e) {
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
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("http://rdomloge.entrydns.org:84/fixtures/"+fixtureId).build();
        
        return client.newCall(request).execute().body().string();
    }
}
