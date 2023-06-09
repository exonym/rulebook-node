package io.exonym.actor.actions;

import com.sun.xml.ws.util.ByteArrayBuffer;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.helpers.XmlHelper;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.time.Timing;
import io.exonym.uri.NamespaceMngt;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.utils.RulebookVerifier;
import io.exonym.utils.storage.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// TODO Node Transfer Protocol
public class NodeVerifier {
	
	private static final Logger logger = LogManager.getLogger(NodeVerifier.class);
	private  KeyContainer rawKeys;
	private  KeyContainerWrapper keys;
	
	private  ConcurrentHashMap<String, ByteArrayBuffer> byteContent;
	private  ConcurrentHashMap<String, ByteArrayBuffer> signatureBytes;
	private  ConcurrentHashMap<String, Object> contents;
	
	private TrustNetwork targetTrustNetwork = null;
	private final TrustNetworkWrapper ownTrustNetwork;
	private XKey nodePublicKey = null;
	private String keyCheck = null;
	private PresentationPolicy presentationPolicy = null;
	private CredentialSpecification credentialSpecification = null;
	private InspectorPublicKey inspectorPublicKey = null;
	private Rulebook rulebook = null;

	private final HashMap<String, IssuerParameters> issuerParameterMap = new HashMap<String, IssuerParameters>();
	private final HashMap<String, RevocationAuthorityParameters> revocationAuthorityMap = new HashMap<String, RevocationAuthorityParameters>();
	private final HashMap<String, RevocationInformation> revocationInformationMap = new HashMap<String, RevocationInformation>();
	private final HashMap<String, PresentationToken> presentationTokenMap = new HashMap<String, PresentationToken>();

	private final String networkName;
	private URL nodeUrl;
	
	private long touched = Timing.currentTime();
	private final boolean amISource; 
	
	private AsymStoreKey publicKey;

	/**
	 * Verify Node Regardless of whether Local Data is Up to Date, or not
	 *
	 * @param primary
	 * @param secondary
	 * @param isTargetSource
	 * @param amISource
	 * @return
	 * @throws Exception
	 */
	public static NodeVerifier tryNode(URL primary, URL secondary,
									   boolean isTargetSource, boolean amISource) throws Exception {
//		throw new Exception();
		try {
			tryUrl(primary, isTargetSource, amISource);
			return new NodeVerifier(primary, isTargetSource, amISource);

		} catch (FileNotFoundException e){
			tryUrl(secondary, isTargetSource, amISource);
			return new NodeVerifier(secondary, isTargetSource, amISource);

		} catch (UnknownHostException e){
			tryUrl(secondary, isTargetSource, amISource);
			return new NodeVerifier(secondary, isTargetSource, amISource);

		} catch (Exception e){
			throw e;

		}
	}

	/**
	 * Verify Node Regardless of whether Local Data is Up to Data, or not
	 *
	 * @param primary
	 * @param secondary
	 * @return
	 * @throws Exception
	 */
	public static void confrimPrimaryAndSecondary(URL primary, URL secondary) throws Exception {
		try {
			tryUrl(primary, false, false);
			tryUrl(secondary, false, false);

		} catch (Exception e){
			throw new UxException("One or more of the publish locations is unavailable.  Check for errors and try again");

		}
	}



	/**
	 * To be used in conjunction with ping(), so the URL has already been established
	 *
	 * @param known
	 * @param isTargetSource
	 * @param amISource
	 * @return
	 * @throws Exception
	 */
	public static NodeVerifier openNode(URL known, boolean isTargetSource, boolean amISource) throws Exception {
		return new NodeVerifier(known, isTargetSource, amISource);

	}

	/**
	 * Establish which URL works
	 * @param primary
	 * @param secondary
	 * @param lastUpdatedTime
	 * @param isTargetSource
	 * @param amISource
	 * @return the URL that worked, or NULL if the lastUpdateTime matched that in the signatures.xml file at the URL
	 *
	 * @throws Exception
	 */
	public static URL ping(URL primary, URL secondary, String lastUpdatedTime,
									   boolean isTargetSource, boolean amISource) throws Exception {
		try {
			String t = tryUrl(primary, isTargetSource, amISource);
			if (t.equals(lastUpdatedTime)){
				return null;

			} else {
				return primary;

			}
		} catch (FileNotFoundException e){
			String t = tryUrl(secondary, isTargetSource, amISource);
			if (t.equals(lastUpdatedTime)){
				return null;

			} else {
				return secondary;

			}
		} catch (UnknownHostException e){
			String t = tryUrl(secondary, isTargetSource, amISource);
			if (t.equals(lastUpdatedTime)){
				return null;

			} else {
				return secondary;

			}
		} catch (Exception e){
			throw e;

		}
	}

	public static NodeVerifier openLocal(URL url, KeyContainer localSourceSig, boolean amISource) throws Exception {
		return new NodeVerifier(url, localSourceSig, amISource);


	}

	private static String tryUrl(URL url, boolean isTargetSource, boolean amISource) throws Exception {
		try {
			URL sig = new URL(url + "/signatures.xml");
			String xml = new String(UrlHelper.readXml(sig), "UTF8");
			KeyContainer kc = JaxbHelper.xmlToClass(xml, KeyContainer.class);
			return kc.getLastUpdateTime();

		} catch (Exception e){
			throw e;

		}
	}

	private NodeVerifier(URL node, boolean isTargetSource, boolean amISource) throws Exception {
		this.amISource=amISource;
		
		if (isTargetSource){
			if (!node.toString().contains("x-source")){
				throw new UxException("URL must be a Source-URL " + node);
				
			}
		}
		this.nodeUrl = trainAtFolder(node);
		String[] parts = this.nodeUrl.toString().split("/");
		networkName = parts[parts.length-2]; 
		
		byteContent = XmlHelper.openXmlBytesAtUrl(this.nodeUrl);
		signatureBytes = computeBytesThatWereSigned(byteContent);
		contents = XmlHelper.deserializeOpenXml(byteContent);
		this.ownTrustNetwork = openMyTrustNetwork(); // flip to network map
		logger.info("Opened URL " + node);
		verification();
		
	}

	private ConcurrentHashMap<String, ByteArrayBuffer> computeBytesThatWereSigned(
			ConcurrentHashMap<String, ByteArrayBuffer> byteContent) throws UnsupportedEncodingException {
		ConcurrentHashMap<String, ByteArrayBuffer> result = new ConcurrentHashMap<>();
		for (String key : byteContent.keySet()){
			String s = new String(byteContent.get(key).getRawData(), "UTF8");
			String t = NodeVerifier.stripStringToSign(s);
			result.put(key, new ByteArrayBuffer(t.getBytes()));

		}
		return result;

	}

	private NodeVerifier(URL url, KeyContainer keys, boolean amISource) throws Exception {
		try {
			networkName = null;

			this.amISource = amISource;
			byteContent = readLocalBytes(url, keys);
			signatureBytes = computeBytesThatWereSigned(byteContent);
			contents = XmlHelper.deserializeOpenXml(byteContent);
			ownTrustNetwork = openMyTrustNetwork();
			verification();

		} catch (FileNotFoundException e){
			throw new HubException("The URL was likely incorrect " + url, e);

		} catch (Exception e){
			throw e;

		}
	}

	private ConcurrentHashMap<String, ByteArrayBuffer> readLocalBytes(URL url, KeyContainer keys) throws Exception {
		ConcurrentHashMap<String, ByteArrayBuffer> result = new ConcurrentHashMap<>();
		String sigXml = JaxbHelper.serializeToXml(keys, KeyContainer.class);
		result.put("signatures.xml", new ByteArrayBuffer(sigXml.getBytes()));

		for (XKey key : keys.getKeyPairs()){
			if (key.getKeyUid().toString().startsWith(NamespaceMngt.URN_PREFIX_COLON)){
				String fn = XContainerJSON.uidToXmlFileName(key.getKeyUid());
				byte[] b = UrlHelper.read(new URL(url.toString() + "/" + fn));
				result.put(fn, new ByteArrayBuffer(b));

			} else {
				logger.debug("Opening materials and ignoring " + key.getKeyUid());

			}
		}
		return result;

	}

	public UIDHelper getUidHelperForMostRecentIssuerParameters() throws Exception {
		TrustNetworkWrapper tnw = new TrustNetworkWrapper(this.getTargetTrustNetwork());
		return new UIDHelper(tnw.getMostRecentIssuerParameters());

	}


	private TrustNetworkWrapper openMyTrustNetwork() throws Exception {
		NodeManager nm = new NodeManager(networkName);
		TrustNetwork t = nm.openMyTrustNetwork(amISource);
		if (t!=null) {
			return new TrustNetworkWrapper(t);

		} else {
			return null;

		}
	}

	public static TrustNetworkWrapper openMyHostTrustNetwork(String name) throws Exception {
		NodeManager nm = new NodeManager(name);
		TrustNetwork t = nm.openMyTrustNetwork(false);
		if (t!=null) {
			return new TrustNetworkWrapper(t);

		} else {
			return null;

		}
	}

	private void verification() throws Exception {
		try {
			rawKeys = (KeyContainer) contents.get("signatures.xml");
			keys = new KeyContainerWrapper(rawKeys);
			logger.info("Found keys and verifying PublicKey signature.");
			
			XKey x = keys.getKey(KeyContainerWrapper.TN_ROOT_KEY);

			this.keyCheck = Base64.encodeBase64String(x.getPublicKey());

			this.nodePublicKey = x;
			
			publicKey = AsymStoreKey.blank();
			publicKey.assembleKey(x.getPublicKey());

			// Verify Signature on Public Key
			verifySignature(x.getPublicKey(), publicKey, x.getSignature());
			verifyMaterialSignatures(keys.getKeyRingUids());
			
			updateObjects();
			verifyChecksum(keys.getKeyRingUids());
			
			verifyPublicKey(this.targetTrustNetwork.getNodeInformation().getNodeUid());
			byteContent.clear();
			contents.clear();
			
		} catch (Exception e) {
			throw e; 
			
		}
	}

	private boolean verifyPublicKey(URI nodeUid) throws Exception {
		// if network map item exists, use that -- 9/3/23
		// otherwise try the source, if and only if the NMIS exists
		// otherwise build NMI

		if (this.ownTrustNetwork!=null) {
			NetworkParticipant ownRecord = this.ownTrustNetwork.getParticipant(nodeUid);
			if (ownRecord!=null) {
				String k = Base64.encodeBase64String(ownRecord.getPublicKey().getPublicKey());
				if (k.equals(keyCheck)) {
					logger.info("Verified there was no change in Public Key");
					return true;
					
				} else {
					throw new SecurityException("XNode invalid - Public Key has changed");
					
				}
			} else {
				logger.warn("Participant is unknown - this should only be seen during the addition of Nodes to the Network");
				return false;
				
			}
		} else {
			logger.info("Node is being established - no public keys are known");
			return false; 
			
		}
	}


	private boolean verifyChecksum(Set<URI> keyRingUids) throws Exception {
		String check = this.nodeUrl.toString();
		ArrayList<URI> tmp = new ArrayList<URI>(keyRingUids);
		Collections.sort(tmp);
		tmp.remove(KeyContainerWrapper.TN_ROOT_KEY);
		tmp.remove(KeyContainerWrapper.SIG_CHECKSUM);
		for (URI uid : tmp) {
			check += uid;
			
		}
		check = check.replaceAll("/", "");
		logger.debug("CHECK(VERIFY)\n\t\t " + check);
		XKey signature = this.keys.getKey(KeyContainerWrapper.SIG_CHECKSUM);
		if (publicKey.verifySignature(check.getBytes(), signature.getSignature())) {
			logger.info("Checksum Verified");
			return true; 
			
		} else {
			throw new UxException("The XNode is invalid - These files do not belong on this domain, or a file was added or removed");
			
		}
	}

	private void updateObjects() throws Exception {
		for (String f : contents.keySet()){
			Object o = contents.get(f);
			if (o instanceof PresentationPolicy){
				presentationPolicy = ((PresentationPolicy) o);
				
			} else if (o instanceof CredentialSpecification){
				credentialSpecification = (CredentialSpecification) o;
				
			} else if (o instanceof InspectorPublicKey){
				inspectorPublicKey = (InspectorPublicKey) o;
				
			} else if (o instanceof IssuerParameters){
				this.issuerParameterMap.put(f, (IssuerParameters) o);

			} else if (o instanceof RevocationAuthorityParameters){
				this.revocationAuthorityMap.put(f, (RevocationAuthorityParameters) o);
				
			} else if (o instanceof RevocationInformation){
				this.revocationInformationMap.put(f, (RevocationInformation) o);
				
			} else if (o instanceof TrustNetwork){
				targetTrustNetwork = (TrustNetwork) o;
				this.nodeUrl = targetTrustNetwork.getNodeInformation().getStaticNodeUrl0();

			} else if (o instanceof PresentationToken){
				this.presentationTokenMap.put(f, (PresentationToken) o);

			} else if (o instanceof Rulebook){
				this.rulebook = new RulebookVerifier((Rulebook) o).getRulebook();

			} else if (o instanceof KeyContainer){
				// do nothing
				
			} else {
				throw new Exception("Unimplemented object type " + o);
				
			}
		}
	}

	private void verifyMaterialSignatures(Set<URI> keyRingUids) throws Exception {
		HashMap<XKey, ByteArrayBuffer> signatures = new HashMap<>();
		
		for (URI uid : keyRingUids){
			if (!uid.equals(KeyContainerWrapper.TN_ROOT_KEY) && 
					!uid.equals(KeyContainerWrapper.SIG_CHECKSUM)) {
				XKey sig = keys.getKey(uid);
				String fn = XContainer.uidToXmlFileName(uid);
				ByteArrayBuffer b = signatureBytes.get(fn);
				signatures.put(sig, b);
				
			}
		}
		checkSignatures(publicKey, signatures);
		
	}

	public static void checkSignatures(AsymStoreKey key, HashMap<XKey, ByteArrayBuffer> signatures) throws Exception{
		if (signatures==null || key==null){
			throw new Exception("A required attribute was null key " + key + " signatures " + signatures);
			
		}
		for (XKey sig : signatures.keySet()){
			URI uid = sig.getKeyUid();
			logger.info("Verifying Signature of Resource " + uid);
			
			if (uid==null){
				throw new Exception("KeyUID was null");
				
			} if (sig.getSignature()==null){
				throw new Exception("Signature was null for KeyUid " + uid);
				
			} 
			ByteArrayBuffer baf = signatures.get(sig);
			if (baf==null || baf.getRawData()==null){
				throw new Exception("Null raw data for file " + uid);
				
			}
			if (!verifySignature(baf.getRawData(), key, sig.getSignature())){
				throw new Exception("Signature was invalid for UID " + uid);
				
			} else {
				logger.info("Signature Verified for " + uid);
				
			}
		}
	}

	public static String stripStringToSign(String xml){
		return xml.replaceAll("\t", "")
				.replaceAll("\n", "")
				.replaceAll(" ", "")
				.replaceAll("\"", "");
	}

	public static boolean verifySignature(byte[] data, AsymStoreKey key, byte[] signature) throws Exception {
		if (data==null){
			throw new UxException("Data was null");
			
		} if (key==null){
			throw new UxException("Public key was null");
			
		} if (signature==null){
			throw new UxException("Signature was null");
			
		}
		if (key.verifySignature(data, signature)){
			logger.info("PublicKey Signature Verified");
			return true; 
			
		} else {
			throw new UxException("Public Key Signature Verification Failed - the XNode is invalid");	
			
		}
	}
	
	public static URL trainAtFolder(URL node) throws UxException, MalformedURLException {
		String f = node.toString();
		if (f.endsWith("x-node/") || f.endsWith("x-source/")){
			return node;
			
		} else if (f.endsWith("x-node") || f.endsWith("x-source")){
			return new URL(node + "/");
			
		} else if (f.contains("x-node")){
			return new URL(f.substring(0, f.indexOf("x-node")) + "x-node/");
			
		} else if (f.contains("x-source")){
			return new URL(f.substring(0, f.indexOf("x-source")) + "x-source/");
			
		} else {
			throw new UxException("Node Verification Error.  An inspectable url ends with either x-node or x-source (" + node + ")");
			
		}
	}

	@Deprecated
	public PresentationPolicyAlternatives getPresentationPolicyAlternatives() throws HubException {
		throw new HubException("You should not be using Presentation Policy Alternatives.  Deprecated.  Moved to Presentation Policy :pp suffix files - check source materials");

	}

	public PresentationPolicy getPresentationPolicy() {
		return presentationPolicy;
	}


	public CredentialSpecification getCredentialSpecification() throws HubException {
		if (credentialSpecification==null){
			throw new HubException("Credential Specification null");
			
		}
		return credentialSpecification;
		
	}

	public InspectorPublicKey getInspectorPublicKey() throws HubException {
		if (inspectorPublicKey==null){
			throw new HubException("InspectorPublicKey null");
			
		}
		return inspectorPublicKey;
		
	}

	public IssuerParameters getIssuerParameters(String fileName) throws HubException {
		return issuerParameterMap.get(fileName);
		
	}


	public Set<String> getIssuerParameterFileNames(){
		return issuerParameterMap.keySet();
		
	}


	public KeyContainer getRawKeys() {
		return rawKeys;
	}

	public RevocationAuthorityParameters getRevocationAuthorityParameters(String fileName) throws HubException {
		return revocationAuthorityMap.get(fileName);
		
	}

	public ArrayList<PresentationToken> getPresentationTokens() {
		return (ArrayList<PresentationToken>) this.presentationTokenMap.values();

	}

	public Set<String> getAllRevocationAuthorityFileNames(){
		return revocationAuthorityMap.keySet();

	}

	public RevocationInformation getRevocationInformation(String fileName) throws HubException {
		return revocationInformationMap.get(fileName);
	}

	public Set<String> getAllRevocationInformationFileNames(){
		return revocationInformationMap.keySet();

	}

	public void loadTokenVerifierFromNodeVerifier(TokenVerifier tokenVerifier, UIDHelper uids) throws Exception {

		tokenVerifier.loadRevocationInformation(
				this.getRevocationInformation(uids.getRevocationInformationFileName()));

		tokenVerifier.loadIssuerParameters(this.getIssuerParameters(uids.getIssuerParametersFileName()));
		tokenVerifier.loadInspectorParams(this.getInspectorPublicKey());
		tokenVerifier.loadRevocationAuthorityParameters(
				this.getRevocationAuthorityParameters(uids.getRevocationAuthorityFileName()));

	}

	public TrustNetworkWrapper getOwnTrustNetwork() {
		return ownTrustNetwork;
	}

	public TrustNetwork getTargetTrustNetwork() throws HubException {
		if (targetTrustNetwork ==null){
			throw new HubException("TrustNetwork is null");
			
		}
		return targetTrustNetwork;
		
	}
	
	public XKey getPublicKey() throws HubException {
		if (nodePublicKey==null){
			throw new HubException("PublicKey is null");
			
		}
		return nodePublicKey;
		
	}	

	public String getNodeName() {
		return networkName;
	}

	public long getTouched() {
		return touched;
	}

	public void setTouched(long touched) {
		this.touched = touched;
		
	}

	public Rulebook getRulebook() {
		return rulebook;
	}




}
