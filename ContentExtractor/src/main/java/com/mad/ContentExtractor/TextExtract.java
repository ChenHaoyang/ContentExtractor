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
	private double main_ratio, delay_ratio;
	private StringBuilder text;
	private ArrayList<Integer> indexDistribution;
	private String m_url_id;
	private BufferedWriter m_bw;
	
	public TextExtract() {
		lines = new ArrayList<String>();
		indexDistribution = new ArrayList<Integer>();
		text = new StringBuilder();
		flag = true;
		threshold	= 60;  
		main_ratio = 0.7;
		max_lines = 100;
		try {
			m_bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/charles/Data/output/source/" + lines + ".txt")));
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
	private static Pattern main_rule = Pattern.compile("(<!DOCTYPE.*?>|<!--.*?-->)",//"(<meta.*?>|<!DOCTYPE.*?>|<!--.*?-->|<script.*?>.*?</script>|<style.*?>.*?</style>|"
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
	private static Pattern sub_rule_05 = Pattern.compile("(</p>|</tr>|</li>|</dd>|</dt>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static Pattern sub_rule_06 = Pattern.compile("(</div>|</table>|</section>|</dl>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	//private static Pattern sub_rule_06 = Pattern.compile("<a[ ]+.*?>.{12,}?</a>", Pattern.DOTALL | Pattern.UNICODE_CASE);
	//private static Pattern sub_rule_03 = Pattern.compile("<[^>]*['\"].*['\"].*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	
	private static String preProcess(String url_id, String source) {
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
		source = sub_rule_06.matcher(source).replaceAll("\n\n\n");
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
	
	private String getText() {
		ArrayList<Integer> list_for_sort = new ArrayList<Integer>();//for sort
		lines = Arrays.asList(html.split("\n",-1));

		text.setLength(0);
		
		int line_count=0;
		int max_line_tokens=0;
		int parse_max_line = (int)Math.ceil(lines.size()*main_ratio);
		int parse_stop_line=0;// = (int)Math.ceil(lines.size()*delay_ratio);
		String[] tokens = new String[lines.size()];
		boolean cal_flag = true;
		//lines = lines.subList(0, parse_max_line);
		indexDistribution.clear();
		
		for(int i=lines.size()-1; i>(lines.size() - blocksWidth); i--){
			tokens[i] = "";
		}
		try{
			m_bw.write(m_url_id + ",");
			for (int i = 0; i < lines.size() - blocksWidth + 1; i++) {
//				if(cal_flag)
//					tokens[i] = lines.get(i).replaceAll("\\s", "");
//				if(tokens[i].length() == 0){
//					indexDistribution.add(0);
//					list_for_sort.add(0);
//					cal_flag=true;
//					continue;
//				}
				//if(i==83)
				//	System.out.print(i + "\n");
				int wordsNum = 0;
				for (int j = i; j < i + blocksWidth; j++) { 
					//lines.set(j, lines.get(j).trim());
					tokens[j] = lines.get(j).replaceAll("\\s", "");
					wordsNum += tokens[j].length();
					//cal_flag = false;
				}
				
				wordsNum = wordsNum / blocksWidth;
				indexDistribution.add(wordsNum);
				list_for_sort.add(wordsNum);
				
				//find the longest text within the main_ratio area
				if(i < parse_max_line && list_for_sort.get(i) > max_line_tokens){
					parse_stop_line = i;
					max_line_tokens = list_for_sort.get(i);
				}
				
				m_bw.write(wordsNum + ",");
				//System.out.println(wordsNum);
			}
			//tmp = indexDistribution.clone();
			
			m_bw.write("\n");
		}
		catch(Exception e){
			e.printStackTrace();
		}
		//System.out.println("lines: " + lines.size());
		Collections.sort(list_for_sort);
		//int avr_top_n = calThreshold(tmp, 3);

		//threshold = avr_top_n / blocksWidth;
		threshold = calThreshold(list_for_sort, 3);
		//System.out.println(threshold);
		
		start = -1; end = -1;
		boolean boolstart = false, boolend = false, first_read = false;
		
		StringBuilder buffer = new StringBuilder();
		int line_number = indexDistribution.size();
		try{
		for (int i = 0; i < line_number ; i++) {
			if (indexDistribution.get(i) >= threshold && ! boolstart) {
				boolstart = true;
				start = i;
				//System.out.println("start: " + start);
				continue;
			}
			if (boolstart) {
				if (indexDistribution.get(i) == 0) {
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
					String txt = tokens[ii];
					if (txt.length() == 0) continue;
					if(line_count > max_lines)
						break;
					else
						line_count++;
					//txt = "本サービスに掲載されております電話番号情報につきましては、  利用規約をご確認ください。下記の地図はGoogleマップを使用しており正確でない場合があります。詳しくは、  利用規約をご確認ください。";
					if(!txt.matches(".*(記事一覧|利用規約)+.*"))
						buffer.append(txt + "\n");
				}
				String str = buffer.toString();
				//System.out.println(str);
				if (str.contains("Copyright")) continue; 
				text.append(str);
				boolstart = boolend = false;
				first_read = true;
			}
		}
		}
		catch(Exception e){
			System.out.println(m_url_id);
			System.out.println(e.toString());
		}
		//for the final iteration
		if (start > end && !(first_read && (start>parse_stop_line)))
		{
			buffer.setLength(0);
			int size_1 = lines.size()-1;
			for (int ii = start; ii <= size_1; ii++) {
				String txt = tokens[ii];
				if (txt.length() == 0) continue;
				if(line_count > max_lines)
					break;
				else
					line_count++;
				if(!txt.matches(".*(記事一覧|利用規約)+.*"))
					buffer.append(txt + "\n");
			}
			String str = buffer.toString();
			//System.out.println(str);
			if ((!str.contains("Copyright"))) 
			{	
				text.append(str);
			}
		}
		
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