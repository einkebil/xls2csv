package org.yokul.xlsparsing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class first2log {
	public static void main(String[] args) throws Exception {
		/* ensure all is loaded in jvm ! */
		Arrays.asList(org.apache.xmlbeans.impl.values.XmlComplexContentImpl.class,
				org.openxmlformats.schemas.drawingml.x2006.main.ThemeDocument.class);

		File dir;

		if (args.length == 0)
			dir = new File(".");
		else
			dir = new File(args[0]);
		String[] elems = new String[] { "§_0=begin:1" };

		/* separation des confs par feuilles*/
		if (args.length == 2) {
			String conf = args[1];
			elems = conf.split(",");

		}
		if (dir.exists()) {
			System.out.println("converting xlsx in " + dir.getCanonicalPath());
			convert(dir, elems);
		} else {
			System.out.println(dir.getName() + " is not accessible");
		}
	}

	private static FilenameFilter filter = new FilenameFilter() {

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".xlsx") && !name.startsWith("~");
		}

	};

	private static void convert(File dir, String[] sheets) throws Exception {
		if (dir.isDirectory()) {
			for (File elem : Arrays.asList(dir.listFiles())) {

				if (elem.isDirectory()) {
					convert(elem, sheets);
					System.out.println("exploring .. " + elem);
				} else

				if (filter.accept(dir, elem.getName())) {
					convert(elem, sheets);
				}
			}
		} else {
			try (FileInputStream fileIS = new FileInputStream(dir)) {

				System.out.println("begin converting " + dir.getCanonicalPath());
				try (XSSFWorkbook workbook = new XSSFWorkbook(fileIS)) {
					convert(workbook, sheets, dir);

				}
				System.out.println("end converting " + dir.getCanonicalPath());
			}
		}
	}

	private static void convert(XSSFWorkbook workbook, String[] sheets, File dir) throws Exception {
		for (String conf : sheets) {
			String[] args = conf.split("=");
			String sheetIdent = args[0];
			String sheetConfig = args.length > 1 ? args[1] : "begin:1";

			XSSFSheet sheet;
			if (sheetIdent.startsWith("§_")) {
				Integer sheetNumber = Integer.valueOf(sheetIdent.substring(2));
				sheet = workbook.getSheetAt(sheetNumber);
			} else {
				sheet = workbook.getSheet(sheetIdent);
			}
			if (sheet != null) {
				String[] config = sheetConfig.split(":");
				int begin = 1;
				if ("begin".equals(config[0])) {
					begin = Integer.valueOf(config[1]);
				}
				String sheetname = sheet.getSheetName();
				List<Map<String, String>> objects = convert(sheet, begin);
				toLog(new File(dir.getCanonicalPath().replace(".xlsx", "-" + sheetname + ".log")), objects);
			}

		}

	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.ENGLISH);
	static {
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private static void toLog(File destination, List<Map<String, String>> objects)
			throws FileNotFoundException, IOException {
		try (final PrintWriter out = new PrintWriter(destination, Charset.forName("UTF-8"))) {

			final AtomicBoolean isFirst = new AtomicBoolean(true);
			String nowHuman = sdf.format(Date.from(Instant.now()));

			objects.forEach(l -> {
				isFirst.set(true);
				l.forEach((k, v) -> {
					if (isFirst.getAndSet(false)) {
						out.print("time=\"" + nowHuman + "\", ");
					} else {
						out.print(", ");
					}

					out.print("\"" + k + "\"=\"" + trad(l, k) + "\"");
				});
				out.print("\r\n");
			});
		}

	}

	private static String trad(Map<String, String> l, String key) {

		String val = l.get(key);
		switch (key) {
		// special cases to handle by name
		}

		return val.replaceAll("[\\r\\n\\s]+", " ").trim();
	}

	private static List<Map<String, String>> convert(XSSFSheet sheet, int firstLine) throws Exception {
		List<Map<String, String>> res = new ArrayList<Map<String, String>>();
		Iterator<Row> iter = sheet.iterator();
		Row row = null;
		for (; iter.hasNext();) {
			row = iter.next();
			if (row.getRowNum() == firstLine - 1)
				break;
		}
		if (row != null || iter.hasNext()) {
			row = row != null ? row : iter.next();
			List<String> fl = readLine(row)/* removing carriage return duplicate spaces */.stream()
					.map(x -> x.replaceAll("[\\r\\n\\s]+", " ").trim()).collect(Collectors.toList());
//			Map<String, String> fline = new HashMap<>();
//			for (int i = 0; i < fl.size(); i++) {
//				fline.put(fl.get(i), fl.get(i));
//
//			}
//			res.add(fline);

			if (iter.hasNext()) {
				for (; iter.hasNext();) {
					row = iter.next();
					if (null != row.getCell(0) && !"".equals(convertCell(row.getCell(0)))) {

						List<String> values = readLine(row);
						Map<String, String> line = new HashMap<>();
						if (values.size() < fl.size()) {
							throw new Exception("invalid number of column for row  :  " + row.getRowNum());
						}
						for (int i = 0; i < fl.size(); i++) {
							line.put(fl.get(i), values.get(i));

						}
						res.add(line);
					}
				}
			}
			row = null;
		}
		return res;
	}

	static SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
	static DecimalFormat decimalFormat = new DecimalFormat("################0.00");

	private static List<String> readLine(Row row) {
		ArrayList<String> res = new ArrayList<String>();

		Iterator<Cell> cellIterator = row.cellIterator();

		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();

			res.add(convertCell(cell));
		}
		return res;
	}

	/***
	 * // Check the cell type and format accordingly
	 * 
	 * @param cell the cell
	 * @return the value translated
	 */
	private static String convertCell(Cell cell) {
		String value;
		CellType type = cell.getCellType();
		if (type == CellType.FORMULA) {
			type = cell.getCachedFormulaResultType();
		}
		switch (type) {
		case NUMERIC:
			if (DateUtil.isCellDateFormatted(cell)) {
				value = (String.valueOf(df.format(cell.getDateCellValue())));
			} else {
				value = (String.valueOf(decimalFormat.format(cell.getNumericCellValue()).toString()).replace(".", ","));
			}
			break;
		case STRING:
			value = (cell.getStringCellValue());
			break;
		case ERROR:
			value = ("#Error");
			break;
		default:
			value = (cell.getStringCellValue());
			break;
		}
		return value;
	}

}
