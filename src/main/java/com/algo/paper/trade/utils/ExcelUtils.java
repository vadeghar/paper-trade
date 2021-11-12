package com.algo.paper.trade.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExcelUtils {
	static String fileNamePrefix = "trades_";
	static String ext = "xlsx";
	
	public static String createExcelFile(String dir) {
		String fileFullPath = StringUtils.EMPTY;
		try {
			File directoryPath = new File(dir);
			if(!directoryPath.exists()) {
				directoryPath.mkdirs();
			}
			Integer fileNo = getLastGenerateFileNo(dir);
			fileNo = fileNo+1;
			fileFullPath = dir+File.separator+fileNamePrefix+""+fileNo+"."+ext;
			addHeader(fileFullPath);
			System.out.println("Excel File has been created successfully.");
			return fileFullPath;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileFullPath;
	}
	
	//Update a specific cell in the Excel file
	public static void updateCellByCellZeroText(String fileFullPath, String c0Text, Integer cNumToUpdate, String textToUpdate) {
		try {
			FileInputStream inputStream = new FileInputStream(new File(fileFullPath));
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            Integer rowNo = 0;
            for (Row r : sheet) {
                if(r.getCell(0).getStringCellValue().equals(c0Text)) {
                	rowNo = r.getRowNum();
                	break;
                }
            }
            Cell cell2Update = sheet.getRow(rowNo).getCell(cNumToUpdate);
            cell2Update.setCellValue(textToUpdate);
            inputStream.close();
            FileOutputStream outputStream = new FileOutputStream(fileFullPath);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isSymbolExistInFile(String fileFullPath, String c0Text) {
		boolean symbolExist = false;
		try {
			FileInputStream inputStream = new FileInputStream(new File(fileFullPath));
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            Integer rowNo = 0;
            for (Row r : sheet) {
                if(r.getCell(0).getStringCellValue().equals(c0Text)) {
                	symbolExist = true;
                	break;
                }
            }
            inputStream.close();
            FileOutputStream outputStream = new FileOutputStream(fileFullPath);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return symbolExist;
	}
	
	/**
	 * @param fileFullPath
	 * @param bookData
	 */
	public static void addOrUpdateRow(String fileFullPath, Object[] bookRow) {
		try {
			FileInputStream inputStream = new FileInputStream(new File(fileFullPath));
            Workbook wb = WorkbookFactory.create(inputStream);
            Sheet sheet = wb.getSheetAt(0);
            int rowCount = getLastRow(sheet);
//            Font headerFont = wb.createFont();
//    		CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
//    		cellStyle.setFont(headerFont);
//    		cellStyle.setAlignment(HorizontalAlignment.RIGHT);
            Row row = getRowBySymbol(sheet, (String)bookRow[0]);
            if(row == null) {
            	row = sheet.createRow(rowCount-1);
            }
            Cell cell;
            for(int i=0; i<= 6; i++) {
            	if(bookRow[i] != null && StringUtils.isNotBlank(bookRow[i].toString())) {
            		cell = row.createCell(i);
//            		cell.setCellStyle(cellStyle);
            		if(bookRow[i] instanceof String)
            			cell.setCellValue((String) bookRow[i]);
            		else if(bookRow[i] instanceof Integer)
                        cell.setCellValue((Integer)bookRow[i]);
            		else if(bookRow[i] instanceof Double) {
//            			cellStyle.setDataFormat(wb.createDataFormat().getFormat("0.00"));
                        cell.setCellValue((Double)bookRow[i]);
            		}
            	}
            }
            updateNetPnl(wb, sheet);
            inputStream.close();
            FileOutputStream outputStream = new FileOutputStream(fileFullPath);
            wb.write(outputStream);
            wb.close();
            outputStream.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param fileFullPath
	 */
	public static List<String> getAllSymbols(String fileFullPath) {
		List<String> symbols = new ArrayList<>();
		try {
			FileInputStream inputStream = new FileInputStream(new File(fileFullPath));
            Workbook wb = WorkbookFactory.create(inputStream);
            Sheet sheet = wb.getSheetAt(0);
            int i = 0;
            for(Row r : sheet) {
            	if(i == 0) {
            		i++;
            		continue;
            	}
            	if(r.getCell(0) != null && StringUtils.isNotBlank(r.getCell(0).toString()))
            		symbols.add(r.getCell(0).toString());
            }
            inputStream.close();
            wb.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return symbols;
	}
	
	/**
	 * @param fileFullPath
	 * @param bookData
	 */
	public static void addOrUpdateRows(String fileFullPath, List<Object[]> bookRows) {
		try {
            for(Object[] bookRow: bookRows) {
            	addOrUpdateRow(fileFullPath, bookRow);
            }
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static Row getRowBySymbol(Sheet sheet, String symbol) {
		for (Row r : sheet) {
            if(r.getCell(0) != null && StringUtils.isNotBlank(r.getCell(0).toString()) && r.getCell(0).toString().equals(symbol)) 
            	return r;
		}
		return null;
	}

	private static int getLastRow(Sheet sheet) {
		int i = 1;
		for (Row r : sheet) {
            if(r.getCell(0) == null || StringUtils.isBlank(r.getCell(0).toString())) 
            	break;
			i++;
		}
		return i;
	}

	private static void addHeader(String fileFullPath) throws FileNotFoundException, IOException {
		Workbook wb = new HSSFWorkbook();
		Sheet sheet = wb.createSheet();
		Object[] bookRow = prepareDataRow("Position","Qty", "Sell Price", "Buy Price", "Current Price", "P&L", "Net P&L");
		int rowCount = 0;
		Font headerFont = wb.createFont();
		headerFont.setColor(IndexedColors.WHITE.index);
		headerFont.setBold(true);
		CellStyle headerCellStyle = sheet.getWorkbook().createCellStyle();
		headerCellStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.index);
		headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerCellStyle.setFont(headerFont);
		headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
		Row row = sheet.createRow(rowCount);
		Cell cell;
		for(int i=0; i< 6; i++) {
			if(i == 0)
				sheet.setColumnWidth(i, 25 * 256);
			else
				sheet.setColumnWidth(i, 15 * 256);
			cell = row.createCell(i);
			cell.setCellStyle(headerCellStyle);
			cell.setCellValue((String) bookRow[i]);
		}
		FileOutputStream outputStream = new FileOutputStream(fileFullPath);
		wb.write(outputStream);
		wb.close();
		outputStream.close();
	}

	public static Object[] prepareDataRow(Object c0, Object c1, Object c2, Object c3, Object c4, Object c5, Object c6) {
		Object[] headerData = {c0, c1, c2, c3, c4, c5, c6};
		return headerData;
	}
	
	public static String getCurrentFileNameWithPath(String dir) {
		String fileName = "trades_"+getLastGenerateFileNo(dir);
		fileName = dir+File.separator+fileName+"."+ext;
		File file = new File(fileName);
		if(!file.exists())
			ExcelUtils.createExcelFile(dir);
		return fileName;
	}
	
	private static Integer getLastGenerateFileNo(String dir) {
		Integer maxNo = 0;
		try {
			File directoryPath = new File(dir);
			List<Integer> fileSeqns = new ArrayList<>();
			List<String> fileNames = null;
			if(directoryPath.exists() && directoryPath.isDirectory()) {
				fileNames = Arrays.asList(directoryPath.list());
			} else {
				directoryPath.mkdirs();
			}
			if(fileNames != null && fileNames.size() > 0) {
				for(int i=0; i < fileNames.size(); i++) {
					if(fileNames.get(i).startsWith(fileNamePrefix) &&  fileNames.get(i).endsWith("."+ext)) {
						String fName = fileNames.get(i).replace("."+ext, StringUtils.EMPTY);
						fileSeqns.add(Integer.valueOf(fName.split("_")[1]));
					}
				}
				maxNo = fileSeqns.stream().max(Integer::compare).get();
			} 
		} catch(Exception e) {
			e.printStackTrace();
			return 9999;
		}
		return maxNo;
	}
	
	public static void updateNetPnl(Workbook wb, Sheet sheet) {
		int lastCellNum = sheet.getRow(0).getLastCellNum();
		Cell formulaCell = sheet.getRow(1).createCell(6);
		String endRow = "F"+sheet.getLastRowNum();
		formulaCell.setCellFormula("SUM(F2:F100)");
		FormulaEvaluator formulaEvaluator = 
				  wb.getCreationHelper().createFormulaEvaluator();
				formulaEvaluator.evaluate(formulaCell);
	}
	
	public static String[][] getFileData(String filePath) {
		String[][] dataTable = null;
		try {
			FileInputStream inputStream = new FileInputStream(new File(filePath));
			Workbook wb = WorkbookFactory.create(inputStream);
			Sheet sheet = wb.getSheetAt(0);
			int noOfRows = getLastRow(sheet) + 1;
			int noOfColumns = sheet.getRow(0).getLastCellNum();
			dataTable = new String[noOfRows][noOfColumns];
			for (int i = sheet.getFirstRowNum(); i < sheet.getLastRowNum() + 1; i++) {
			    Row row = sheet.getRow(i);
			    for (int j = row.getFirstCellNum(); j < noOfColumns; j++) {
			        Cell cell = row.getCell(j);
			        dataTable[i][j] = getCellValueAsString(cell);
			    }
			}
			wb.close();
		} catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	    return dataTable;
	}
	
	private static String getCellValueAsString(Cell cell) {
		if(cell == null) return "";
	    CellType cellType = CellType.forInt(cell.getCellType());
	    String val = "";

	    switch (cellType) {
	        case STRING:
	            val = StringUtils.isNotBlank(cell.getStringCellValue()) ? cell.getStringCellValue() : StringUtils.EMPTY;
	            break;
	        case NUMERIC:
	            DataFormatter dataFormatter = new DataFormatter();
	            val = cell.getNumericCellValue() != 0 ? String.valueOf(cell.getNumericCellValue()) : "0";
	            break;
	        case BOOLEAN:
	            val = String.valueOf(cell.getBooleanCellValue());
	            break;

	        case BLANK:
	            break;
	           
	        case FORMULA:
	        	break;
	    }
	    return val;
	}
	
}
