package com.openfaas.function;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.management.RuntimeErrorException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.openfaas.model.IRequest;
import com.openfaas.model.IResponse;
import com.openfaas.model.Response;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

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

            JsonObject matchSession = fetchMatchSessionForFixture(
                fixture.get("homeTeamId").getAsInt(), 
                fixture.get("matchDate").getAsString());

            JsonObject details = new JsonObject();
            details.add("fixture", fixture);
            details.add("session", matchSession);

            res.setBody(details.getAsString());
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


    private static final SimpleDateFormat fixtureDateFormat = new SimpleDateFormat("YYYY-mm-DD");

    private JsonObject fetchMatchSessionForFixture(int teamId, String fixtureDateStr) throws IOException, ParseException {
        
        DateTime fixtureDate = new DateTime(fixtureDateFormat.parse(fixtureDateStr).getTime());
        
        String clubJson = fetchClub(teamId);
        JsonObject club = JsonParser.parseString(clubJson).getAsJsonObject();
        JsonElement matchSessions = club.get("matchSessions");
        JsonArray sessions = matchSessions.getAsJsonArray();
        for(int i=0; i < sessions.size(); i++) {
            JsonObject session = sessions.get(i).getAsJsonObject();
            int dayOfWeek = daysAsJodaDayOfWeekInt(session.get("days").getAsString());
            if(dayOfWeek == fixtureDate.getDayOfWeek()) {
                System.out.println("Match");
                return session;
            }
            else {
                System.out.println(session.get("days").getAsString() + "("+dayOfWeek+") does not match "+fixtureDate.getDayOfWeek());
            }
        }

        throw new RuntimeException("Could not find a matching session for date "+fixtureDateStr+" on "+fixtureDate.getDayOfWeek());
    }

    public int daysAsJodaDayOfWeekInt(String days) {
		if("Mondays".equalsIgnoreCase(days)) return DateTimeConstants.MONDAY;
		if("Tuesdays".equalsIgnoreCase(days)) return DateTimeConstants.TUESDAY;
		if("Wednesdays".equalsIgnoreCase(days)) return DateTimeConstants.WEDNESDAY;
		if("Thursdays".equalsIgnoreCase(days)) return DateTimeConstants.THURSDAY;
		if("Fridays".equalsIgnoreCase(days)) return DateTimeConstants.FRIDAY;
		if("Saturdays".equalsIgnoreCase(days)) return DateTimeConstants.SATURDAY;
		if("Sundays".equalsIgnoreCase(days)) return DateTimeConstants.SUNDAY;
		throw new RuntimeException("Could not map "+days);
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
