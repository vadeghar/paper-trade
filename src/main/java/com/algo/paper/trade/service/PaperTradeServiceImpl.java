package com.algo.paper.trade.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.algo.paper.trade.model.LTPQuote;
import com.algo.paper.trade.model.OpstOptionChain;
import com.algo.paper.trade.model.OpstOptionData;
import com.algo.paper.trade.model.Position;
import com.algo.paper.trade.utils.CommonUtiils;
import com.algo.paper.trade.utils.Constants;
import com.algo.paper.trade.utils.DateUtils;
import com.algo.paper.trade.utils.ExcelUtils;

@Service
public class PaperTradeServiceImpl {
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private OptionChainServiceImpl optionChainService;
	
	@Autowired
	private AngelSmartServiceImpl angelSmartService;
	
	@Value("${app.paperTrade.dataDir}")
	private String dataDir;
	
	@Value("${app.paperTrade.opstSymbol}")
	private String opstSymbol;
	
	@Value("${app.paperTrade.deltaVal:15.0}")
	private Double deltaVal;
	
	@Value("${app.paperTrade.expiry}")
	private String expiry;
	
	@Value("${app.paperTrade.qty:50}")
	private Integer qty;
	
	@Value("${app.paperTrade.adjustmentPerc:50}")
	private Integer adjustmentPerc;
	
	@Value("${app.paperTrade:false}")
	private boolean paperTrade;
	
	@Value("${app.paperTrade.isWeekly:true}")
	private boolean isWeekly;
	
	@Value("${app.angel.nfo.dataFile}")
	private String nfoDataFile;
	
	@Value("${app.api.use}")
	private String apiUse;
	
	LocalTime closeTime = LocalTime.parse(Constants.CLOSEING_TIME);
	LocalTime openingTime = LocalTime.parse(Constants.OPENING_TIME);
	
	/**
	 * To start new strangle strategy, Not implemented yet for PAPER
	 * @param opstSymbol
	 * @param expiry
	 * @param deltaVal
	 * @param qty
	 */
	public void placeStrangleStrategy() {
		String opstExpiry = DateUtils.opstraFormattedExpiry(expiry);
		Double nearDelta = 3.0;
		Map<String, OpstOptionData> strikes = optionChainService.getOpstStrangleStrikes(opstSymbol, opstExpiry, deltaVal, nearDelta);
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
		if(paperTrade) {
			boolean peSell = false;
			//(OpstOptionData opstOption, Integer qty,  String ceOrPe)
			boolean ceSell = sell(ceOption, opstExpiry, qty * -1, Constants.CE);
			if(ceSell) {
				peSell = sell(peOption, opstExpiry, qty * -1, Constants.PE);
			}
			if(peSell) {
				Double totalPremReceived = (Double.valueOf(ceOption.getCallLTP()) + Double.valueOf(peOption.getPutLTP())) * qty;
				Double target = (totalPremReceived * 80) / 100;
				ExcelUtils.updateCellByRowAndCellNums(ExcelUtils.getCurrentFileNameWithPath(dataDir), 1, 7, target);
			}
		} 
		System.out.println("*******************************************************************************************\n\n");
	}
	
	@Scheduled(cron = "0/10 * * * * ?")
	public void monitorKiteStrangleAndDoAdjustments() throws JSONException, IOException {
		System.out.println("\n\n\n\t\t\tPAPER - POSITIONS AS ON: "+DateUtils.getDateTime(LocalDateTime.now()));
		if(isTargetAchieved()) {
			return;
		}
		List<Position> netPositions = getNetPaperPositions();
		if(CollectionUtils.isEmpty(netPositions)) {
			System.out.println("\t\t NO PAPER POSITIONS FOUND \n");
			return;
		}
		netPositions = netPositions.stream().filter(p -> p.getNetQuantity() < 0).collect(Collectors.toList());
		if(netPositions.size() > 2) {
			System.out.println("\t\tFOUND MORE THAN TWO PAPER POSITIONS ******************");
			return;
		}
		Position p1 = netPositions.get(0);
		Position p2 = netPositions.get(1);
		String p1Symbol = p1.getTradingSymbol();
		String p2Symbol = p2.getTradingSymbol();
		Map<String, LTPQuote> ltps = getLastTradedPrices(netPositions);
		LTPQuote p1Ltp = ltps.get(p1Symbol);
		LTPQuote p2Ltp = ltps.get(p2Symbol);
		
		Double diffInPerc = priceDiffInPerc(p1Ltp.lastPrice, p2Ltp.lastPrice);
		System.out.println("\t\t\tCE AND PE PRICE DIFFERENCE: "+String.format("%.2f", diffInPerc)+"%\n\t\t\tWAITING FOR DIFFERENCE IF: "+adjustmentPerc+"%\n");
		Double newSellPremium = 0.0;
		if(Double.valueOf(String.format("%.2f", diffInPerc)) > adjustmentPerc) {
			System.out.println("**************************************************************************************");
			System.out.println("\t\t\tTIME TO TAKE ROBO ACTION");
			Position posToClose = null;
			double p1Pnl = getPositionPnl(p1, ltps);
			double p2Pnl = getPositionPnl(p2, ltps);
			if(p1Pnl >  p2Pnl) {
				System.out.println("\t\t\tCLOSING POSITION: "+p1.getTradingSymbol());
				System.out.println("\t\t\tP/L of : "+p1.getTradingSymbol()+" ("+p1Pnl+") IS HIGHER THAN OF POSITION: "+p2.getTradingSymbol()+" ("+p2Pnl+")");
				posToClose = p1;
				newSellPremium = (p2Ltp.lastPrice * 85) / 100;
			}
			if(p2Pnl >  p1Pnl) {
				System.out.println("\t\t\tCLOSING POSITION: "+p2.getTradingSymbol());
				System.out.println("\t\t\tP/L of : "+p2.getTradingSymbol()+" ("+p2Pnl+") IS HIGHER THAN OF POSITION: "+p1.getTradingSymbol()+" ("+p1Pnl+")");
				posToClose = p2;
				newSellPremium = (p1Ltp.lastPrice * 85) / 100;
			}
			String ceOrPe = null;
			OpstOptionData newSellOption = null;
			String opstSymbol = this.opstSymbol;
			String opstExpiry = DateUtils.opstraFormattedExpiry(expiry); //DateUtils.kiteToOpstra.get(getKiteExpiry(posToClose.getTradingSymbol()));
			if(posToClose.getTradingSymbol().endsWith(Constants.CE)) {
				System.out.println("\t\t\tFINDING CALL OPTION AT PRICE: "+newSellPremium);
				newSellOption=  optionChainService.getOptionNearPrice(Constants.CE, opstSymbol, opstExpiry, newSellPremium);
				ceOrPe = Constants.CE;
			} else {
				System.out.println("\t\t\tFINDING PUT OPTION AT PRICE: "+newSellPremium+"");
				newSellOption= optionChainService.getOptionNearPrice(Constants.PE, opstSymbol, opstExpiry, newSellPremium);
				ceOrPe = Constants.PE;
			}
			System.out.println("\t\t\tFOUND OPTION STRIKE: "+newSellOption.getStrikePrice()+ceOrPe+" AT PRICE CE: "+newSellOption.getCallLTP()+"/ PE: "+newSellOption.getPutLTP());
			if(newSellOption.getStrikePrice().equals(getStrikePrice(posToClose.getTradingSymbol()))) {
				System.out.println("************* YOU SHOULD NOT SEE THIS: Got the same strike price to open new position, Closing position Strike Price: "+posToClose.getTradingSymbol()+" and new strike price: "+newSellOption.getStrikePrice()+"\n");
				return;
			}
			startAdjustment(posToClose, newSellOption, ceOrPe, ltps);
			System.out.println("**************************************************************************************");
		}
		printPaperNetPositions(getAllPaperPositions());
		updteTradeFile();
	}
	
	private boolean isTargetAchieved() {
		String target = ExcelUtils.getCellValByRowAndCellNums(ExcelUtils.getCurrentFileNameWithPath(dataDir), 1, 7);
		Double targetDbl = 0.0;
		Double netPnlDbl = 0.0;
		if(StringUtils.isNotBlank(target))
			targetDbl = Double.valueOf(target);
		String netPnl = ExcelUtils.getCellValByRowAndCellNums(ExcelUtils.getCurrentFileNameWithPath(dataDir), 1, 6);
		if(StringUtils.isNotBlank(netPnl)) {
//			netPnl = String.format("%.2f",netPnl);
			netPnlDbl = Double.valueOf(netPnl);
		}
		System.out.println("\t\t\tCurrent P/L: "+netPnl+"\n\t\t\tTARGET: "+target);
		if(netPnlDbl >= targetDbl) {
			System.out.println("\n\n\n\t\t\tTARGET IS REACHED CLOSE ALL TRADES: "+targetDbl+", Current P/L: "+netPnlDbl);
			return true;
		}
		return false;
	}
	
	private void startAdjustment(Position position, OpstOptionData newSellOption, String ceOrPe, Map<String, LTPQuote> ltps) {
		System.out.println("\n\n************* ADJUSTMENT STARTED *********************");
		String opstExpiry = DateUtils.opstraFormattedExpiry(expiry);
		boolean buyCompleted = clsoe(position, opstExpiry, ltps.get(position.getTradingSymbol()), ceOrPe);
		if(!buyCompleted)
			return;
		boolean sellCompleted = open(newSellOption, opstExpiry, ceOrPe);
		if(sellCompleted) {
			System.out.println("************* ADJUSTMENT COMPLETED *********************\n\n");
		}
	}

	private void updteTradeFile() {
		try {
			List<Position> netPositions = getAllPaperPositions();
			Map<String, LTPQuote> ltps = getLastTradedPrices(netPositions);
			List<Object[]> netPositionRows = new ArrayList<>();
			String fileToUpdate = ExcelUtils.getCurrentFileNameWithPath(dataDir);
			double netPnl = 0.0;
			for(Position position : netPositions) {
				double lastPrice = ltps.get(position.getTradingSymbol()).lastPrice;
				double pnl = 0.0;
				if(position.getNetQuantity() == 0) {
					pnl = position.getPnl();
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
		} catch(Exception e) {
			e.printStackTrace();
		}

	}
	
	private void printPaperNetPositions(List<Position> netPositions) {
		Map<String, LTPQuote> ltps = getLastTradedPrices(netPositions);
		
		System.out.println("\t-------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
		System.out.println("\tTRADING SYMBOL\t\t|\tTRADE TYPE\t|\tQty\t|\tSell Price\t|\tBuy Price\t|\tCurrent Price\t|\tP/L\t| Net P/L\t|\n");
		System.out.println("\t-------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
		String sell = Constants.SELL;
		String buy = Constants.BUY;
		double netPnl = 0.0;
		for(Position p: netPositions) {
			double lastPrice = ltps.get(p.getTradingSymbol()).lastPrice;
			double pnl = 0.0;
			if(p.getNetQuantity() == 0) {
				pnl = p.getPnl();
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

	private String getStrikePrice(String tradingSymbol) {
		// 1730025NOV2021PE
		// last 2 chars ce or pe
		// then 9 chars - expity
		tradingSymbol = tradingSymbol.substring(0, 11);
		return tradingSymbol;
	}
	
	private Map<String, LTPQuote> getLastTradedPrices(List<Position> netPositions) {
		if(StringUtils.isNotBlank(apiUse) && apiUse.equals("ANGEL")) {
			return getAngelLTP(netPositions);
		} else {
			return getOpstraLTP(netPositions);
		}
			
	}

	/**
	 * This method will fetch the data from Opstra API
	 * Use getLTP2, to fetch data from Angel
	 * @param netPositions
	 * @return
	 */
	private Map<String, LTPQuote> getOpstraLTP(List<Position> netPositions) { //NIFTY18NOV2117750PE - opstraFormattedExpiry - NIFTY18NOV2021
		long startTime = System.currentTimeMillis();
		OpstOptionChain optionChain =  optionChainService.getOpstOptionChainData(opstSymbol, DateUtils.opstraFormattedExpiry(expiry));
		Map<String, LTPQuote> ltpData = new HashMap<>();
		for(Position p : netPositions) {
			String ceOrPe = p.getTradingSymbol().endsWith("CE") ? Constants.CE : Constants.PE;
			OpstOptionData data = optionChain.getData().stream().filter( d -> (opstSymbol+DateUtils.getAngelFormatExpiry(expiry)+d.getStrikePrice()+ceOrPe).equals(p.getTradingSymbol())).findFirst().get();
			LTPQuote ltp = new LTPQuote();
			ltp.lastPrice = ceOrPe.equals(Constants.CE) ? Double.valueOf(data.getCallLTP()) : Double.valueOf(data.getPutLTP());
			ltpData.put(opstSymbol+DateUtils.getAngelFormatExpiry(expiry)+data.getStrikePrice()+ceOrPe, ltp);
		}
		long endTime = System.currentTimeMillis();
		System.out.println("getLTP TIME TAKEN: "+(endTime-startTime)+" M.SEC");
		return ltpData;
	}
	
	private Map<String, LTPQuote> getAngelLTP(List<Position> netPositions) {
		long startTime = System.currentTimeMillis();
		List<String> symbols = netPositions.stream()
				              .map(Position::getTradingSymbol)
				              .collect(Collectors.toList());
		Map<String, LTPQuote> ltpData = angelSmartService.getLtps(symbols, "NFO", nfoDataFile);
		long endTime = System.currentTimeMillis();
		log.info("getLTP2 TIME TAKEN: "+(endTime-startTime)+" M.SEC");
		return ltpData;
	}

	private List<Position> getNetPaperPositions() {
		List<Position> netPositions = getAllPaperPositions();
		netPositions = netPositions.stream().filter(p -> p.getNetQuantity() != 0.0).collect(Collectors.toList());
		return netPositions;
	}
	
	private List<Position> getAllPaperPositions() {
		String paperTradeFile = ExcelUtils.getCurrentFileNameWithPath(dataDir);
		String[][] data = ExcelUtils.getFileData(paperTradeFile);
		List<Position> allPositions = new ArrayList<>();
		int i = 0;
		for(String[] row : data) {
			if(i == 0) {
				i++;
				continue;
			}
			//Position	Qty	Sell Price	Buy Price	Current Price	P&L	Net P&L
			if(StringUtils.isBlank(row[0]))
				continue;
			Position p = new Position();
			p.setTradingSymbol(row[0]);
			p.setNetQuantity(Double.valueOf(row[1]).intValue());
			p.setSellPrice(Double.valueOf(row[2]));
			p.setBuyPrice(Double.valueOf(row[3]));
			p.setLastPrice(Double.valueOf(row[4]));
			p.setPnl(Double.valueOf(row[5]));
			allPositions.add(p);
		}
		return allPositions;
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
	public boolean buy(OpstOptionData opstOption, String opstExpiry, Integer qty, String ceOrPe) {
		try {
			System.out.println("************* NEW BUY POSITION TRADING SYMBOL: "+(opstOption.getStrikePrice()+opstExpiry+ceOrPe)+" ***************\n");
			String fileToUpdate = ExcelUtils.getCurrentFileNameWithPath(dataDir);
			Double price = 0.0;
			if(Constants.CE.equals(ceOrPe))
				price = Double.valueOf(opstOption.getCallLTP());
			else
				price = Double.valueOf(opstOption.getPutLTP());
			List<Object[]> netPositionRows = new ArrayList<>();
			netPositionRows.add(
					ExcelUtils.prepareDataRow(opstOption.getStrikePrice()+opstExpiry+ceOrPe,
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
			System.out.println("*************  PROBLEM WHILE PLACING A NEW ORDER - DO IT MANUALLY : "+opstOption.getStrikePrice()+opstExpiry+ceOrPe);
			CommonUtiils.beep();
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
	public boolean sell(OpstOptionData opstOption, String opstExpiry, Integer qty,  String ceOrPe) {
		try {
			System.out.println("************* NEW SELL POSITION TRADING SYMBOL: "+(opstOption.getStrikePrice()+opstExpiry+ceOrPe)+" ***************\n");
			String fileToUpdate = ExcelUtils.getCurrentFileNameWithPath(dataDir);
			Double price = 0.0;
			if(Constants.CE.equals(ceOrPe))
				price = Double.valueOf(opstOption.getCallLTP());
			else
				price = Double.valueOf(opstOption.getPutLTP());
			List<Object[]> netPositionRows = new ArrayList<>();
			netPositionRows.add(
					ExcelUtils.prepareDataRow(opstOption.getStrikePrice()+opstExpiry+ceOrPe, 
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
			System.out.println("************* PROBLEM WHILE PLACING A NEW ORDER - DO IT MANUALLY : "+opstOption.getStrikePrice()+opstExpiry+ceOrPe);
			CommonUtiils.beep();
		}
		return false;
	}
	
	private boolean clsoe(Position position, String opstExpiry, LTPQuote ltp, String ceOrPe) {
		try {
			String fileToUpdate = ExcelUtils.getCurrentFileNameWithPath(dataDir);
			Double price = 0.0;
			if(Constants.CE.equals(ceOrPe))
				price = Double.valueOf(ltp.lastPrice);
			else
				price = Double.valueOf(ltp.lastPrice);
			List<Object[]> netPositionRows = new ArrayList<>();
			double pnl = 0.0;
			if(position.getNetQuantity() == 0) {
				pnl = position.getPnl();
			} else {
				pnl = (position.getSellPrice() - ltp.lastPrice) * position.getNetQuantity();
			}
			if(position.getNetQuantity() < 0 && position.getSellPrice() > ltp.lastPrice)
				pnl = Math.abs(pnl);
			if(position.getNetQuantity() < 0 && position.getSellPrice() < ltp.lastPrice)
				pnl = -1 * pnl;
			netPositionRows.add(
					ExcelUtils.prepareDataRow(position.getTradingSymbol(), 
					0, 
					position.getSellPrice(), 
					price, 
					ltp.lastPrice,
					pnl, 
					StringUtils.EMPTY));
			ExcelUtils.addOrUpdateRows(fileToUpdate, netPositionRows);
			System.out.println("************* OPENING POSITION ***************\n");
			System.out.println("************* ORDER PLACED SUCCESSFULLY AT : ");
			return true;
		} catch (JSONException e) {
			e.printStackTrace();
			System.out.println("******************* "+e.getLocalizedMessage()+" ******************************");
			System.out.println("************* PROBLEM WHILE PLACING A NEW ORDER - DO IT MANUALLY : "+position.getTradingSymbol());
			CommonUtiils.beep();
		}
		return false;
	}
	
	private boolean open(OpstOptionData opstOption, String opstExpiry, String ceOrPe) {
		try {
			String fileToUpdate = ExcelUtils.getCurrentFileNameWithPath(dataDir);
			Double price = 0.0;
			if(Constants.CE.equals(ceOrPe))
				price = Double.valueOf(opstOption.getCallLTP());
			else
				price = Double.valueOf(opstOption.getPutLTP());
			List<Object[]> netPositionRows = new ArrayList<>();
			netPositionRows.add(
					ExcelUtils.prepareDataRow(opstOption.getStrikePrice()+opstExpiry+ceOrPe, 
					qty * -1, 
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
			System.out.println("************* PROBLEM WHILE PLACING A NEW ORDER - DO IT MANUALLY : "+opstOption.getStrikePrice()+opstExpiry+ceOrPe);
			CommonUtiils.beep();
		}
		return false;
	}
	
	public static Double priceDiffInPerc(Double p1, Double p2) {
		Double big = p1 > p2 ? p1  : p2;
		Double small = p1 < p2 ? p1  : p2;
		Double perc = (big - small) / big * 100;
		return perc;
	}
	
	private double getPositionPnl(Position p, Map<String, LTPQuote> ltps) {
		double lastPrice = ltps.get(p.getTradingSymbol()).lastPrice;
		double pnl = 0.0;
		if(p.getNetQuantity() == 0) {
			pnl = p.getPnl();
		} else {
			pnl = (p.getSellPrice() - lastPrice) * p.getNetQuantity();
		}
		if(p.getNetQuantity() < 0 && p.getSellPrice() > lastPrice)
			pnl = Math.abs(pnl);
		if(p.getNetQuantity() < 0 && p.getSellPrice() < lastPrice)
			pnl = -1 * pnl;
		return pnl;
	}

}
