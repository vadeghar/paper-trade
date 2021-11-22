package com.algo.paper.trade.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.algo.model.LTPQuote;
import com.algo.opstra.model.OpstOptionChain;
import com.algo.opstra.model.OpstOptionData;
import com.algo.opstra.model.OpstraResponse;
import com.algo.opstra.model.OpstraSpotResponse;
import com.algo.utils.CommonUtils;
import com.algo.utils.Constants;
import com.algo.utils.ExcelUtils;

@Service
public class OpstraConnect {
	static Logger log = LoggerFactory.getLogger(ExcelUtils.class);
	@Value("${app.opstra.api.optionPrice}")
	private String optionPrice;
	
	@Value("${app.opstra.api.optionChain}")
	private String optionChain;
	
	@Value("${app.opstra.api.spot}")
	private String spot;
	
//	@Value("${app.opstra.api.cookieVal}")
//	private String cookieVal;
	
	@Autowired
	RestTemplate restTemplate;
	
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
		String url = optionChain+symbol+"&"+expiry;
		ResponseEntity<OpstOptionChain> response = restTemplate.exchange(url, HttpMethod.GET, entity, OpstOptionChain.class);
		OpstOptionChain processedResponse = processResponse(response.getBody());
		return processedResponse;
	}
	
	//https://opstra.definedge.com/api/free/strategybuilder/optionprice/NIFTY&25NOV2021&17650&CE
	//https://opstra.definedge.com/api/free/strategybuilder/optionprice/BANKNIFTY&25NOV2021&33000&CE
	public Map<String, LTPQuote> getLTP(List<String> symbols) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36");
		headers.set("sec-ch-ua", "\"Google Chrome\";v=\"95\", \"Chromium\";v=\"95\", \";Not A Brand\";v=\"99\"");
		headers.set("sec-ch-ua-mobile", "?0");
		headers.set("sec-ch-ua-platform", "\"Windows\"");
		headers.set("sec-fetch-mode", "cors");
		headers.set("sec-fetch-site", "same-origin");
//		headers.set("cookie", cookieVal);
		headers.set("referer", "https://opstra.definedge.com/strategy-builder");
		HttpEntity entity = new HttpEntity(headers);
		Map<String, LTPQuote> ltps = new HashMap<>();
		for(String symbol : symbols) {
			String url = optionPrice+CommonUtils.getOpstraSymbol(symbol)+"&"+CommonUtils.getOpstraExpiry(symbol)+"&"+CommonUtils.getOpstraStrikePrice(symbol)+"&"+CommonUtils.getOpstraOptionType(symbol);
			log.info("URL: "+url);
			ResponseEntity<OpstraResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, OpstraResponse.class);
			if(response.getStatusCode() == HttpStatus.OK) {
				OpstraResponse resp = response.getBody();
				ltps.put(symbol, new LTPQuote(0, resp != null ? (resp.getOptionPrice() != null ? Double.valueOf(resp.getOptionPrice()) : 0.0) : 0.0));
			}
		}
		return ltps;
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
		log.info("CE MAX: "+ceMaxDelta+" CE MIN:"+ceMinDelta);
		log.info("PE MAX: "+peMaxDelta+" PE MIN:"+peMinDelta);
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

	public OpstraSpotResponse getSpotResponse(String symbol, String expiry) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36");
		headers.set("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"96\", \"Google Chrome\";v=\"96\"");
		headers.set("sec-ch-ua-mobile", "?0");
		headers.set("sec-ch-ua-platform", "\"Windows\"");
		headers.set("sec-fetch-mode", "cors");
		headers.set("sec-fetch-site", "same-origin");
//		headers.set("cookie", cookieVal);
		headers.set("referer", "https://opstra.definedge.com/strategy-builder");
		HttpEntity entity = new HttpEntity(headers);
		String url = spot+symbol+"&"+expiry;
		log.info("URL: "+url);
		ResponseEntity<OpstraSpotResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, OpstraSpotResponse.class);
		return response.getBody();
	}
	
	
}
