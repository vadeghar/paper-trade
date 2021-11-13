package com.algo.paper.trade.service;

import org.junit.jupiter.api.Test;

import com.algo.paper.trade.utils.DateUtils;

public class DateUtilsTest {
	private String expiry= "18/11/2021";
	
	@Test
	public void angelDateTest() {
		
		String angelFormat = DateUtils.getAngelFormatExpiry(expiry);
		System.out.println("angelFormat: "+angelFormat);
	}

}
