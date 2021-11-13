package com.algo.paper.trade.service;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;

import com.algo.paper.trade.model.LTPQuote;
import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.exceptions.SmartAPIException;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class AngelSmartServiceImplTest {
	
	@LocalServerPort
	private int port;
	
	@Value("${app.angel.nfo.dataFile}")
	private String nfoDataFile;

	@Value("${app.angel.nse.dataFile}")
	private String nseDataFile;
	
	@Autowired
	AngelSmartServiceImpl angelSmartService;
	
	@Autowired
	SmartConnect smartConnect;
	
//	@Test
	public void getLTPTest() {
		try {
			String token = getSpotInstToken("NIFTY");
			System.out.println("TOKEN: "+token);
			org.json.JSONObject niftySpot = angelSmartService.getLTP("NSE", "NIFTY", token);
			System.out.println("RESP: "+niftySpot);
			LTPQuote q = new LTPQuote();
			q.instrumentToken = niftySpot.getLong("symboltoken");
			q.lastPrice = niftySpot.getDouble("ltp");
			System.out.println("LTPQuote: "+q);
		} catch (IOException | SmartAPIException | JSONException e) {
			e.printStackTrace();
		}
	}
	
	public List<LTPQuote> getNfoLTPs(List<String> symbols, String exchange) {
		List<LTPQuote> ltps = new ArrayList<>();
		try {
			for(String symbol : symbols) {
				String token = getNfoInstToken(symbol);
				System.out.println("TOKEN: "+token);
				org.json.JSONObject niftySpot = angelSmartService.getLTP(exchange, symbol, token);
				System.out.println("RESP: "+niftySpot);
				LTPQuote q = new LTPQuote();
				q.instrumentToken = niftySpot.getLong("symboltoken");
				q.lastPrice = niftySpot.getDouble("ltp");
				ltps.add(q);
			}
			System.out.println("LTPQuote: "+ltps);
		} catch (IOException | SmartAPIException | JSONException e) {
			e.printStackTrace();
		}
		return ltps;
	}
	
	@Test
	public void getLtpsTest() {
		List<String> symbols = Arrays.asList("NIFTY","BANKNIFTY");
		List<LTPQuote> ltps = getNfoLTPs(symbols, "NFO");
		System.out.println("LTPS: "+ltps);
	}
	
	public String getSpotInstToken(String symbol) {
		try {
			JSONParser parser = new JSONParser();
			JSONArray nseArray = (JSONArray) parser.parse(new FileReader(nseDataFile));
			for(int i=0; i<nseArray.size() ; i++) {
				JSONObject jsonObject = (JSONObject) nseArray.get(i);
				if(((String)jsonObject.get("symbol")).equals(symbol)) {
					return (String)jsonObject.get("token");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getNfoInstToken(String symbol) {
		try {
			JSONParser parser = new JSONParser();
			JSONArray nseArray = (JSONArray) parser.parse(new FileReader(nfoDataFile));
			for(int i=0; i<nseArray.size() ; i++) {
				JSONObject jsonObject = (JSONObject) nseArray.get(i);
				System.out.println(jsonObject.get("symbol"));
				if(((String)jsonObject.get("symbol")).equals(symbol)) {
					return (String)jsonObject.get("token");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
//	@Test
//	public void webSocketTest() {
//		User user = smartConnect.getUser();
//		String strwatchlistscrips = "nse_cm|2885&nse_cm|1594&nse_cm|11536";
//		String FEED_TOKEN = user.getFeedToken();
//		String _req = "{\"task\":\"mw\",\"channel\":" + strwatchlistscrips + ",\"token\":" +FEED_TOKEN  + ",\"user\": \"' + CLIENT_CODE + '\",\"acctid\":\"' + CLIENT_CODE + '\"}"; 
//		
//		String jwtToken = user.getAccessToken(); 
//		SmartWebsocket w = new SmartWebsocket(smartConnect.getUserId(), jwtToken, smartConnect.getApiKey(), "mw", _req);
//		w.connect();
//	}

}
