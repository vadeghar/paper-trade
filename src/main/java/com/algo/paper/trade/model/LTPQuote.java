package com.algo.paper.trade.model;

import com.google.gson.annotations.SerializedName;

/**
 * A wrapper for instrument token, OHLC data.
 */
public class LTPQuote {

    @SerializedName("instrument_token")
    public long instrumentToken;
    @SerializedName("last_price")
    public double lastPrice;
    
	@Override
	public String toString() {
		return "LTPQuote [instrumentToken=" + instrumentToken + ", lastPrice=" + lastPrice + "]";
	}

}
