package io.exonym.actor.actions;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.Const;
import io.exonym.lite.time.Timing;
import io.exonym.utils.storage.KeyContainer;
import io.exonym.utils.storage.KeyContainerWrapper;
import io.exonym.utils.storage.TrustNetwork;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Logger;

public class MyTrustNetworkAndKeys {

    private final Path sigPath, trustNetworkPath;

    private KeyContainerWrapper kcw;

    private final static Logger logger = Logger.getLogger(MyTrustNetworkAndKeys.class.getName());

    private AsymStoreKey myPublicKey;

    private final TrustNetwork trustNetwork;

    public MyTrustNetworkAndKeys(boolean amILead) throws Exception {
        Path root = null;
        if (amILead){
            root = Path.of(Const.PATH_OF_STATIC, Const.LEAD);
        } else {
            root = Path.of(Const.PATH_OF_STATIC, Const.MODERATOR);
        }
        this.sigPath = root.resolve(Const.SIGNATURES_XML);
        this.kcw = openSignature();

        this.trustNetworkPath = root.resolve(
                IdContainerJSON.uidToXmlFileName(Const.TRUST_NETWORK_UID));

        trustNetwork = determineOutcome();

    }

    private TrustNetwork determineOutcome() throws UxException {
        try {
            XKey signature = kcw.getKey(Const.TRUST_NETWORK_UID);
            XKey xkey = kcw.getKey(KeyContainerWrapper.TN_ROOT_KEY);
            myPublicKey = AsymStoreKey.blank();
            myPublicKey.assembleKey(xkey.getPublicKey());
            return openTrustNetwork(this.trustNetworkPath, signature, myPublicKey);

        } catch (InvalidKeySpecException e) {
            throw new UxException(ErrorMessages.DB_TAMPERING, e);

        } catch (UxException e) {
            throw e;

        } catch (Exception e) {
            throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED, e);

        }
    }

    private TrustNetwork openTrustNetwork(Path trustNetworkPath, XKey signature, AsymStoreKey mk) throws Exception {
        try {
            String tnw = Files.readString(trustNetworkPath);
            String stripped = NodeVerifier.stripStringToSign(tnw);
            mk.verifySignature(stripped.getBytes(StandardCharsets.UTF_8), signature.getSignature());
            TrustNetwork tn = JaxbHelper.xmlToClass(tnw, TrustNetwork.class);
            return tn;

        } catch (Exception e) {
            throw new UxException(ErrorMessages.DB_TAMPERING);

        }

    }

    private KeyContainerWrapper openSignature() throws Exception {
        String sigs = Files.readString(this.sigPath);
        KeyContainer kcPublic = JaxbHelper.xmlToClass(sigs, KeyContainer.class);
        return new KeyContainerWrapper(kcPublic);

    }

    public TrustNetwork getTrustNetwork() {
        return trustNetwork;
    }

    public AsymStoreKey getMyPublicKey() {
        return myPublicKey;
    }

    public KeyContainerWrapper getKcw() {
        return kcw;
    }

    public static void main(String[] args) throws Exception {



    }
}
