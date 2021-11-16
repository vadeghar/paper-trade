package com.algo.paper.trade.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.algo.paper.trade.model.LTPQuote;
import com.algo.paper.trade.model.MyPosition;
import com.algo.paper.trade.model.OpstOptionData;
import com.algo.paper.trade.utils.CommonUtils;
import com.algo.paper.trade.utils.Constants;
import com.algo.paper.trade.utils.DateUtils;
import com.algo.paper.trade.utils.ExcelUtils;

@Service
public class PaperUtils {

	@Value("${app.paperTrade.dataDir}")
	private String dataDir;
	
	@Value("${app.straddle.expiry}")
	private String expiry;
	
	@Value("${app.straddle.opstSymbol}")
	private String opstSymbol;
	
	@Value("${app.straddle.deltaVal}")
	private Double deltaVal;
	
	@Value("${app.straddle.qty:50}")
	private Integer qty;
	
	@Autowired
	OpstraConnect opstraConnect;
	
	/**
	 * Gives the current positions which are not closed yet
	 * @return
	 */
	public List<MyPosition> getPaperNetPositions() {
		List<MyPosition> myNetPositions = new ArrayList<>();
		try {
			List<MyPosition> paperPositions = getNetPaperPositions();
			if(CollectionUtils.isNotEmpty(paperPositions)) {
				for(MyPosition p: paperPositions) {
					MyPosition myPosition = new MyPosition();
					myPosition.setTradingSymbol(p.getTradingSymbol());
					myPosition.setSymbol(CommonUtils.getSymbol(p.getTradingSymbol()));
					myPosition.setExpiry(CommonUtils.getExpiry(p.getTradingSymbol()));
					myPosition.setStrikePrice(CommonUtils.getStrikePrice(p.getTradingSymbol()));
					myPosition.setOptionType(CommonUtils.getOptionType(p.getTradingSymbol()));
					myPosition.setBuyPrice(p.getBuyPrice());
					myPosition.setSellPrice(p.getSellPrice());
					myPosition.setNetQuantity(p.getNetQuantity());
					myPosition.setCurrentPrice(null);
					myPosition.setPositionPnl(p.getPositionPnl());
					myPosition.setSellQuantity(p.getNetQuantity() < 0 ? Math.abs(p.getNetQuantity()) : 0);
					myPosition.setBuyQuantity(p.getNetQuantity() > 0 ? Math.abs(p.getNetQuantity()) : 0);
					myNetPositions.add(myPosition);
				}
				setKiteCurrentPrices(myNetPositions);
			}
			myNetPositions = myNetPositions.stream().sorted(Comparator.comparing(MyPosition::getTradingSymbol)).collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return myNetPositions;
	}

	/**
	 * This will gives the new position details part of adjustment
	 * @param posToClose
	 * @return
	 */
	public MyPosition getNewSellPositionNearPremium(MyPosition posToClose, Double otherOptPrem) {
		MyPosition posToOpen = new MyPosition();
		try {
			Double newSellPremium = (otherOptPrem * 85) / 100;
			System.out.println("\t\t\tFINDING CALL OPTION AT PRICE: "+newSellPremium);
			List<String> nearestTenSymbols = getNearestTenKiteSymbols(posToClose);
			Map<String, LTPQuote> ltps = opstraConnect.getLTP(nearestTenSymbols);
			String tradeSymbol = compareLastPriceBy(newSellPremium, ltps, 1);
			if(StringUtils.isBlank(tradeSymbol))
				tradeSymbol = compareLastPriceBy(newSellPremium, ltps, 2);
			if(StringUtils.isBlank(tradeSymbol))
				tradeSymbol = compareLastPriceBy(newSellPremium, ltps, 3);
			if(StringUtils.isBlank(tradeSymbol))
				tradeSymbol = compareLastPriceBy(newSellPremium, ltps, 4);
			if(StringUtils.isBlank(tradeSymbol))
				tradeSymbol = compareLastPriceBy(newSellPremium, ltps, 5);
			if(tradeSymbol == null)
				return null;
			posToOpen.setTradingSymbol(tradeSymbol);
			posToOpen.setExpiry(posToClose.getExpiry());
			posToOpen.setOptionType(posToClose.getOptionType());
			posToOpen.setCurrentPrice(ltps.get(tradeSymbol).lastPrice);
			posToOpen.setStrikePrice(CommonUtils.getStrikePrice(posToOpen.getTradingSymbol()));
			posToOpen.setSymbol(CommonUtils.getSymbol(posToOpen.getTradingSymbol()));
			posToOpen.setSellQuantity(posToClose.getSellQuantity()); // Same quantity
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return posToOpen;
	}
	
	public void addStopLossToSheet(MyPosition posToKeep, MyPosition posToOpen) {
		String otherStrikePrice = posToKeep.getStrikePrice();
		if(Integer.valueOf(posToOpen.getStrikePrice()) <= Integer.valueOf(otherStrikePrice)) {
			System.out.println("\t\t\t** POSITION IS STRADDLE NOW, ADDING STOP LOSS **");
			Double totPrem = posToOpen.getCurrentPrice() + posToKeep.getCurrentPrice();
			Double sl = totPrem + (totPrem * 0.1);
			System.out.println("\t\t\tSTOP LOSS IS: "+sl);
			ExcelUtils.updateCellByRowAndCellNums(ExcelUtils.getCurrentFileNameWithPath(dataDir), 1, 8, sl);
		}
	}

	/**
	 * Set the CMP on all net positions
	 * @param myNetPositions
	 */
	private void setKiteCurrentPrices(List<MyPosition> myNetPositions) {
		List<String> symbols = new ArrayList<>();
		Map<String, LTPQuote> ltps = new HashMap<>();
		try {
			for(MyPosition p: myNetPositions) {
				symbols.add(p.getTradingSymbol());
			}
			ltps = opstraConnect.getLTP(symbols);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for(MyPosition p: myNetPositions) {
			p.setCurrentPrice(ltps.get(p.getTradingSymbol()) !=null ? ltps.get(p.getTradingSymbol()).lastPrice : null);
			getPositionPnl(p);
		}

	}

	/**
	 * Print net positions on console
	 */
	public void printKiteNetPositions() {

		List<MyPosition> netPositions = getAllPaperPositions();
		System.out.println("\t-------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
		System.out.println("\tTRADING SYMBOL\t\t|\tTRADE TYPE\t|\tQty\t|\tSell Price\t|\tBuy Price\t|\tCurrent Price\t|\tP/L\t| Net P/L\t|\n");
		System.out.println("\t-------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
		String sell = Constants.SELL;
		String buy = Constants.BUY;
		double netPnl = 0.0;
		for(MyPosition p: netPositions) {
			double lastPrice = p.getCurrentPrice();
			double pnl = 0.0;
			if(p.getNetQuantity() == 0) {
				pnl = p.getPositionPnl();
			} else {
				pnl = (p.getSellPrice() - lastPrice) * p.getNetQuantity();
			}
			if(p.getNetQuantity() < 0 && p.getSellPrice() > lastPrice)
				pnl = Math.abs(pnl);
			if(p.getNetQuantity() < 0 && p.getSellPrice() < lastPrice)
				pnl = -1 * pnl;
			netPnl = netPnl + pnl;
			System.out.println("\t"+p.getTradingSymbol()+
					"\t|\t"+(p.getNetQuantity() < 0 ? sell : (p.getNetQuantity() > 0) ? buy : "CLOSED")+
					"\t\t|\t"+p.getNetQuantity()+
					"\t|\t\t"+String.format("%.2f",p.getSellPrice())+
					"\t|\t"+String.format("%.2f",p.getBuyPrice())+
					"\t\t|\t"+String.format("%.2f",lastPrice)+
					"\t\t|\t"+String.format("%.2f",pnl)+
					"\t| "+String.format("%.2f",netPnl)+"\t"+
					"|\n");
		}
		System.out.println("\t-------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");

	}

	/**
	 * Gives the current position's pnl and set this pnl on same object
	 * @param p
	 * @return
	 */
	public double getPositionPnl(MyPosition p) {
		double lastPrice = p.getCurrentPrice();
		double pnl = 0.0;
		if(p.getNetQuantity() == 0) {
			pnl = p.getPositionPnl();
		} else {
			pnl = (p.getSellPrice() - lastPrice) * p.getNetQuantity();
		}
		if(p.getNetQuantity() < 0 && p.getSellPrice() > lastPrice)
			pnl = Math.abs(pnl);
		if(p.getNetQuantity() < 0 && p.getSellPrice() < lastPrice)
			pnl = -1 * pnl;
		p.setPositionPnl(pnl);
		return pnl;
	}

	/**
	 * Updates the excel sheet with current trades with pnl
	 */
	public void updteTradeFile() {
		try {
			List<MyPosition> netPositions = getPaperNetPositions();
			updateCMPForOldStrikesInFile();
			List<Object[]> netPositionRows = new ArrayList<>();
			String fileToUpdate = ExcelUtils.getCurrentFileNameWithPath(dataDir);
			double netPnl = 0.0;
			for(MyPosition position : netPositions) {
				double lastPrice = position.getCurrentPrice();
				double pnl = 0.0;
				if(position.getNetQuantity() == 0) {
					pnl = position.getPositionPnl();
				} else {
					pnl = (position.getSellPrice() - lastPrice) * position.getNetQuantity();
				}
				if(position.getNetQuantity() < 0 && position.getSellPrice() > lastPrice)
					pnl = Math.abs(pnl);
				if(position.getNetQuantity() < 0 && position.getSellPrice() < lastPrice)
					pnl = -1 * pnl;
				netPnl = netPnl + pnl;
				netPositionRows.add(
						ExcelUtils.prepareDataRow(position.getTradingSymbol(), 
								position.getNetQuantity(), 
								position.getSellPrice(), 
								position.getBuyPrice(), 
								Double.valueOf(lastPrice), 
								Double.valueOf(pnl), 
								StringUtils.EMPTY));

			} 
			ExcelUtils.addOrUpdateRows(fileToUpdate, netPositionRows);
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Adjustment method
	 * @param posToClose
	 * @param posToOpen
	 */
	public void startAdjustment(MyPosition posToClose, MyPosition posToOpen) {
		System.out.println("************* ADJUSTMENT STARTED *********************");
		boolean buyCompleted = buy(posToClose.getStrikePrice(), 
				posToClose.getSymbol(), 
				posToClose.getExpiry(),
				posToClose.getSellQuantity(), 
				posToClose.getOptionType(),
				posToClose.getCurrentPrice());

		if(!buyCompleted)
			return;
		boolean sellCompleted = sell(posToOpen.getStrikePrice(), 
				posToOpen.getSymbol(), 
				posToOpen.getExpiry(),
				posToOpen.getSellQuantity(), 
				posToOpen.getOptionType(),
				posToOpen.getCurrentPrice());
		if(sellCompleted) {
			System.out.println("************* ADJUSTMENT COMPLETED *********************");
		}
	}



	/**
	 * Place BUY order (Market Order - Regular)
	 * @param strikePrice
	 * @param symbol
	 * @param expiry
	 * @param exchange
	 * @param qty
	 * @param product
	 * @param ceOrPe
	 * @return
	 */
	public boolean buy(String strikePrice, String symbol, String expiry, Integer qty, String ceOrPe, double price) {
		try {
			System.out.println("************* NEW BUY POSITION TRADING SYMBOL: "+(symbol+expiry+strikePrice+ceOrPe)+" ***************\n");
			String fileToUpdate = ExcelUtils.getCurrentFileNameWithPath(dataDir);
			List<Object[]> netPositionRows = new ArrayList<>();
			netPositionRows.add(
					ExcelUtils.prepareDataRow(symbol+expiry+strikePrice+ceOrPe,
					qty, 
					StringUtils.EMPTY, 
					price, 
					price, 
					0, 
					StringUtils.EMPTY));
			ExcelUtils.addOrUpdateRows(fileToUpdate, netPositionRows);
			System.out.println("************* POSITION CLOSED ***************\n ORDER ID: ");
			return true;
		} catch (JSONException e) {
			e.printStackTrace();
			System.out.println("******************* "+e.getLocalizedMessage()+" ******************************");
			System.out.println("*************  PROBLEM WHILE PLACING A NEW ORDER - DO IT MANUALLY : "+symbol+expiry+strikePrice+ceOrPe);
			CommonUtils.beep();
		}
		return false;
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
			System.out.println("************* NEW SELL POSITION TRADING SYMBOL: "+(symbol+expiry+strikePrice+ceOrPe)+" ***************\n");
			String fileToUpdate = ExcelUtils.getCurrentFileNameWithPath(dataDir);
			List<Object[]> netPositionRows = new ArrayList<>();
			netPositionRows.add(
					ExcelUtils.prepareDataRow(symbol+expiry+strikePrice+ceOrPe, 
					qty, 
					price, 
					0, 
					price, 
					0, 
					StringUtils.EMPTY));
			ExcelUtils.addOrUpdateRows(fileToUpdate, netPositionRows);
			System.out.println("************* OPENING POSITION ***************\n");
			System.out.println("************* ORDER PLACED SUCCESSFULLY AT : ");
			return true;
		} catch (JSONException e) {
			e.printStackTrace();
			System.out.println("******************* "+e.getLocalizedMessage()+" ******************************");
			System.out.println("************* PROBLEM WHILE PLACING A NEW ORDER - DO IT MANUALLY : "+symbol+expiry+strikePrice+ceOrPe);
			CommonUtils.beep();
		}
		return false;
	}
	
	
	public boolean checkTargetAndClosePositions(List<MyPosition> netPositions) {
		boolean isClosedAll =false;
		String target = ExcelUtils.getCellValByRowAndCellNums(ExcelUtils.getCurrentFileNameWithPath(dataDir), 1, 7);
		System.out.println("\t\t\tTARGET: "+target);
		if(StringUtils.isNotBlank(target)) {
			Double taregtDbl = Double.valueOf(target);
			Double netPnl = CommonUtils.getNetPnl(netPositions);
			System.out.println("\t\t\tCurrent P/L: "+Constants.DECIMAL_FORMAT.format(netPnl)+" OF "+target);
			if(netPnl >= taregtDbl) {
				System.err.println("***** TARGET ACHIVED: TARGET:"+target+" NET P/L:"+netPnl);
				isClosedAll = closeAll(netPositions);
			}
		}
		return isClosedAll;
	}
	
	public boolean checkSLAndClosePositions(List<MyPosition> netPositions) {
		boolean isClosedAll = false;
		String stopLoss = ExcelUtils.getCellValByRowAndCellNums(ExcelUtils.getCurrentFileNameWithPath(dataDir), 1, 8);
		if(StringUtils.isBlank(stopLoss) || Double.valueOf(stopLoss) == 0.0) {
			System.out.println("\t\t\tSTOP LOSS: (NO STOP LOSS ADDED YET)");
			return isClosedAll;
		}
		System.out.println("\t\t\tSTOP LOSS: "+stopLoss);
		if(StringUtils.isNotBlank(stopLoss)) {
			Double stopLossDbl = Double.valueOf(stopLoss);
			Double netPrice = CommonUtils.getNetPrice(netPositions);
			if(netPrice  >= stopLossDbl) {
				System.err.println("STOP LOSS HIT: "+stopLossDbl+" NET PRICE OF EXISTING POSITIONS: "+netPrice);
				isClosedAll = closeAll(netPositions);
			}
		}
		return isClosedAll;
	}
	
	
	public void placeStrangleStrategy() {
		String opstExpiry = DateUtils.opstraFormattedExpiry(expiry);
		Double nearDelta = 3.0;
		Map<String, OpstOptionData> strikes = opstraConnect.getOpstStrangleStrikes(opstSymbol, opstExpiry, deltaVal, nearDelta);
		if(strikes == null || CollectionUtils.isEmpty(strikes.keySet()))
			throw new RuntimeException("Strike prices not Found near "+deltaVal+" +/- "+nearDelta+" delta");
		if(strikes.get(Constants.CE) == null)
			throw new RuntimeException("Strike price CALL not Found near "+deltaVal+" +/- "+nearDelta+" delta");
		if(strikes.get(Constants.PE) == null)
			throw new RuntimeException("Strike price PUT not Found near "+deltaVal+" +/- "+nearDelta+" delta");
		ExcelUtils.createExcelFile(dataDir);
		OpstOptionData peOption = strikes.get(strikes.keySet().stream().filter(s -> s.equals(Constants.PE)).findFirst().get());
		System.out.println("peOption: "+peOption);
		OpstOptionData ceOption = strikes.get(strikes.keySet().stream().filter(s -> s.equals(Constants.CE)).findFirst().get());
		System.out.println("ceOption: "+ceOption);
		System.out.println("*******************************************************************************************");
		boolean peSell = false;
		boolean ceSell = sell(ceOption.getStrikePrice(), opstSymbol, opstExpiry, qty * -1, Constants.CE, Double.valueOf(ceOption.getCallLTP()));
		if(ceSell) {
			peSell = sell(peOption.getStrikePrice(), opstSymbol, opstExpiry, qty * -1, Constants.PE, Double.valueOf(peOption.getPutLTP()));
		}
		if(peSell) {
			Double totalPremReceived = (Double.valueOf(ceOption.getCallLTP()) + Double.valueOf(peOption.getPutLTP())) * qty;
			Double target = (totalPremReceived * 80) / 100;
			ExcelUtils.updateCellByRowAndCellNums(ExcelUtils.getCurrentFileNameWithPath(dataDir), 1, 7, target);
		}
		System.out.println("*******************************************************************************************\n\n");
	}
	
	private boolean closeAll(List<MyPosition> netPositions) {
		boolean closed = false;
		try {
			for(MyPosition p: netPositions) {
				closed |= buy(p.getStrikePrice(), p.getSymbol(), p.getExpiry(), p.getSellQuantity(), p.getOptionType(), p.getCurrentPrice());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return closed;
	}

	private void updateCMPForOldStrikesInFile() {
		List<MyPosition> netPositions = getAllPaperPositions();
		List<String> tradingSymbols = getSymbols(netPositions);
		Map<String, LTPQuote> ltps = opstraConnect.getLTP(tradingSymbols);
		List<Object[]> netPositionRows = new ArrayList<>();
		String fileToUpdate = ExcelUtils.getCurrentFileNameWithPath(dataDir);
		double netPnl = 0.0;
		for(MyPosition position : netPositions) {
			double lastPrice = ltps.get(position.getTradingSymbol()).lastPrice;
			double pnl = 0.0;
			if(position.getNetQuantity() == 0) {
				pnl = position.getPositionPnl();
			} else {
				pnl = (position.getSellPrice() - lastPrice) * position.getNetQuantity();
			}
			if(position.getNetQuantity() < 0 && position.getSellPrice() > lastPrice)
				pnl = Math.abs(pnl);
			if(position.getNetQuantity() < 0 && position.getSellPrice() < lastPrice)
				pnl = -1 * pnl;
			netPnl = netPnl + pnl;
			netPositionRows.add(
					ExcelUtils.prepareDataRow(position.getTradingSymbol(), 
					position.getNetQuantity(), 
					position.getSellPrice(), 
					position.getBuyPrice(), 
					Double.valueOf(lastPrice), 
					Double.valueOf(pnl), 
					StringUtils.EMPTY));
			
		} // For loop end
		//System.out.println(" TRADES: "+netPositionRows.size());
		ExcelUtils.addOrUpdateRows(fileToUpdate, netPositionRows);
	}






	private List<String> getSymbols(List<MyPosition> netPositions) {
		List<String> allSymbols = new ArrayList<>();
		for(MyPosition p : netPositions) {
			allSymbols.add(p.getTradingSymbol());
		}
		return allSymbols;
	}

	private String compareLastPriceBy(Double priceNear, Map<String, LTPQuote> ltps, double d) {
		for(Entry<String, LTPQuote> e: ltps.entrySet()) {
			if(e.getValue().lastPrice >= (priceNear-d) && e.getValue().lastPrice <= (priceNear+d)) {
				return e.getKey();
			}
		}
		return null;
	}


	//NIFTY&25NOV2021&17650&CE - NIFTY18NOV202118300CE
	private List<String> getNearestTenKiteSymbols(MyPosition posToClose) {
		List<String> tradingSymbols = new ArrayList<>();
		Integer strikeDiff = posToClose.getSymbol().equals("NIFTY") ? 50 : 100;
		Integer curStrikePrice= Integer.valueOf(posToClose.getStrikePrice());
		String symbol = posToClose.getSymbol(); //NIFTY21NOV16800PE
		String expiry = posToClose.getExpiry();
		if(posToClose.getOptionType().equals(Constants.CE)) {
			for(int i=0; i<10;i++) {
				curStrikePrice = curStrikePrice - strikeDiff;
				tradingSymbols.add(symbol.toUpperCase()+expiry.toUpperCase()+curStrikePrice+Constants.CE);
			}
		} else if(posToClose.getOptionType().equals(Constants.PE)) {
			for(int i=0; i<10;i++) {
				curStrikePrice = curStrikePrice + strikeDiff;
				tradingSymbols.add(symbol.toUpperCase()+expiry.toUpperCase()+curStrikePrice+Constants.PE);
			}
		}
		System.out.println("\t\t\tGETTING LTP FOR SYMBOLS: "+tradingSymbols);
		return tradingSymbols;
	}

	
	
	private List<MyPosition> getNetPaperPositions() {
		List<MyPosition> netPositions = getAllPaperPositions();
		netPositions = netPositions.stream().filter(p -> p.getNetQuantity() != 0.0).collect(Collectors.toList());
		return netPositions;
	}
	
	private List<MyPosition> getAllPaperPositions() {
		String paperTradeFile = ExcelUtils.getCurrentFileNameWithPath(dataDir);
		String[][] data = ExcelUtils.getFileData(paperTradeFile);
		List<MyPosition> allPositions = new ArrayList<>();
		int i = 0;
		for(String[] row : data) {
			if(i == 0) {
				i++;
				continue;
			}
			//Position	Qty	Sell Price	Buy Price	Current Price	P&L	Net P&L
			if(StringUtils.isBlank(row[0]))
				continue;
			MyPosition p = new MyPosition();
			p.setTradingSymbol(row[0]);
			p.setNetQuantity(Double.valueOf(row[1]).intValue());
			p.setSellPrice(Double.valueOf(row[2]));
			p.setBuyPrice(Double.valueOf(row[3]));
			p.setCurrentPrice(Double.valueOf(row[4]));
			p.setPositionPnl(Double.valueOf(row[5]));
			allPositions.add(p);
		}
		return allPositions;
	}

	
}
