package com.algo.paper.trade.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.algo.paper.trade.model.OpstOptionChain;
import com.algo.paper.trade.model.OpstOptionData;
import com.algo.paper.trade.model.OptionChain;
import com.algo.paper.trade.model.OptionData;
import com.algo.paper.trade.utils.Constants;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class OptionChainServiceImplTest {

	@LocalServerPort
	private int port;
	
	@Autowired
	OptionChainServiceImpl optionChainService;
	
	@Autowired
	private RestTemplate restTemplate;
	
	//@Test
	public void getOptionChainDataTest() throws IOException {
		OptionChain optionChain = optionChainService.getOptionChainData("BANKNIFTY", "2021-11-03");
		System.out.println("OUT PUT: "+optionChain.getAtmStrike());
		assertThat(optionChain).isNotNull();
		for(OptionData peOption : optionChain.getOptionDataList()) {
			if(peOption.getOptionType().equals("CE"))
				continue;
			//System.out.println(" == "+peOption.getStrikePrice()+" : "+peOption.getLtp());
		}
	}
	
	//@Test
	public void getStrikePrices() throws IOException {
		OptionData ceOption = optionChainService.getCEStrikePrice("BANKNIFTY", "2021-11-03", "115", 2);
		OptionData peOption = optionChainService.getPEStrikePrice("BANKNIFTY", "2021-11-03", "115", 2);
		assertThat(ceOption).isNotNull();
		assertThat(peOption).isNotNull();
		System.out.println("CE OUT PUT: "+ceOption.getStrikePrice()+": "+ceOption.getLtp());
		System.out.println("PE OUT PUT: "+peOption.getStrikePrice()+": "+peOption.getLtp());
	}
	
	//@Test
	public void getOpstOptionChainData() throws IOException {
		OpstOptionChain chain = optionChainService.getOpstOptionChainData("NIFTY", "25NOV2021");
		assertThat(chain).isNotNull();
	}
	
//	@Test
	public void getOptionNearPriceTest() {
		OpstOptionData data = optionChainService.getOptionNearPrice(Constants.CE, "NIFTY", "25NOV2021", 65.0);
		assertThat(data).isNotNull();
	}
	
	//@Test
	public void getStrangleStrikesTest() {
		//String symbol, String expiry, Double deltaVal, Double deltaMaxDiff
		Map<String, OpstOptionData> response = optionChainService.getOpstStrangleStrikes("NIFTY", "25NOV2021", 15.0, 1.0);
		assertThat(response).isNotNull();
		System.out.println(" CE ----  "+response.get("CE"));
		System.out.println(" PE ----  "+response.get("PE"));
	}
	
	@Test
	public void responseEntityTest() {
		String reqUrl = "https://web.sensibull.com/option-chain";
		ResponseEntity<Void> responseEntity = restTemplate.exchange(reqUrl, HttpMethod.GET, null, Void.class);
		System.out.println(responseEntity.getHeaders());
	}
}
