package io.exonym.rulebook.context;

import com.google.gson.JsonObject;
import com.ibm.zurich.idmix.abc4trust.facades.IssuanceMessageFacade;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import com.ibm.zurich.idmx.util.bigInt.BigIntFactoryImpl;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.PseudonymInToken;
import io.exonym.actor.actions.IdContainerJSON;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.standard.Form;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.storage.IdContainer;
import io.exonym.utils.storage.ImabAndHandle;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.UnmarshalException;
import java.util.HashMap;
import java.util.List;

@WebServlet("/issue")
public class IssueSybilServlet extends HttpServlet {

    public static final String ATT_CONTEXT = "context";
    public static final String ATT_TOKEN = "token";
    
    private static final Logger logger = LogManager.getLogger(IssueSybilServlet.class);
    

    // ISSUER - INIT (Step 1)
    // 			OWNER - Step A
    // ISSUER - Step (Step 2)
    // 			OWNER - Step B
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            HashMap<String, String> in = WebUtils.buildParams(req, resp);
            IAuthenticator auth = IAuthenticator.getInstance();
            auth.authenticateApiKey(in);
            MembershipManagerWeb mmw = MembershipManagerWeb.getInstance();
            RulebookNodeProperties props = RulebookNodeProperties.instance();
            PassStore store = new PassStore(props.getNodeRoot(), false);
            if (in.containsKey(ATT_TOKEN)){
                // Step 2
                issuanceStep(in, mmw, resp);

            } else if (in.containsKey(ATT_CONTEXT)){
                // Step 1
                initIssuance(in, mmw, store, resp);

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e), resp);

        }
    }

    private void issuanceStep(HashMap<String, String> in, MembershipManagerWeb mmw,
                              HttpServletResponse resp) throws Exception {
        String tokenB64 = in.get(ATT_TOKEN);
        String context = in.get(ATT_CONTEXT);

        try {
            String token = new String(Base64.decodeBase64(tokenB64), StandardCharsets.UTF_8);
            IssuanceMessage im = (IssuanceMessage) JaxbHelperClass.deserialize(token).getValue();
            IssuanceMessageFacade imf = new IssuanceMessageFacade(im, new BigIntFactoryImpl());

            List<PseudonymInToken> nyms = imf.getIssuanceToken()
                    .getIssuanceTokenDescription()
                    .getPresentationTokenDescription()
                    .getPseudonym();

            String sybilScope = null;
            String sybilNym = null;

            for (PseudonymInToken nym : nyms){
                if (nym.isExclusive()){
                    sybilScope = nym.getScope();
                    sybilNym = Form.toHex(nym.getPseudonymValue());
                }
            }

            logger.debug(">>>>>>>>>>>>> ------------------------------------------------ ");
            logger.debug(">");
            logger.debug(">");
            logger.debug(">");
            logger.debug(">");
            logger.debug("> IssuanceMessageContent=" + token);
            logger.debug(">");
            logger.debug(">");
            logger.debug(">");
            logger.debug(">");

            ImabAndHandle imab = mmw.ssiIssuerStep(context, im);
            imab.setSybilScope(sybilScope);
            imab.setSybilNym(sybilNym);

            logger.debug("handle @ sybil = "+ imab.getHandle());
            logger.debug("nym @ sybil = "+ imab.getSybilNym());
            logger.debug("scope @ sybil = "+ imab.getSybilScope());
            respond(imab, resp);

        } catch (UnmarshalException e) {
            throw new UxException(ErrorMessages.TOKEN_INVALID, "Parsing error");

        }
    }

    private void initIssuance(HashMap<String, String> in,
                              MembershipManagerWeb mmw,
                              PassStore store,
                              HttpServletResponse resp) throws Exception {
        IssuanceMessageAndBoolean imab = mmw.ssiIssuerInit(in.get("context"),
                in.get("sybilClass"), store);
        respond(imab, resp);

    }

    private void respond(IssuanceMessageAndBoolean imab, HttpServletResponse resp) throws Exception {
        String xml = IdContainerJSON.convertObjectToXml(imab);
        logger.info(xml);
        String issue0 = Base64.encodeBase64String(xml.getBytes(StandardCharsets.UTF_8));
        JsonObject o = new JsonObject();
        o.addProperty("imab", issue0);
        WebUtils.respond(resp, o);

    }

    private void respond(ImabAndHandle imab, HttpServletResponse resp) throws Exception {
        String issue0 = Base64.encodeBase64String(
                IdContainerJSON.convertObjectToXml(imab.getImab()).getBytes(StandardCharsets.UTF_8)
        );
        JsonObject o = new JsonObject();
        o.addProperty("imab", issue0);
        o.addProperty("h", imab.getHandle());
        o.addProperty("nym", imab.getSybilNym());
        o.addProperty("scope", imab.getSybilScope());
        o.addProperty("issuerUid", imab.getIssuerUID().toString());
        WebUtils.respond(resp, o);

    }
}
