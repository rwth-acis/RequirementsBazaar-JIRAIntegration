package i5.las2peer.services.servicePackage;

import net.rcarz.jiraclient.ICredentials;
import org.apache.http.HttpRequest;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.Map;

/**
 * @author Adam Gavronek <gavronek@dbis.rwth-aachen.de>
 * @since 4/9/2015
 */
public class OAuthCredentials implements ICredentials {
    private final Token accessToken;
    private final OAuthService oauthService;

    public OAuthCredentials(Token accessToken, OAuthService oAuthService) {
        this.accessToken = accessToken;
        this.oauthService = oAuthService;
    }

    @Override
    public void authenticate(HttpRequest httpRequest) {
        String uri = httpRequest.getRequestLine().getUri();
        Verb requestMethod = Verb.valueOf(httpRequest.getRequestLine().getMethod().toUpperCase());
        OAuthRequest oAuthRequest = new OAuthRequest(requestMethod, uri);
        oauthService.signRequest(accessToken,oAuthRequest);
        for (Map.Entry<String, String> headerEntry : oAuthRequest.getHeaders().entrySet()) {
            httpRequest.addHeader(headerEntry.getKey(),headerEntry.getValue());
        }
    }

    @Override
    public String getLogonName() {
        return accessToken.getToken();
    }
}
