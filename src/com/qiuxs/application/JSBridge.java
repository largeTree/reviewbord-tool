package com.qiuxs.application;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

public class JSBridge implements Serializable {
	private static final long serialVersionUID = -3634465161789955883L;

	private static final String CONFIG_FILE = "./conf/config.json";

	private static final String NEW_CMD = "{prefix}\\depend\\RBTools\\RBTools\\Python27\\python.exe {prefix}\\depend\\RBTools\\RBTools\\Python27\\Scripts\\rbt-script.py post -p -d --summary={summary} --description={description} --target-group={group} --target-people={people} --repository={respository} --server={server} --username={username} --password={password} --svn-show-copies-as-adds=y";
	private static final String UPDATE_CMD = "{prefix}\\depend\\RBTools\\RBTools\\Python27\\python.exe {prefix}\\depend\\RBTools\\RBTools\\Python27\\Scripts\\rbt-script.py post -p -d --diff-only --review-request-id={requestId} --summary={summary} --description={description} --target-group={group} --target-people={people} --repository={respository} --server={server} --username={username} --password={password} --svn-show-copies-as-adds=y";
	
	private ExecutorService pool = Executors.newCachedThreadPool();
	
	private Stage stage;

	public JSBridge(Stage stage) {
		this.stage = stage;
	}

	/**
	 * 提交review
	 *  
	 * @author qiuxs  
	 * @param params
	 * @return
	 */
	public String submitReview(String params) {
		JSONObject jParams = JSON.parseObject(params);
		String cmd;

		if (jParams.getIntValue("reqType") == 0) {
			cmd = NEW_CMD;
		} else {
			cmd = UPDATE_CMD;
		}

		for (String key : jParams.keySet()) {
			 try {
				 cmd = cmd.replace("{" + key + "}", new String(jParams.getString(key).getBytes("UTF-8"), "GBK"));
//			cmd = cmd.replace("{" + key + "}", jParams.getString(key));
			 } catch (UnsupportedEncodingException e) {
				 e.printStackTrace(System.err);
			 }
		}

		JSONObject config = JSON.parseObject(this.loadConfig());
		cmd = cmd.replace("{server}", config.getString("serverUrl")).replace("{username}", config.getString("userName")).replace("{password}",
				config.getString("password"));

		// 设置命令为绝对路径
		try {
			String currentDir = new java.io.File(".").getCanonicalPath();
			cmd = cmd.replace("{prefix}", currentDir);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		
		String directory = jParams.getString("directory");
		
		this.saveCmdLine(cmd);
		
		// 发起review请求
		Pair<String, Boolean> res = this.exec(directory, cmd);
		
		// 记住上次目录
		config.put("directory", directory);
		this.saveConfig(config.toJSONString());
		
		String msg = res.getKey();
		if (res.getValue()) {
			String desc = this.makeResStr(msg, jParams.getString("summary"), config.getString("serverUrl"), jParams.getString("people"));
			return desc;
		} else {
			return "提交失败：\n" + msg;
		}
	}
	
	private void saveCmdLine(String cmd) {
		File file = new File("./logs/cmd.txt");
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(file), "GB2312");
			writer.append(cmd);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	private Pattern pattern = Pattern.compile("#[0-9]{4,10}");
	private Pattern urlPatt = Pattern.compile("http://172.81.210.34/r/[0-9]{4,10}/");
	
	private String makeResStr(String res, String summary, String serverUrl, String people) {
		Matcher requestIdMatcher = pattern.matcher(res);
		String requestId = null;
		if (requestIdMatcher.find()) {
			requestId = requestIdMatcher.group();
		}
		String reviewUrl = null;
		Matcher urlPattMatcher = urlPatt.matcher(res);
		if (urlPattMatcher.find()) {
			reviewUrl = urlPattMatcher.group();
		}
		StringBuilder desc = new StringBuilder();
		desc.append("review:").append(requestId.substring(1)).append("\n")
		// .append("reviewUrl:").append(serverUrl).append("r").append(requestId).append("\n")
		.append("reviewUrl:").append(reviewUrl).append("\n")
		.append("reviewer:").append(people).append("\n")
		.append("提交日志:").append(summary);
		return desc.toString();
	}

	private Pair<String, Boolean> exec(String dir, String cmd) {
		try {
			Runtime runtime = Runtime.getRuntime();
			// 执行reviewRequest
			Process exec = runtime.exec(cmd, null, new File(dir));
			// Process exec = new ProcessBuilder("cmd /c start " + cmd).redirectErrorStream(true).directory(new File(dir)).start();
			Future<String> errFuture = this.readStreamAsync(exec.getErrorStream());
			Future<String> resFuture = this.readStreamAsync(exec.getInputStream());

			String res = resFuture.get();
			System.out.println(res);
			if (res != null && res.length() > 0) {
				return new Pair<String, Boolean>(res, true);
			}
			String err = errFuture.get();
			System.err.println(err);
			return new Pair<String, Boolean>(err, false);
		} catch (Exception e) {
			return new Pair<String, Boolean>(e.getLocalizedMessage(), false);
		}

	}
	
	private Future<String> readStreamAsync(InputStream errorStream) {
		return pool.submit(() -> getString(errorStream));
	}

	private String getString(InputStream is) {
		ByteArrayOutputStream bos = null;
		try {
			bos = new ByteArrayOutputStream();
			byte[] buffer = new byte[2048];
			int length = 0;
			while ((length = is.read(buffer)) != -1) {
				bos.write(buffer, 0, length);
			}
			String res = new String(bos.toByteArray());
			return res;
		} catch (IOException e) {
			e.printStackTrace(System.err);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
			}
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		return "";
	}

	public static void main(String[] args) {
		JSBridge bridge = new JSBridge(null);
		bridge.makeResStr("Review request #4743 posted.\r\n" + 
				"\r\n" + 
				"http://172.81.210.34/r/4743/\r\n" + 
				"http://172.81.210.34/r/4743/diff/", "中文测试", "http://172.81.210.34/", "cyz");
	}

	public String saveConfig(String config) {
		File cfgFile = new File(CONFIG_FILE);
		if (!cfgFile.exists()) {
			try {
				cfgFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace(System.err);
				return e.getLocalizedMessage();
			}
		}
		FileWriter fw = null;
		try {
			fw = new FileWriter(cfgFile);
			fw.write(config);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return e.getLocalizedMessage();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		return null;
	}

	public String loadConfig() {
		File confDir = new File("./conf");
		if (!confDir.exists()) {
			confDir.mkdirs();
		}
		File file = new File(CONFIG_FILE);
		if (!file.exists()) {
			return "{}";
		}
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(file);
			br = new BufferedReader(fr);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} catch (Exception e) {
			return "{\"error\":\"" + e.getLocalizedMessage() + "\"}";
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String selectDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File file = directoryChooser.showDialog(stage);
		if (file == null) {
			return "";
		}
		String path = file.getPath();
		return path;
	}

}
