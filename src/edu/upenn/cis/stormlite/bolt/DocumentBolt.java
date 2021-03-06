package edu.upenn.cis.stormlite.bolt;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sleepycat.je.DatabaseException;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.spout.URLSpout;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;
import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.DBWrapperFactory;

public class DocumentBolt implements IRichBolt {
	
	static Logger log = Logger.getLogger(DocumentBolt.class);
	
	//Instance variables
	private static DBWrapper db;
//	private static WebsiteRecord webrecord;
	OutputCollector collector;
	Fields schema = new Fields("EXTRACTED_URL");
	
	
	String executorId = UUID.randomUUID().toString();
	
//	public void setWebsiteRecord(WebsiteRecord webrecord){
//		this.webrecord = webrecord;
//	}
	
	public DocumentBolt(){
		
	}
	
	@Override
	public String getExecutorId() {
		return executorId;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(schema);
	}

	@Override
	public void cleanup() {
		db.close();
	}

	@Override
	public void execute(Tuple input) {
		// TODO Auto-generated method stub
		String documentUri = input.getStringByField("documentUri");
		String documentBody = input.getStringByField("documentBody");
		
		// check if we have this content already. 
		//  If we do, then don't emit 
		//  If we don't, then store and emit
		
		//log.info("got document for url " + documentUri);
		
		// dont store, just pass along
		if( ! documentUri.endsWith(".xml") ){ // html file?

			URLInfo url = new URLInfo(documentUri);
			Document doc = Jsoup.parse(documentBody);

			Elements links = doc.select("a[href]");

			for( Element link : links  ){

				String abs_url = url.getBaseUrl() + link.attr("href");

				Values vals = new Values(abs_url);
				collector.emit(vals);
			}
		} 
		
		
		if( db.containsWebPage(documentBody) == true){
			
			// dont store

			
		} else{
						
			//store the document in db.
			db.putWebPage(documentUri, documentBody);
			
			// update the timestamp for this document in seenURLs doc. for the Head requests
//			synchronized( webrecord ){
//				Date now = Calendar.getInstance().getTime();
//				webrecord.seenUrls.put(documentUri, now);
//			}
			
		}
		
	}

	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		
		
		this.collector = collector;
		String dbDir = stormConf.get("dbDir");
		
		try {
			
			DBWrapperFactory factory = new DBWrapperFactory();
			factory.setDBDir(dbDir);
			// set up db connection.
			db = factory.getDBWrapper() ;
			
		} catch (DatabaseException e) {
			
			e.printStackTrace();
			System.exit(1);
			
		} catch (IOException e) {
			
			e.printStackTrace();
			System.exit(1);
			
		}
		
		
	}

	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
		
	}

	@Override
	public Fields getSchema() {
		return schema;
	}

}
