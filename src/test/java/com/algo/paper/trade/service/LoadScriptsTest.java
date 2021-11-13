package com.algo.paper.trade.service;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class LoadScriptsTest {

	@LocalServerPort
	private int port;

	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	PaperTradeServiceImpl paperTradeService;

	@Value("${app.angel.nfo.dataFile}")
	private String nfoDataFile;

	@Value("${app.angel.nse.dataFile}")
	private String nseDataFile;
//	@Test
	public void responseEntityTest() {
		String reqUrl = "https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json";
		ResponseEntity<String> responseEntity = restTemplate.exchange(reqUrl, HttpMethod.GET, null, String.class);
		System.out.println(responseEntity.getHeaders());
		JSONArray nfoArray = new JSONArray();
		JSONArray nseArray = new JSONArray();
		if(responseEntity.getBody() != null) {
			String jsonResponse = responseEntity.getBody();
			Set<String> symbols = new HashSet<>();
			try {
				JSONArray jsonArray = new JSONArray(jsonResponse);
				System.out.println("SIZE: "+jsonArray.length());
				for(int i=0; i<jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					symbols.add((String)jsonObject.get("exch_seg"));
					if(jsonObject.get("exch_seg").equals("NFO")) {
						nfoArray.put(jsonObject);
					} else if(jsonObject.get("exch_seg").equals("NSE")) {
						nseArray.put(jsonObject);
					}
					System.out.println(jsonObject.get("symbol")+" : "+jsonObject.get("exch_seg"));
				}
				System.out.println("NFO: "+nfoArray.length());
				System.out.println("NSE: "+nseArray.length());
				
				for(int i=0; i<nfoArray.length(); i++) {
					JSONObject jsonObject = nfoArray.getJSONObject(i);
					if(jsonObject.get("exch_seg").equals("NFO") && ((String)jsonObject.get("symbol")).startsWith("NIFTY")) {
						System.out.println(jsonObject.get("symbol")+" : "+jsonObject.get("exch_seg"));
					}
				}
				
				File nfoFile = new File(nfoDataFile);
				if(!nfoFile.exists())
					nfoFile.createNewFile();
				FileWriter fileWriter = new FileWriter(nfoFile, false);
				fileWriter.write(nfoArray.toString()); 
				fileWriter.flush();
				fileWriter.close();

				File nseFile = new File(nseDataFile);
				if(!nseFile.exists())
					nseFile.createNewFile();
				fileWriter = new FileWriter(nseFile, false);
				fileWriter.write(nseArray.toString()); 
				fileWriter.flush();
				fileWriter.close();

				symbols.stream().forEach(s -> System.out.println(s));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("RESPONSE NOT FOUND SERVICE");
		}

	}
	
	@Test public void placeStrangleStrategyTest() {
		paperTradeService.placeStrangleStrategy();
	}


	//	@Test
	//	public void updateScriptTest() {
	//		System.out.println("FILE: "+dataFile);
	//		try {
	//			FileWriter fileWriter = new FileWriter(dataFile);
	//			fileWriter.write("TESTTTT"); 
	//			fileWriter.flush();
	//			fileWriter.close();
	//        } catch (IOException e) {
	//            e.printStackTrace();
	//        }
	//	}

}
