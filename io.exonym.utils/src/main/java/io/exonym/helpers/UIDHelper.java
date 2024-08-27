package io.exonym.helpers;

import eu.abc4trust.xml.CredentialInPolicy;
import eu.abc4trust.xml.CredentialInToken;
import eu.abc4trust.xml.PresentationToken;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.Namespace;
import io.exonym.lite.standard.WhiteList;
import io.exonym.lite.time.DateHelper;
import io.exonym.utils.storage.IdContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.nio.file.Path;

public class UIDHelper {


    private static final Logger logger = LogManager.getLogger(UIDHelper.class);
    private String leadName;
    private String moderatorName;
    private  URI credentialSpec;
    private  URI presentationPolicy;
    private  URI issuedCredential;
    private  URI revocationAuthority;
    private  URI revocationAuthorityInfo;
    private  URI issuancePolicy;
    private  URI issuerParameters;
    private  URI inspectorParams;

    private  URI rulebookUID;

    private  String credentialSpecFileName;
    private  String presentationPolicyFileName;
    private  String issuedCredentialFileName;
    private  String revocationAuthorityFileName;
    private  String revocationInfoFileName;
    private  String issuancePolicyFileName;
    private  String issuerParametersFileName;
    private  String inspectorParamsFileName;
    private  String rulebookFileName;

    private  URI moderatorUid;

    private  URI leadUid;


    public UIDHelper(URI issuerParameters) throws Exception {
        assemble(issuerParameters.toString());

    }

    public UIDHelper(String issuerParameters) throws Exception {
        assemble(issuerParameters);

    }

    private void assemble(String issuerParameters) throws Exception {
        String[] parts = issuerParameters.split(":");
        String mod = parts[0] + ":" +parts[1] + ":" +parts[2] + ":" +parts[3]+ ":" +parts[4] + ":" +parts[5];
        String lead = parts[0] + ":" +parts[1] + ":" +parts[2] + ":" +parts[3] + ":" +parts[5];

        this.leadName = parts[3];
        this.moderatorName = parts[4];
        this.leadUid = URI.create(lead);
        this.moderatorUid = URI.create(mod);
        this.issuerParameters = URI.create(issuerParameters);
        this.issuerParametersFileName = IdContainer.uidToXmlFileName(issuerParameters);
        this.rulebookUID = URI.create(Namespace.URN_PREFIX_COLON + parts[2] + ":" + parts[5]);

        String root = Namespace.URN_PREFIX_COLON  + IdContainer
                .stripUidSuffix(this.issuerParameters, 1);

        issuedCredential = URI.create(root + ":ic");
        issuedCredentialFileName  = IdContainer.uidToXmlFileName(issuedCredential);

        this.presentationPolicy = URI.create(lead + ":pp");
        presentationPolicyFileName = IdContainer.uidToXmlFileName(presentationPolicy);

        revocationAuthority = URI.create(root + ":ra");
        revocationAuthorityFileName = IdContainer.uidToXmlFileName(revocationAuthority);

        revocationAuthorityInfo = URI.create(root + ":rai");
        revocationInfoFileName = IdContainer.uidToXmlFileName(revocationAuthorityInfo);

        issuancePolicy = URI.create(root + ":ip");
        issuancePolicyFileName = IdContainer.uidToXmlFileName(issuancePolicy);

        this.inspectorParams = URI.create(mod + ":ins");
        inspectorParamsFileName = IdContainer.uidToXmlFileName(inspectorParams);

        this.credentialSpec = credentialSpecFromSourceUID(this.leadUid);
        credentialSpecFileName = IdContainer.uidToXmlFileName(credentialSpec);

        this.rulebookFileName = IdContainer.uidToFileName(rulebookUID) + ".json";

    }

    public static String computeLeadNameFromModOrLeadUid(URI sourceOrAdvocateUid){
        if (sourceOrAdvocateUid==null){
            throw new NullPointerException();
        }
        return sourceOrAdvocateUid.toString().split(":")[2];
    }

    public static String computeModNameFromModUid(URI advocateUid){
        if (advocateUid==null){
            throw new NullPointerException();
        }
        return advocateUid.toString().split(":")[3];
    }

    public static String computeRulebookHashFromLeadUid(URI sourceUid){
        if (sourceUid==null){
            return null;
        }
        return sourceUid.toString().split(":")[3];
    }

    public static String computeRulebookHashFromRulebookId(String rulebookId){
        return rulebookId.split(":")[2];
    }

    public static String computeRulebookIdFromAdvocateUid(URI advocateUid){
        if (advocateUid==null){
            throw new NullPointerException();
        }
        return advocateUid.toString().split(":")[5];
    }


    public static URI credentialSpecFromSourceUID(URI sourceUid){
        String[] parts = sourceUid.toString().split(":");
        return URI.create(Namespace.URN_PREFIX_COLON + parts[2] + ":" + parts[4] + ":c");

    }

    public static String stripPrefix(URI uid){
        return stripPrefix(uid.toString());

    }

    public static String stripPrefix(String uid){
        return uid.replaceAll(Namespace.URN_PREFIX_COLON, "");

    }

    public static boolean isSourceUid(URI uid){
        return WhiteList.isSourceUid(uid);

    }

    public static boolean isAdvocateUid(URI uid){
        return WhiteList.isModeratorUid(uid);

    }

    public static String uidToFileName(String uri) throws Exception{
        return IdContainer.uidToFileName(uri);
    }

    public CredentialInPolicy.IssuerAlternatives.IssuerParametersUID computeIssuerParametersUID(){
        CredentialInPolicy.IssuerAlternatives.IssuerParametersUID params = new
                CredentialInPolicy.IssuerAlternatives.IssuerParametersUID();
        params.setValue(this.getIssuerParameters());
        params.setRevocationInformationUID(this.getRevocationInfoParams());
        return params;
    }

    public static CredentialInPolicy.IssuerAlternatives.IssuerParametersUID computeIssuerParametersUID(URI issuerUID){
        URI r = URI.create(issuerUID.toString().replaceAll(":i", ":rai"));
        CredentialInPolicy.IssuerAlternatives.IssuerParametersUID params = new
                CredentialInPolicy.IssuerAlternatives.IssuerParametersUID();
        params.setValue(issuerUID);
        params.setRevocationInformationUID(r);
        return params;
    }


    public static URI fileNameToUid(String filename) throws Exception{
        return URI.create(IdContainer.fileNameToUid(filename));
    }


    public URI getRulebookUID() {
        return rulebookUID;
    }

    public String getRevocationInfoFileName() {
        return revocationInfoFileName;
    }

    public String getRulebookFileName() {
        return rulebookFileName;
    }

    public URI getCredentialSpec() {
        return credentialSpec;
    }

    public URI getIssuedCredential() {
        return issuedCredential;
    }

    public URI getRevocationAuthority() {
        return revocationAuthority;
    }

    public URI getRevocationInfoParams() {
        return revocationAuthorityInfo;
    }

    public URI getIssuancePolicy() {
        return issuancePolicy;
    }

    public URI getIssuerParameters() {
        return issuerParameters;
    }

    public URI getPresentationPolicy() {
        return presentationPolicy;
    }

    public URI getInspectorParams() {
        return inspectorParams;
    }

    public URI getModeratorUid() {
        return moderatorUid;
    }

    public URI getLeadUid() {
        return leadUid;
    }

    public String getCredentialSpecFileName() {
        return credentialSpecFileName;
    }

    public String getPresentationPolicyFileName() {
        return presentationPolicyFileName;
    }

    public String getIssuedCredentialFileName() {
        return issuedCredentialFileName;
    }

    public String getRevocationAuthorityFileName() {
        return revocationAuthorityFileName;
    }

    public String getRevocationInformationFileName() {
        return revocationInfoFileName;
    }

    public String getIssuancePolicyFileName() {
        return issuancePolicyFileName;
    }

    public String getIssuerParametersFileName() {
        return issuerParametersFileName;
    }

    public String getInspectorParamsFileName() {
        return inspectorParamsFileName;
    }

    public String getLeadName() {
        return leadName;
    }

    public String getModeratorName() {
        return moderatorName;
    }

    public static URI computeModUidFromMaterialUID(URI modMaterialUID) throws Exception {
        if (modMaterialUID==null){
            throw new NullPointerException();
        }
        String[] parts = modMaterialUID.toString().split(":");
        StringBuilder result = new StringBuilder();
        result.append(Namespace.URN_PREFIX_COLON);
        result.append(parts[2]);
        result.append(":");
        result.append(parts[3]);
        result.append(":");
        result.append(parts[4]);
        result.append(":");
        result.append(parts[5]);
        URI moderatorUid = URI.create(result.toString());
        if (WhiteList.isModeratorUid(moderatorUid)) {
            return moderatorUid;

        } else {
            throw new UxException(moderatorUid.toString());

        }
    }
    /**
     *
     * @param modUid
     * @return [sourceUid, nodeUid]
     * @throws Exception
     */
    public static URI computeLeadUidFromModUid(URI modUid) throws Exception {
        try {
            if (modUid!=null){
                String[] discovery = modUid.toString().split(":");
                if (discovery.length==6) {
                    URI leadUid = URI.create(
                            discovery[0] + ":"
                                    + discovery[1] + ":"
                                    + discovery[2] + ":"
                                    + discovery[3] + ":"
                                    + discovery[5]);
                    return leadUid;

                } else if (discovery.length==5){
                    return modUid;

                } else {
                    throw new HubException("Invalid Node UID (6 components expected)" + modUid + " got " + discovery.length);

                }
            } else {
                throw new NullPointerException();

            }
        } catch (Exception e){
            throw e;

        }
    }

    public static String tokenFileName(URI advocateUid) throws Exception {
        return DateHelper.currentIsoUtcDateTime()
                + "-" + IdContainer.uidToFileName(advocateUid)
                + ".t.xml";
    }

    public static URI extractFirstIssuerFromPresentationToken(PresentationToken token){
        CredentialInToken credential = token.getPresentationTokenDescription().getCredential().get(0);
        credential.getCredentialSpecUID();
        return credential.getIssuerParametersUID();

    }

    public static URI ensureTrailingSlash(String uri){
        return ensureTrailingSlash(URI.create(uri));
    }

    public static URI ensureTrailingSlash(URI uri){
        logger.info("Trailing slash path" + uri);
        String path = uri.getPath();
        logger.info("Trailing slash path" + path);
        if (path == null || path.isEmpty()) {
            return uri.resolve("/");

        } else if (!path.endsWith("/")) {
            return uri.resolve(path + "/");

        }
        return uri;

    }


    public void out(){
        logger.info(">>>>>>>>>>>>> ");
        logger.info("> Output Projected UIDS");
        logger.info("");
        logger.info(leadName);
        logger.info(moderatorName);
        logger.info(leadUid);
        logger.info(moderatorUid);
        logger.info(rulebookUID);
        logger.info(rulebookFileName);
        logger.info(credentialSpec);
        logger.info(credentialSpecFileName);
        logger.info(presentationPolicy);
        logger.info(presentationPolicyFileName);
        logger.info(issuedCredential);
        logger.info(issuedCredentialFileName);
        logger.info(revocationAuthority);
        logger.info(revocationAuthorityFileName);
        logger.info(revocationAuthorityInfo);
        logger.info(revocationInfoFileName);
        logger.info(issuancePolicy);
        logger.info(issuancePolicyFileName);
        logger.info(inspectorParams);
        logger.info(inspectorParamsFileName);
        logger.info(issuerParameters);
        logger.info(issuerParametersFileName);
        logger.info("");
        logger.info(">>>>>>>>>>>>> ");

    }

    public static void main(String[] args) {
        URI staticUrl = URI.create("http://example.com/")
                .resolve("/static/")
                .resolve("c30/")
                .resolve("lead/");
        System.out.println(staticUrl);
        Path parent = Path.of(staticUrl.getPath())
                .getParent().resolve("rulebook.json");


        System.out.println(parent);


    }

}
