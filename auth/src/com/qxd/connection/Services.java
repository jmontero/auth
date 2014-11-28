package com.qxd.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import android.util.Log;

public class Services {

	private AbstractHttpClient mClient = null;
	private String port = null;
	private String url = null;
	private String auth = null;

	public Services(){

	}

	public Services(String url, String port) {
		this.url = url;
		this.port = port;
	}
	
	public Services(String url, String port, String auth) {
		this.url = url;
		this.port = port;
		this.auth = auth;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	private void ensureClient() {
		if (null == mClient) {				
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			HttpConnectionParams.setSoTimeout(httpParameters, 120000);
			HttpConnectionParams.setSocketBufferSize(httpParameters, 8192);
			httpParameters.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
			httpParameters.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
			httpParameters.setParameter(CoreProtocolPNames.USER_AGENT, "Auth Service");

			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			//schemeRegistry.register(new Scheme("https", sf, 443));

			ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager(httpParameters, schemeRegistry);

			mClient = new DefaultHttpClient(connectionManager, httpParameters); 
		}
	}

	private HttpPost buildPost(String port,	String server, String auth) {
		HttpPost request;
		String url;

		url = server + ":" + port;
		Log.v("HTTP Post", url);
		request = new HttpPost(url);
		//String basicAuth = "Basic " + Base64.encode(userName + ":" + password);
		request.setHeader("Authorization", auth);
		request.setHeader("Content-Type", "application/json");
		return request;
	}

	public String sendRequest(String action, JSONObject params) {
		HttpResponse resp = null;
		String strResponse = "";
		int code = 0;
		ensureClient();

		HttpPost post = buildPost(port, url, auth);
		try {
			post.setEntity(new StringEntity(params.toString()));
			resp = mClient.execute(post);

			code = resp.getStatusLine().getStatusCode();
			if (code != 200) {
				return null;
			}

			strResponse = getResponseString(resp);
			
			return strResponse;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String getResponseString(HttpResponse response) {
		InputStream is = null;

		int bytesRead = 0;
		int bufferSize = 1024;
		byte[] buffer = null;

		ByteArrayOutputStream byteStream = null;
		String respString = null;

		try {
			is = response.getEntity().getContent();

			byteStream = new ByteArrayOutputStream();
			buffer = new byte[bufferSize];

			while ((bytesRead = is.read(buffer)) != -1) {
				byteStream.write(buffer, 0, bytesRead);
			}

			// Remove non UTF-8 chars.
			CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
			utf8Decoder.onMalformedInput(CodingErrorAction.IGNORE);
			utf8Decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);

			respString = utf8Decoder.decode(
					ByteBuffer.wrap(byteStream.toByteArray())).toString();
			respString = respString.replace("\r", "");
			respString = respString.replace("\n", "");

			byteStream.close();			
		} catch (Exception e) {
			respString = null;
		}

		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {}
		}

		return respString;
	}

}
