package io.exonym.rulebook.context;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import io.exonym.actor.storage.RootProperties;
import io.exonym.actor.storage.SFTPLogonData;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.time.DateHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;

public class RulebookNodeProperties extends RootProperties {
    
    private static final Logger logger = LogManager.getLogger(RulebookNodeProperties.class);

    private AsymStoreKey appDistributionKey = null;

    protected RulebookNodeProperties() throws Exception {
        super();

    }

    /*
     * Singleton
     */
    private static RulebookNodeProperties instance;

    static {
        try {
            instance = new RulebookNodeProperties();

        } catch (Exception e) {
            logger.error("Instantiation Error", e);

        }
    }

    public static RulebookNodeProperties instance() {
        return instance;

    }

    protected void createNewDistributionKey(){
        try {
            XKey k = XKey.createNew(this.getNodeRoot());
            k.setKeyUid(URI.create(DateHelper.currentBareIsoUtcDate()));
            CloudantClient client = CouchDbClient.instance();
            Database dba = client.database(CouchDbHelper.getDbUsers(), false);
            CouchRepository<XKey> repo = new CouchRepository<>(dba, XKey.class);
            repo.create(k);

        } catch (Exception e) {
            logger.error("Error", e);
        }
    }


    protected AsymStoreKey getAppDistributionKey() {
        try {
            if (this.appDistributionKey ==null){
                logger.info("---Opening Key--");
                CloudantClient client = CouchDbClient.instance();
                Database dba = client.database(CouchDbHelper.getDbUsers(), false);
                CouchRepository<XKey> xKeyRepo = new CouchRepository<>(dba, XKey.class);
                QueryBasic q = QueryBasic.selectType("xkey");
                LinkedList<XKey> keys = new LinkedList<>(xKeyRepo.read(q));
                XKey key = keys.getLast();
                Cipher dec = CryptoUtils.generatePasswordCipher(Cipher.DECRYPT_MODE, this.getNodeRoot(), null);
                this.appDistributionKey = AsymStoreKey.build(key.getPublicKey(), key.getPrivateKey(), dec);

            }
        } catch (Exception e) {
            logger.error("!! CRITICAL-- Unable to open key", e);

        }
        return appDistributionKey;

    }

    private static void addShippingKey() throws Exception{
        CloudantClient client = CouchDbClient.instance();
        Database dba = client.database(CouchDbHelper.getDbUsers(), false);
        CouchRepository<XKey> repo = new CouchRepository<>(dba, XKey.class);
        RulebookNodeProperties props = RulebookNodeProperties.instance();
        XKey key = XKey.createNew(props.getNodeRoot());
        key.setType("xkey");
        repo.create(key);

    }

    @Override
    protected String getDbUsername() {
        return super.getDbUsername();
    }

    @Override
    protected String getDbPassword() {
        return super.getDbPassword();
    }

    @Override
    protected SFTPLogonData getPrimarySftpCredentials() {
        return super.getPrimarySftpCredentials();
    }

    @Override
    protected String getPrimaryDomain() {
        return super.getPrimaryDomain();
    }

    @Override
    protected String getPrimaryStaticDataFolder() {
        return super.getPrimaryStaticDataFolder();
    }

    @Override
    protected String getNodeSupportNumber() {
        return super.getNodeSupportNumber();
    }

    @Override
    protected String getSpawnWiderNetworkFrom() {
        return super.getSpawnWiderNetworkFrom();
    }

    @Override
    protected String getIsoCountryCode() {
        return super.getIsoCountryCode();
    }

    @Override
    protected String getDbPrefix() {
        return super.getDbPrefix();
    }

    @Override
    protected URL getDbUrl() {
        return super.getDbUrl();
    }

    @Override
    protected String getPrimaryAdminUsername() {
        return super.getPrimaryAdminUsername();
    }

    @Override
    protected String getNodeRoot() {
        return super.getNodeRoot();
    }


    @Override
    protected String getBroadcastUrl() {
        return super.getBroadcastUrl();
    }


    @Override
    protected String getAuthorizedDomain() {
        return super.getAuthorizedDomain();
    }

    @Override
    protected SFTPLogonData getTokenTransfer() {
        return super.getTokenTransfer();
    }

    @Override
    protected boolean isOpenSubscription() {
        return super.isOpenSubscription();
    }

    @Override
    public String getRulebookNodeURL() {
        return super.getRulebookNodeURL();
    }

    @Override
    public boolean isOpenSourcePublication() {
        return super.isOpenSourcePublication();
    }

    @Override
    protected String getMqttBroker() {
        return super.getMqttBroker();
    }

    @Override
    protected String messageOnFail(String env, String message) throws UxException {
        return super.messageOnFail(env, message);
    }

    @Override
    protected String optional(String env, String def) {
        return super.optional(env, def);
    }

    @Override
    protected String mandatory(String env) throws UxException {
        return super.mandatory(env);
    }
}
