package com.algo.paper.trade.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
public class PaperTradeStraddleService {
	Logger log = LoggerFactory.getLogger(this.getClass());
	LocalTime closeTime = LocalTime.parse(Constants.CLOSEING_TIME);
	LocalTime openingTime = LocalTime.parse(Constants.OPENING_TIME);

	@Value("${app.straddle.closeOnTarget:false}")
	private boolean closeOnTarget;

	@Value("${app.straddle.useStopLoss:false}")
	private boolean useStopLoss;
	
	@Value("${app.straddle.adjustmentPerc:50}")
	private Integer adjustmentPerc;

	@Autowired
	StraddleServiceImpl straddleService;

	public void placeStraddleStrategy() {
		straddleService.placeStraddleStrategy();
	}

	@Scheduled(cron = "${app.straddle.cron.expression}")
	public void monitorPaperStraddleAndDoAdjustments() {
//		if((LocalTime.now().isBefore(openingTime)) || (LocalTime.now().isAfter(closeTime) || LocalTime.now().equals(closeTime))) {
//			System.out.println("\n\n\n\n(STRADDLE) MARKET CLOSED");
//			log.info("(STRADDLE) MARKET CLOSED");
//			straddleService.printAllPositionsFromSheet();
//			return;
//		}
		System.out.println("\n\n\n\n\n\t\t\t(STRADDLE) PAPER - POSITIONS AS ON: "+DateUtils.getDateTime(LocalDateTime.now()));
		log.info("PAPER (STRADDLE) - POSITIONS AS ON: "+DateUtils.getDateTime(LocalDateTime.now()));
		List<MyPosition> netPositions = straddleService.getPaperNetPositions();
		if(CollectionUtils.isEmpty(netPositions)) {
			System.out.println("************* NO PAPER (STRADDLE) POSITIONS FOUND ******************");
			log.info("NO PAPER (STRADDLE) POSITIONS FOUND");
			return;
		}
		List<MyPosition> sellPositions = netPositions.stream().filter(p -> p.getNetQuantity() < 0).collect(Collectors.toList());
		if(CollectionUtils.isEmpty(sellPositions) || sellPositions.size() > 2) {
			System.out.println("************* (STRADDLE) FOUND MORE THAN TWO PAPER (STRADDLE) POSITIONS ******************");
			log.info("FOUND MORE THAN TWO PAPER (STRADDLE) POSITIONS");
			return;
		}
		if(closeOnTarget) {
			boolean isCLosedAll = straddleService.checkTargetAndClosePositions(sellPositions);
			if(isCLosedAll)
				return;
		}
		if(useStopLoss) {
			boolean isCLosedAll = straddleService.checkSLAndClosePositions(sellPositions);
			if(isCLosedAll)
				return;
		}
		Double totCePrem = totalPositionPremium(sellPositions, Constants.CE);
		Double totPePrem = totalPositionPremium(sellPositions, Constants.PE);
		String newSellOptType = StringUtils.EMPTY;
		Double newSellPremNear = 0.0;
		Double diffInPerc = CommonUtils.priceDiffInPerc(totCePrem, totPePrem);
		System.out.println("\t\t\t(STRADDLE) CE AND PE PRICE DIFFERENCE: "+String.format("%.2f", diffInPerc)+"%\n\t\t\tWAITING FOR DIFFERENCE IF: "+adjustmentPerc+"%");
		log.info("\t\t\t(STRADDLE) CE AND PE PRICE DIFFERENCE: "+String.format("%.2f", diffInPerc)+"%\n\t\t\tWAITING FOR DIFFERENCE IF: "+adjustmentPerc+"%");
		if(Double.valueOf(String.format("%.2f", diffInPerc)) > adjustmentPerc) {
			if(totCePrem > totPePrem) {
				newSellOptType = Constants.PE;
				newSellPremNear = totCePrem * 25 / 100;
			} else {
				newSellOptType = Constants.CE;
				newSellPremNear = totPePrem * 25 / 100;
			}
			initAdjustmentAction(newSellOptType, newSellPremNear);
		}
		straddleService.printAllPositionsFromSheet();
		straddleService.updteTradeFile(false);
	}

	private void initAdjustmentAction(String optType, Double premNear) {
		System.out.println("CODE NOT COMPLETED HERE");
	}
	
	private Double totalPositionPremium(List<MyPosition> positions, String optType) {
		Double prem = 0.0;
		List<MyPosition> netPositions = positions.stream().filter(mp -> mp.getNetQuantity() < 0 && mp.getOptionType().equals(optType)).collect(Collectors.toList());
		for(MyPosition pos: netPositions) {
			prem = prem + pos.getCurrentPrice();
		}
		return prem;
	}

}
