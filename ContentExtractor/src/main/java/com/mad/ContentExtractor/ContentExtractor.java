package com.mad.ContentExtractor;

import java.io.*;
import java.net.*;

import org.mozilla.universalchardet.UniversalDetector;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.ArrayList;
//import org.htmlcleaner.*;
//import org.mozilla.intl.chardet.nsDetector;
//import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
//import org.mozilla.intl.chardet.nsPSMDetector;

public class ContentExtractor {

	private CloseableHttpClient httpClient;
	private RequestConfig requestConfig;
	private UniversalDetector detector;
	
	public ContentExtractor(){
		httpClient = HttpClients.custom()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.build();
		requestConfig = RequestConfig.custom()
				.setSocketTimeout(5000)
				.setConnectTimeout(5000)
				.setConnectionRequestTimeout(5000)
				.build();
		detector = new UniversalDetector(null);
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		long time = System.currentTimeMillis();
		String line = null;
		String[] tokens;
		String[] result;
		BufferedReader br = null;
		BufferedWriter bw = null;
		TextExtract te = new TextExtract();
		HttpURLConnection.setFollowRedirects(true);
		
		//HtmlCleaner cleaner = new HtmlCleaner();
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
				//System.out.println(result[3]);
				if(result != null){
					
					String main_text = "";
					if(result[3] != null)
						main_text = te.parse(tokens[0], result[3]).trim();
					bw.write("\n<document id=\"" + tokens[0] + "\" url=\"" + tokens[1] + "\">\n");
					bw.write("<title>" + result[0] + "</title>\n");
					bw.write("<description>" + result[1] + "</description>\n");
					bw.write("<keywords>" + result[2] + "</keywords>\n");
					if(main_text != "")
						bw.write("<main>" + "\n" + main_text + "\n" + "</main>\n</document>\n");
					else
						bw.write("<main></main>\n</document>\n");
					bw.flush();
				}
				line = br.readLine();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				line = br.readLine();
				System.out.println(tokens[0]);
				System.out.println(e.toString());
				//System.out.println(e.toString());
				continue;
			}
		}
		bw.write("</data>");
		br.close();
		bw.close();
		System.out.println("Run Time: " + (System.currentTimeMillis()-time)/1000 + "s");
	}

	public String[] getHTML(String strURL) throws Exception {
		String[] result = new String[4];
		ArrayList url_info = readURL(strURL);
		
		if(url_info.get(0) == null) return null;
		//System.out.print(url_info.get(0));
		Document doc = Jsoup.parse((String)url_info.get(0), "", Parser.xmlParser().setTrackErrors(0));
		//doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
		//doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
		//doc.outputSettings().prettyPrint(false);
		
		//System.out.println(doc.outerHtml());
		//check charset again
		String detected_cs = doc.charset().name();
		String page_cs_str = doc.select("meta[http-equiv=\"Content-Type\"]").attr("content");
		if(!page_cs_str.equals("")){
			String[] outter = page_cs_str.split(";");
			if(outter.length > 1){
				String[] inner = outter[1].split("=");
				if(inner.length > 1){
					detected_cs = inner[1].trim();
				}
			}
		}
		else{
			String page_cs_str_01 = doc.select("meta").attr("charset");
			if(!page_cs_str_01.equals("")){
				detected_cs = page_cs_str_01.trim();
			}
		}
		//if not coincident with predict charset
		if(!detected_cs.equals(doc.charset().name())){
			url_info.set(0, new String((byte[])url_info.get(1), detected_cs));
			url_info.set(0, changeCharset((String)url_info.get(0), "UTF-8"));
			doc = Jsoup.parse((String)url_info.get(0), "", Parser.xmlParser().setTrackErrors(0));
		}
		
		doc.outputSettings().prettyPrint(false);
		
		result[0] = doc.title();
		result[1] = doc.select("meta[name=\"description\"]").attr("content");
		result[2] = doc.select("meta[name=\"keywords\"]").attr("content");
		
		//Filtering unnecessary html tags
		Element body = doc.select("body").first();
		//System.out.println(body.outerHtml());
		if(body == null){
			result[3] = null;
			return result;
		}
		//System.out.println(body.outerHtml());
		//body.select("meta").remove();
		//remove link block
		Elements link_blocks = body.select("div:has(a), span:has(a), ul:has(a)");
		for(Element node:link_blocks){
			int child_of_a=0;
			int a_txt_num = 0;
			Elements e_a = node.select("a");
			for(Element a:e_a){
				child_of_a += (a.getAllElements().size() - 1);
				a_txt_num += StringEscapeUtils.unescapeHtml(a.text()).replaceAll("[\\s ]","").length();
			}
			//the number of tokens besides links
			String node_txt = node.text();
			node_txt = StringEscapeUtils.unescapeHtml(node_txt).replaceAll("[\\s ]", "");
			if(node_txt.length() - a_txt_num >= 100)
				continue;
			
			int direct_a_num = node.select(">a").size();
			int a_num = e_a.size();
			int br_node_num = node.select("br").size();
			int span_node_num = node.select("span").size();

			double child_node_num = Math.max(a_num, (double)(node.getAllElements().size() - 1 - child_of_a - a_num + direct_a_num - br_node_num - span_node_num));
			if( a_num / child_node_num  > 0.5 ){
				node.remove();//drop the link block
			}
		}
		//System.out.println(body.outerHtml());
		//remove topic blocks(for fc2)added in 2016/05/24
		Elements topic_blocks = body.select("div:matchesOwn(^トピックス$)");
		for(Element node:topic_blocks){
			node.parent().remove();
		}
		//System.out.println(body.outerHtml());
		body.select("[id~=(?i)(header|footer|ft|side|links|keywords|calendar|calender|rule|attention|banner|bn|navi|recommend|plugin|[_-]+ad[_-]+|^ad[_-]+|[_-]+ad$){1}]").remove();
		//System.out.print(body.outerHtml());
		body.select("[class~=(?i)(header|footer|links|calendar|calender|no_display|nodisplay|rule|attention|banner|bn|navi|month|recommend|plugin|[_-]+ad[_-]+|^ad[_-]+|[_-]+ad$){1}]").remove();
		//System.out.print(body.outerHtml());
		body.select("[style~=(?i)(display[\\s]*:[\\s]*none|visible[\\s]*:[\\s]*hidden){1}]").remove();
		//System.out.print(body.outerHtml());
		body.select("select, noscript, head, header, script, style, footer, aside, time, small, h1, h2, h3, h4, h5, h6").remove();
		//System.out.println(body.outerHtml());
		body.select("form, iframe, textarea, input").remove();
		body.select("span[data-tipso]").remove();
		//body.select("table[class]")
		//System.out.println(body.outerHtml());
		//Document dd = Jsoup.parse("<body><t1>11<t11></t11></t1><t2><t2></t2></t2></body>");
		//int test = dd.select("body").first().childNodeSize();
		//body.select("li:has(a), dt:has(a), dd:has(a)").remove();
		//System.out.println(body.outerHtml());

		//System.out.println(body.outerHtml());
		//remove long text links
//		Elements noise_links = body.select("a");
//		for(Element node:noise_links){
//			if(node.html().length() > 12)
//				node.remove();
//		}
		//remove pagelink
		body.select("a:matches(前\\d+|次\\d+|最新\\d+|^\\d+$|前へ|次へ|戻る|トップページ|ホーム|記事|もっと見る|利用規約|案内|問い合わせ|プライバシー|スマホ版)").remove();
		//remove nodes whose font-size < 10(px) | 7.5(pt) | 0.625(em)
		Elements nodes_with_font_size = body.select("[style~=(?i)(font-size){1}]");
		for(Element node:nodes_with_font_size){
			String[] str = node.attr("style").toLowerCase().split(";");
			for(String style:str){
				if(style.contains("font-size")){
					String[] key_val = style.split(":");
					key_val[1] = key_val[1].trim();
					String unit = key_val[1].substring(key_val[1].length()-2);
					String font_size = key_val[1].substring(0, key_val[1].length()-2);
					if(unit.equals("px")){
						if(Integer.parseInt(font_size) < 10)
							node.remove();
					}
					else if(unit.equals("pt")){
						if(Double.parseDouble(font_size) < 7.5)
							node.remove();
					}
					else if(unit.equals("em")){
						if(Double.parseDouble(font_size) < 0.625)
							node.remove();
					}
				}
			}
		}
		
		//pass the html contents after filtering
		result[3] = body.outerHtml();
		//System.out.println(result[3]);
		
		return result;
	}
	
	private ArrayList readURL(String strURL) throws Exception{
		ArrayList result = new ArrayList();
		String html=null,encoding;
		HttpGet httpGet = new HttpGet(strURL);
		httpGet.setConfig(requestConfig);
		CloseableHttpResponse response = httpClient.execute(httpGet);
		try{
			HttpEntity entity = response.getEntity();
			if(entity != null){
				InputStream in = entity.getContent();
				ByteArrayOutputStream bao = new ByteArrayOutputStream();
				byte[] buff = new byte[4096];
				int bytesRead;

				while((bytesRead = in.read(buff)) > 0 ){
					if(!detector.isDone())
						detector.handleData(buff, 0, bytesRead);
					bao.write(buff, 0, bytesRead);
				}
				detector.dataEnd();
				byte[] data = bao.toByteArray();
				encoding = detector.getDetectedCharset();
				if(encoding != null){
					html = new String(data,encoding);
					if(encoding != "UTF-8")
						html = changeCharset(html, "UTF-8");
				}	
				else{
					html = new String(data, "UTF-8");
				}
				result.add(html);
				result.add(data);
				detector.reset();
			}
		}
		finally{
			response.close();
		}
//		
		return result;
	}
	private String changeCharset(String scr, String newCharset)  throws UnsupportedEncodingException {
		if(scr != null)
		{
			if(newCharset != null){
				return new String(scr.getBytes(newCharset), newCharset);
			}
			else
				return scr;
		}
		return null;
	}
	
}
