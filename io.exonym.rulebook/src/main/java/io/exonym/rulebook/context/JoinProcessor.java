package io.exonym.rulebook.context;

import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.VerifiedClaim;
import io.exonym.actor.actions.ExonymMatrix;
import io.exonym.actor.actions.NodeVerifier;
import io.exonym.actor.actions.TokenVerifier;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.PenaltyException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.Form;
import io.exonym.lite.standard.PassStore;
import io.exonym.lite.standard.QrCode;
import io.exonym.lite.time.DateHelper;
import io.exonym.rulebook.schema.IdContainer;
import io.exonym.utils.storage.ImabAndHandle;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBElement;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class JoinProcessor {

    private static final Logger logger = LogManager.getLogger(JoinProcessor.class);
    private final VerifiedClaim claim;

    private JoinSupportSingleton support = JoinSupportSingleton.getInstance();

    private RulebookNodeProperties props = RulebookNodeProperties.instance();

    private final HashMap<URI, String> riToXj = new HashMap<>();

    private String hashOfNonce = null;


    public JoinProcessor() throws Exception {
        if (support!=null){
            claim = new VerifiedClaim(support.getMyModCs());

        } else {
            throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED,
                    "This Rulebook Node is not fully defined.",
                    "Please be patient while the administrator sets up this node");

        }

    }

    protected String joinChallenge(boolean qr) throws Exception {
        try {
            IssuanceMessageAndBoolean imab = initIssuance();
            Rulebook challengedRulebook = support.getRulebookVerifier().getDeepCopy();
            String xml = IdContainer.convertObjectToXml(imab);
            byte[] compressed = WebUtils.compress(xml.getBytes(StandardCharsets.UTF_8));
            String b64Xml = Base64.encodeBase64String(compressed);
            String link = Namespace.UNIVERSAL_LINK_JOIN_REQUEST + b64Xml;
            if (qr){
                String qrImage = QrCode.computeQrCodeAsPngB64(link, 300);
                challengedRulebook.setChallengeB64(qrImage);

            }
            challengedRulebook.setLink(link);
            return JaxbHelper.gson.toJson(challengedRulebook, Rulebook.class);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);

        }
    }

    private IssuanceMessageAndBoolean initIssuance() throws Exception {
        IssuancePolicy policy = support.buildIssuancePolicy();
        byte[] nonce = policy.getPresentationPolicy().getMessage().getNonce();
        this.hashOfNonce = CryptoUtils.computeSha256HashAsHex(nonce);

        String xml = io.exonym.utils.storage.IdContainer.convertObjectToXml(policy);
        logger.debug(xml);

        return support.getMyModIssuer().issueInit(claim, policy, support.getStore().getEncrypt(),
                URI.create("urn:" + UUID.randomUUID()));

    }

    protected IssuanceMessageAndBoolean rejoin(IssuanceMessage im,
                                               IssuanceToken issuanceToken, Vio vio) throws Exception {

        PresentationTokenDescription ptd = issuanceToken
                .getIssuanceTokenDescription()
                .getPresentationTokenDescription();

        ImabAndHandle imab = support.getMyModIssuer().issueStep(
                im, support.getStore().getDecipher());

        String x0 = buildExonymMap(ptd);
        addMemberToDb(imab, x0);
        publishExonymMapAndBroadcast(new ArrayList<>(), vio); // see register potential honesty claims.
        return imab.getImab();

    }

    protected IssuanceMessageAndBoolean join(IssuanceMessage im,
                                             IssuanceToken issuanceToken) throws Exception {

        PresentationTokenDescription ptd = issuanceToken
                .getIssuanceTokenDescription()
                .getPresentationTokenDescription();

        // This will throw the penalty exception.
        ArrayList<String> uncontrolled = verifyConditionsToJoin(ptd);

        ImabAndHandle imab = support.getMyModIssuer().issueStep(
                im, support.getStore().getDecipher());

        String x0 = buildExonymMap(ptd);
        addMemberToDb(imab, x0);
        publishExonymMapAndBroadcast(uncontrolled, null); // see register potential honesty claims.
        return imab.getImab();

    }

    private void addMemberToDb(ImabAndHandle imab, String x0) throws Exception {
        try {
            logger.info("Adding user to the database");
            String[] nibbles = ExonymMatrix.extractNibbles(x0);
            String hashOfH = CryptoUtils.computeSha256HashAsHex(
                    imab.getHandle().toByteArray());
            IUser user = new IUser();
            user.setType(IUser.I_USER_MEMBER);
            user.setX0(x0);
            user.setNibble6(nibbles[1]);
            user.set_id(hashOfH);
            CouchDbHelper.repoUsersAndAdmins().create(user);

        } catch (Exception e) {
            throw e;

        }
    }

    private String buildExonymMap(PresentationTokenDescription token) {
        List<PseudonymInToken> nyms = token.getPseudonym();
        String x0 = null;
        for (PseudonymInToken nym : nyms){
            String xj = Form.toHex(nym.getPseudonymValue());
            if (nym.isExclusive() && x0==null) {
                x0 = xj;
            }
            String ri = nym.getScope();
            this.riToXj.put(URI.create(ri), xj);

        }
        return x0;
    }

    private ArrayList<URI> registerPotentialHonestyClaim(TokenVerifier verifier,
                                                            PresentationToken token) throws Exception {
        PresentationTokenDescription ptd = token.getPresentationTokenDescription();
        List<CredentialInToken> credentials = ptd.getCredential();
        URI sourceUuid = support.getMyModerator().getLeadUID();

        for (CredentialInToken cit : credentials){
            URI ip = cit.getIssuerParametersUID();
            String ips = ip.toString();

            if (!(ips.contains("sybil") && ips.contains("anticlone"))) { // todo error
                UIDHelper helper = new UIDHelper(ip);

                if (helper.getLeadUid().equals(sourceUuid)){
                    return loadVerifierWithCurrentProofsOfHonesty(verifier, ip, token);

                } else {
                    throw new UxException(ErrorMessages.PROOF_IS_OUT_OF_SCOPE,
                            helper.getLeadUid().toString(), sourceUuid.toString());
                }
            }
        }
        return new ArrayList<>();
    }


    private ArrayList<URI> loadVerifierWithCurrentProofsOfHonesty(TokenVerifier verifier,
                                                                     URI issuerUid, PresentationToken token) throws Exception {
        UIDHelper uids = new UIDHelper(issuerUid);


        NodeVerifier n0 = NodeVerifier.openNode(support.getMyModerator().getStaticURL0(), false, false);
        n0.loadTokenVerifierFromNodeVerifier(verifier, uids);

        /**
         * If the user is already honest we must establish what rules we control
         *
         */
        return collectUncontrolledRules(uids.getModeratorUid(), token);
//        throw new RuntimeException("There was this funny noKeyHere test that you didn't understand - TODO");
//
//        if (!files.containsKey("noKeyHere")){
//            throw new UxException(ErrorMessages.TOKEN_INVALID, "Not implemented current proofs of honesty");
//
//        }
    }

    private ArrayList<URI> collectUncontrolledRules(URI host, PresentationToken token) throws Exception {
        ArrayList<URI> uncontrolledRules = new ArrayList<>();
        byte[] unverifiedX0Bytes = token
                .getPresentationTokenDescription()
                .getPseudonym().get(0)
                .getPseudonymValue();

        String unverifiedX0 = Form.toHex(unverifiedX0Bytes);
        String n6 = unverifiedX0.substring(0, 6);
        ExonymMatrixManagerGlobal global = new ExonymMatrixManagerGlobal(
                (NetworkMapItemModerator) support.getNetworkMap().nmiForNode(host),
                support.getMyModerator(), n6, this.props.getNodeRoot());

        try {
            ExonymMatrix matrix = global.openUncontrolledList(unverifiedX0);
            ExonymMatrixRow row = matrix.findExonymRow(unverifiedX0);
            ArrayList<URI> nodeRules = matrix.getRuleUrns();

            int n = 0;
            for (String xn : row.getExonyms()) {
                if (!xn.equals("null")){
                    uncontrolledRules.add(nodeRules.get(n));

                }
                n++;
            }
        } catch (HubException e) {
            logger.warn("Expected Error if the target has no uncontrolled rules\n\t\t" + e.getMessage());

        }
        return uncontrolledRules;
    }

    private ArrayList<String> verifyConditionsToJoin(PresentationTokenDescription token) throws Exception {
        ArrayList<String> result = new ArrayList<>();
        ApplicantReport report = performSearch(token, support.getMyRules());
        logger.info(JaxbHelper.gson.toJson(report));

        if (report!=null){
            if (!report.getExceptions().isEmpty()){
                throw report.getExceptions();

            }
            if (report.isMember()){
                ArrayList<URI> previous = report.getPreviousHosts();
                String[] p = new String[previous.size()];
                int i = 0;
                for (URI a : previous){
                    p[i] = a.toString();
                    i++;

                }
                throw new UxException(ErrorMessages.ALREADY_JOINED, p);

            }
            if (report.isUnresolvedOffences()){
                logger.info(JaxbHelper.gson.toJson(report));
                PenaltyException e = new PenaltyException(ErrorMessages.BANNED_UNTIL);
                e.setReport(report);
                throw e;

            }
        }
        // TODO note that the report needs to return a list of the rules that are uncontrolled
        return result;

    }

    private ApplicantReport performSearch(PresentationTokenDescription token, ArrayList<URI> rules) throws Exception {
        ExonymSearch network = new ExonymSearch(token, rules,
                CouchDbHelper.repoExoMatrix(), support.getNetworkMap(),
                this.props.getNodeRoot());

        ExonymResult result = network.search();
        logger.info("Expanding results if not null: " + JaxbHelper.gson.toJson(result));

        if (result!=null){
            return network.expandResults(result);

        } else {
            return null;

        }
    }

    private void publishExonymMapAndBroadcast(ArrayList<String> uncontrolled, Vio vio) throws Exception {
        ExonymMatrixRowAndX0 xyLists = buildXYLists(uncontrolled);
        ExonymMatrixManagerLocal local = new ExonymMatrixManagerLocal(
                support.getMyModContainer(),
                support.getMyRules(), support.getMyModerator(),
                props.getNodeRoot());

        local.addExonymRow(vio, xyLists.getExox(), xyLists.getExoy());
        broadcastJoin(xyLists.getN6(), xyLists.getX0(), vio);

    }

    private ExonymMatrixRowAndX0 buildXYLists(ArrayList<String> uncontrolledRules) {
        ExonymMatrixRowAndX0 result = new ExonymMatrixRowAndX0();
        ArrayList<String> u = new ArrayList<>();
        ArrayList<String> c = new ArrayList<>();
        ArrayList<URI> myRules = support.getMyRules();
        String x0 = riToXj.get(myRules.get(0));
        String x0Hash = CryptoUtils.computeSha256HashAsHex(x0);
        String n6 = x0.substring(0,6);

        for (URI ri : myRules){
            if (uncontrolledRules.contains(ri)){
                u.add(riToXj.get(ri));
                c.add("null");

            } else {
                u.add("null");
                c.add(riToXj.get(ri));

            }
        }
        ExonymMatrixRow controlled = new ExonymMatrixRow();
        controlled.addExonyms(c);
        ExonymMatrixRow uncontrolled = new ExonymMatrixRow();
        uncontrolled.addExonyms(u);
        result.setExox(controlled);
        result.setExoy(uncontrolled);
        result.setN6(n6);
        result.setX0(x0Hash);
        if (uncontrolledRules.isEmpty()){
            result.setExox(controlled);
            result.setExoy(null);

        } else {
            result.setExox(controlled);
            result.setExoy(uncontrolled);

        }
        return result;
    }

    protected void broadcastJoin(String n6, String x0Hash, Vio vio) throws Exception {
        ExoNotify notify = new ExoNotify();
        notify.setType(ExoNotify.TYPE_JOIN);
        notify.setNibble6(n6);
        notify.setHashOfX0(x0Hash);
        notify.setNodeUid(support.getMyModerator().getNodeUID());
        notify.setT(DateHelper.currentIsoUtcDateTime());
        notify.setTimeOfViolation(vio!=null ? vio.getTimeOfViolation() : null);
        signAndSend(notify);

    }

    protected void signAndSend(ExoNotify notify) {
        try {
            byte[] toSign = ExoNotify.signatureOn(notify);
            RulebookNodeProperties props = RulebookNodeProperties.instance();
            PassStore store = new PassStore(props.getNodeRoot(), false);
            ModeratorNotificationSigner signer = ModeratorNotificationSigner.getInstance();
            byte[] sigBytes = signer.sign(toSign,
                    support.getMyModerator().getNodeUID().toString(), store);

            String sig = Base64.encodeBase64String(sigBytes);
            notify.setSigB64(sig);

            NotificationPublisher.getInstance()
                    .getPipe().put(notify);

        } catch (Exception e) {
            throw new RuntimeException(e);

        }
    }

    public String getHashOfNonce() {
        return hashOfNonce;
    }
}
