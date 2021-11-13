package com.algo.paper.trade.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

	static Logger log = LoggerFactory.getLogger(FileUtils.class);
	public static String readFile(String file) {
		log.info("readFile>");
		String line = "";  
		String splitBy = ",";  
		StringBuilder sb = new StringBuilder();
		int i = 0;
		Double netPL = 0.0;
		try {  
			BufferedReader br = new BufferedReader(new FileReader(file));  
			sb.append("|--------------------------------------------------------------------------------------------------------------------------------\n");
			sb.append("|TRADING SYMBOL\t\t|\tQty\t|\tEntry Price\t|\tCurrent Price\t|\tExit Price\t|\tP/L\t|\n");
			sb.append("|--------------------------------------------------------------------------------------------------------------------------------\n");
			while ((line = br.readLine()) != null) {  
				if(i==0) {
					i++;
					continue;
				}
				String[] lines = line.split(splitBy);
				netPL = netPL + Double.valueOf(lines[5].trim());
				sb.append("|"+lines[0]+"\t|\t"+lines[1]+"\t|\t"+lines[2]+"\t\t|\t"+lines[3]+"\t\t|\t"+lines[4]+"\t\t|\t"+lines[5]+"\t|\n");
			}
			sb.append("---------------------------------------------------------------------------------------------------------------------------------\n");
			sb.append("|NET P/L:\t\t\t\t\t\t\t\t\t\t\t\t\t\t"+Math.round(netPL)+"\t|\n");
			sb.append("---------------------------------------------------------------------------------------------------------------------------------\n");
			br.close();
		} catch (IOException e) {  
			e.printStackTrace();  
			return StringUtils.EMPTY;
		} 
		log.info("<readFile");
		return sb.toString();
	}


	public static String getSymbolToken(String symbol, String dataFilePath) {
		try {
			JSONParser parser = new JSONParser();
			JSONArray nseArray = (JSONArray) parser.parse(new FileReader(dataFilePath));
			for(int i=0; i<nseArray.size() ; i++) {
				JSONObject jsonObject = (JSONObject) nseArray.get(i);
//				System.out.println(jsonObject.get("symbol"));
				if(((String)jsonObject.get("symbol")).equals(symbol)) {
					return (String)jsonObject.get("token");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
