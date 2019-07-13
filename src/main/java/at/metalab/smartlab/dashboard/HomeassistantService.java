package at.metalab.smartlab.dashboard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HomeassistantService {

	@Value("${dashboard.haEndpoint:http://10.20.30.97:8123/api}")
	private String haEndpoint;

	@Value("${dashboard.haApiToken:}")
	private String haApiToken;

	public void service(String domain, String serviceName, String body) {
		new Thread() {
			public void run() {
				try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
					String strUrl = String.format("%s/services/%s/%s", haEndpoint, domain, serviceName);

					HttpPost httpPost = new HttpPost(strUrl);
					httpPost.addHeader("Content-Type", "application/json");
					httpPost.addHeader("Authorization", String.format("Bearer %s", haApiToken));
					httpPost.setEntity(new StringEntity(body));

					httpclient.execute(httpPost);

					System.out.println(strUrl);
				} catch (Exception ignore) {
					ignore.printStackTrace();
				}
			};
		}.start();
	}

	public String get(String endpoint) throws IOException {
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			String strUrl = String.format("%s/%s", haEndpoint, endpoint);

			HttpGet httpGet = new HttpGet(strUrl);
			httpGet.addHeader("Authorization", String.format("Bearer %s", haApiToken));

			CloseableHttpResponse response = httpclient.execute(httpGet);
			return IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
		} catch (RuntimeException e) {
			throw e;
		}
	}

	public String post(String endpoint, String body) throws IOException {
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			String strUrl = String.format("%s/%s", haEndpoint, endpoint);

			HttpPost httpPost = new HttpPost(strUrl);
			httpPost.addHeader("Content-Type", "application/json");
			httpPost.addHeader("Authorization", String.format("Bearer %s", haApiToken));
			httpPost.setEntity(new StringEntity(body));

			CloseableHttpResponse response = httpclient.execute(httpPost);
			return IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
		} catch (RuntimeException e) {
			throw e;
		}
	}

	public void haTurn(String entityId, boolean on) {
		service("homeassistant", on ? "turn_on" : "turn_off", "{ \"entity_id\" : \"" + entityId + "\" }");
	}

}
