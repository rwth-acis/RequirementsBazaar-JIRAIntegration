package i5.las2peer.services.servicePackage;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;
import org.scribe.services.RSASha1SignatureService;
import org.scribe.services.SignatureService;
import sun.security.pkcs.PKCS8Key;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * @author Adam Gavronek <gavronek@dbis.rwth-aachen.de>
 * @since 4/8/2015
 */
public class JiraApi extends DefaultApi10a
{

    private static final String JIRA_BASE_URL = "http://layers.dbis.rwth-aachen.de/jira";
    private static final String AUTHORIZE_URL = JIRA_BASE_URL + "/plugins/servlet/oauth/authorize?oauth_token=%s";
    private static final String REQUEST_TOKEN_RESOURCE = JIRA_BASE_URL + "/plugins/servlet/oauth/request-token";
    private static final String ACCESS_TOKEN_RESOURCE = JIRA_BASE_URL + "/plugins/servlet/oauth/access-token";


    @Override
    public String getAccessTokenEndpoint() {
        return ACCESS_TOKEN_RESOURCE;
    }

    @Override
    public String getRequestTokenEndpoint() {
        return REQUEST_TOKEN_RESOURCE;
    }

    @Override
    public String getAuthorizationUrl(Token requestToken) {
        return String.format(AUTHORIZE_URL, requestToken.getToken());
    }

    @Override
    public SignatureService getSignatureService() {
        return new RSASha1SignatureService(getPrivateKey());
    }

    private PrivateKey getPrivateKey() {
        try {
            String pathname = "C:\\Users\\Adam\\Documents\\University\\Work\\RequirementsBazaar-JiraIntegration\\etc\\keys\\myNotAnyMoreProtectedPrivate.key";
            File f = new File(pathname);

            FileInputStream fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int)f.length()];
            dis.readFully(keyBytes);
            dis.close();

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            return kf.generatePrivate(spec);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}