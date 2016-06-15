package io.pivotal;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.support.MBeanServerConnectionFactoryBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class SpringConfiguration {

	private static final String JMX_URL = "service:jmx:rmi://%s:%s/jndi/rmi://%s:%s/jmxrmi";

	@Profile("jmx")
	@Bean
	public MBeanServerConnectionFactoryBean connection(@Value("${jmx.host}") String host,
			@Value("${jmx.port}") String port, @Value("${jmx.username}") String username,
			@Value("${jmx.password}") String password) {

		skipSSL();
		MBeanServerConnectionFactoryBean factoryBean = new MBeanServerConnectionFactoryBean();
		port = port != null ? port : "44444";
		System.out.println("PORT IS " + port);

		try {
			String serviceURL = createServiceURL(host, port);
			factoryBean.setServiceUrl(serviceURL);
			Map<String, Object> environmentMap = new HashMap<String, Object>();
			String[] credentials = { username, password };
			environmentMap.put("jmx.remote.credentials", credentials);
			factoryBean.setEnvironmentMap(environmentMap);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return factoryBean;
	}

	private void skipSSL() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		try {
			System.out.println("Hacking SSL Validation");
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			SSLContext.setDefault(sc);
			System.out.println("SSL Validation hacked");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	CloudFoundryClient cloudFoundryClient(@Value("${cf.target}") String target,
			@Value("${cf.username}") String username, @Value("${cf.password}") String password) {
		CloudCredentials credentials = new CloudCredentials(username, password);
		CloudFoundryClient client = new CloudFoundryClient(credentials, getTargetURL(target), true);
		client.login();
		return client;
	}

	@Bean
	public JavaMailSender javaMailSender(@Value("${mail.host:null}") String host,
			@Value("${mail.port:-1}") Integer port, @Value("${mail.username:null}") String username,
			@Value("${mail.password:null}") String password, @Value("${mail.starttls:null}") String starttls,
			@Value("${mail.auth:null}") String auth, @Value("${mail.protocol:smtp}") String protocol) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		if (host == null || port == -1) {
			Properties mailProperties = new Properties();
			if (auth != null)
				mailProperties.put("mail.smtp.auth", auth);
			if (starttls != null)
				mailProperties.put("mail.smtp.starttls.enable", starttls);
			mailSender.setJavaMailProperties(mailProperties);
			mailSender.setHost(host);
			mailSender.setPort(port);
			mailSender.setProtocol(protocol);
			if (username != null && password != null) {
				mailSender.setUsername(username);
				mailSender.setPassword(password);
			}
		}
		return mailSender;
	}

	private static URL getTargetURL(String target) {
		try {
			return URI.create(target).toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException("The target URL is not valid: " + e.getMessage());
		}
	}

	private String createServiceURL(String host, String port) {
		String serviceURL = String.format(JMX_URL, host, port, host, port);
		System.out.println(serviceURL);
		return serviceURL;
	}

}
