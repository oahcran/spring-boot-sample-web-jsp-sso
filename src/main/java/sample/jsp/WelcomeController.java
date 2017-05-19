/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.jsp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.cloudfoundry.identity.oauth2.openid.OpenIDTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@Controller
@EnableOAuth2Sso
public class WelcomeController {

	@Value("${application.message:Hello World}")
	private String message = "Hello World";

	// property set by spring-cloud-sso-connector
	@Value("${ssoServiceUrl:placeholder}")
	private String ssoServiceUrl;
	@Value("${security.oauth2.client.clientId:placeholder}")
	private String clientId;

	@Autowired(required = false)
	private OAuth2RestTemplate oauth2RestTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@PostConstruct
	public void init() {
		oauth2RestTemplate.setAccessTokenProvider(accessTokenProviderChain());
	}

	@Bean
	public AccessTokenProvider accessTokenProviderChain() {
		return new AccessTokenProviderChain(Arrays.<AccessTokenProvider> asList(new OpenIDTokenProvider(),
				new AuthorizationCodeAccessTokenProvider(), new ImplicitAccessTokenProvider(),
				new ResourceOwnerPasswordAccessTokenProvider(), new ClientCredentialsAccessTokenProvider()));
	}
	
	@RequestMapping("/")
	public String welcome(Map<String, Object> model) {
		model.put("time", new Date());
		model.put("message", this.message);
		return "welcome";
	}

	@RequestMapping("/foo")
	public String foo(Map<String, Object> model) {
		throw new RuntimeException("Foo");
	}
	

	@RequestMapping("/authorization_code")
	public String authCode(Map<String, Object> model) throws Exception {
		if (ssoServiceUrl.equals("placeholder")) {
			model.put("header", "Warning: You need to bind to the SSO service.");
			model.put("warning", "Please bind your app to restore regular functionality");
			return "configure_warning";
		}
		
		Map<?, ?> userInfoResponse = oauth2RestTemplate.getForObject("{ssoServiceUrl}/userinfo", Map.class,
				ssoServiceUrl);
		model.put("ssoServiceUrl", ssoServiceUrl);
		model.put("response", toPrettyJsonString(userInfoResponse));

		OAuth2AccessToken accessToken = oauth2RestTemplate.getOAuth2ClientContext().getAccessToken();
		if (accessToken != null) {
			model.put("access_token", toPrettyJsonString(parseToken(accessToken.getValue())));
			model.put("id_token",
					toPrettyJsonString(parseToken((String) accessToken.getAdditionalInformation().get("id_token"))));
		}
		
		return "authorization_code";
	}

	private Map<String, ?> parseToken(String base64Token) throws IOException {
		String token = base64Token.split("\\.")[1];
		return objectMapper.readValue(Base64.decodeBase64(token), new TypeReference<Map<String, ?>>() {
		});
	}

	private String toPrettyJsonString(Object object) throws Exception {
		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
	}
	

}
