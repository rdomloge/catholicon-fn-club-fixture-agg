package com.openfaas.function;

public enum MatchcardStatus {
    UNCONFIRMED, ENTEREDBYLEAGUEADMIN, CONFIRMED, UNKNOWN;

    public static MatchcardStatus convert(int websiteCardStatus) {
        switch(websiteCardStatus) {
            case 1:
            case 3:
                return UNCONFIRMED;
            case 4:
                return ENTEREDBYLEAGUEADMIN;
            case 5:
                return CONFIRMED;
            default:
                return UNKNOWN;
        }
    }
}
