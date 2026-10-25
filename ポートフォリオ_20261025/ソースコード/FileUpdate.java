import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FileUpdate {
    public static void main(String[] args) {
        
    	// 第1:Send、Receiveフラグ
    	if (args == null || args.length != 1) {
    		System.out.println("引数:1まで");
    		return;
    	}
    	
        // csvファイルを読み込む
    	FileUpdate app = new FileUpdate();
    	String currentDir = System.getProperty("user.dir");
        String outputFileDir = currentDir + "\\" + "sqlOutput";
        File targetDir = new File(outputFileDir);
        Path fileFolderDir = Paths.get(targetDir.getPath());
        
        // サブディレクトリ
        String orderFlag = args[0];
        Path orderFilePath = null;
        if (Files.exists(fileFolderDir) && Files.isDirectory(fileFolderDir)) {
        	File[] files = targetDir.listFiles();
        	List<String> fileName = new ArrayList<String>();
        	if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                    	fileName.add(file.getName());
                    }
                }
            }
        	
        	// 呼び出し先の分岐
            if ("Send".equals(orderFlag)) {
            	for (int i=0;i<fileName.size();i++) {
            		int flagcnt = fileName.get(i).indexOf("Send");
            		if (flagcnt > 0) {
            			orderFilePath = Paths.get(outputFileDir, fileName.get(i));
            		}
            	}
            }
            if ("Receive".equals(orderFlag)) {
            	for (int i=0;i<fileName.size();i++) {
            		int flagcnt = fileName.get(i).indexOf("Receive");
            		if (flagcnt > 0) {
            			orderFilePath = Paths.get(outputFileDir, fileName.get(i));
            		}
            	}
            }
            
        }
        // 更新用
        List<Map<String,String>> updateItem = app.csvUpdate(outputFileDir, orderFilePath);
        
        // updateファイル出力
        Path writeFile = Paths.get(targetDir.getPath(), orderFlag);
        app.csvUpdateFolder(outputFileDir,writeFile,updateItem);
        System.out.println("正常終了しました");

    }
    
    // update出力
    private void csvUpdateFolder(String targetDirPath, Path writeFile,List<Map<String,String>> updateItem) {
    	File targetDir = new File(targetDirPath);
        Path fileFolderDir = Paths.get(targetDir.getPath());
        if (Files.exists(fileFolderDir) && Files.isDirectory(fileFolderDir)) {
        	
        	// テキスト出力
        	int jsonFileCnt = 1;
        	for (Map<String,String> item : updateItem) {
        		try {
        			// 顧客口座更新パラメーター
        			String jsonFileName = writeFile.toString() + "_" + jsonFileCnt + ".json";
        			File outputFile = new File(jsonFileName);
        			
					new ObjectMapper().writeValue(outputFile, item);
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        	
        }
        
    }
    
    // csvファイルを取得
    private List<Map<String,String>> csvUpdate(String targetDirPath, Path targetFile) {
    	File targetDir = new File(targetDirPath);
        Path fileFolderDir = Paths.get(targetDir.getPath());
        // 戻り値
        List<Map<String,String>> updateItem = null;
		if (Files.exists(fileFolderDir) && Files.isDirectory(fileFolderDir)) {
			try {
				List<String> lines = Files.readAllLines(targetFile);
				
				// ヘッダーカウント
				Map<Integer,String> bkHeader = new HashMap<Integer,String>();
				for(String item : lines) {
					String[] itemCol = item.split(",");
					for(int cntHeader = 0; cntHeader<itemCol.length;cntHeader++) {
						String headerName = itemCol[cntHeader];
						// 顧客ID
						if("customerId".equals(headerName)) {
							bkHeader.put(cntHeader, "customerId");
						}
						// 取引金額
						if("amount".equals(headerName)) {
							bkHeader.put(cntHeader, "amount");
						}
						// 手数料
						if("commission".equals(headerName)) {
							bkHeader.put(cntHeader, "commission");
						}
					}
					// ヘッダー取得で抜ける
					break;
				}
				
				// ボディ
				Map<String,String> keyMap = new HashMap<String,String>();
				updateItem = new ArrayList<Map<String,String>>();
				boolean roopFlag = false;
				for(String item : lines) {
					
					// ヘッダースキップ
					if (roopFlag == false) {
						roopFlag = true;
						continue;
					}
					String[] itemCol = item.split(",");
					
					for (int col=0;col<itemCol.length;col++) {
						
						if ("customerId".equals(bkHeader.get(col))) {
							keyMap.put("customerId", itemCol[col]);
						}
						if ("amount".equals(bkHeader.get(col))) {
							keyMap.put("amount", itemCol[col]);
						}
						if ("commission".equals(bkHeader.get(col))) {
							keyMap.put("commission", itemCol[col]);
						}
					}
					// 1レコード取得
					updateItem.add(keyMap);				
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return updateItem;
    }

}




