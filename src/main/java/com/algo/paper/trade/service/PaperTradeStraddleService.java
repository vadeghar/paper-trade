package com.algo.paper.trade.service;

import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.algo.utils.Constants;

@Service
public class PaperTradeStraddleService {
	
	LocalTime closeTime = LocalTime.parse(Constants.CLOSEING_TIME);
	LocalTime openingTime = LocalTime.parse(Constants.OPENING_TIME);
	
	@Autowired
	StraddleServiceImpl straddleService;
	
	public void placeStraddleStrategy() {
		straddleService.placeStraddleStrategy();
	}

}
