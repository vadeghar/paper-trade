package com.algo.paper.trade.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.algo.paper.trade.model.LTPQuote;
import com.algo.paper.trade.utils.FileUtils;
import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.models.Gtt;
import com.angelbroking.smartapi.models.GttParams;
import com.angelbroking.smartapi.models.Order;
import com.angelbroking.smartapi.models.OrderParams;
import com.angelbroking.smartapi.utils.Constants;

@Service
public class AngelSmartServiceImpl {
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	SmartConnect smartConnect;
	
	public Order modifyOrder(OrderParams orderParams, String orderId) throws SmartAPIException, IOException {
		return smartConnect.modifyOrder(orderId, orderParams, Constants.VARIETY_NORMAL);
	}
	
	public Order cancelOrder(String orderId) throws SmartAPIException, IOException {
		return smartConnect.cancelOrder(orderId, Constants.VARIETY_NORMAL);
	}
	
	public JSONObject getOrderHistory() throws SmartAPIException, IOException {
		return  smartConnect.getOrderHistory(smartConnect.getUserId());
	}
	
	public JSONObject getLTP(String exchange, String tradingSymbol, String symboltoken) throws SmartAPIException, IOException {
		return smartConnect.getLTP(exchange, tradingSymbol, symboltoken);
	}
	
	public JSONObject getTrades() throws SmartAPIException, IOException {
		return smartConnect.getTrades();
	}
	
	public JSONObject getRMS() throws SmartAPIException, IOException {
		return smartConnect.getRMS();
	}

	public JSONObject getHolding() throws SmartAPIException, IOException {
		return smartConnect.getHolding();
	}

	public JSONObject getPosition() throws SmartAPIException, IOException {
		return smartConnect.getPosition();
	}
	
	public Map<String, LTPQuote> getLtps(List<String> symbols, String exchange, String dataFilePath) {
		Map<String, LTPQuote> ltps = new HashMap<>();
		try {
			for(String symbol : symbols) {
				String token = FileUtils.getSymbolToken(symbol, dataFilePath);
				log.info("TOKEN : "+token);
				org.json.JSONObject niftySpot = getLTP(exchange, symbol, token);
				log.info("RESP : "+niftySpot);
				LTPQuote q = new LTPQuote();
				q.instrumentToken = niftySpot.getLong("symboltoken");
				q.lastPrice = niftySpot.getDouble("ltp");
				ltps.put(symbol, q);
			}
			log.info("LTPQuote: "+ltps);
		} catch (IOException | SmartAPIException | JSONException e) {
			e.printStackTrace();
		}
		return ltps;
	}
	
	/** convert Position 
	 * JSONObject requestObejct = new JSONObject();
		requestObejct.put("exchange", "NSE");
		requestObejct.put("oldproducttype", "DELIVERY");
		requestObejct.put("newproducttype", "MARGIN");
		requestObejct.put("tradingsymbol", "SBIN-EQ");
		requestObejct.put("transactiontype", "BUY");
		requestObejct.put("quantity", 1);
		requestObejct.put("type", "DAY");
		*/
	public JSONObject convertPosition(String exchange, String oldproducttype, String newproducttype, 
			String tradingsymbol, String transactiontype, String quantity, String type) throws SmartAPIException, IOException {
		JSONObject requestObejct = new JSONObject();
		requestObejct.put("exchange", exchange);
		requestObejct.put("oldproducttype", oldproducttype);
		requestObejct.put("newproducttype", newproducttype);
		requestObejct.put("tradingsymbol", tradingsymbol);
		requestObejct.put("transactiontype", transactiontype);
		requestObejct.put("quantity", quantity);
		requestObejct.put("type", type);
		return smartConnect.convertPosition(requestObejct);
	}

	/** Create Gtt Rule 
	 * GttParams gttParams = new GttParams();

		gttParams.tradingsymbol = "SBIN-EQ";
		gttParams.symboltoken = "3045";
		gttParams.exchange = "NSE";
		gttParams.producttype = "MARGIN";
		gttParams.transactiontype = "BUY";
		gttParams.price = 100000.01;
		gttParams.qty = 10;
		gttParams.disclosedqty = 10;
		gttParams.triggerprice = 20000.1;
		gttParams.timeperiod = 300;*/
	public Gtt createRule(GttParams gttParams) throws SmartAPIException, IOException {
		return smartConnect.gttCreateRule(gttParams);
	}

	/** Modify Gtt Rule 
	 * GttParams gttParams = new GttParams();

		gttParams.tradingsymbol = "SBIN-EQ";
		gttParams.symboltoken = "3045";
		gttParams.exchange = "NSE";
		gttParams.producttype = "MARGIN";
		gttParams.transactiontype = "BUY";
		gttParams.price = 100000.1;
		gttParams.qty = 10;
		gttParams.disclosedqty = 10;
		gttParams.triggerprice = 20000.1;
		gttParams.timeperiod = 300;

		Integer id = 1000051;*/
	public Gtt modifyRule(Integer id, GttParams gttParams) throws SmartAPIException, IOException {
		return smartConnect.gttModifyRule(id, gttParams);
	}

	/** Cancel Gtt Rule */
	public Gtt cancelRule(Integer id, String symboltoken, String exchange) throws SmartAPIException, IOException {
		return smartConnect.gttCancelRule(id, symboltoken, exchange);
	}

	/** Gtt Rule Details */
	public JSONObject gttRuleDetails(Integer id) throws SmartAPIException, IOException {
		return smartConnect.gttRuleDetails(id);
	}
	
	/** Gtt Rule Lists 
	 * List<String> status = new ArrayList<String>() {
			{
				add("NEW");
				add("CANCELLED");
				add("ACTIVE");
				add("SENTTOEXCHANGE");
				add("FORALL");
			}
		};
		Integer page = 1;
		Integer count = 10;*/
	public JSONArray ruleList(List<String> status, Integer page, Integer count) throws SmartAPIException, IOException {
		return smartConnect.gttRuleList(status, page, count);
	}

	/** Historic Data 
	 * JSONObject requestObejct = new JSONObject();
		requestObejct.put("exchange", "NSE");
		requestObejct.put("symboltoken", "3045");
		requestObejct.put("interval", "ONE_MINUTE");
		requestObejct.put("fromdate", "2021-03-08 09:00");
		requestObejct.put("todate", "2021-03-09 09:20");*/
	public String getCandleData(String exchange, String symboltoken, String interval, String fromdate, String todate) throws SmartAPIException, IOException {
		JSONObject requestObejct = new JSONObject();
		requestObejct.put("exchange", exchange);
		requestObejct.put("symboltoken", symboltoken);
		requestObejct.put("interval", interval);
		requestObejct.put("fromdate", fromdate);
		requestObejct.put("todate", todate);
		return smartConnect.candleData(requestObejct);
	}
	
	/** Logout user. */
	public JSONObject logout() throws SmartAPIException, IOException {
		return smartConnect.logout();
	}
	
	public Order placeOrder(OrderParams orderParams, String variety) {
		return smartConnect.placeOrder(orderParams, variety);
	}
	
	public Order modifyOrder(String orderId, OrderParams orderParams, String variety) {
		return smartConnect.modifyOrder(orderId, orderParams, variety);
	}
	
	public Order cancelOrder(String orderId, String variety) {
		return smartConnect.cancelOrder(orderId, variety);
	}
	
	public JSONObject getOrderHistory(String clientId) {
		return smartConnect.getOrderHistory(clientId);
	}
	
}
