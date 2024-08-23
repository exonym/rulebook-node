package io.exonym.lite.standard;

import java.net.URI;
import java.nio.file.Path;

public class Const {
	
	public static final String ASYM_ENCRYPTION_ALGORITHM = "RSA";
	public static final String ASYM_STANDARD_CIPHER_ALGORITHM = "RSA/ECB/OAEPPadding";// "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
	public static final int ASYM_KEY_SIZE = 2048;
	public static final String SYM_ENCRYPTION_ALGORITHM = "AES";
	public static final int SYM_KEY_SIZE = 128;
	public static final String MESSAGE_AUTHENTICATION_CODE_ALGORITHM = "HmacSHA1";

	public static final String BINDING_ALIAS = "urn:io:exonym";
	public static final String LEAD = "lead";
	public static final String MODERATOR = "moderator";

	public static final String SIGNATURES_XML = "signatures.xml";

	public static final URI TRUST_NETWORK_URN = URI.create("urn:rulebook:trust-network:ni");

	public static final String PATH_OF_HTML = Path.of("/var", "www", "html").toString();
	public static final String PATH_OF_STATIC = PATH_OF_HTML + "/static";

	public static final String LOCAL_PATH_OF_STATIC = Path.of("io.exonym.rulebook", "static").toString();

}
