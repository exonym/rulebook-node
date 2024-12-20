package io.exonym.utils.storage;

import eu.abc4trust.xml.IssuanceMessageAndBoolean;

import java.math.BigInteger;
import java.net.URI;

public class ImabAndHandle {

    private IssuanceMessageAndBoolean imab;
    private BigInteger handle;

    private String sybilNym;
    private String sybilScope;

    private URI issuerUID;

    public IssuanceMessageAndBoolean getImab() {
        return imab;
    }

    public void setImab(IssuanceMessageAndBoolean imab) {
        this.imab = imab;
    }

    public BigInteger getHandle() {
        return handle;
    }

    public void setHandle(BigInteger handle) {
        this.handle = handle;
    }

    public URI getIssuerUID() {
        return issuerUID;
    }

    public void setIssuerUID(URI issuerUID) {
        this.issuerUID = issuerUID;
    }

    public String getSybilNym() {
        return sybilNym;
    }

    public void setSybilNym(String sybilNym) {
        this.sybilNym = sybilNym;
    }

    public String getSybilScope() {
        return sybilScope;
    }

    public void setSybilScope(String sybilScope) {
        this.sybilScope = sybilScope;
    }
}


