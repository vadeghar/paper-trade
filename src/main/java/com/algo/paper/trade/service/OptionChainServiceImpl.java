package com.algo.paper.trade.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.algo.paper.trade.model.OpstOptionChain;
import com.algo.paper.trade.model.OpstOptionData;
import com.algo.paper.trade.model.OptionChain;
import com.algo.paper.trade.model.OptionData;
import com.algo.paper.trade.utils.Constants;

@Service
public class OptionChainServiceImpl {
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Value("${app.oiUrl}")
	private String oiUrl;
	
	@Autowired
	RestTemplate restTemplate;
	
	private static final String OPS_URL = "https://opstra.definedge.com/api/free/strategybuilder/optionchain/";
	private OptionChain optionChain;
	
	public OpstOptionChain getOpstOptionChainData(String symbol, String expiry) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36");
		headers.set("sec-ch-ua", "\"Google Chrome\";v=\"95\", \"Chromium\";v=\"95\", \";Not A Brand\";v=\"99\"");
		headers.set("sec-ch-ua-mobile", "?0");
		headers.set("sec-ch-ua-platform", "\"Windows\"");
		headers.set("sec-fetch-mode", "cors");
		headers.set("sec-fetch-site", "same-origin");
		headers.set("referer", "https://opstra.definedge.com/strategy-builder");
		HttpEntity entity = new HttpEntity(headers);
		String url = OPS_URL+symbol+"&"+expiry;
		ResponseEntity<OpstOptionChain> response = restTemplate.exchange(url, HttpMethod.GET, entity, OpstOptionChain.class);
		OpstOptionChain processedResponse = processResponse(response.getBody());
		return processedResponse;
	}
	
	
	private OpstOptionChain processResponse(OpstOptionChain body) {
		OpstOptionChain processedResponse = new OpstOptionChain();
		processedResponse.setAtmStrike(body.getAtmStrike());
		processedResponse.setFuturesPrice(body.getFuturesPrice());
		processedResponse.setSpotPrice(body.getSpotPrice());
		List<OpstOptionData> data = new ArrayList<>();
		for(OpstOptionData optData : body.getData()) {
			OpstOptionData processedOptData = new OpstOptionData();
			processedOptData.setCallDelta((StringUtils.isEmpty(optData.getCallDelta()) || optData.getCallDelta().trim().equals("-")) ? "0.0" : optData.getCallDelta());
			processedOptData.setCallGamma((StringUtils.isEmpty(optData.getCallGamma()) || optData.getCallGamma().trim().equals("-")) ? "0.0" : optData.getCallGamma());
			processedOptData.setCallIV((StringUtils.isEmpty(optData.getCallIV()) || optData.getCallIV().trim().equals("-")) ? "0.0" : optData.getCallIV());
			processedOptData.setCallLTP((StringUtils.isEmpty(optData.getCallLTP()) || optData.getCallLTP().trim().equals("-")) ? "0.0" : optData.getCallLTP());
			processedOptData.setCallTheta((StringUtils.isEmpty(optData.getCallTheta()) || optData.getCallTheta().trim().equals("-")) ? "0.0" : optData.getCallTheta());
			processedOptData.setCallVega((StringUtils.isEmpty(optData.getCallVega()) || optData.getCallVega().trim().equals("-")) ? "0.0" : optData.getCallVega());
			processedOptData.setPutDelta((StringUtils.isEmpty(optData.getPutDelta()) || optData.getPutDelta().trim().equals("-")) ? "0.0" : optData.getPutDelta());
			processedOptData.setPutGamma((StringUtils.isEmpty(optData.getPutGamma()) || optData.getPutGamma().trim().equals("-")) ? "0.0" : optData.getPutGamma());
			processedOptData.setPutIV((StringUtils.isEmpty(optData.getPutIV()) || optData.getPutIV().trim().equals("-")) ? "0.0" : optData.getPutIV());
			processedOptData.setPutLTP((StringUtils.isEmpty(optData.getPutLTP()) || optData.getPutLTP().trim().equals("-")) ? "0.0" : optData.getPutLTP());
			processedOptData.setPutTheta((StringUtils.isEmpty(optData.getPutTheta()) || optData.getPutTheta().trim().equals("-")) ? "0.0" : optData.getPutTheta());
			processedOptData.setPutVega((StringUtils.isEmpty(optData.getPutVega()) || optData.getPutVega().trim().equals("-")) ? "0.0" : optData.getPutVega());
			processedOptData.setStrikePrice((StringUtils.isEmpty(optData.getStrikePrice()) || optData.getStrikePrice().trim().equals("-")) ? "0.0" : optData.getStrikePrice());
			data.add(processedOptData);
		}
		processedResponse.setData(data);
		return processedResponse;
	}


	public Map<String, OpstOptionData> getOpstStrangleStrikes(String symbol, String expiry, Double deltaVal, Double deltaMaxDiff) {
		OpstOptionChain chain = getOpstOptionChainData(symbol, expiry);
		Map<String, OpstOptionData> response = new HashMap<>();
		if(chain == null || CollectionUtils.isEmpty(chain.getData()))
			return null;
		Double ceMaxDelta = deltaVal + deltaMaxDiff;
		Double ceMinDelta = deltaVal  - deltaMaxDiff;
		Double peMaxDelta = deltaVal - (2*deltaVal) + deltaMaxDiff; // 15 - 2*15 + 1 = -14
		Double peMinDelta = deltaVal - (2*deltaVal) - deltaMaxDiff; // 15 - 2*15 - 1 = -16
		Double cDelta = 0.0;
		Double pDelta = 0.0;
		System.out.println("CE MAX: "+ceMaxDelta+" CE MIN:"+ceMinDelta);
		System.out.println("PE MAX: "+peMaxDelta+" PE MIN:"+peMinDelta);
		for(OpstOptionData data: chain.getData()) {
			if(StringUtils.isNotBlank(data.getCallDelta()) && !data.getCallDelta().equals("-")) {
				cDelta = Double.valueOf(data.getCallDelta());
			}
			if(StringUtils.isNotBlank(data.getPutDelta()) && !data.getPutDelta().equals("-")) {
				pDelta = Double.valueOf(data.getPutDelta());
			}
			if(cDelta < ceMaxDelta && cDelta > ceMinDelta) {
				response.put(Constants.CE, data);
			}
			if(pDelta < peMaxDelta && pDelta > peMinDelta) {
				response.put(Constants.PE, data);
			}
		}
		return response;
	}
	
	public OpstOptionData getOptionNearPrice(String optionType, String symbol, String expiry, Double priceNear) {
		OpstOptionChain chain = getOpstOptionChainData(symbol, expiry);
		OpstOptionData response = null;
		if(Constants.PE.equals(optionType)) {
			log.info("*********** GET PUT BETWEEN "+(priceNear-1)+" AND "+(priceNear+1));
			response = getPutOptionDataAt(priceNear, chain, 1.0);
			if(response == null) {
				log.info("***********RETRY - GET PUT BETWEEN "+(priceNear-2)+" AND "+(priceNear+2));
				response = getPutOptionDataAt(priceNear, chain, 2.0);
			}
			if(response == null) {
				log.info("***********RETRY - GET PUT BETWEEN "+(priceNear-3)+" AND "+(priceNear+3));
				response = getPutOptionDataAt(priceNear, chain, 3.0);
			}
			if(response == null) {
				log.info("***********RETRY - GET PUT BETWEEN "+(priceNear-4)+" AND "+(priceNear+4));
				response = getPutOptionDataAt(priceNear, chain, 4.0);
			}
			if(response == null) {
				log.info("***********RETRY - GET PUT BETWEEN "+(priceNear-5)+" AND "+(priceNear+5));
				response = getPutOptionDataAt(priceNear, chain, 5.0);
			}
			
		} else if(Constants.CE.equals(optionType)) {
			log.info("*********** GET CALL AT "+(priceNear-1)+" AND "+(priceNear+1));
			response = getCallOptionDataAt(priceNear, chain, 1.0);
			if(response == null) {
				log.info("***********RETRY - GET CALL BETWEEN "+(priceNear-2)+" AND "+(priceNear+2));
				response = getCallOptionDataAt(priceNear, chain, 2.0);
			}
			if(response == null) {
				log.info("***********RETRY - GET CALL BETWEEN "+(priceNear-3)+" AND "+(priceNear+3));
				response = getCallOptionDataAt(priceNear, chain, 3.0);
			}
			if(response == null) {
				log.info("***********RETRY - GET CALL BETWEEN "+(priceNear-4)+" AND "+(priceNear+4));
				response = getCallOptionDataAt(priceNear, chain, 4.0);
			}
			if(response == null) {
				log.info("***********RETRY - GET CALL BETWEEN "+(priceNear-5)+" AND "+(priceNear+5));
				response = getCallOptionDataAt(priceNear, chain, 5.0);
			}
			
		}
		return response;
	}
	
	private OpstOptionData getPutOptionDataAt(Double priceNear, OpstOptionChain chain, double d) {
		OpstOptionData response = null;;
		for(OpstOptionData data: chain.getData()) {
			if(Double.valueOf(data.getPutLTP()) >= (priceNear-d) && Double.valueOf(data.getPutLTP()) <= (priceNear+d)) {
				return data;
			}
		}
		return response;
	}
	
	private OpstOptionData getCallOptionDataAt(Double priceNear, OpstOptionChain chain, double d) {
		OpstOptionData response = null;;
		for(OpstOptionData data: chain.getData()) {
			if(Double.valueOf(data.getCallLTP()) >= (priceNear-d) && Double.valueOf(data.getCallLTP()) <= (priceNear+d)) {
				return data;
			}
		}
		return response;
	}


	public OptionChain getOptionChainData(String symbol, String expiry) {
		oiUrl = oiUrl.replace("symbol", symbol).replace("expiry", expiry);
		System.out.println("URL: "+oiUrl);
		OptionChain chain = new OptionChain();
		chain.setExpiry(expiry);
		chain.setScriptName(symbol);
		try {
			URL url = new URL(oiUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));
			String output;
			String lin = StringUtils.EMPTY;
			System.out.println("Output from Server .... \n");
			String pageContent = StringUtils.EMPTY;
			while ((output = br.readLine()) != null) {
				if(StringUtils.isNotBlank(output)) {
					output = output.replace("- ", StringUtils.EMPTY).replace("   ", StringUtils.EMPTY);
					if(output.startsWith("<td")) {
						lin = lin + output;
						continue;
					} else if(output.endsWith("</td>")) {
						lin = lin + output+"\n";
						output = new String(lin);
						lin = StringUtils.EMPTY;
					} 
					pageContent = pageContent+"\n"+output;
				}
			}
			chain.setOptionDataList(new ArrayList<>());
			OptionData callOptData = null;
			OptionData putOptData = null;
			int firstStartTableIndex = pageContent.indexOf("<table");
			int firstEndTableIndex = pageContent.indexOf("</table>");
			StringUtils.ordinalIndexOf("pageContent", "<table", 2);
			pageContent = pageContent.substring(pageContent.indexOf("<table",firstStartTableIndex+1 ), pageContent.indexOf("</table>",firstEndTableIndex+1 ));
			String[] lineArr =pageContent.split("\n");
			List<String> lines = Arrays.asList(lineArr);
			List<String> lines2 = new ArrayList<>();
			for(String ln : lines) {
				if(StringUtils.isEmpty(ln))
					continue;
				if(ln.contains("><td")) {
					ln = ln.replace("><td", ">\n<td");
				}
				System.out.println(ln.trim());
				lines2.add(ln.trim());
			}
			String content = null;
			for(String ln : lines2) {
				if(ln.startsWith("<tr")) {
					callOptData = new OptionData();
					putOptData = new OptionData();
				} else if(ln.startsWith("<td id=\"call_open_int")) {
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					callOptData.setOi(content.trim());
				} if(ln.startsWith("<td id=\"call_oi_change")) {
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					callOptData.setOiChange(content.trim());
				} if(ln.startsWith("<td id=\"call_volume")) {
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					callOptData.setVolume(content.trim());
				} if(ln.startsWith("<td id=\"call_change_ltp")) {
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					callOptData.setChangeInLtp(content.trim());
				} if(ln.startsWith("<td id=\"call_ltp")) {
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					callOptData.setLtp(content.trim());
				} if(ln.startsWith("<td id=\"origin")) {
					if(ln.contains("orange_bg")) {
						chain.setAtmStrike(content.trim());
					}
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					callOptData.setStrikePrice(content.trim());
					putOptData.setStrikePrice(content.trim());
				} if(ln.startsWith("<td id=\"puts_ltp")) {
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					putOptData.setLtp(content.trim());
				} if(ln.startsWith("<td id=\"puts_change_ltp")) {
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					putOptData.setChangeInLtp(content.trim());
				} if(ln.startsWith("<td id=\"puts_volume")) {
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					putOptData.setVolume(content.trim());
				} if(ln.startsWith("<td id=\"puts_oi_change85")) {
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					putOptData.setOiChange(content.trim());
				} if(ln.startsWith("<td id=\"puts_open_int")) {
					content = ln.substring(ln.indexOf(">")+1, ln.indexOf("</"));
					putOptData.setOi(content.trim());
				} else if(ln.endsWith("</tr>")) {
					callOptData.setOptionType("CE");
					putOptData.setOptionType("PE");
					chain.getOptionDataList().add(callOptData);
					chain.getOptionDataList().add(putOptData);
				}
			}
			conn.disconnect();
			System.out.println("Size: "+chain.getOptionDataList().size());
		} catch(Exception e) {
			e.printStackTrace();
		}
		chain.setLastUpdated(System.currentTimeMillis());
		this.optionChain = chain;
		return chain;
	}
	
	
	public OptionData getCEStrikePrice(String symbol, String expiry, String price, Integer diff) {
		if(this.optionChain == null || optionChain.getLastUpdated() < System.currentTimeMillis()) {
			this.optionChain = getOptionChainData(symbol, expiry);
		}
		OptionData response = null;
		Double min = Double.valueOf(price) - diff;
		Double max = Double.valueOf(price) + diff;
		List<OptionData> filteredList = optionChain.getOptionDataList().stream()
			      .filter(opt -> 
		      		opt.getOptionType().equals("CE") && StringUtils.isNotBlank(opt.getLtp().replace(",", "")) &&
		      		((Double.valueOf(opt.getLtp().replace(",", "")) < max) && (Double.valueOf(opt.getLtp().replace(",", "")) > min)))
			        .collect(Collectors.toList());

		if(CollectionUtils.isNotEmpty(filteredList)) {
			response = filteredList.get(0);
			System.out.println(response);
		}
		return response;
	}
	
	public OptionData getPEStrikePrice(String symbol, String expiry, String price, Integer diff) {
		if(this.optionChain == null || optionChain.getLastUpdated() < System.currentTimeMillis()) {
			this.optionChain = getOptionChainData(symbol, expiry);
		}
		OptionData response = null;
		Double min = Double.valueOf(price) - diff;
		Double max = Double.valueOf(price) + diff;
		List<OptionData> filteredList = optionChain.getOptionDataList().stream()
			      .filter(opt -> 
			      		opt.getOptionType().equals("PE") && StringUtils.isNotBlank(opt.getLtp()) && StringUtils.isNotBlank(opt.getLtp().replace(",", "")) &&
			      		((Double.valueOf(opt.getLtp().replace(",", "")) < max) && (Double.valueOf(opt.getLtp().replace(",", "")) > min)))
			        .collect(Collectors.toList());

		if(CollectionUtils.isNotEmpty(filteredList)) {
			response = filteredList.get(0);
			System.out.println(response);
		}
		return response;
	} 
	
}
