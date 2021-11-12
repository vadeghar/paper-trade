package com.algo.paper.trade.model;

import com.google.gson.annotations.SerializedName;

/**
 * A wrapper for position.
 */
public class Position {

    @SerializedName("product")
    public String product;
    @SerializedName("exchange")
    public String exchange;
    @SerializedName("sell_value")
    public Double sellValue;
    @SerializedName("last_price")
    public Double lastPrice;
    @SerializedName("unrealised")
    public Double unrealised;
    @SerializedName("buy_price")
    public Double buyPrice;
    @SerializedName("sell_price")
    public Double sellPrice;
    @SerializedName("m2m")
    public Double m2m;
    @SerializedName("tradingsymbol")
    public String tradingSymbol;
    @SerializedName("quantity")
    public int netQuantity;
    @SerializedName("sell_quantity")
    public int sellQuantity;
    @SerializedName("realised")
    public Double realised;
    @SerializedName("buy_quantity")
    public int buyQuantity;
    @SerializedName("net_value")
    public Double netValue;
    @SerializedName("buy_value")
    public Double buyValue;
    @SerializedName("multiplier")
    public Double multiplier;
    @SerializedName("instrument_token")
    public String instrumentToken;
    @SerializedName("close_price")
    public Double closePrice;
    @SerializedName("pnl")
    public Double pnl;
    @SerializedName("overnight_quantity")
    public int overnightQuantity;
    @SerializedName("buy_m2m")
    public double buym2m;
    @SerializedName("sell_m2m")
    public double sellm2m;
    @SerializedName("day_buy_quantity")
    public double dayBuyQuantity;
    @SerializedName("day_sell_quantity")
    public double daySellQuantity;
    @SerializedName("day_buy_price")
    public double dayBuyPrice;
    @SerializedName("day_sell_price")
    public double daySellPrice;
    @SerializedName("day_buy_value")
    public double dayBuyValue;
    @SerializedName("day_sell_value")
    public double daySellValue;
    @SerializedName("value")
    public double value;
    @SerializedName("average_price")
    public double averagePrice;
	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}
	public String getExchange() {
		return exchange;
	}
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}
	public Double getSellValue() {
		return sellValue;
	}
	public void setSellValue(Double sellValue) {
		this.sellValue = sellValue;
	}
	public Double getLastPrice() {
		return lastPrice;
	}
	public void setLastPrice(Double lastPrice) {
		this.lastPrice = lastPrice;
	}
	public Double getUnrealised() {
		return unrealised;
	}
	public void setUnrealised(Double unrealised) {
		this.unrealised = unrealised;
	}
	public Double getBuyPrice() {
		return buyPrice;
	}
	public void setBuyPrice(Double buyPrice) {
		this.buyPrice = buyPrice;
	}
	public Double getSellPrice() {
		return sellPrice;
	}
	public void setSellPrice(Double sellPrice) {
		this.sellPrice = sellPrice;
	}
	public Double getM2m() {
		return m2m;
	}
	public void setM2m(Double m2m) {
		this.m2m = m2m;
	}
	public String getTradingSymbol() {
		return tradingSymbol;
	}
	public void setTradingSymbol(String tradingSymbol) {
		this.tradingSymbol = tradingSymbol;
	}
	public int getNetQuantity() {
		return netQuantity;
	}
	public void setNetQuantity(int netQuantity) {
		this.netQuantity = netQuantity;
	}
	public int getSellQuantity() {
		return sellQuantity;
	}
	public void setSellQuantity(int sellQuantity) {
		this.sellQuantity = sellQuantity;
	}
	public Double getRealised() {
		return realised;
	}
	public void setRealised(Double realised) {
		this.realised = realised;
	}
	public int getBuyQuantity() {
		return buyQuantity;
	}
	public void setBuyQuantity(int buyQuantity) {
		this.buyQuantity = buyQuantity;
	}
	public Double getNetValue() {
		return netValue;
	}
	public void setNetValue(Double netValue) {
		this.netValue = netValue;
	}
	public Double getBuyValue() {
		return buyValue;
	}
	public void setBuyValue(Double buyValue) {
		this.buyValue = buyValue;
	}
	public Double getMultiplier() {
		return multiplier;
	}
	public void setMultiplier(Double multiplier) {
		this.multiplier = multiplier;
	}
	public String getInstrumentToken() {
		return instrumentToken;
	}
	public void setInstrumentToken(String instrumentToken) {
		this.instrumentToken = instrumentToken;
	}
	public Double getClosePrice() {
		return closePrice;
	}
	public void setClosePrice(Double closePrice) {
		this.closePrice = closePrice;
	}
	public Double getPnl() {
		return pnl;
	}
	public void setPnl(Double pnl) {
		this.pnl = pnl;
	}
	public int getOvernightQuantity() {
		return overnightQuantity;
	}
	public void setOvernightQuantity(int overnightQuantity) {
		this.overnightQuantity = overnightQuantity;
	}
	public double getBuym2m() {
		return buym2m;
	}
	public void setBuym2m(double buym2m) {
		this.buym2m = buym2m;
	}
	public double getSellm2m() {
		return sellm2m;
	}
	public void setSellm2m(double sellm2m) {
		this.sellm2m = sellm2m;
	}
	public double getDayBuyQuantity() {
		return dayBuyQuantity;
	}
	public void setDayBuyQuantity(double dayBuyQuantity) {
		this.dayBuyQuantity = dayBuyQuantity;
	}
	public double getDaySellQuantity() {
		return daySellQuantity;
	}
	public void setDaySellQuantity(double daySellQuantity) {
		this.daySellQuantity = daySellQuantity;
	}
	public double getDayBuyPrice() {
		return dayBuyPrice;
	}
	public void setDayBuyPrice(double dayBuyPrice) {
		this.dayBuyPrice = dayBuyPrice;
	}
	public double getDaySellPrice() {
		return daySellPrice;
	}
	public void setDaySellPrice(double daySellPrice) {
		this.daySellPrice = daySellPrice;
	}
	public double getDayBuyValue() {
		return dayBuyValue;
	}
	public void setDayBuyValue(double dayBuyValue) {
		this.dayBuyValue = dayBuyValue;
	}
	public double getDaySellValue() {
		return daySellValue;
	}
	public void setDaySellValue(double daySellValue) {
		this.daySellValue = daySellValue;
	}
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
	}
	public double getAveragePrice() {
		return averagePrice;
	}
	public void setAveragePrice(double averagePrice) {
		this.averagePrice = averagePrice;
	}
	@Override
	public String toString() {
		return "Position [product=" + product + ", exchange=" + exchange + ", sellValue=" + sellValue + ", lastPrice="
				+ lastPrice + ", unrealised=" + unrealised + ", buyPrice=" + buyPrice + ", sellPrice=" + sellPrice
				+ ", m2m=" + m2m + ", tradingSymbol=" + tradingSymbol + ", netQuantity=" + netQuantity
				+ ", sellQuantity=" + sellQuantity + ", realised=" + realised + ", buyQuantity=" + buyQuantity
				+ ", netValue=" + netValue + ", buyValue=" + buyValue + ", multiplier=" + multiplier
				+ ", instrumentToken=" + instrumentToken + ", closePrice=" + closePrice + ", pnl=" + pnl
				+ ", overnightQuantity=" + overnightQuantity + ", buym2m=" + buym2m + ", sellm2m=" + sellm2m
				+ ", dayBuyQuantity=" + dayBuyQuantity + ", daySellQuantity=" + daySellQuantity + ", dayBuyPrice="
				+ dayBuyPrice + ", daySellPrice=" + daySellPrice + ", dayBuyValue=" + dayBuyValue + ", daySellValue="
				+ daySellValue + ", value=" + value + ", averagePrice=" + averagePrice + "]";
	}
    
    
}

