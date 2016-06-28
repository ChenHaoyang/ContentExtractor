package com.mad.ContentExtractor;

import java.io.*;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseAnalyzer;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseTokenizer;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.dict.UserDictionary;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.tokenattributes.BaseFormAttribute;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.languagetool.JLanguageTool;
import org.languagetool.language.BritishEnglish;
import org.languagetool.language.English;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
//import org.htmlcleaner.*;
//import org.mozilla.intl.chardet.nsDetector;
//import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
//import org.mozilla.intl.chardet.nsPSMDetector;

/**
 * @author charles
 *
 */
public class ContentExtractor {

	private Tokenizer m_tokenizer;
	private JapaneseAnalyzer m_analyzer;
	private CharTermAttribute m_term;
	private BaseFormAttribute m_baseForm;
	private PartOfSpeechAttribute m_partOfSpeech;
	private TextExtract m_textExtract;
	private boolean m_lowerCase;
	private CharArraySet m_stopSet;
	private Set<String> m_stopTags;
	private JLanguageTool m_langTool;
	
	public ContentExtractor() throws Exception{
		this(true, true, 0, 4, 5, 0.8, 5, 50);
	}
	
	/**
	 * @param isUsrDict whether using the user dictionary or not
	 * @param discardPunctuation discard the punctuation during parsing 
	 * @param mode specify the mode of tokenizer. (0:normal, 1:search, 2:extended)
	 * @param blocks_width set the block width of the content extractor. (default: 4)
	 * @param min_tokens set the minimal number of tokens that can be retrieved as contents. (default: 5)
	 * @param main_ratio set the main content ratio. (default: 0.8)
	 * @param max_blocks set the maximal lines can be retrieved. (default: 100)
	 * @author charles
	 * @version 1.0.0
	 * @throws Exception 
	 */
	public ContentExtractor(boolean isUsrDict, boolean discardPunctuation, int mode, int blocks_width, int min_tokens, double main_ratio, int max_blocks, int max_lines_in_block) throws Exception{
		UserDictionary usrDict = null;
		m_lowerCase = false;
		BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/stopwords_en.txt")));
		String sw = br.readLine();
		
		if(isUsrDict){
			usrDict = UserDictionary.open(new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/user_dict.txt"))));
		}
		
		switch(mode){
		case 0:
			m_tokenizer = new JapaneseTokenizer(usrDict, discardPunctuation, JapaneseTokenizer.Mode.NORMAL);
			//m_analyzer = new JapaneseAnalyzer(usrDict, JapaneseTokenizer.Mode.NORMAL, JapaneseAnalyzer.getDefaultStopSet(),JapaneseAnalyzer.getDefaultStopTags());
			break;
		case 1:
			m_tokenizer = new JapaneseTokenizer(usrDict, discardPunctuation, JapaneseTokenizer.Mode.SEARCH);
			//m_analyzer = new JapaneseAnalyzer(usrDict, JapaneseTokenizer.Mode.SEARCH, JapaneseAnalyzer.getDefaultStopSet(),JapaneseAnalyzer.getDefaultStopTags());
			break;
		case 2:
			m_tokenizer = new JapaneseTokenizer(usrDict, discardPunctuation, JapaneseTokenizer.Mode.EXTENDED);
			//m_analyzer = new JapaneseAnalyzer(usrDict, JapaneseTokenizer.Mode.EXTENDED, JapaneseAnalyzer.getDefaultStopSet(),JapaneseAnalyzer.getDefaultStopTags());
			break;
		default:
			m_tokenizer = new JapaneseTokenizer(usrDict, discardPunctuation, JapaneseTokenizer.Mode.NORMAL);
		}
		//m_tokenizer = new JapaneseTokenizer(null, discardPunctuation, JapaneseTokenizer.Mode.NORMAL);
		m_term = m_tokenizer.addAttribute(CharTermAttribute.class);
		m_baseForm = m_tokenizer.addAttribute(BaseFormAttribute.class);
		m_partOfSpeech = m_tokenizer.addAttribute(PartOfSpeechAttribute.class);
		m_langTool = new JLanguageTool(new BritishEnglish());
		for (Rule rule : m_langTool.getAllRules()) {
			  if (!rule.isDictionaryBasedSpellingRule()) {
				  m_langTool.disableRule(rule.getId());
			  }
		}
		m_textExtract = new TextExtract(blocks_width, min_tokens, main_ratio, max_blocks, max_lines_in_block);
		m_stopSet = JapaneseAnalyzer.getDefaultStopSet();
		m_stopTags = JapaneseAnalyzer.getDefaultStopTags();
		
		//Add stop words of English language
		while(sw != null){
			m_stopSet.add(sw);
			sw = br.readLine();
		}
		br.close();
		br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/stopwords_jp.txt")));
		sw = br.readLine();
		while(sw != null){
			if(!m_stopSet.contains(sw))
				m_stopSet.add(sw);
			sw = br.readLine();
		}
		br.close();
		
	}
	
	/**
	 * @param isUsrDict whether using the user dictionary or not
	 * @param discardPunctuation discard the punctuation during parsing 
	 * @param mode specify the mode of tokenizer. (0:normal, 1:search, 2:extended)
	 * @author charles
	 * @version 1.0.0
	 * @throws Exception 
	 */
	public ContentExtractor(boolean isUsrDict, boolean discardPunctuation, int mode) throws Exception{
		this(isUsrDict, discardPunctuation, mode, 4, 5, 0.8, 5, 50);
	}
	
	/**
	 * @param lowerCase specify the lower-case mode
	 */
	public void setLowerCase(boolean lowerCase){
		m_lowerCase = lowerCase;
	}
	
	/**
	 * @return the current lower-case mode
	 */
	public boolean getlowerCase(){
		return m_lowerCase;
	}
	
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		
//		//while(true){
//		try{
//			ContentExtractor ce = new ContentExtractor(true, true, true, 0);
//			String test_data = "<html><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><title>JTB、パスポート番号含む793万人分の個人情報流出--メールの添付ファイルから感染</title>"
//					+ "<meta name=\"description\" content=\"　ジェイティービー（JTB）は6月14日、同社子会社でEC事業を展開するi.JTBのサーバに\">"
//					+ "<meta name=\"keywords\" content=\"ニュース,IT・科学,IT総合\">"
//					+ "<body>流出したのは、「JTBホームページ」「るるぶトラベル」「JAPANiCAN」で予約したユーザーに加え、JTBグループ内外のオンライン販売提携先でJTB商品を予約したユーザーの個人情報。ネットで予約後、店舗で精算したユーザーも対象となる。"
//					+ "個人情報には、氏名（漢字、カタカナ、ローマ字）、性別、生年月日、メールアドレス、住所、郵便番号、電話番号、パスポート番号、パスポート取得日が含まれている。また、流出したパスポート番号とパスポート取得日のうち、現在有効なのは約4300件。クレジットカード番号や銀行口座情報、旅行予約の内容は含まれていないとしている。</body></html>";
//			//ce.forTest();
//			long time = System.currentTimeMillis();
//			HashMap re = ce.extract(test_data);
//			System.out.println("Run Time: " + (System.currentTimeMillis()-time) + "ms");
//			System.out.println(re.get("meta_title"));
//			System.out.println(re.get("meta_description"));
//			System.out.println(re.get("meta_keywords"));
//			System.out.println(re.get("main_text"));
//			System.out.println(re.get("keywords"));
//			
//		}
//		catch(Exception e){
//			e.printStackTrace();
//		}
//		//}
//		
//	}
	
	/**
	 * @param html The input HTML string
	 * @return a HashMap with 5 elements. key values:("meta_title", "meta_description", "meta_keywords", "main_text", "keywords")
	 * @author charles
	 * @version 1.0.0
	 * @throws Exception throw exception if error happens 
	 */
	public HashMap<String, String> analyse(String html) throws Exception{
		return extract(html);
	}
	
	private HashMap<String, String> extract(String html) throws Exception{
		if(html==null || "".equals(html)) return null;
		
		String keyword_list = "";
		TokenStream tokenStream;
		HashMap<String, String> result = new HashMap<String, String>();
		HashMap<String, Integer> word_count = new HashMap<String, Integer>();
		
		Document doc = Jsoup.parse(html, "", Parser.xmlParser().setTrackErrors(0));
		String meta_title = doc.title();
		String meta_description = doc.select("meta[name=\"description\"]").attr("content");
		String meta_keywords = doc.select("meta[name=\"keywords\"]").attr("content");
		String body = tagFiltering(doc.select("body").first());
		String main_text = m_textExtract.parse(body);
		StringReader sr = new StringReader((meta_title + " " + meta_description + " " + meta_keywords + " " + main_text).replaceAll("[^"+TextExtract.m_targetTokens+" ]", ""));
		m_tokenizer.setReader(sr);
		//m_tokenizer.setReader(new StringReader(meta_title + meta_description + meta_keywords + main_text));
		//m_tokenizer.setReader(new StringReader("無料ﾎﾑﾍﾟ素材も超充実"));
		tokenStream = new StopFilter(m_tokenizer, m_stopSet);
		tokenStream = new JapanesePartOfSpeechStopFilter(tokenStream, m_stopTags);
		if(m_lowerCase)
			tokenStream = new LowerCaseFilter(tokenStream);
		tokenStream.reset();
		//List<RuleMatch> matches = m_langTool.check("xmouxzt");
		
		while(tokenStream.incrementToken()){
			String speech = m_partOfSpeech.getPartOfSpeech();
			String base = m_baseForm.getBaseForm();
			String kw;
			//System.out.println(m_term.toString() + "\t" + base + "\t" +  speech);
			if(m_term.length() > 1){
				if((speech.contains("名詞") && !speech.matches(".*(数|特殊|接尾)+.*"))){
					kw = m_term.toString();
				}
				else if(speech.contains("形容詞")){
					if(base != null) kw = base;
					else kw = m_term.toString();		
				}
				else kw = null;

				if(kw != null){
					if(kw.matches("[a-zA-Z]+")){
						if(!kw.matches("[A-Z]+")){
							String s1 = kw.substring(0,1);
							String s2 = kw.substring(1).toLowerCase();

							if(m_langTool.check(s1 + s2).size() > 0)
								continue;
							else{
								if(m_langTool.check(s1.toLowerCase() + s2).size() > 0)
									kw = s1 + s2;
								else
									kw = s1.toLowerCase() + s2;
							}
						}
					}
					if(word_count.containsKey(kw))
						word_count.put(kw, (Integer)word_count.get(kw)+1);
					else
						word_count.put(kw, 1);
				}
			}
		}
		List<Entry<String, Integer>> sorted_map = new ArrayList<Entry<String, Integer>>(word_count.entrySet());
		
		Collections.sort(sorted_map, new Comparator<Entry<String, Integer>>(){
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2){
				return o2.getValue() - o1.getValue();
			}
		});
		
		for(Entry<String, Integer> entry:sorted_map){
			keyword_list += (entry.getKey() + ":" + entry.getValue() + ",");
		}
		
		if(keyword_list.length() > 0)
			keyword_list = keyword_list.substring(0, keyword_list.length()-1);
		
		result.put("meta_title", meta_title);
		result.put("meta_description", meta_description);
		result.put("meta_keywords", meta_keywords);
		result.put("main_text", main_text);
		result.put("keywords", keyword_list);
		
		tokenStream.end();
		tokenStream.close();
			
		return result;
	}
	
	private String tagFiltering(Element html_body){
		//System.out.println(body.outerHtml());
		if(html_body == null){
			return null;
		}
		//System.out.println(body.outerHtml());
		//body.select("meta").remove();
		//remove link block
		Elements link_blocks = html_body.select("div:has(a), span:has(a), ul:has(a), td:has(a)");
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
		//System.out.println(html_body.outerHtml());
		//remove topic blocks(for fc2)added in 2016/05/24
		Elements topic_blocks = html_body.select("div:matchesOwn(^トピックス$)");
		for(Element node:topic_blocks){
			node.parent().remove();
		}
		//remove shopping guide(for rakuten) added in 2016/06/27
//		Elements sg_blocks = html_body.select("tr:matches(お知らせ|利用条件|注意事項|対応可能エリア|配送について|お支払方法|クレジット決済|あす楽)");
//		for(Element node:sg_blocks){
//			try{
//				if(node.text().matches(".*(お知らせ|利用条件|注意事項|対応可能エリア|配送について|お支払方法|クレジット決済|あす楽)+.*")){
//					node.parent().remove();
//				}
//			}
//			catch(Exception e){
//				
//			}
//		}
		
		
		//System.out.println(html_body.outerHtml());
		html_body.select("[id~=(?i)(header|footer|ft|side|links|keywords|calendar|calender|rule|attention|banner|bn|navi|recommend|plugin|[_-]+ad[_-]+|^ad[_-]+|[_-]+ad$){1}]").remove();
		//System.out.print(body.outerHtml());
		html_body.select("[class~=(?i)(header|footer|links|calendar|calender|no_display|nodisplay|rule|attention|banner|bn|navi|month|recommend|plugin|[_-]+ad[_-]+|^ad[_-]+|[_-]+ad$){1}]").remove();
		//System.out.print(body.outerHtml());
		html_body.select("[style~=(?i)(display[\\s]*:[\\s]*none|visible[\\s]*:[\\s]*hidden){1}]").remove();
		//System.out.print(body.outerHtml());
		html_body.select("select, noscript, head, header, script, style, footer, aside, time, small, h1, h2, h3, h4, h5, h6").remove();
		//System.out.println(body.outerHtml());
		html_body.select("form, iframe, textarea, input").remove();
		html_body.select("span[data-tipso]").remove();
		
		html_body.select("a:matchesOwn((?i)(http|https|.com|.cn|.net|.jp)+)").remove();
		//body.select("table[class]")
		//System.out.println(body.outerHtml());
		//Document dd = Jsoup.parse("<body><t1>11<t11></t11></t1><t2><t2></t2></t2></body>");
		//int test = dd.select("body").first().childNodeSize();
		//body.select("li:has(a), dt:has(a), dd:has(a)").remove();
		//System.out.println(body.outerHtml());

		//System.out.println(body.outerHtml());
		//remove long text links
//				Elements noise_links = body.select("a");
//				for(Element node:noise_links){
//					if(node.html().length() > 12)
//						node.remove();
//				}
		//remove pagelink
		html_body.select("a:matches(前\\d+|次\\d+|最新\\d+|^\\d+$|前へ|次へ|戻る|トップページ|ホーム|記事|もっと見る|利用規約|案内|問い合わせ|プライバシー|スマホ版)").remove();
		//remove nodes whose font-size < 10(px) | 7.5(pt) | 0.625(em)
		Elements nodes_with_font_size = html_body.select("[style~=(?i)(font-size){1}]");
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
		
		return html_body.outerHtml();
	}
	
}
