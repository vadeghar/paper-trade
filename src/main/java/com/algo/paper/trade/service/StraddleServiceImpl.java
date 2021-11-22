package com.algo.paper.trade.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.algo.model.LTPQuote;
import com.algo.opstra.model.OpstraSpotResponse;
import com.algo.utils.CommonUtils;
import com.algo.utils.Constants;
import com.algo.utils.DateUtils;
import com.algo.utils.ExcelUtils;

@Service
public class StraddleServiceImpl {
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Value("${app.straddle.opstSymbol}")
	private String opstSymbol;
	@Value("${app.straddle.expiry}")
	private String expiry;
	@Value("${app.straddle.dataDir}")
	private String dataDir;
	@Value("${app.straddle.qty:25}")
	private Integer qty;
	@Value("${app.straddle.sl:30}")
	private Integer stopLoss;
	

	@Autowired
	OpstraConnect opstraConnect;
	
	@PostConstruct  
    public void postConstruct() {  
       expiry = DateUtils.opstraFormattedExpiry(expiry);
       System.out.println("Straddle expiry: postConstruct "+expiry);  
    }  
	
	public void placeStraddleStrategy() {
		
		OpstraSpotResponse response =  opstraConnect.getSpotResponse(opstSymbol, expiry);
		Integer strikePrice = getATMStrikePrice(response);
		List<Integer> availableStrikes = response.getStrikes();
		if(!availableStrikes.contains(strikePrice)) {
			log.info("Found invalid strike price");
			System.out.println("INVALID STRIKE");
		}
		log.info("Current Price: "+response.getSpotPrice()+" Strike: "+strikePrice);
		ExcelUtils.createExcelFile(dataDir);
		System.out.println("Current Price: "+response.getSpotPrice()+" Strike: "+strikePrice);
		List<String> symbols = new ArrayList<>();
		String ceTrSymbol = opstSymbol.toUpperCase()+expiry.toUpperCase()+strikePrice+Constants.CE;
		String peTrSymbol = opstSymbol.toUpperCase()+expiry.toUpperCase()+strikePrice+Constants.PE;
		symbols.add(ceTrSymbol);
		symbols.add(peTrSymbol);
		//symbol.toUpperCase()+expiry.toUpperCase()+curStrikePrice+Constants.CE
		Map<String, LTPQuote> ltps = opstraConnect.getLTP(symbols);
		Double cePremRcvd = ltps.get(ceTrSymbol).lastPrice;
		Double pePremRcvd = ltps.get(peTrSymbol).lastPrice;
		Double netPremRcvd = cePremRcvd  + pePremRcvd;
		boolean ceSell = sell(strikePrice.toString(), opstSymbol, expiry, qty * -1, Constants.CE, ltps.get(ceTrSymbol).lastPrice);
		boolean peSell = false;
		if(ceSell)
			peSell = sell(strikePrice.toString(), opstSymbol, expiry, qty * -1, Constants.PE, ltps.get(peTrSymbol).lastPrice);
		if(peSell) {
			Double totalTarget = qty * ((netPremRcvd * 10) / 100);
			Double ceSL = cePremRcvd + (ltps.get(ceTrSymbol).lastPrice * stopLoss /100);
			Double peSL = pePremRcvd + (ltps.get(peTrSymbol).lastPrice * stopLoss /100);
			
			
			ExcelUtils.setValueWithRedBgByCellReference(ExcelUtils.getCurrentFileNameWithPath(dataDir), ExcelUtils.SHEET_SL_VAL, StringUtils.EMPTY);
			ExcelUtils.setValueByCellReference(ExcelUtils.getCurrentFileNameWithPath(dataDir), ExcelUtils.SHEET_STRATEGY_NAME_VAL, "STRADDLE");
			ExcelUtils.setValueByCellReference(ExcelUtils.getCurrentFileNameWithPath(dataDir), ExcelUtils.SHEET_TARGET_VAL, Constants.DECIMAL_FORMAT.format(totalTarget));
			ExcelUtils.setValueByCellReference(ExcelUtils.getCurrentFileNameWithPath(dataDir), ExcelUtils.SHEET_CE_SL_VAL, Constants.DECIMAL_FORMAT.format(ceSL));
			ExcelUtils.setValueByCellReference(ExcelUtils.getCurrentFileNameWithPath(dataDir), ExcelUtils.SHEET_PE_SL_VAL, Constants.DECIMAL_FORMAT.format(peSL));
			System.out.println("Straddled placed successfully");
			log.info("Straddled placed successfully");
		}
	}
	
	/**
	 * Place SELL order (Market Order - Regular)
	 * @param strikePrice
	 * @param symbol
	 * @param expiry
	 * @param exchange
	 * @param qty
	 * @param product
	 * @param ceOrPe
	 * @return
	 */
	public boolean sell(String strikePrice, String symbol, String expiry, Integer qty, String ceOrPe, double price) {
		try {
			System.out.println("************* SELL ORDER FOR TRADING SYMBOL: "+(symbol+expiry+strikePrice+ceOrPe)+" ***************\n");
			log.info("SELL ORDER FOR TRADING SYMBOL: "+(symbol+expiry+strikePrice+ceOrPe));
			String fileToUpdate = ExcelUtils.getCurrentFileNameWithPath(dataDir);
			List<Object[]> netPositionRows = new ArrayList<>();
			netPositionRows.add(
					ExcelUtils.prepareDataRow(symbol+expiry+strikePrice+ceOrPe, // Position
					qty, // Wty
					price, // Sell Price
					0, // Buy Price
					price, // Current Price
					0, //P&L
					DateUtils.getDateTime(LocalDateTime.now()), //Ex Time
					StringUtils.EMPTY // Close Time
					));
			ExcelUtils.addOrUpdateRows(fileToUpdate, netPositionRows);
			System.out.println("************* SELL ORDER FOR TRADING SYMBOL: "+(symbol+expiry+strikePrice+ceOrPe)+" IS COMPLETED ");
			log.info("SELL ORDER FOR TRADING SYMBOL: "+(symbol+expiry+strikePrice+ceOrPe)+" IS COMPLETED ");
			return true;
		} catch (JSONException e) {
			e.printStackTrace();
			System.out.println("******************* "+e.getLocalizedMessage()+" ******************************");
			System.out.println("************* PROBLEM WHILE PLACING A NEW SELL ORDER - DO IT MANUALLY : "+symbol+expiry+strikePrice+ceOrPe);
			log.error(e.getLocalizedMessage());
			log.error("PROBLEM WHILE PLACING A NEW SELL ORDER - DO IT MANUALLY : "+symbol+expiry+strikePrice+ceOrPe);
			CommonUtils.beep();
		}
		return false;
	}



	private Integer getATMStrikePrice(OpstraSpotResponse response) {
		Integer strikePrice = 0;
		int l = String.valueOf(response.getSpotPrice()).length();
		String d = "1";
		for(int i=0; i<l; i++) {
			d = d+"0";
		}
		if(opstSymbol.equals("BANKNIFTY"))
			strikePrice = closestNumber(response.getSpotPrice().intValue(), 100);
		if(opstSymbol.equals("NIFTY"))
			strikePrice = closestNumber(response.getSpotPrice().intValue(), 50);
		return strikePrice;
	}
	
	
	
	private int closestNumber(int n, int m) {
        int q = n / m;
        int n1 = m * q;
        int n2 = (n * m) > 0 ? (m * (q + 1)) : (m * (q - 1));
        if (Math.abs(n - n1) < Math.abs(n - n2))
            return n1;
        return n2;   
    }

}
