package io.exonym.actor.storage;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.standard.CryptoUtils;

import java.net.URI;
import java.util.ArrayList;

public class RevocationRequestWrapper {

    private ArrayList<RevocationRequest> requests = new ArrayList<>();

    private byte[] signature;

    private URI moderator;


    public ArrayList<RevocationRequest> getRequests() {
        return requests;
    }

    public void setRequests(ArrayList<RevocationRequest> requests) {
        this.requests = requests;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public URI getModerator() {
        return moderator;
    }

    public void setModerator(URI moderator) {
        this.moderator = moderator;
    }


    public static String signatureOn(ArrayList<RevocationRequest> requests){
        return CryptoUtils.computeSha256HashAsHex(JaxbHelper.gson.toJson(requests));

    }
}
