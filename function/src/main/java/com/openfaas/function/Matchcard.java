package com.openfaas.function;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Matchcard {

	private String id;
	
	private List<String> homePlayers;
	
	private List<String> awayPlayers;
	
	private int homeScore;
	
	private int awayScore;

	private MatchcardStatus status;
	
	private List<Boolean> homeTeamWins;
	
	private List<Rubber> rubbers = new LinkedList<Rubber>();
	
	private boolean teamSize6;

	private String matchDate;
	
	
	
	public Matchcard(Map<Integer, Rubber> scoreMap, String[] homePlayers, String[] awayPlayers, String homeTeam,
			String awayTeam, String matchDate, int homeScore, int awayScore, Boolean[] homeTeamWins,
			boolean teamSize6, Fixture fixture, MatchcardStatus status) {
		
		rubbers.addAll(scoreMap.values());
		this.homePlayers = Arrays.asList(homePlayers);
		this.awayPlayers = Arrays.asList(awayPlayers);
		this.matchDate = convertDayMonthYearToYearMonthDay(matchDate);
		
		this.homeScore = homeScore;
		this.awayScore = awayScore;
		
		this.homeTeamWins = Arrays.asList(homeTeamWins);
		this.teamSize6 = teamSize6;
		this.status = status;
	}
	
	public Matchcard() {
		
	}

	public static String convertDayMonthYearToYearMonthDay(String dayMonthYear) {
		SimpleDateFormat dmyFormat = new SimpleDateFormat("dd MMM yyyy");
		SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return ymdFormat.format(dmyFormat.parse(dayMonthYear));
		} 
		catch (ParseException e) {
			return "[Cannot parse] "+dayMonthYear;
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMatchDate() {
		return matchDate;
	}
	
	public void setMatchDate(String matchDate) {
		this.matchDate = matchDate;
	}
	
	public List<String> getHomePlayers() {
		return homePlayers;
	}

	public void setHomePlayers(List<String> homePlayers) {
		this.homePlayers = homePlayers;
	}

	public List<String> getAwayPlayers() {
		return awayPlayers;
	}

	public void setAwayPlayers(List<String> awayPlayers) {
		this.awayPlayers = awayPlayers;
	}

	public int getHomeScore() {
		return homeScore;
	}

	public void setHomeScore(int homeScore) {
		this.homeScore = homeScore;
	}

	public int getAwayScore() {
		return awayScore;
	}

	public void setAwayScore(int awayScore) {
		this.awayScore = awayScore;
	}

	public List<Boolean> getHomeTeamWins() {
		return homeTeamWins;
	}

	public void setHomeTeamWins(List<Boolean> homeTeamWins) {
		this.homeTeamWins = homeTeamWins;
	}

	public List<Rubber> getRubbers() {
		return rubbers;
	}

	public void setRubbers(List<Rubber> rubbers) {
		this.rubbers = rubbers;
	}

	public boolean isTeamSize6() {
		return teamSize6;
	}

	public void setTeamSize6(boolean teamSize6) {
		this.teamSize6 = teamSize6;
	}

	public MatchcardStatus getStatus() {
		return status;
	}

	public void setStatus(MatchcardStatus status) {
		this.status = status;
	}
}
