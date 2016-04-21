package com.mad.ContentExtractor;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.*;

import org.apache.commons.lang.StringEscapeUtils;;


public class TextExtract {
	
	private List<String> lines;
	private final static int blocksWidth=3;
	private int threshold;
	private String html;
	private static boolean flag;
	private int start;
	private int end, max_lines;
	private double main_ratio, stop_ratio;
	private StringBuilder text;
	private ArrayList<Integer> indexDistribution;
	
	public TextExtract() {
		lines = new ArrayList<String>();
		indexDistribution = new ArrayList<Integer>();
		text = new StringBuilder();
		flag = true;
		threshold	= 60;  
		main_ratio = 0.8;
		stop_ratio = 0.7;
		max_lines = 100;
	}
	
	public String parse(String url_id, String _html) {
		return parse(url_id, _html, flag);
	}
	

	public String parse(String url_id, String _html, boolean _flag) {
		flag = _flag;
		html = _html;
		html = preProcess(url_id, html);
//		System.out.println(html);
		return getText();
	}
	//private static int FREQUENT_URL = 30;
	//private static Pattern links = Pattern.compile("<[aA]\\s+[Hh][Rr][Ee][Ff]=[\"|\']?([^>\"\' ]+)[\"|\']?\\s*[^>]*>([^>]+)</a>(\\s*.{0,"+FREQUENT_URL+"}\\s*<a\\s+href=[\"|\']?([^>\"\' ]+)[\"|\']?\\s*[^>]*>([^>]+)</[aA]>){2,100}", Pattern.DOTALL);
	private static String preProcess(String url_id, String source) {
		source = StringEscapeUtils.unescapeHtml(source);
		//source = source.replaceAll("(?is)<!DOCTYPE.*?>", "");
		//source = source.replaceAll("(?is)<!--.*?-->", "");				// remove html comment
		//source = source.replaceAll("(?is)<(script|style|title|span|select|noscript).*?>.*?</(script|style|title|span|select|noscript)>", "");
		source = source.replaceAll("(?is)(<!DOCTYPE.*?>|<!--.*?-->|<script.*?>.*?</script>|<style.*?>.*?</style>|<title.*?>.*?</title>|<select.*?>.*?</select>|<noscript.*?>.*?</noscript>)", "");
		//source = source.replaceAll("(?is)<script.*?>.*?</script>", "");
		//source = source.replaceAll("(?is)<style.*?>.*?</style>", "");
		//source = source.replaceAll("(?is)<title.*?>.*?</title>", "");
		//source = source.replaceAll("(?is)<span.*?>.*?</span>", "");
		//source = source.replaceAll("(?is)<select.*?>.*?</select>", "");
		//source = source.replaceAll("(?is)<noscript.*?>.*?</noscript>", "");
		if(flag)
			source = source.replaceAll("(?is)<a.*?>.*?</a>", "");
		//System.out.println(source);
		int len = source.length();
//		while ((source = links.matcher(source).replaceAll("")).length() != len)
//		{
//			len = source.length();
//		}
			//continue;
		
		//source = links.matcher(source).replaceAll("");
		//System.out.println(source);
		while(Pattern.compile("(<br>[\\s]*?){2}").matcher(source).find())
			source = source.replaceAll("(<br>[\\s]*?){2}", "<br>");
		source = source.replaceAll("(<br>|\r\n)", "\n");
		source = source.replaceAll("<[^>]*>", "");

//		try {
//			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/charles/Data/output/source/" + url_id + ".txt")));
//			bw.write(source);
//			bw.close();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		//System.out.println(source);
		return source;
	
	}
	
	private int calThreshold(ArrayList<Integer> data, int n){
		if(data == null || n <=0) return -1;

		int sum=0;
		int len = data.size();
		for(int i=1; i<=n; i++){
			sum += data.get(len-i);
		}
		
		return sum / n;
	}
	
	private String getText() {
		ArrayList<Integer> tmp = new ArrayList<Integer>();
		lines = Arrays.asList(html.split("\n"));
		
		int line_count=0;
		int parse_max_line = (int)Math.ceil(lines.size()*main_ratio);
		int parse_stop_line = (int)Math.ceil(lines.size()*stop_ratio);
		//lines = lines.subList(0, parse_max_line);
		indexDistribution.clear();

		for (int i = 0; i < lines.size() - blocksWidth; i++) {
			int wordsNum = 0;
			for (int j = i; j < i + blocksWidth; j++) { 
				lines.set(j, lines.get(j).replaceAll("\\s+", " ").trim());
				wordsNum += lines.get(j).length();
			}
			wordsNum = wordsNum / blocksWidth;
			indexDistribution.add(wordsNum);
			
			if(i < parse_max_line)
				tmp.add(lines.get(i).length());
			//System.out.println(wordsNum);
		}
		//System.out.println("lines: " + lines.size());
		Collections.sort(tmp);
		int avr_top_n = calThreshold(tmp, 4);

		threshold = avr_top_n / blocksWidth;
		//System.out.println(threshold);
		
		start = -1; end = -1;
		boolean boolstart = false, boolend = false, first_read = false;
		text.setLength(0);
		
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < indexDistribution.size() - 1; i++) {
			if (indexDistribution.get(i) >= threshold && ! boolstart) {
				if (indexDistribution.get(i+1).intValue() != 0 
					|| indexDistribution.get(i+2).intValue() != 0
					|| indexDistribution.get(i+3).intValue() != 0) {
					boolstart = true;
					start = i;
					//System.out.println("start: " + start);
					continue;
				}
			}
			if (boolstart) {
				if (indexDistribution.get(i).intValue() < 5 ) {
					end = i;
					//System.out.println("end: " + end);
					boolend = true;
				}
			}
		
			if (boolend) {
				if(first_read && (start>parse_stop_line))
					break;
				buffer.setLength(0);
				//System.out.println(start+1 + "\t\t" + end+1);
				for (int ii = start; ii < end; ii++) {
					if (lines.get(ii).length() < 5) continue;
					if(line_count > max_lines)
						break;
					else
						line_count++;
					buffer.append(lines.get(ii) + "\n");
				}
				String str = buffer.toString();
				//System.out.println(str);
				if (str.contains("Copyright")) continue; 
				text.append(str);
				boolstart = boolend = false;
				first_read = true;
			}
		}
		
		if (start >= end && !(first_read && (start>parse_stop_line)))
		{
			buffer.setLength(0);
			int size_1 = lines.size()-1;
			for (int ii = start; ii <= size_1; ii++) {
				if (lines.get(ii).length() < 5) continue;
				if(line_count > max_lines)
					break;
				else
					line_count++;
				buffer.append(lines.get(ii) + "\n");
			}
			String str = buffer.toString();
			//System.out.println(str);
			if ((!str.contains("Copyright"))) 
			{	
				text.append(str);
			}
		}
		
		return text.toString();
	}
/*	
	public static void main(String[] args)
	{
		System.out.println("===============");
		String s = "<img  class='fit-image' onload='javascript:if(this.width>498)this.width=498;' />hello";
		//source = source.replaceAll("<[^'\"]*['\"].*['\"].*?>", "");
System.out.println(TextExtract.preProcess(s));
	}
*/
}