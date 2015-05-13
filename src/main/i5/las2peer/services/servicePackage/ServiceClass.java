package i5.las2peer.services.servicePackage;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.*;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import i5.las2peer.security.Context;
import i5.las2peer.security.UserAgent;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import net.rcarz.jiraclient.*;

import net.minidev.json.JSONObject;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

/**
 * LAS2peer Service
 * 
 * This is a template for a very basic LAS2peer service
 * that uses the LAS2peer Web-Connector for RESTful access to it.
 * 
 */
@Path("example")
@Version("0.1")
public class ServiceClass extends Service {

	private String jdbcDriverClassName;
	private String jdbcLogin;
	private String jdbcPass;
	private String jdbcUrl;
	private String jdbcSchema;


	private OAuthService oAuthService;
	private Map<String,Token> userTokenStore;
	private Map<String,String> reqbazAccessTokenStore;

	public ServiceClass() {
		// read and set properties values
		// IF THE SERVICE CLASS NAME IS CHANGED, THE PROPERTIES FILE NAME NEED TO BE CHANGED TOO!
		setFieldValues();
		// instantiate a database manager to handle database connection pooling and credentials
//		dbm = new DatabaseManager(jdbcDriverClassName, jdbcLogin, jdbcPass, jdbcUrl, jdbcSchema);

		 oAuthService = new ServiceBuilder()
				.provider(JiraApi.class)
				.apiKey("ReqBaz")
				.apiSecret("your_api_secret")
				.debug()
				.build();
		userTokenStore = new ConcurrentHashMap<>();
		reqbazAccessTokenStore = new ConcurrentHashMap<>();
	}

	/**
	 * Simple function to validate a user login.
	 * Basically it only serves as a "calling point" and does not really validate a user
	 * (since this is done previously by LAS2peer itself, the user does not reach this method
	 * if he or she is not authenticated).
	 * 
	 */
	@GET
	@Path("login/{reqbaz_access_token}")
	public String jiraLogin(@PathParam("reqbaz_access_token") String reqBazAccessToken){
		try {
			if (reqBazAccessToken.isEmpty()) return "No requirements bazaar access token has been given!";

			String userIdentifier = UUID.randomUUID().toString();

			Token requestToken = oAuthService.getRequestToken();

			reqbazAccessTokenStore.put(userIdentifier,reqBazAccessToken);
			userTokenStore.put(userIdentifier, requestToken);

			String authorizationUrl = oAuthService.getAuthorizationUrl(requestToken);

			JSONObject response = new JSONObject();
			response.put("verification_url", authorizationUrl);
			response.put("session_id", userIdentifier);

			return response.toJSONString();
		} catch (Exception e) {
			return e.getLocalizedMessage();
		}
	}

	/**
	 * Another example method.
	 *
	 * @param verificationCode
	 *
	 */
	@POST
	@Path("/validate-login/{verificationCode}/{session_id}")
	public String validateLogin(
			@PathParam("verificationCode") String verificationCode,
			@PathParam("session_id") String session) {
		Verifier v = new Verifier(verificationCode);
		Token requestToken = userTokenStore.get(session);
		Token accessToken = oAuthService.getAccessToken(requestToken, v);
		userTokenStore.put(session,accessToken);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("success", true);
		return jsonObject.toJSONString();
	}

	@GET
	@Path("jira/projects/{session_id}")
	public String getJiraProjects(
			@PathParam("session_id") String session) {
		JiraClient jira = getJiraClient(session);
		JSON json = null;
		try {
			json = JSONSerializer.toJSON(jira.getProjects());
		} catch (JiraException e) {
			return e.getLocalizedMessage();
		}
		return json.toString();
	}

	@GET
	@Path("jira/projects/{projectId}/components/{session_id}")
	public String getJiraComponents(
			@PathParam("projectId") String projectId,
			@PathParam("session_id") String session) {
		JiraClient jira = getJiraClient(session);
		JSON json = null;
		try {
			//TODO Fix
			json = JSONSerializer.toJSON(jira.getComponentsAllowedValues(projectId, jira.getIssueTypes().get(0).getName()));
		} catch (JiraException e) {
			return e.getLocalizedMessage();
		}
		return json.toString();
	}

	@POST
	@Path("/mapping/projects/{projectId}")
	public String mapProjects(
			@PathParam("projectId") String jiraProjectId,
			@PathParam("session_id") String session,
			@ContentParam String reqbazProjectId) {
		JiraClient jira = getJiraClient(session);
		try {
			Project project = jira.getProject(jiraProjectId);
			//TODO: GetReqBaz
			//TODO save mapping


		} catch (JiraException e) {
			return e.getLocalizedMessage();
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("success", true);
		return jsonObject.toJSONString();
	}

	private JiraClient getJiraClient(String session) {
		Token accessToken = userTokenStore.get(session);
		ICredentials credentials = new OAuthCredentials(accessToken, oAuthService);
//		BasicCredentials creds = new BasicCredentials("gavronek", "");
		return new JiraClient("http://layers.dbis.rwth-aachen.de/jira",  credentials);
	}


	/**
	 * Method for debugging purposes.
	 * Here the concept of restMapping validation is shown.
	 * It is important to check, if all annotations are correct and consistent.
	 * Otherwise the service will not be accessible by the WebConnector.
	 * Best to do it in the unit tests.
	 * To avoid being overlooked/ignored the method is implemented here and not in the test section.
	 * @return  true, if mapping correct
	 */
	public boolean debugMapping() {
		String XML_LOCATION = "./restMapping.xml";
		String xml = getRESTMapping();

		try {
			RESTMapper.writeFile(XML_LOCATION, xml);
		} catch (IOException e) {
			e.printStackTrace();
		}

		XMLCheck validator = new XMLCheck();
		ValidationResult result = validator.validate(xml);

		if (result.isValid())
			return true;
		return false;
	}

	/**
	 * This method is needed for every RESTful application in LAS2peer. There is no need to change!
	 * 
	 * @return the mapping
	 */
	public String getRESTMapping() {
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

}
