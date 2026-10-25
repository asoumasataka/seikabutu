import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DbUpdate {
    public static void main(String[] args) {
        
    	// 第1:環境変数
    	// 第2:updateファイル
    	// 第3:Send、Receiveフラグ
    	if (args == null || args.length != 3) {
    		System.out.println("引数:3まで");
    		return;
    	}

        // 環境変数フォルダ
    	String environmentFile = args[0];
        String currentDir = System.getProperty("user.dir");
        String environmentFileDir = currentDir + "\\" + "environment";
        Path environment = Paths.get(environmentFileDir, environmentFile);
        DbUpdate app = new DbUpdate();
        Map<String,String> conDBItem = app.environmentFolder(environmentFileDir,environment);
        
        // DB接続
        String conDB = app.setupDB(conDBItem);
        
        // ドライバーの読み込み
        try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
        
        // update:SQLファイル
        String updateFile = args[1];
        String orderFlag = args[2];
        String inputFileDir = currentDir + "\\" + "sqlInput";
        Path updateFileDirPath = Paths.get(inputFileDir, updateFile);
        
        // update:対象パラメーター
        String updateFileDir = currentDir + "\\" + "sqlOutput";
    	Path targetFilePath = Paths.get(updateFileDir);
    	try (Stream<Path> paths = Files.walk(targetFilePath,1)) {
    		// サブディレクトリのファイル名を取得
    		List<String> fileList = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
    		
    		// 特定の文字列だけに絞る：Send、Receive
    		List<String> filteredList = fileList.stream()
                    .filter(file -> file.contains(orderFlag))
                    .filter(file -> file.contains(".json"))
                    .collect(Collectors.toList());
    		
    		// Sendファイルを取得する
    		for(String fileName : filteredList) {
    			Path updateParamPath = Paths.get(updateFileDir, fileName);
    			// 更新処理
    			app.updateDB(conDB, updateFileDirPath,updateParamPath);
    		}
    	} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }
    
    // 更新処理
    private void updateDB(String conDB, Path updateFileDirPath, Path updateParamPath) {
    	try (Connection con = DriverManager.getConnection(conDB);
				Statement stmt = con.createStatement();) {
    		System.out.println("DB接続に成功しました");
    		// update文
    		String sqlContent = Files.readString(updateFileDirPath, StandardCharsets.UTF_8);
    		
    		// updateパラメーター
    		String sqlParamContent = Files.readString(updateParamPath, StandardCharsets.UTF_8);
    		ObjectMapper objectMapper = new ObjectMapper();
    		Map<String, Object> map = objectMapper.readValue(sqlParamContent, Map.class);
    		Set<String> keys = map.keySet();
    		List<String> paramKey = new ArrayList<String>();
    		for (String key : keys) {
    			paramKey.add(key);
    		}
    		for(String param : paramKey) {
    			String paramItem = "#" + param;
    			String replaceItem = "'" + map.get(param) + "'";
    			sqlContent = sqlContent.replace(paramItem,replaceItem);
    		}
    		
    		// update実行
    		stmt.executeUpdate(sqlContent);
    		System.out.println("更新しました");
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	
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

}




