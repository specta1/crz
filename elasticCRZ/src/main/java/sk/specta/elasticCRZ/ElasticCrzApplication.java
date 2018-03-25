package sk.specta.elasticCRZ;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ElasticCrzApplication {

	DateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
	DecimalFormat decimalFormat;
	
	@PostConstruct
	public void init() throws IOException, ParseException
	{
		RestHighLevelClient client = null;
		try {
			final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY,
			        new UsernamePasswordCredentials("elastic", "testPass"));
			
			HttpHost host = new HttpHost("localhost", 9200, "http");
			RestClientBuilder restCl = RestClient.builder(host).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
	            @Override
	            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
	                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
	            }
	        });;
			client = new RestHighLevelClient(restCl);

			Calendar d = Calendar.getInstance();
			d.setTime(format.parse("01.08.2017"));
			Date now = new Date();
			
/*			DeleteIndexRequest dreq = new DeleteIndexRequest(getIndexName(currentDayString));
			IndicesClient indices = client.indices();
			if (indices != null)
			{
				DeleteIndexResponse response = client.indices().delete(dreq);
				
			}
*/			
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setGroupingSeparator(',');
			symbols.setDecimalSeparator('.');
			String pattern = "########0.00";
			decimalFormat = new DecimalFormat(pattern, symbols);
			decimalFormat.setParseBigDecimal(true);
			
		
			do
			{
				List<String> ret = readWebPageForDay(format.format(d.getTime()));
				writeToElastic(client, ret, d);
				d.add(Calendar.DATE, 1);
			} while (d.getTime().getTime() < now.getTime());
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			client.close();
		}
		
	}
	
	public List<String> readWebPageForDay(String currentDayString) throws IOException, ParseException
	{
		List<String> ret = new ArrayList<String>();
		
		int pageNum = 0;
		
		List<String> r = null;
		boolean finished = true;
		do
		{
			r = readPerPage(currentDayString, pageNum);
			if (r.size()==0)
			{
//				if(r.get(0).startsWith("Table:"))
//				{
					finished = false;	
				}else
				{
					ret.addAll(r);
					pageNum++;
				}
//			}
			System.out.println(pageNum + " : " + currentDayString);
			try {
				Thread.sleep((long) ((Math.random() * 2000f)));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		while (finished);
		
		return ret;

	}

	private List<String> readPerPage(String currentDayString, int pageNum)
			throws IOException, ParseException {
		List<String> ret = new ArrayList<String>();
		String url = "http://www.crz.gov.sk/index.php?ID=2171273&art_zs2=&art_predmet=&art_ico=&art_suma_zmluva_od=&art_suma_zmluva_do=&art_datum_zverejnene_od="+currentDayString+"&art_datum_zverejnene_do="+currentDayString+"&page="+ pageNum +"&art_rezort=0&art_zs1=&nazov=&art_ico1=&odoslat=Vyh%C4%BEada%C5%A5";
		boolean success = true;
		Document document = null;
		do
		{
			try {
				document = Jsoup.connect(url).get();
				success = false;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		while (success);

        String table = document.select("div#container div#content div#page table.table_list tbody tr").text();
        System.out.println("Table: " + table);

        Elements answerers = document.select("div#container div#content div#page table.table_list tbody tr");
        for (Element rows : answerers) {
        	String day = rows.select("td.cell1 span").text();
        	if (day != null && day.length() > 0)
        	{
	        	Date d = format.parse(currentDayString);
//	            System.out.println("Rows: " + d.toString());

	        	String contractName = rows.select("td.cell2 a").text();
//	            System.out.println("Contract Name : " + contractName);
	        	String contractNumber = rows.select("td.cell2 span").text();
//	            System.out.println("Contract Number: " + contractNumber);
        	
	        	String amountText = rows.select("td.cell3").text();
	        	String amountTextUpdated = amountText.substring(0, amountText.length() - 3);
	        	amountTextUpdated = amountTextUpdated.replaceAll("\\s+", "");
	        	Number amount = decimalFormat.parse(amountTextUpdated);
//	            System.out.println("Contract amount: " + amount.toString());
        	
	        	String contractor = rows.select("td.cell4").text();
//	            System.out.println("Contractor : " + contractor);
	        	String contractIssuer = rows.select("td.cell5").text();
//	            System.out.println("Contract issuer: " + contractIssuer);
	        	
	    		// INITIALIZE ELASTIC
	    		XContentBuilder builder = jsonBuilder()
	    			    .startObject()
	    			        .field("contractName", contractName)
	    			        .field("contractNumber", contractNumber)
	    			        .field("amount", amount)
	    			        .field("contractor", contractor)
	    			        .field("contractIssuer", contractIssuer)
	    			        .field("date", d)
	    			    .endObject();	
        	
	    		ret.add(builder.string());
	    		
	    		System.out.println(builder.string());

        	}
        }
        return ret;
	}
	
	public void writeToElastic(RestHighLevelClient client, List<String> items, Calendar d)
	{
		int i = 0;
		for (String item : items)
		{
			i++;
			IndexRequest indexRequest = new IndexRequest(getIndexName(d.get(Calendar.YEAR) + "-" + d.get(Calendar.MONTH)), "doc");
			indexRequest.source(item, XContentType.JSON);
			try {
				client.index(indexRequest);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

	private String getIndexName(String index) {
		return "crz-" + index;
	}
	
	
	public static void main(String[] args) throws IOException, ParseException {
		SpringApplication.run(ElasticCrzApplication.class, args);

	}
}
