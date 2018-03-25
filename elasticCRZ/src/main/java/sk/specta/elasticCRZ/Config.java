package sk.specta.elasticCRZ;

import java.net.UnknownHostException;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//@EnableElasticsearchRepositories(basePackages = "sk/specta/elasticCRZ")
public class Config {

	/*
	@Bean
	public ElasticsearchOperations elasticsearchTemplate() throws UnknownHostException {
		return new ElasticsearchTemplate(client());
		// return new ElasticsearchTemplate(nodeBuilder().local(true).node().client());
	}
	*/

	/*
	@Bean
	public Client client() throws UnknownHostException {
		// Build the settings for our client.
		Settings settings = Settings.builder()
				// Setting "transport.type" enables this module:
				// .put("transport.type",
				// "org.elasticsearch.transport.netty.FoundNettyTransport")
				// Create an api key via the console and add it here:
				// .put("transport.found.api-key", parameters.apiKey)
				.put("cluster.name", "docker-cluster")
				.put("xpack.security.user", "elastic:testPass")
				// .put("client.transport.ignore_cluster_name", false)
				// .put("client.transport.nodes_sampler_interval", "30s")
				// .put("client.transport.ping_timeout", "30s")
				.build();

		
		
		// instantiate TransportClient and add ES to list of addresses to connect to
//		Client client = new PreBuiltXPackTransportClient(settings)
//				.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9200));
		return client;
	}
	*/
}
