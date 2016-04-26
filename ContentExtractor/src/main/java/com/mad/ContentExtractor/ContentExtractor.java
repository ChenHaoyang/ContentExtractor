package com.mad.ContentExtractor;

import java.io.*;
//import java.net.URL;
import java.nio.charset.Charset;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
//import org.mozilla.intl.chardet.nsDetector;
//import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
//import org.mozilla.intl.chardet.nsPSMDetector;

public class ContentExtractor {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		long time = System.currentTimeMillis();
		String line = null;
		String[] tokens;
		String[] result;
		BufferedReader br = null;
		BufferedWriter bw = null;
		TextExtract te = new TextExtract();
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream("/home/charles/Data/input/url.csv")));
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/charles/Data/output/chen.txt")));
			bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<data>");
			line = br.readLine();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-1);
		}

		while (line != null) {
			tokens = line.split(",");
			try {
				//System.out.println(tokens[0]);
				result = new ContentExtractor().getHTML(tokens[1].trim());
				//System.out.println("got html");
				String main_text = te.parse(tokens[0], result[3]);
				bw.write("\n<document id=\"" + tokens[0] + "\" url=\"" + tokens[1] + "\">\n");
				bw.write("<title>" + result[0] + "</title>\n");
				bw.write("<description>" + result[1] + "</description>\n");
				bw.write("<keywords>" + result[2] + "</keywords>\n");
				bw.write("<main>" + main_text + "</main>\n</document>\n");
				bw.flush();
				line = br.readLine();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				line = br.readLine();
				System.out.println(tokens[0]);
				//System.out.println(e.toString());
				continue;
				// e.printStackTrace();
			}
		}
		bw.write("</data>");
		br.close();
		bw.close();
		System.out.println("Run Time: " + (System.currentTimeMillis()-time)/1000 + "s");
	}

	public String[] getHTML(String strURL) throws IOException {
		String[] result = new String[4];
		//Entities.EscapeMode.base.getMap().clear();
		Document doc = Jsoup.connect(strURL)
				.timeout(5000)
				.get();

		String html = doc.outerHtml();
		//System.out.print(html);
		Charset old = doc.charset();
		//System.out.println(html);
		if(old.name() != "UTF-8")
			html = changeCharset(html, old.name(), "UTF-8");
		
		result[0] = doc.title();
		result[1] = doc.select("meta[name=\"description\"]").attr("content");
		result[2] = doc.select("meta[name=\"keywords\"]").attr("content");
		result[3] = html;
		
		return result;
	}
	
	private String changeCharset(String scr, String oldCharset, String newCharset)  throws UnsupportedEncodingException {
		if(scr != null && newCharset != null)
		{
			byte[] bs = null;
			if(oldCharset != null){
				bs = scr.getBytes(newCharset);
			}
			else
				bs = scr.getBytes();
			return new String(bs, newCharset);
		}
		return null;
	}
	
}
