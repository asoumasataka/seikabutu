import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class FileSelect {
    public static void main(String[] args) {
        
    	// 第1:DB環境変数
    	// 第2:SQLファイル
    	// 第3:結果出力ファイル名(csv)
    	if (args == null || args.length != 3) {
    		System.out.println("引数:3まで");
    		return;
    	}

        // 環境変数フォルダ
    	String environmentFile = args[0];
        String currentDir = System.getProperty("user.dir");
        String environmentFileDir = currentDir + "\\" + "environment";
        Path environment = Paths.get(environmentFileDir, environmentFile);
        FileSelect app = new FileSelect();
        Map<String,String> conDBItem = app.environmentFolder(environmentFileDir,environment);
        
        // DB接続
        String conDB = app.setupDB(conDBItem);
        
        // ドライバーの読み込み
        try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
        
        // 第1引数のSQLファイルを読み込む
        String sqlInputFile = args[1];
        String inputFileDir = currentDir + "\\" + "sqlInput";
        Path sqlInputPath = Paths.get(inputFileDir, sqlInputFile);
        List<Object> itemObj = app.execDB(conDB, sqlInputPath);

        
        // 出力フォルダ
        String sqlOutputFile = args[2];
        String outputFileDir = currentDir + "\\" + "sqlOutput";
        Path filePath = Paths.get(outputFileDir, sqlOutputFile);
        
        // フォルダ作成
        app.outputFolder(outputFileDir,filePath,itemObj);
        System.out.println("正常終了しました");

    }
    
    // 取得フォルダ
    private List<Object> execDB(String conDB, Path inputSqlPath) {
    	
    	List<Object> parentList = null;
    	List<String> header = null;
    	
		try (Connection con = DriverManager.getConnection(conDB);
				Statement stmt = con.createStatement();) {
		    
			System.out.println("DB接続に成功しました");
			String sqlContent = Files.readString(inputSqlPath, StandardCharsets.UTF_8);
			
			try (ResultSet rs = stmt.executeQuery(sqlContent)) {
				
				// 取得カラムヘッダー数
				ResultSetMetaData metaData = rs.getMetaData();
				int colCount = metaData.getColumnCount();
				header = new ArrayList<String>();
				for (int i=1; i <= colCount; i++) {
				    String colLabel = metaData.getColumnLabel(i);
				    header.add(colLabel);
				}
				
				List<Object> obj = new ArrayList<Object>();
				parentList = new ArrayList<Object>();
				while (rs.next()) {
					// 1レコードを取得
					for (int headerCnt=0; headerCnt < colCount; headerCnt++) { 
						obj.add(rs.getObject(header.get(headerCnt)));
					}
					parentList.add(obj);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 末尾にヘッダー情報
		parentList.add(header);
		
		return parentList;
		
    }

    
    // DB接続セットアップ
    private String setupDB(Map<String,String> conDBItem) {
    	String DB_SERVER_NAME = conDBItem.get("DB_SERVER_NAME");
    	String DB_SERVER_PORT = conDBItem.get("DB_SERVER_PORT");
    	String DB = conDBItem.get("DB");
    	String DB_USER = conDBItem.get("DB_USER");
    	String DB_PASSWORD = conDBItem.get("DB_PASSWORD");
    	
    	String conDB = 
                "jdbc:sqlserver://" + DB_SERVER_NAME + ":" + DB_SERVER_PORT + ";" +
                "databaseName=" + DB + ";" +
                "user=" + DB_USER + ";" +
                "password=" + DB_PASSWORD + ";" +
                "encrypt=true;" +
                "trustServerCertificate=true;";
    	
    	return conDB;
    	
    }
    
    
    // environmentを取得
    private Map<String,String> environmentFolder(String targetDirPath, Path targetFile) {
    	// 対象の存在チェック
        File targetDir = new File(targetDirPath);
        Path fileFolderDir = Paths.get(targetDir.getPath());
        
        // 戻り値
        Map<String,String> keyMap = null;
        if (Files.exists(fileFolderDir) && Files.isDirectory(fileFolderDir)) {
        	
        	try {
				List<String> lines = Files.readAllLines(targetFile);
				keyMap = new HashMap<String,String>();
				for (String item : lines) {
					String[] parts = item.split("=", 2);
					
					// 空白除去
					String key = "";
					String value = "";
					for(int i = 0;i < parts.length;i++) {
						if(i == 0) {
							key = parts[i].trim();
						}
						
						if(i == 1) {
							value = parts[i].trim();
						}
					}
					
					keyMap.put(key, value);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
        	
        }
        
        return keyMap;
    	
    }
    
    
    
    // 出力フォルダ
    private void outputFolder(String targetDirPath, Path writeFile, List<Object> itemObj) {
    	
        File targetDir = new File(targetDirPath);
        Path fileFolderDir = Paths.get(targetDir.getPath());
        
        try {
        	// 存在しない場合はフォルダを作成する
        	if (!Files.exists(fileFolderDir)) {
        		Files.createDirectories(fileFolderDir);
            }
        	
            if (Files.exists(fileFolderDir) && Files.isDirectory(fileFolderDir)) {
            	
    			// ヘッダーを取得
    			Object headers = itemObj.get(itemObj.size()-1);
    			List<?> headerList = (List<?>) headers;
    			String[] stringHeaders = headerList.stream()
    			        .map(String.class::cast)
    			        .toArray(String[]::new);
    			
    			// CSV出力
    			CSVFormat format = CSVFormat.DEFAULT.builder()
    			        .setHeader(stringHeaders)
    			        .get();
    		
    			try (
    					BufferedWriter writer = Files.newBufferedWriter(writeFile, StandardCharsets.UTF_8);
    					CSVPrinter printer = new CSVPrinter(writer, format)
    				) {
    				
    				// csvファイル書き込み
    				int recordSize = itemObj.size();
    				for (int i = 0; i< recordSize-1;i++) {
    					List<Object> innerList = (List<Object>) itemObj.get(i);
    					printer.printRecord(innerList);
    				}
    				
    			} catch (IOException e) {
    	    		e.printStackTrace();
    			}
    			
            }
            
        } catch (IOException e) {
    		e.printStackTrace();
    	}
    	
    }

}




