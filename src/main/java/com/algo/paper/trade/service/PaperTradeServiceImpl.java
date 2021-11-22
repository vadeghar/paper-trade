package com.algo.paper.trade.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.algo.model.MyPosition;
import com.algo.utils.CommonUtils;
import com.algo.utils.Constants;
import com.algo.utils.DateUtils;

@Service
public class PaperTradeServiceImpl {
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Value("${app.strangle.adjustmentPerc:50}")
	private Integer adjustmentPerc;
	
	@Value("${app.strangle.closeOnTarget:false}")
	private boolean closeOnTarget;
	
	@Value("${app.strangle.useStopLoss:false}")
	private boolean useStopLoss;
	
	@Value("${app.strangle.adjustAtEnd:false}")
	private boolean adjustAtEnd;
	
	LocalTime closeTime = LocalTime.parse(Constants.CLOSEING_TIME);
	LocalTime openingTime = LocalTime.parse(Constants.OPENING_TIME);
	
	@Autowired
	PaperUtils paperUtils;
	
	/**
	 * To start new strangle strategy, Not implemented yet for PAPER
	 * @param opstSymbol
	 * @param expiry
	 * @param deltaVal
	 * @param qty
	 */
	public void placeStrangleStrategy() {
		paperUtils.placeStrangleStrategy();
	}
	
	@Scheduled(cron = "0/30 * * * * ?")
	public void monitorPaperStrangleAndDoAdjustments() throws JSONException, IOException {
//		if((LocalTime.now().isBefore(openingTime)) || (LocalTime.now().isAfter(closeTime) || LocalTime.now().equals(closeTime))) {
//			System.out.println("\n\n\n\nMARKET CLOSED");
//			log.info("MARKET CLOSED");
//			paperUtils.printPositions();
//			return;
//		}
		System.out.println("\n\n\n\n\n\t\t\tPAPER - POSITIONS AS ON: "+DateUtils.getDateTime(LocalDateTime.now()));
		log.info("PAPER - POSITIONS AS ON: "+DateUtils.getDateTime(LocalDateTime.now()));
		List<MyPosition> netPositions = paperUtils.getPaperNetPositions();
		if(CollectionUtils.isEmpty(netPositions)) {
			System.out.println("************* NO PAPER POSITIONS FOUND ******************");
			log.info("NO PAPER POSITIONS FOUND");
			return;
		}
		List<MyPosition> sellPositions = netPositions.stream().filter(p -> p.getNetQuantity() < 0).collect(Collectors.toList());
		if(CollectionUtils.isEmpty(sellPositions) || sellPositions.size() > 2) {
			System.out.println("************* FOUND MORE THAN TWO PAPER POSITIONS ******************");
			log.info("FOUND MORE THAN TWO PAPER POSITIONS");
			return;
		}
		if(closeOnTarget) {
			boolean isCLosedAll = paperUtils.checkTargetAndClosePositions(sellPositions);
			if(isCLosedAll)
				return;
		}
		
		if(useStopLoss) {
			boolean isCLosedAll = paperUtils.checkSLAndClosePositions(sellPositions);
			if(isCLosedAll)
				return;
		}
		
		//List<MyPosition> buyPositions = netPositions.stream().filter(p -> p.getNetQuantity() > 0).collect(Collectors.toList());
		MyPosition p1 = sellPositions.get(0);
		MyPosition p2 = sellPositions.get(1);
		Double diffInPerc = CommonUtils.priceDiffInPerc(p1.getCurrentPrice(), p2.getCurrentPrice());
		if(adjustAtEnd && LocalTime.now().isAfter(closeTime.minusMinutes(5))) {
			adjustmentPerc = 20;
			System.out.println("\t\t\t** Adjustment Perc is Changed to: "+adjustmentPerc);
			log.info("\t\t\t** Adjustment Perc is Changed to: "+adjustmentPerc);
		}
		System.out.println("\t\t\tCE AND PE PRICE DIFFERENCE: "+String.format("%.2f", diffInPerc)+"%\n\t\t\tWAITING FOR DIFFERENCE IF: "+adjustmentPerc+"%");
		log.info("\t\t\tCE AND PE PRICE DIFFERENCE: "+String.format("%.2f", diffInPerc)+"%\n\t\t\tWAITING FOR DIFFERENCE IF: "+adjustmentPerc+"%");
		if(Double.valueOf(String.format("%.2f", diffInPerc)) > adjustmentPerc) {
			initAdjustmentAction(p1, p2);
		}
		paperUtils.printPositions();
		paperUtils.updteTradeFile(false);
		
	}
	
	private void initAdjustmentAction(MyPosition p1, MyPosition p2) {
		System.out.println("**************************************************************************************");
		log.info("TIME TO TAKE ROBO ACTION");
		MyPosition posToClose = null;
		MyPosition posToKeep = null;
		double p1Pnl = paperUtils.getPositionPnl(p1);
		double p2Pnl = paperUtils.getPositionPnl(p2);
		Double otherOptPrem = 0.0;
		if(p1Pnl >  p2Pnl) {
			System.out.println("\t\t\tCLOSING POSITION: "+p1.getTradingSymbol());
			log.info("CLOSING POSITION: "+p1.getTradingSymbol());
			System.out.println("\t\t\tP/L of : "+p1.getTradingSymbol()+" ("+p1Pnl+") IS HIGHER THAN OF POSITION: "+p2.getTradingSymbol()+" ("+p2Pnl+")");
			log.info("P/L of : "+p1.getTradingSymbol()+" ("+p1Pnl+") IS HIGHER THAN OF POSITION: "+p2.getTradingSymbol()+" ("+p2Pnl+")");
			posToClose = p1;
			posToKeep = p2;
		}
		if(p2Pnl >  p1Pnl) {
			System.out.println("CLOSING POSITION: "+p2.getTradingSymbol());
			log.info("CLOSING POSITION: "+p2.getTradingSymbol());
			System.out.println("\t\t\tP/L of : "+p2.getTradingSymbol()+" ("+p2Pnl+") IS HIGHER THAN OF POSITION: "+p1.getTradingSymbol()+" ("+p1Pnl+")");
			log.info("P/L of : "+p2.getTradingSymbol()+" ("+p2Pnl+") IS HIGHER THAN OF POSITION: "+p1.getTradingSymbol()+" ("+p1Pnl+")");
			posToClose = p2;
			posToKeep = p1;
		}
		otherOptPrem = posToKeep.getCurrentPrice();
		System.out.println("CLOSING POSITION: "+posToClose.getTradingSymbol());
		log.info("CLOSING POSITION: "+posToClose.getTradingSymbol());
		MyPosition posToOpen = paperUtils.getNewSellPositionNearPremium(posToClose, otherOptPrem);
		if(posToOpen == null) {
			System.out.println("\t\t\t***NOT FOUND NEAREST OPTION");
			log.info("***NOT FOUND NEAREST OPTION");
			return;
		}
		paperUtils.addStopLossToSheet(posToKeep, posToOpen);
		paperUtils.startAdjustment(posToClose, posToOpen);
		System.out.println("\n**************************************************************************************");
		log.info("**************************************************************************************");
	}
	
	

}
