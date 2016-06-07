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
	private final static int blocksWidth=4;
	private final static int min_tokens = 5;
	private long threshold;
	private String html;
	private static boolean flag;
	private int start;
	private int end, max_lines;
	private double main_ratio;
	private StringBuilder text;
	private ArrayList<Integer> indexDistribution;
	private String m_url_id;
	private BufferedWriter m_bw, m_bw_block, m_bw_f2;
	
	public TextExtract() {
		lines = new ArrayList<String>();
		indexDistribution = new ArrayList<Integer>();
		text = new StringBuilder();
		flag = true;
		threshold	= 60;  
		main_ratio = 0.8;
		//delay_ratio = 0.5;
		max_lines = 100;
		try {
			m_bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/charles/Data/output/source/lines.txt")));
			m_bw_block = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/charles/Data/output/source/block.txt")));
			m_bw_f2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/charles/Data/output/source/f2.txt")));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String parse(String url_id, String _html) {
		return parse(url_id, _html, flag);
	}

	public String parse(String url_id, String _html, boolean _flag) {
		flag = _flag;
		html = _html;
		m_url_id = url_id;
		html = preProcess(url_id, html.replaceAll("[\b\t\r\n\f]", ""));
		String tmp = html;
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


	private static String preProcess(String url_id, String source) {
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
		for(int i=1; i<blocksWidth;i++)
			n_str += "\n";
		source = sub_rule_06.matcher(source).replaceAll(n_str);
		//System.out.println(source);
		source = sub_rule_04.matcher(source).replaceAll("");
		//System.out.println(source);
		source = StringEscapeUtils.unescapeHtml(source);

		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/charles/Data/output/source/" + url_id + ".txt")));
			bw.write(source);
			bw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(source);
		for(int i=0; i < blocksWidth-1; i++)
			source = source + "\n";
		return source;
	
	}
	private long calThreshold(ArrayList<Long> data) throws IOException{
		if(data == null || data.size()==0)
			return -1;
		long max_f2=0;
		int max_f2_idx=0;
		int list_size = data.size();
		
		m_bw_f2.write(m_url_id + ",");
		m_bw_block.write(m_url_id + ",");
		for(int i=0; i < list_size; i++){
			m_bw_block.write(data.get(i) + ",");
		}

		Collections.sort(data);

		switch(list_size){
		case 1:
			m_bw_f2.write("\n");
			m_bw_block.write("\n");
			m_bw_f2.flush();
			m_bw_block.flush();
			return Math.max(min_tokens, data.get(0));
		case 2:
			m_bw_f2.write("\n");
			m_bw_block.write("\n");
			m_bw_f2.flush();
			m_bw_block.flush();
			return Math.max(min_tokens, data.get(1));
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
			if(i==1 || i==list_size-2)
				m_bw_f2.write(f2 + "," + f2 + ",");
			else
				m_bw_f2.write(f2 + ",");
			//if the point of list_size-2 got the max value, then we take the last point.(for safety reason)
			if(f2 > max_f2){
				max_f2 = f2;
				max_f2_idx = i;
			}
		}
		
		if(max_f2_idx == list_size-2 || list_size == 3)
			max_f2_idx = list_size - 1;
		
		m_bw_f2.write("\n");
		m_bw_block.write("\n");
		m_bw_f2.flush();
		m_bw_block.flush();
		return Math.max(min_tokens, data.get(max_f2_idx));
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
		
		return Math.max(1,sum / (n*blocksWidth));
	}
	
	private int getCharacterNum(String str){
		int count=0;
		Pattern p = Pattern.compile("[\\w\u4E00-\u9FFF\u3040-\u309F\u30A0-\u30FF]");
		Matcher m = p.matcher(str);
		
		while(m.find()) count++;
		
		return count;
	}
	
	private String getText() {
		ArrayList<Long> list_for_sort = new ArrayList<Long>();//for sort
		ArrayList<ArrayList> block_list = new ArrayList();
		lines = Arrays.asList(html.split("\n",-1));

		text.setLength(0);
		
		int line_count=0;
		//int max_line_tokens=0;
		int parse_max_line = (int)Math.ceil(lines.size()*main_ratio);
		//int parse_stop_line=0;
		String[] tokens = new String[lines.size()];

		indexDistribution.clear();
		
		for(int i=lines.size()-1; i>(lines.size() - blocksWidth); i--){
			tokens[i] = "";
		}
		try{
			m_bw.write(m_url_id + ",");
			for (int i = 0; i < lines.size() - blocksWidth + 1; i++) {
				int wordsNum = 0;
				for (int j = i; j < i + blocksWidth; j++) { 
					//lines.set(j, lines.get(j).trim());
					tokens[j] = lines.get(j).replaceAll("[\\s ]", "");
					tokens[j] = tokens[j].replaceAll("[^\\w\uFF10-\uFF19\uFF21-\uFF3A\uFF41-\uFF5A\u4E00-\u9FFF\u3040-\u309F\u30A0-\u30FF]", "");
					wordsNum += tokens[j].length();
					//cal_flag = false;
				}
				
				wordsNum = (int)Math.round((double)wordsNum / (double)blocksWidth);
				indexDistribution.add(wordsNum);
				//list_for_sort.add(wordsNum);
				
				//find the longest text within the main_ratio area
//				if(i < parse_max_line && wordsNum > max_line_tokens){
//					parse_stop_line = i;
//					max_line_tokens = wordsNum;
//				}
				
				m_bw.write(wordsNum + ",");
				//System.out.println(wordsNum);
			}
			
			m_bw.write("\n");
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
		int line_number = indexDistribution.size();
		long block_max_tokens = 0;
		long block_token_sum = 0;
		
		try{
			for(int i = 0; i < line_number ; i++) {
				if (tokens[i].length() > 0 && ! boolstart) {
					boolstart = true;
					start = i;
					block_token_sum += tokens[i].length();
					if(indexDistribution.get(i) > block_max_tokens)
						block_max_tokens = indexDistribution.get(i);
					//System.out.println("start: " + start);
					continue;
				}
				if (boolstart) {
					block_token_sum += tokens[i].length();
					if(indexDistribution.get(i) > block_max_tokens)
						block_max_tokens = indexDistribution.get(i);
					if (indexDistribution.get(i) == 0) {
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
			threshold = calThreshold(list_for_sort);
			//start to choose blocks
			for(ArrayList block:block_list){
				long b_max_tokens = (Long)block.get(2);
				long b_token_sum = (Long)block.get(3);
				
				if(b_max_tokens >= threshold){
					int b_start = (Integer)block.get(0);
					int b_end = (Integer)block.get(1);
					
					if(b_start >= parse_max_line && b_token_sum < 200 )
						continue;
					
					buffer.setLength(0);
					//System.out.println(start+1 + "\t\t" + end+1);
					for (int ii = b_start; ii < b_end; ii++) {
						String txt = tokens[ii];
						if (txt.length() == 0) continue;
						if(line_count > max_lines)
							break;
						if(!txt.matches(".*(記事一覧|利用規約|Copyright)+.*")){
							buffer.append(lines.get(ii).trim() + "\n");
							line_count++;
						}
					}
					String str = buffer.toString();
					text.append(str);				
				}
			}
		}
		catch(Exception e){
			System.out.println(m_url_id);
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