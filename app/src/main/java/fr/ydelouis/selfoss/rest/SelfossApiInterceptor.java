package fr.ydelouis.selfoss.rest;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import fr.ydelouis.selfoss.BuildConfig;
import fr.ydelouis.selfoss.config.model.Config;
import fr.ydelouis.selfoss.config.model.ConfigManager;
import fr.ydelouis.selfoss.util.Streams;

@EBean
public class SelfossApiInterceptor implements ClientHttpRequestInterceptor {

	private static final String TAG = "Selfoss API";
	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD = "password";
	private static boolean LOG_REQUEST = BuildConfig.DEBUG && true;
	private static boolean LOG_FULL_REQUEST = BuildConfig.DEBUG && true;
	private static boolean LOG_RESPONSE = BuildConfig.DEBUG && true;

	@Bean protected ConfigManager configManager;

	private Config config;

	public void setConfig(Config config) {
		this.config = config;
	}

	private Config getConfig() {
		if (config != null) {
			return config;
		}
		return configManager.get();
	}

	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		HttpRequest apiRequest = new ApiHttpRequest(request);
		logRequest(apiRequest);
		ApiHttpResponse response = new ApiHttpResponse(execution.execute(apiRequest, body));
		logResponse(response);
		return response;
	}

	private void logResponse(ApiHttpResponse response) throws IOException {
		if(LOG_RESPONSE)
			Log.i(TAG, response.getStatusCode().value() + " : " + Streams.stringOf(response.getBody()));
	}

	private void logRequest(HttpRequest request) throws UnsupportedEncodingException {
		String requestUri = URLDecoder.decode(request.getURI().toString(), "UTF-8");
		if(!LOG_FULL_REQUEST && LOG_REQUEST) {
			String url = getConfig().getUrl();
			int start = requestUri.indexOf(url);
			if (start != -1) {
				requestUri = requestUri.substring(start+url.length());
			}
		}
		requestUri = hidePassword(requestUri);
		Log.i(TAG, request.getMethod() + " : " + requestUri);
	}

	private String hidePassword(String requestUri) {
		if (getConfig().requireAuth()) {
			return requestUri.replace(getConfig().getPassword(), "************");
		}
		return requestUri;
	}

	private class ApiHttpRequest implements HttpRequest
	{
		private HttpRequest httpRequest;

		public ApiHttpRequest(HttpRequest httpRequest) {
			this.httpRequest = httpRequest;
			this.httpRequest.getHeaders().set("Content-Length", "0");
			this.httpRequest.getHeaders().remove("Content-Type");

			if (getConfig().requireAuth()) {
				String auth = getConfig().getUsername() + ":" + getConfig().getPassword();
				String authHeader = "Basic " + Base64.encodeToString(auth.getBytes(Charset.forName("US-ASCII")), Base64.DEFAULT);
				this.httpRequest.getHeaders().set("Authorization", authHeader);
			}
		}

		@Override
		public HttpHeaders getHeaders() {
			return httpRequest.getHeaders();
		}

		@Override
		public HttpMethod getMethod() {
			return httpRequest.getMethod();
		}

		@Override
		public URI getURI() {
			URI uri = httpRequest.getURI();
			Uri.Builder builder = new Uri.Builder();
			builder.scheme(getScheme());
			builder.authority(getConfig().getUrl());
			builder.path(uri.getPath());
			builder.encodedQuery(uri.getQuery());
			if (getConfig().requireAuth()) {
				builder.appendQueryParameter(KEY_USERNAME, getConfig().getUsername());
				builder.appendQueryParameter(KEY_PASSWORD, getConfig().getPassword());
			}
			Uri newUri = builder.build();
			String uriStr = newUri.toString().replace(newUri.getEncodedAuthority(), newUri.getAuthority());
			return URI.create(uriStr);
		}

		private String getScheme() {
			return getConfig().useHttps() ? "https" : "http";
		}
	}

	private class ApiHttpResponse implements ClientHttpResponse
	{
		private ClientHttpResponse response;
		private byte[] byteResponse;

		public ApiHttpResponse(ClientHttpResponse response) {
			try {
				this.response = response;
				this.response.getHeaders().set("Content-Type", "application/json");
				byteResponse = Streams.byteArrayOf(response.getBody());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public InputStream getBody() throws IOException {
			return new ByteArrayInputStream(byteResponse);
		}

		@Override
		public HttpHeaders getHeaders() {
			return response.getHeaders();
		}

		@Override
		public void close() {
			response.close();
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return response.getRawStatusCode();
		}

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return response.getStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return response.getStatusText();
		}
	}
}

