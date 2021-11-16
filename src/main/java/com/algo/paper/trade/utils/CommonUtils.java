package com.algo.paper.trade.utils;

import java.awt.Toolkit;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.algo.paper.trade.model.MyPosition;

public class CommonUtils {
	
	public static void beep() {
		for(int i= 0; i<10;i++) {
			try {
				Toolkit.getDefaultToolkit().beep();
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * NIFTY21NOV16800PE = 16800 = NIFTY18NOV2118400CE
	 * @param tradingSymbol
	 * @return
	 */
	public static String getStrikePrice(String tradingSymbol) {
		String strikePrice = tradingSymbol
				.replace(getSymbol(tradingSymbol), StringUtils.EMPTY)
				.replace(getExpiry(tradingSymbol), StringUtils.EMPTY)
				.replace(getOptionType(tradingSymbol), StringUtils.EMPTY);
		return strikePrice;
	}

	/**
	 * NIFTY21NOV16800PE = PE = NIFTY18NOV202118400CE = CE
	 * @param tradingSymbol
	 * @return
	 */
	public static String getOptionType(String tradingSymbol) {
		return tradingSymbol.substring(tradingSymbol.length()-2);
	}

	/**
	 * NIFTY21NOV16800PE = NIFTY = NIFTY18NOV202118400CE
	 * @param tradingSymbol
	 * @return
	 */
	public static String getSymbol(String tradingSymbol) {
		String opstSymbol = tradingSymbol.substring(0, tradingSymbol.length() - 16);
		return opstSymbol;
	}

	/**
	 * NIFTY21NOV16800PE = 21NOV/21N03/21N18 = NIFTY18NOV202118400CE = 18NOV2021/25NOV2021
	 * @param tradingSymbol
	 * @return
	 */
	public static String getExpiry(String tradingSymbol) {
		String opstExpiry = tradingSymbol.substring(getSymbol(tradingSymbol).length(), tradingSymbol.length() - 7);
		return opstExpiry;
	}
	
	public static Double getNetPnl(List<MyPosition> netPositions) {
		Double netPnl = 0.0;
		for(MyPosition p : netPositions) {
			netPnl = p.getPositionPnl() != null ? (netPnl + p.getPositionPnl()) : 0.0;
		}
		return netPnl;
	}
	
	public static Double getNetPrice(List<MyPosition> netPositions) {
		Double netPrice = 0.0;
		for(MyPosition p : netPositions) {
			netPrice = p.getCurrentPrice() != null ? (netPrice + p.getCurrentPrice()) : 0.0;
		}
		return netPrice;
	}
	
	public static Double priceDiffInPerc(Double p1, Double p2) {
		Double big = p1 > p2 ? p1  : p2;
		Double small = p1 < p2 ? p1  : p2;
		Double perc = (big - small) / big * 100;
		return perc;
	}

}
