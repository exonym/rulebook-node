package io.exonym.lite.pojo;

import io.exonym.lite.exceptions.ExceptionCollection;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.ArrayList;

public class ApplicantReport {
    private String x0;
    private String n6;
    private int totalOffences = 0;
    private boolean unresolvedOffences = false;
    private ArrayList<URI> previousHosts = new ArrayList<>();
    private String lastHost;
    private ArrayList<String> activeHostsWrtRn = new ArrayList<>();
    private ExceptionCollection exceptions;
    private DateTime mostRecentOffenceTimeStamp;
    private boolean isMember = false;

    public int getTotalOffences() {
        return totalOffences;
    }

    public void setTotalOffences(int totalOffences) {
        this.totalOffences = totalOffences;
    }

    public boolean isUnresolvedOffences() {
        return unresolvedOffences;
    }

    public void setUnresolvedOffences(boolean unresolvedOffences) {
        this.unresolvedOffences = unresolvedOffences;
    }

    public ArrayList<URI> getPreviousHosts() {
        return previousHosts;
    }

    public void setPreviousHosts(ArrayList<URI> previousHosts) {
        this.previousHosts = previousHosts;
    }

    public String getLastHost() {
        return lastHost;
    }

    public void setLastHost(String lastHost) {
        this.lastHost = lastHost;
    }

    public ArrayList<String> getActiveHostsWrtRn() {
        return activeHostsWrtRn;
    }

    public void setActiveHostsWrtRn(ArrayList<String> activeHostsWrtRn) {
        this.activeHostsWrtRn = activeHostsWrtRn;
    }

    public ExceptionCollection getExceptions() {
        return exceptions;
    }

    public void setExceptions(ExceptionCollection exceptions) {
        this.exceptions = exceptions;
    }

    public String getX0() {
        return x0;
    }

    public void setX0(String x0) {
        this.x0 = x0;
    }

    public String getN6() {
        return n6;
    }

    public void setN6(String n6) {
        this.n6 = n6;
    }

    public DateTime getMostRecentOffenceTimeStamp() {
        return mostRecentOffenceTimeStamp;
    }

    public void setMostRecentOffenceTimeStamp(DateTime mostRecentOffenceTimeStamp) {
        this.mostRecentOffenceTimeStamp = mostRecentOffenceTimeStamp;
    }

    public boolean isMember() {
        return isMember;
    }

    public void setMember(boolean member) {
        isMember = member;
    }
}
