package com.mad.ContentExtractor;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.*;

import org.apache.commons.lang.StringEscapeUtils;;


public class TextExtract {
	
	private List<String> m_lines;
	private static int m_blocksWidth=4;
	private static int m_minTokens = 5;
	private long m_threshold;
	private String m_html;
	private int m_maxLines;
	private double m_mainRatio;
	private StringBuilder m_text;
	private ArrayList<Integer> m_indexDistribution;
	
	public TextExtract() {
		m_lines = new ArrayList<String>();
		m_indexDistribution = new ArrayList<Integer>();
		m_text = new StringBuilder();
		m_mainRatio = 0.8;
		m_maxLines = 100;
	}
	
	public TextExtract(int blocks_width, int min_tokens, double main_ratio, int max_lines){
		m_lines = new ArrayList<String>();
		m_indexDistribution = new ArrayList<Integer>();
		m_text = new StringBuilder();
		m_blocksWidth = blocks_width;
		m_minTokens = min_tokens;
		m_mainRatio = main_ratio;
		m_maxLines = max_lines;
	}

	public String parse(String _html) {
		if(_html==null) return "";
		m_html = _html;
		m_html = preProcess(m_html.replaceAll("[\b\t\r\n\f]", ""));
		String tmp = m_html;
		if(!tmp.replaceAll("[\b\t\r\n\f\\s]", "").equals(""))
			return getText();
		else
			return "";
	}
	//private static int FREQUENT_URL = 30;
	//private static Pattern links = Pattern.compile("<[aA]\\s+[Hh][Rr][Ee][Ff]=[\"|\']?([^>\"\' ]+)[\"|\']?\\s*[^>]*>([^>]+)</a>(\\s*.{0,"+FREQUENT_URL+"}\\s*<a\\s+href=[\"|\']?([^>\"\' ]+)[\"|\']?\\s*[^>]*>([^>]+)</[aA]>){2,100}", Pattern.DOTALL);
	private static Pattern main_rule = Pattern.compile("(<!DOCTYPE.*?>|<!--.*?-->|<meta.*?>|<link.*?>)",//|<!DOCTYPE.*?>|<!--.*?-->|<script.*?>.*?</script>|<style.*?>.*?</style>|"
			//+ "<title.*?>.*?</title>|<select.*?>.*?</select>|<noscript.*?>.*?</noscript>|"
			//+ "<link.*?>|<li.*?>.*?</li>|<ul.*?>.*?</ul>|<ol.*?>.*?</ol>|<dl.*?>.*?</dl>|"
			//+ "<[^>]*?(display:none|visible:hidden){1}.*?>.*?</.*?>|"
			//+ "<div[^>]*?(footer|header|links){1}.*?>(<div.*?>.*?</div>)+</div>|"
			//+ "<div[^>]*?(footer|header|links){1}.*?>.*?</div>|"
			//+ "<p[^>]*?(footer|header|links){1}.*?>(<p.*?>.*?</p>)+</p>|"
			//+ "<p[^>]*?(footer|header|links){1}.*?>.*?</p>)",  
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	private static Pattern sub_rule_01 = Pattern.compile("(<br[^<]*?>[ \b\t\n\f\r　]*){2}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static Pattern sub_rule_02 = Pattern.compile("(<br[^<]*?>|\r\n)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	//private static Pattern sub_rule_03 = Pattern.compile("<([^>\"]*[\"].*?[\"])+.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static Pattern sub_rule_04 = Pattern.compile("<.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static Pattern sub_rule_05 = Pattern.compile("(</p>|</tr>|<li.*?>|<dd.*?>|<dt.*?>|<div.*?>|<ol.*?>|<ul.*?>|<dl.*?>|<table.*?>|<section.*?>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static Pattern sub_rule_06 = Pattern.compile("(</ol>|</ul>|</dl>|</div>|</table>|</section>)", 
															Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	//private static Pattern sub_rule_06 = Pattern.compile("<a[ ]+.*?>.{12,}?</a>", Pattern.DOTALL | Pattern.UNICODE_CASE);
	//private static Pattern sub_rule_03 = Pattern.compile("<[^>]*['\"].*['\"].*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);


	private static String preProcess(String source) {
		String n_str="";
		//System.out.println(source);
		//source = "<div id=\"header_ad\"><script></script><div></div></div><div></div>";
		source = main_rule.matcher(source).replaceAll("");
		//System.out.println(source);
		//source = source.replaceAll("<a [^>]*?>[^<]{11,}?</a>", "");
		//System.out.println(source);
		while(sub_rule_01.matcher(source).find())
			//source = source.replaceAll("(<br>[\\s]*?){2}", "<br>");
			source = sub_rule_01.matcher(source).replaceAll("<br>");
		//System.out.println(source);
		//System.out.println(source);
		//source = source.replaceAll("(<br>|\r\n)", "\n");
		//source = source.replaceAll("(?is)<.*?>", "");
		source = sub_rule_02.matcher(source).replaceAll("\n");
		//System.out.println(source);
		source = sub_rule_05.matcher(source).replaceAll("\n");
		for(int i=1; i<m_blocksWidth;i++)
			n_str += "\n";
		source = sub_rule_06.matcher(source).replaceAll(n_str);
		//System.out.println(source);
		source = sub_rule_04.matcher(source).replaceAll("");
		//System.out.println(source);
		source = StringEscapeUtils.unescapeHtml(source);

		//System.out.println(source);
		for(int i=0; i < m_blocksWidth-1; i++)
			source = source + "\n";
		return source;
	
	}
	private long calThreshold(ArrayList<Long> data) throws IOException{
		if(data == null || data.size()==0)
			return -1;
		long max_f2=0;
		int max_f2_idx=0;
		int list_size = data.size();
		
		Collections.sort(data);

		switch(list_size){
		case 1:
			return Math.max(m_minTokens, data.get(0));
		case 2:
			return Math.max(m_minTokens, data.get(1));
		case 3:
			
		}
		
		for(int i=1; i<list_size-1; i++){
			long f2=0;
			
//			if(i==0)
//				f2 = (data.get(i+2)-2*data.get(i+1)+data.get(i));
//			else if(i==list_size-1)
//				f2 = (data.get(i)-2*data.get(i-1)+data.get(i-2));
//			else
			f2 = (data.get(i-1)-2*data.get(i)+data.get(i+1));
			//if the point of list_size-2 got the max value, then we take the last point.(for safety reason)
			if(f2 > max_f2){
				max_f2 = f2;
				max_f2_idx = i;
			}
		}
		
		if(max_f2_idx == list_size-2 || list_size == 3)
			max_f2_idx = list_size - 1;
		
		return Math.max(m_minTokens, data.get(max_f2_idx));
	}
//	private int calThreshold(ArrayList<Integer> data){
//		double min_dbi = Double.MAX_VALUE;
//		int min_dbi_idx=0;
//		int list_size = data.size();
//		
//		Collections.sort(data);
//		
//		for(int i=0; i<list_size-1; i++){
//			double mean_left=0, mean_right=0, dbi=0;
//			double intra_dist_left=0, intra_dist_right=0, inter_dist=0;
//			for(int j=0; j<=i; j++){
//				mean_left += data.get(j);
//			}
//			mean_left /= (i+1);
//			for(int j=0; j<=i; j++){
//				intra_dist_left += Math.abs(data.get(j) - mean_left);
//			}
//			intra_dist_left /= (i+1);
//			for(int k=i+1; k<list_size; k++){
//				mean_right += data.get(k);
//			}
//			mean_right /= (list_size-i-1);
//			for(int k=i+1; k<list_size; k++){
//				intra_dist_right += Math.abs(data.get(k) - mean_right);
//			}
//			intra_dist_right /= (list_size-i-1);
//			inter_dist = mean_right - mean_left;
//			dbi = (intra_dist_left + intra_dist_right) / inter_dist;
//			if(dbi < min_dbi){
//				min_dbi = dbi;
//				min_dbi_idx = i;
//			}
//		}
//		return data.get(min_dbi_idx);
//	}
	private int calThreshold(ArrayList<Integer> data, double total_sum){
		int list_size = data.size();
		double min_otsu=Double.MAX_VALUE;
		int min_otsu_idx=0;
		int left_sum=0;
		Collections.sort(data);
		
		for(int i=0; i<list_size-1; i++){
			left_sum += data.get(i);
			double left_mean=0, right_mean=0, left_var=0, right_var=0, left_weight=0,otsu=0;
			for(int j=0; j<=i; j++){
				left_mean += (j * data.get(j) / left_sum);
			}
			for(int j=0; j<=i; j++){
				left_var += (Math.pow(j-left_mean, 2) * data.get(j) / left_sum);
			}
			//left_avg = left_avg / (i+1);
			for(int k=i+1; k<list_size; k++){
				right_mean += (k * data.get(k) / (total_sum - left_sum));
			}
			for(int k=i+1; k<list_size; k++){
				right_var += (Math.pow(k-right_mean, 2) * data.get(k) / (total_sum - left_sum));
			}
			left_weight = left_sum / total_sum;
			otsu = left_weight * left_var + (1-left_weight) * right_var;
			if(otsu < min_otsu){
				min_otsu = otsu;
				min_otsu_idx = i;
			}
		}
		return data.get(min_otsu_idx);
	}
	private int calThreshold(ArrayList<Integer> data, int n){
		if(data == null || n <=0) return -1;
		if(data.size()<n) n=data.size();
		int sum=0;
		int len = data.size();
		for(int i=1; i<=n; i++){
			sum += data.get(len-i);
		}
		
		return Math.max(1,sum / (n*m_blocksWidth));
	}
	
	private int getCharacterNum(String str){
		int count=0;
		Pattern p = Pattern.compile("[\\w\u4E00-\u9FFF\u3040-\u309F\u30A0-\u30FF]");
		Matcher m = p.matcher(str);
		
		while(m.find()) count++;
		
		return count;
	}
	
	private String getText() {
		int start, end;
		ArrayList<Long> list_for_sort = new ArrayList<Long>();//for sort
		ArrayList<ArrayList> block_list = new ArrayList();
		m_lines = Arrays.asList(m_html.split("\n",-1));

		m_text.setLength(0);
		
		int line_count=0;
		//int max_line_tokens=0;
		int parse_max_line = (int)Math.ceil(m_lines.size()*m_mainRatio);
		//int parse_stop_line=0;
		String[] tokens = new String[m_lines.size()];

		m_indexDistribution.clear();
		
		for(int i=m_lines.size()-1; i>(m_lines.size() - m_blocksWidth); i--){
			tokens[i] = "";
		}
		try{
			for (int i = 0; i < m_lines.size() - m_blocksWidth + 1; i++) {
				int wordsNum = 0;
				for (int j = i; j < i + m_blocksWidth; j++) { 
					//lines.set(j, lines.get(j).trim());
					tokens[j] = m_lines.get(j).replaceAll("[\\s ]", "");
					tokens[j] = tokens[j].replaceAll("[^\\w\uFF10-\uFF19\uFF21-\uFF3A\uFF41-\uFF5A\u4E00-\u9FFF\u3040-\u309F\u30A0-\u30FF]", "");
					wordsNum += tokens[j].length();
					//cal_flag = false;
				}
				
				wordsNum = (int)Math.round((double)wordsNum / (double)m_blocksWidth);
				m_indexDistribution.add(wordsNum);
				//list_for_sort.add(wordsNum);
				
				//find the longest text within the main_ratio area
//				if(i < parse_max_line && wordsNum > max_line_tokens){
//					parse_stop_line = i;
//					max_line_tokens = wordsNum;
//				}
		
				//System.out.println(wordsNum);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		//System.out.println("lines: " + lines.size());
		//Collections.sort(list_for_sort);
		//int avr_top_n = calThreshold(tmp, 3);

		//threshold = avr_top_n / blocksWidth;
		//threshold = calThreshold(list_for_sort, 3);
		//threshold = (int)Math.ceil(delay_ratio * (double)max_line_tokens);
		//System.out.println(threshold);
		
		start = -1; end = -1;
		boolean boolstart = false, boolend = false, first_read = false;
		StringBuilder buffer = new StringBuilder();
		int line_number = m_indexDistribution.size();
		long block_max_tokens = 0;
		long block_token_sum = 0;
		
		try{
			for(int i = 0; i < line_number ; i++) {
				if (tokens[i].length() > 0 && ! boolstart) {
					boolstart = true;
					start = i;
					block_token_sum += tokens[i].length();
					if(m_indexDistribution.get(i) > block_max_tokens)
						block_max_tokens = m_indexDistribution.get(i);
					//System.out.println("start: " + start);
					continue;
				}
				if (boolstart) {
					block_token_sum += tokens[i].length();
					if(m_indexDistribution.get(i) > block_max_tokens)
						block_max_tokens = m_indexDistribution.get(i);
					if (m_indexDistribution.get(i) == 0) {
						end = i;
						ArrayList tmp_list = new ArrayList();
						tmp_list.add(start);
						tmp_list.add(end);
						tmp_list.add(block_max_tokens);
						tmp_list.add(block_token_sum);
						block_list.add(tmp_list);
						list_for_sort.add(block_max_tokens);
						//list_for_sort.add(block_token_sum);
						//total_sum += block_max_tokens;
						//System.out.print(block_max_tokens + "\n");
						block_max_tokens = 0;
						block_token_sum = 0;
						//System.out.println("end: " + end);
						boolstart = false;
					}
				}
			
	//			if (boolend) {
	//				if(first_read && (start>parse_stop_line))
	//					break;
	//				buffer.setLength(0);
	//				//System.out.println(start+1 + "\t\t" + end+1);
	//				for (int ii = start; ii < end; ii++) {
	//					String txt = tokens[ii];
	//					if (txt.length() == 0) continue;
	//					if(line_count > max_lines)
	//						break;
	//					else
	//						line_count++;
	//					if(!txt.matches(".*(記事一覧|利用規約)+.*"))
	//						buffer.append(lines.get(ii).trim() + "\n");
	//				}
	//				String str = buffer.toString();
	//				//System.out.println(str);
	//				if (str.contains("Copyright")) continue; 
	//				text.append(str);
	//				boolstart = boolend = false;
	//				first_read = true;
	//			}
			}
			
			if(start > end){
				ArrayList tmp_list = new ArrayList();
				tmp_list.add(start);
				tmp_list.add(line_number);
				tmp_list.add(block_max_tokens);
				tmp_list.add(block_token_sum);
				block_list.add(tmp_list);
				list_for_sort.add(block_max_tokens);
				//list_for_sort.add(block_token_sum);
				//total_sum += block_max_tokens;
			}
			m_threshold = calThreshold(list_for_sort);
			//start to choose blocks
			for(ArrayList block:block_list){
				long b_max_tokens = (Long)block.get(2);
				long b_token_sum = (Long)block.get(3);
				
				if(b_max_tokens >= m_threshold){
					int b_start = (Integer)block.get(0);
					int b_end = (Integer)block.get(1);
					
					if(b_start >= parse_max_line && b_token_sum < 200 )
						continue;
					
					buffer.setLength(0);
					//System.out.println(start+1 + "\t\t" + end+1);
					for (int ii = b_start; ii < b_end; ii++) {
						String txt = tokens[ii];
						if (txt.length() == 0) continue;
						if(line_count > m_maxLines)
							break;
						if(!txt.matches(".*(記事一覧|利用規約|Copyright)+.*")){
							buffer.append(m_lines.get(ii).trim() + "\n");
							line_count++;
						}
					}
					String str = buffer.toString();
					m_text.append(str);				
				}
			}
		}
		catch(Exception e){
			System.out.println(e.toString());
		}
		//for the final iteration
//		if (start > end && !(first_read && (start>parse_stop_line)))
//		{
//			buffer.setLength(0);
//			int size_1 = lines.size()-1;
//			for (int ii = start; ii <= size_1; ii++) {
//				String txt = tokens[ii];
//				if (txt.length() == 0) continue;
//				if(line_count > max_lines)
//					break;
//				else
//					line_count++;
//				if(!txt.matches(".*(記事一覧|利用規約)+.*"))
//					buffer.append(lines.get(ii).trim() + "\n");
//			}
//			String str = buffer.toString();
//			//System.out.println(str);
//			if ((!str.contains("Copyright"))) 
//			{	
//				text.append(str);
//			}
//		}
		
		//if(text.length() == 0)
		//	return lines.get(parse_stop_line);
		return m_text.toString();
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