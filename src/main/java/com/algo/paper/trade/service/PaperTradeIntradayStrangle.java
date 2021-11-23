package com.algo.paper.trade.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.algo.model.AlogLtpData;
import com.algo.paper.trade.connect.GrConnect;
import com.algo.utils.CommonUtils;
import com.algo.utils.Constants;
import com.algo.utils.DateUtils;

@Service
public class PaperTradeIntradayStrangle {
	Logger log = LoggerFactory.getLogger(this.getClass());
	LocalTime closeTime = LocalTime.parse(Constants.CLOSEING_TIME);
	LocalTime openingTime = LocalTime.parse(Constants.OPENING_TIME);
	
	@Value("${app.straddle.opstSymbol}")
	private String opstSymbol;
	
	@Autowired
	GrConnect grConnect;
	
	public void placeIntradayStrangleStrategy() {
		LocalDate currentExpiry = DateUtils.getCurrentExpiry();
		Long days = Duration.between(LocalDate.now().atStartOfDay(), currentExpiry.atStartOfDay()).toDays();
		List<String> symbols = new ArrayList<>();
		symbols.add(opstSymbol);
		//BANKNIFTY02DEC202137100PE
		Map<String, AlogLtpData> response =  grConnect.getSpotLtpData(symbols);
		AlogLtpData ltpData = response.get(opstSymbol);
		Integer atmStrike = getATMStrikePrice(ltpData);
		List<String> symbols2 = prepareSymbols(opstSymbol, atmStrike, days, currentExpiry);
		System.out.println("LTP OF "+symbols2);
		Map<String, AlogLtpData> optionsToSell =  grConnect.getLtpData(symbols2);
		System.out.println(optionsToSell);
		
	}
	
	
	private List<String> prepareSymbols(String opstSymbol, Integer atmStrike, Long days, LocalDate expiry) {
		List<String> symbols = new ArrayList<>();
		String op1 = null;
		String op2 = null;
		if(opstSymbol.equals("BANKNIFTY")) {
			op1 = opstSymbol+DateUtils.opstraFormattedExpiry(expiry)+(atmStrike - (days+1) * 100)+Constants.PE;
			op2 = opstSymbol+DateUtils.opstraFormattedExpiry(expiry)+(atmStrike + (days+1) * 100)+Constants.CE;
		} else if(opstSymbol.equals("NIFTY")) {
			op1 = opstSymbol+DateUtils.opstraFormattedExpiry(expiry)+(atmStrike - (days+1) * 50)+Constants.PE;
			op2 = opstSymbol+DateUtils.opstraFormattedExpiry(expiry)+(atmStrike + (days+1) * 50)+Constants.CE;
		}
		symbols.add(op1.replace(CommonUtils.getOpstraExpiry(op1), CommonUtils.getSpecialExpiry(op1, DateUtils.isMonthlyExpiry(expiry))));
		symbols.add(op2.replace(CommonUtils.getOpstraExpiry(op2), CommonUtils.getSpecialExpiry(op2, DateUtils.isMonthlyExpiry(expiry))));
		return symbols;
	}


	private Integer getATMStrikePrice(AlogLtpData response) {
		Integer strikePrice = 0;
		int l = String.valueOf(response.getLastPrice()).length();
		String d = "1";
		for(int i=0; i<l; i++) {
			d = d+"0";
		}
		if(opstSymbol.equals("BANKNIFTY"))
			strikePrice = closestNumber(response.getLastPrice(), 100);
		if(opstSymbol.equals("NIFTY"))
			strikePrice = closestNumber(response.getLastPrice(), 50);
		return strikePrice;
	}
	
	private int closestNumber(double n, int m) {
		int q = Double.valueOf(n).intValue() / m;
		int n1 = m * q;
		int n2 = (n * m) > 0 ? (m * (q + 1)) : (m * (q - 1));
		if (Math.abs(n - n1) < Math.abs(n - n2))
			return n1;
		return n2;   
	}
	
}
