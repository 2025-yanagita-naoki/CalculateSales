package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	//商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	//商品定義集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String BRANCH_FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String COMMODITY_FILE_INVALID_FORMAT = "商品定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_SERIAL ="売上ファイル名が連番になっていません";
	private static final String PRICE_NUMBER_ERROR ="合計⾦額が10桁を超えました";

	//ファイルフォーマット条件
	private static final String BRANCH_FILE_FORMAT ="^[0-9]{3}$";
	private static final String COMMODITY_FILE_FORMAT ="^[a-zA-Z0-9]{8}$";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {

		//コマンドライン引数代入エラー
				if(args.length != 1) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと商品金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();


		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales, BRANCH_FILE_FORMAT, BRANCH_FILE_INVALID_FORMAT)) {
			return;
		}

		//商品定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales, COMMODITY_FILE_FORMAT,COMMODITY_FILE_INVALID_FORMAT)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();

		List<File> rcdFiles = new ArrayList<>();

		//該当ファイルの判定
		for (int i = 0; i < files.length; i++) {
			String fileName = files[i].getName();
			if (files[i].isFile() && fileName.matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		//売上集計ファイルのソート
		Collections.sort(rcdFiles);
		for(int i = 0; i < rcdFiles.size() - 1; i++) {
			int formar = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			//売上集計ファイルが連番となっていない場合のエラー
			if(latter - formar != 1) {
				System.out.println(FILE_NOT_SERIAL);
				return;
			}
		}

		for (int i = 0; i < rcdFiles.size(); i++) {
			File file = new File(args[0], rcdFiles.get(i).getName());

			//集計ファイルの読み込み
			BufferedReader br = null;

			try {
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				//支店コード・金額のリスト化
				String line;
				List<String> items = new ArrayList<>();
				while ((line = br.readLine()) != null) {
					items.add(line);
				}

				String fileName = rcdFiles.get(i).getName();
				//売上ファイルのフォーマットエラー処理
				if(items.size() != 3) {
					System.out.println("<" + fileName +">" + "のフォーマットが不正です");
					return;
				}

				String branchKey = items.get(0);
				String CommodityKey = items.get(1);

				//保持KeyとマップのKeyの不一致エラー
				if(!branchSales.containsKey(branchKey)) {
					System.out.println("<" + fileName +">" + "の支店コードが不正です"); //支店コードの不一致
					return;
				} else if(!commoditySales.containsKey(CommodityKey)) {
					System.out.println("<" + fileName +">" + "の商品コードが不正です"); //商品コードの不一致
					return;
				}



				//売上金額が数字になっていない場合のエラー処理
				if(!items.get(2).matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				//合計金額の計算・マップへの追加
				long fileSale = Long.parseLong(items.get(2));
				Long branchSaleAmount = branchSales.get(branchKey) + fileSale;
				Long CommoditySaleAmount = commoditySales.get(CommodityKey) + fileSale;


				//桁数超過のエラー処理
				if(branchSaleAmount >= 10000000000L) {
					System.out.println(PRICE_NUMBER_ERROR); //支店別合計金額の桁数超過
					return;
				} else if(CommoditySaleAmount >= 10000000000L){
					System.out.println(PRICE_NUMBER_ERROR); //商品別合計金額の桁数超過
					return;
				}

				branchSales.put(branchKey, branchSaleAmount);
				commoditySales.put(CommodityKey, CommoditySaleAmount);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				// ファイルを開いている場合
				if (br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

		// 商品別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> mapNames,
			Map<String, Long> mapSales, String fileFormat, String invalidFormat) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			//ファイルが存在しない場合
			if (!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");

				//ファイルフォーマットのエラー処理
				if((items.length != 2) || (!items[0].matches(fileFormat))) {
					System.out.println(invalidFormat);
					return false;
				}

				mapNames.put(items[0], items[1]);
				mapSales.put(items[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> mapNames,
			Map<String, Long> mapSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)

		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			//支店別集計ファイルへの書き込み処理
			for (String key : mapNames.keySet()) {
				bw.write(key + "," + mapNames.get(key) + "," + mapSales.get(key));
				bw.newLine();
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}
