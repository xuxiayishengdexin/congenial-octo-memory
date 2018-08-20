package com.cas.graph.mining.util;

import java.io.*;

public class TextUtil {

	public static StringBuffer readWithFileReader(String fileName) {
		StringBuffer sb = new StringBuffer();
		FileReader fr;
		try {
			fr = new FileReader(fileName);
			int ch = 0;
			while ((ch = fr.read()) != -1) {
				sb.append((char) ch);
			}
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sb;
	}

	public static StringBuffer readWithStreamReader(String fileName) {
		return readWithStreamReader(fileName, "utf-8");
	}

	public static StringBuffer readWithStreamReader(String fileName, String charsetName) {
		if (charsetName != null && charsetName.length() <= 0)
			return null;

		if (fileName != null && fileName.length() <= 0)
			return null;

		StringBuffer sb = new StringBuffer();
		InputStream fis = null;
		char[] cbuf = new char[8192];
		try {
			fis = new FileInputStream(fileName);

			InputStreamReader isr = new InputStreamReader(fis, charsetName);

			int len = 0;
			while ((len = isr.read(cbuf)) != -1)
				sb.append(cbuf, 0, len);

			fis.close();
		} catch (FileNotFoundException e) {
			System.out.println("文件没有找到");
		} catch (IOException e) {
			System.out.println("读取失败");
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		return sb;
	}

	public static StringBuffer readWithBufferedReader(String path) {
		if (path == null || path.length() <= 0) {
			return null;
		}
		StringBuffer rs = new StringBuffer();
		try {
			FileReader read = new FileReader(path);
			BufferedReader br = new BufferedReader(read);
			String row = null;
			while ((row = br.readLine()) != null) {
				rs.append(row + "\n");
			br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rs;
	}

	public static void write(String path, String content, boolean append) {
		if (path == null || path.length() <= 0) {
			return;
		}
		if (content == null || content.length() <= 0) {
			return;
		}
		FileWriter fw;
		try {
			fw = new FileWriter(path, append);
			fw.write(content, 0, content.length());
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void write(String path, String content, String charsetName) {
		if (path == null)
			return;
		if (content == null)
			return;

		if (charsetName == null || charsetName.length() <= 0)
			charsetName = "utf-8";

		try {
			OutputStream out = new FileOutputStream(path);
			OutputStreamWriter outw = new OutputStreamWriter(out, charsetName);
			outw.write(content);
			outw.flush();
			outw.close();
			out.close();

		} catch (FileNotFoundException e) {
			System.out.println("文件没有找到");
		} catch (IOException e) {
			System.out.println("读取失败");
		}
	}

	public static void write(String path, String content) {
		if (path == null || path.length() <= 0) {
			return;
		}
		if (content == null || content.length() <= 0) {
			return;
		}
		FileWriter fw;
		try {
			fw = new FileWriter(path);
			fw.write(content, 0, content.length());
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		write("D:/text.txt", "1");
		
	}
}

