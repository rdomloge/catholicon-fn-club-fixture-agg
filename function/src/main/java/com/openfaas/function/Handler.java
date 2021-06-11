package com.openfaas.function;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private static final String FIXTURE_PARAM = "fixtureId";

    // public static final String FIXTURE_HOSTPORT = "rdomloge.entrydns.org:81";
    public static final String FIXTURE_HOSTPORT = "catholicon-ms-matchcard-service:84";
    public static final String FIXTURE_URL = "http://"+FIXTURE_HOSTPORT+"/fixtures/search/findByExternalFixtureId?externalFixtureId=%1$s";

    // public static final String CLUBS_HOSTPORT = "rdomloge.entrydns.org:81";
    public static final String CLUBS_HOSTPORT = "catholicon-ms-club-service:85";
    public static final String CLUBS_URL = "http://"+CLUBS_HOSTPORT+"/clubs/search/findClubByTeamId?teamId=%1$s";

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
            int fixtureId = Integer.parseInt(query.get(FIXTURE_PARAM));
            res.setBody(merge(fixtureId));
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

    private String merge(int fixtureId) throws IOException, ParseException {
        String fixtureJson = fetchFixture(fixtureId);
        JsonObject fixture = JsonParser.parseString(fixtureJson).getAsJsonObject();

        JsonObject matchSession = fetchMatchSessionForFixture(
            fixture.get("homeTeamId").getAsInt(), 
            fixture.get("matchDate").getAsString());

        JsonObject details = new JsonObject();
        details.add("fixture", fixture);
        details.add("session", matchSession);
        return details.toString();
    }

    public static void main(String[] args) throws IOException, ParseException {
        Handler h = new Handler();
        String merged = h.merge(2282);
        System.out.println("Merged: "+merged);
    }


    private static final SimpleDateFormat fixtureDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private JsonObject fetchMatchSessionForFixture(int teamId, String fixtureDateStr) throws IOException, ParseException {
        
        Date parse = fixtureDateFormat.parse(fixtureDateStr);
        DateTime fixtureDate = new DateTime(parse.getTime());
        
        String clubJson = fetchClub(teamId);
        JsonObject club = JsonParser.parseString(clubJson).getAsJsonObject();
        JsonElement matchSessions = club.get("matchSessions");
        JsonArray sessions = matchSessions.getAsJsonArray();
        for(int i=0; i < sessions.size(); i++) {
            JsonObject session = sessions.get(i).getAsJsonObject();
            int dayOfWeek = daysAsJodaDayOfWeekInt(session.get("days").getAsString());
            if(dayOfWeek == fixtureDate.getDayOfWeek()) return session;
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
        if( ! query.containsKey(FIXTURE_PARAM)) {
            throw new BadRequestException("Missing fixture ID");
        }
    }

    private String fetchClub(int teamId) throws IOException {
        Request request = new Request.Builder().url(String.format(CLUBS_URL, teamId)).build();
        
        Call call = client.newCall(request);
        okhttp3.Response response = call.execute();

        if( ! response.isSuccessful()) throw new IOException("Request to fixture failed("+response.code()+"): "+response.body().string());
        return response.body().string();
    }

    private String fetchFixture(int fixtureId) throws IOException {
        
        Request request = new Request.Builder().url(String.format(FIXTURE_URL, fixtureId)).build();
        
        Call call = client.newCall(request);
        okhttp3.Response response = call.execute();

        if( ! response.isSuccessful()) throw new IOException("Request to fixture failed("+response.code()+"): "+response.body().string());
        return response.body().string();
    }
}
