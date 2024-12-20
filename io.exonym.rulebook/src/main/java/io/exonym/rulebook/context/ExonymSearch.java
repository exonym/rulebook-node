package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import eu.abc4trust.xml.PresentationTokenDescription;
import eu.abc4trust.xml.PseudonymInToken;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.AbstractNetworkMap;
import io.exonym.lite.couchdb.Query;
import io.exonym.lite.couchdb.QueryElement;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.ExceptionCollection;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.Form;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExonymSearch {

    private static final Logger logger = LogManager.getLogger(ExonymSearch.class);
    private final HashMap<URI, String> riToXi = new HashMap<>();
    private final HashMap<URI, String> otherNyms = new HashMap<>();
    private final URI r0;
    private final CouchRepository<ExoMatrix> repoExonym;
    private final String root;
    private final NetworkMapItemModerator myModNmi;
    private final AbstractNetworkMap networkMap;

    /**
     * When instantiated we perform local data integrity checks:
     *  correct exonyms, are the exonyms bound and exclusive?
     *
     *  .search() will execute a local database search and return ExonymResult
     *      if it returns null, no results are discovered.
     *      otherwise; a list of modUids is returned.
     *
     *  .expand() will discover which rules are broken,
     *      who controls which rule,
     *      which hosts can be interpreted as being honest under a lower rule
     *
     * @param token
     * @param expectedRules
     * @throws Exception
     */
    protected ExonymSearch(PresentationTokenDescription token,
                           ArrayList<URI> expectedRules,
                           CouchRepository<ExoMatrix> repoExonym,
                           AbstractNetworkMap networkMap,
                           String root) throws Exception {
        if (expectedRules.isEmpty()){
            throw new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR,
                    "You haven't populated the rules for this mod");

        }
        this.repoExonym = repoExonym;
        this.root = root;
        this.networkMap=networkMap;
        this.myModNmi = networkMap.nmiForMyNodesModerator();
        List<PseudonymInToken> nyms = token.getPseudonym();
        if (nyms.isEmpty()){
            throw new UxException(ErrorMessages.NO_EXONYMS_IN_TOKEN);

        }
        extractPseudonyms(nyms);
        verifyRuleUrns(expectedRules);
        this.r0 = expectedRules.get(0);

    }

    private void extractPseudonyms(List<PseudonymInToken> nyms) {
        for (PseudonymInToken nym : nyms){
            String ri = nym.getScope();
            String xi = Form.toHex(nym.getPseudonymValue());

            if (nym.isExclusive()){
                logger.debug(ri + " " + xi);
                riToXi.put(URI.create(ri), xi);

            } else {
                logger.debug("Non-Exclusive: " + ri + " " + xi);
                otherNyms.put(URI.create(ri), xi);

            }
        }
    }

    private void verifyRuleUrns(ArrayList<URI> expectedRules) throws UxException {
        ArrayList<URI> missing = new ArrayList<>();
        for (URI r : expectedRules){
            if (!riToXi.containsKey(r)) {
                missing.add(r);


            }
        }
        if (!missing.isEmpty()){
            String message = "";
            for (URI m : missing){
                if (message.length()!=0){
                    message += ", ";

                }
                message += m;

            }
            throw new UxException(ErrorMessages.MISSING_EXONYMS, message);

        }
    }

    protected ExonymResult search() throws Exception {
        try {
            String x0 = riToXi.get(r0);
            logger.debug("Searching for " + x0);

            if (x0!=null){
                String n6 = x0.substring(0, 6);
                String x0Hash = CryptoUtils.computeSha256HashAsHex(x0);
                logger.info("Searching for" + x0Hash + " nibble6=" +n6);
                QueryElement q = new QueryElement(ExoMatrix.FIELD_X0, x0Hash, Query.EQUAL);
                List<ExoMatrix> candidates = repoExonym.read(q);
                ArrayList<ExoMatrix> matches = new ArrayList<>();

                for (ExoMatrix can : candidates){
                    if (can.getNibble6().equals(n6)){
                        matches.add(can);

                    }
                }
                if (matches.isEmpty()){
                    logger.debug("No Exonyms Found - Matches Empty");
                    return null;

                } else {
                    return evaluateMatches(n6, x0Hash, x0, matches);

                }
            } else {
                throw new UxException(ErrorMessages.MISSING_EXONYMS,
                        "Failed to find x0 for r0" + this.r0);

            }
        } catch (NoDocumentException e) {
            logger.debug("No Exonyms Found");
            return null;

        } catch (Exception e) {
            throw e;

        }
    }

    private ExonymResult evaluateMatches(String n6, String x0Hash, String x0,
                                         ArrayList<ExoMatrix> matches) {
        ExonymResult result = new ExonymResult();
        result.setX0Hash(x0Hash);
        result.setNibble6(n6);
        result.setX0(x0);
        ArrayList<URI> modUids = result.getModUids();
        for (ExoMatrix m : matches){
            modUids.add(m.getModUid());

        }
        return result;

    }

    protected ApplicantReport expandResults(ExonymResult input){
        String n6 = input.getNibble6();
        String x0 = input.getX0();
        ArrayList<URI> hosts = input.getModUids();
        ExceptionCollection collection = new ExceptionCollection();
        ArrayList<ExonymDetailedResult> resultSet = collectDetailedReports(n6, x0, hosts, collection);
        ApplicantReport report = analyseDetailedReports(n6, x0, resultSet, collection);
        report.setDetailedResults(resultSet);
        return report;

    }

    private ArrayList<ExonymDetailedResult> collectDetailedReports(String n6, String x0,
                                                                   ArrayList<URI> mods,
                                                                   ExceptionCollection collection) {
        ArrayList<ExonymDetailedResult> resultSet = new ArrayList<>();

        for (URI modUid : mods){
            try {
                ExonymMatrixManagerGlobal matrixManager = new ExonymMatrixManagerGlobal(
                        (NetworkMapItemModerator) this.networkMap.nmiForNode(modUid),
                        myModNmi, n6,  root);
                ExonymDetailedResult result = matrixManager.detailedResult(x0);
                resultSet.add(result);

            } catch (Exception e) {
                logger.error("Error", e);
                collection.addException(e);

            }
        }
        return resultSet;

    }

    private ApplicantReport analyseDetailedReports(String n6, String x0,
                                                   ArrayList<ExonymDetailedResult> resultSet,
                                                   ExceptionCollection collection) {
        ApplicantReport report = new ApplicantReport();
        report.setN6(n6);
        report.setX0(x0);
        report.setExceptions(collection);

        int offences = 0;
        boolean unsettled = false;

        ArrayList<URI> hosts = report.getPreviousHosts();

        DateTime mostRecent = new DateTime(DateTimeZone.UTC)
                .withDate(2020, 1 ,1);

        for (ExonymDetailedResult detail : resultSet){
            offences += detail.getOffences();
            logger.debug("Detail for analysis: " + JaxbHelper.gson.toJson(detail));

            if (detail.isUnsettled()){
                unsettled = true;

            }
            hosts.add(detail.getModUID());

            if (detail.getLastViolationTime()!=null){
                if (mostRecent.isBefore(detail.getLastViolationTime())){
                    mostRecent = detail.getLastViolationTime();

                }
            }
            if (!detail.getActiveControlledRules().isEmpty()){
                if (!detail.isOverridden()){
                    report.setMember(true);

                }
            }
        }
        logger.info("Setting unresolved: " + unsettled);
        report.setTotalOffences(offences);
        report.setUnresolvedOffences(unsettled);

        if (mostRecent.getYear()!=2020){
            report.setMostRecentOffenceTimeStamp(mostRecent);

        }

        return report;

    }
}
