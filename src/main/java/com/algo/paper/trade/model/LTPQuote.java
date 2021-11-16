package com.algo.paper.trade.model;

import com.google.gson.annotations.SerializedName;

/**
 * A wrapper for instrument token, OHLC data.
 */
public class LTPQuote {
	public LTPQuote(long instrumentToken, double lastPrice) {
		this.instrumentToken = instrumentToken;
		this.lastPrice = lastPrice;
	}
	public LTPQuote() {
		
	}
    @SerializedName("instrument_token")
    public long instrumentToken;
    @SerializedName("last_price")
    public double lastPrice;
    
	@Override
	public String toString() {
		return "LTPQuote [instrumentToken=" + instrumentToken + ", lastPrice=" + lastPrice + "]";
	}

}
