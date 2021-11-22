package com.algo.paper.trade.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.algo.model.MyPosition;
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

	@Autowired
	StraddleServiceImpl straddleService;

	public void placeStraddleStrategy() {
		straddleService.placeStraddleStrategy();
	}

	@Scheduled(cron = "0/30 * * * * ?")
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
		
		
		
		straddleService.printAllPositionsFromSheet();
		straddleService.updteTradeFile(false);
	}

}
