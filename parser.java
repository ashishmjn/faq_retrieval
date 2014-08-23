import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.apache.lucene.document.Field;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.CharStream;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.tangentum.phonetix.Metaphone;
import java.io.PrintWriter;
import java.io.FileWriter;
public class parser extends QueryParser{

	List <sms> mySms;
	static List <faq> myFaq;
	static faq[] faqList = new faq[10000];
	static sms[] smsList = new sms[10000];
	org.w3c.dom.Document dom;
	org.w3c.dom.Document dom2;
	static final int MAX_FILES = 6;
	static final int MAX_SMS = 30;
	static final int MAX_RANKS = 100000;
	static final int TOTAL_DOMAINS = 30;
	static int stringLength =0;
	static String domainList[] = new String[TOTAL_DOMAINS];
	static int domainsListed =0;
	int domainListLength =0;
	
	rank[] Ranks = new rank [MAX_RANKS];              // declares an array of integers
    
	
	public parser(CharStream arg0){
		super(arg0);
		//create a list to hold the employee objects
		mySms = new ArrayList<sms>();
		myFaq = new ArrayList<faq>();
		for (int i = 0 ; i< MAX_RANKS; i++){
			 Ranks[i] = new rank();
			 Ranks[i].setHits(0);
			 Ranks[i].setId("");
			 Ranks[i].setQuestion("");
			 Ranks[i].setDomain("");
		}
		
	}

	
	public void index() throws Exception {
		
		//parse the xml file and get the dom object
		parse_sms();
		
		//get each employee element and create a Employee object
		index2();
		
		
	}
	
	
	private void parse_sms(){
		//get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {
			
			//Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			//parse using builder to get DOM representation of the XML file
			dom = db.parse("eng-mono.xml");

		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	
	private void index2()throws Exception{
		
		int counter=0;
		int correct = 0;
		int i =0;   // counter for total no of sms queries
		float maxScoreChk   = 0;
		PrintWriter pw = null;
		
		//For LUCENE
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		
		Directory index = new RAMDirectory();
		//Lucene declarations end here
		
		//PHONETIX keyword generator ( m1 )
		
		Metaphone m1 = new Metaphone();
		
		//PHONETIX 2.0 declaration ends here
		
		
			IndexWriter w = new IndexWriter(index, analyzer, true,IndexWriter.MaxFieldLength.UNLIMITED);
			
					
			//Creating Index by parsing XML files.------------------------------------------------------
			Parsefaq();
			
			myFaq.toArray(faqList);
			write_Faq_domain(myFaq.size());//-------------------------------------------------------------------------writes domain names to domain.txt

			int count = 0;//----------------------------------keep track of FAQS
			
			System.out.println("Processing Faqs");

    		while(count < myFaq.size()) {   //---------------------------runs for each faq n adds info abt faq in the doc(1 for each) including phonetic equivalent of questns
    			
    			Document document = new Document();
    			document.add(new Field("id",faqList[count].getId(),Field.Store.YES,
    		 			Field.Index.NO));
    			document.add(new Field("domain",faqList[count].getDomain(),Field.Store.YES,
    		 			Field.Index.NO));
    		    
    			String s1 =  " "; 
    				
    			String tokensFaq[] = new String[2000];
    			
    			/***********************************************************************************
						EXTRACTING WORDS FROM THE FAQ QUES also removes STOP-WORDS
    			 ************************************************************************************/
    			tokensFaq = extractTokens(faqList[count].getQuestion());
    			
    			int z;

    		    for (z=0; z<stringLength;z++){
    		    /***********************************************************************
    		    		 FINDING PHONETIC EQUIVALENTS OF THE WORDS IN FAQ
	 			***********************************************************************/
    		    	s1 = s1.concat(m1.generateKey(tokensFaq[z])+" ");//---------------------------------------------------------------PHONETIC EQUIVALENTS
    		    	  		    		
    		    }
    			
    			faqList[count].setQuestion(s1);
    		    document.add(new Field("question",s1,Field.Store.YES,
        				Field.Index.ANALYZED));
    		   
    		    //System.out.println(faqList[count].getQuestion());
    		    
    		    /*************************************************************
    		     			PHONETIC equivalents generated ...
    		     *************************************************************/
    			
    		    w.addDocument(document);
    		    count++;
    		}
    		w.optimize();
    	
		w.close();
		
		System.out.println("FAQS Processed");


		
		//get the root elememt
		Element docEle = dom.getDocumentElement();
		//get a nodelist of <sms> elements
		NodeList nl = docEle.getElementsByTagName("SMS");
		try {

		    /*Map<String,String> dict=new HashMap<String,String>();
		    dict.put("a", "abcd");
		    
		    System.out.println(dict.get("a"));*/

	        pw = new PrintWriter(new FileWriter("ANS.txt",true));
	        
	        System.out.println("Adding sms to list and processing each of them");

	     if(nl != null && nl.getLength() > 0) {
			for(i = 0 ; i < nl.getLength();i++) {
				
				
				String smsShortened = " ";
				
				String[] tokens = new String [100];
				int totalRanks =0;// important
				
				//get the sms element
				Element sl = (Element)nl.item(i);
				
				//get the sms object
				sms s = getsms(sl);
				mySms.add(s);
				
				
				/***********************************************************************************
				 					EXTRACTING WORDS FROM THE SMS TEXT
				************************************************************************************/
				
				tokens = extractTokens(s.getText());
				
				/*************************************************************************************
				 		THE ABOVE LINE OF CODE ALSO REMOVES ANY STOP-WORD IF THERE
				 *************************************************************************************/
				
				
				
				int queryLength = tokens.length;
				for (int m = 0 ; m< MAX_RANKS; m++){
					 Ranks[m].setHits(0);
					 Ranks[m].setId("");
					 Ranks[m].setQuestion("");
				}
				


						int  l ;
						

		    			
						
		    			for (l =0; l<stringLength;l++){
		    				queryLength = stringLength;
		    				//int dontProcess =0;
		    				
		    				
		    				/***********************************************************************
		    				 	FOLLOWING LINE OF CODE STEMS THE WORDS IN SMS
		    				 ***********************************************************************/
		    				

		    				smsShortened = smsShortened.concat(m1.generateKey(tokens[l].toLowerCase()));
		    				smsShortened = smsShortened.concat(" ");
		    				String querystr= new String(m1.generateKey(tokens[l].toLowerCase()));
		    				
		    				/*******************************************************************************
		                      
		                      FOLLOWING CODE WHEN NOT IN GREEN COLOUR REMOVES SMS queries containg nos.   
		                 
		    				 ********************************************************************************/
		    			
		    				
		 		/*
		    				for (int numChk =0;numChk < 9;numChk++){
		    					if (tokens[l].contains(chkInt[numChk])){
		    							dontProcess = 1;
		    							break;
		    						}
		    				}
		    				if (dontProcess == 1){
		    					continue;
		    				}
		    			*/
		    	/*******************************************************************************
                Query removing code ends here, if the above code is green, queries with nos will be present 
                  ********************************************************************************/			
		    				Query q;
		    			    q = new FuzzyQuery(new Term("question",querystr),0.99f,0);
		    			    
		    			    int hitsPerPage = 1000;
		    			    
		    			    IndexSearcher searcher = new IndexSearcher(index, true);
		    			    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		    			    searcher.search(q, collector);
		    			    ScoreDoc[] hits = collector.topDocs().scoreDocs;
		    			    
		    			    
		    			    for(int hitCount=0;hitCount<hits.length;++hitCount) {
		    			      int docId = hits[hitCount].doc;
		    			      Document d = searcher.doc(docId);
		    			      
		   
		    			      if (maxScoreChk < hits[hitCount].score)
		    			    	  maxScoreChk = hits[hitCount].score;
		    			      
		    			      int n;
		    			      for (n =0;n<totalRanks;n++){
		    			    	  if (d.get("id").equals(Ranks[n].getId()))
		    			    		  break;
		    			      }
		    			      
		    			      if (n==totalRanks){   // its a new faq for that sms
		    			    	  Ranks[n].setId(d.get("id"));
		    			    	  Ranks[n].setQuestion(d.get("question"));
		    			    	  Ranks[n].setDomain(d.get("domain"));
		    			    	  Ranks[n].setHits(hits[hitCount].score);
		    			    	  totalRanks++;
		    			      }
		    			      else if (Ranks[n].getId().equals(d.get("id"))) {
		    			    	  Ranks[n].incHits(hits[hitCount].score);
		    			      }
		    			    }
		    			    searcher.close();
		    			}
		    			
		    			Arrays.sort(Ranks);
		    			String none = "NONE";
		    			
		    			int answers =5;
		    			
		    			
		    			if ( Ranks[0].getHits() < ((float)queryLength*1.22)){
		    				Ranks[0].setId(none);
		    				Ranks[0].setDomain(none);
		    				Ranks[1].setId(none);
		    				Ranks[1].setDomain(none);
		    				Ranks[2].setId(none);
		    				Ranks[2].setDomain(none);
		    				Ranks[3].setId(none);
		    				Ranks[3].setDomain(none);
		    				Ranks[4].setId(none);
		    				Ranks[4].setDomain(none);
		    				answers = 0;
		    			}
		    			else if (Ranks[1].getHits() < ((float)queryLength*1.22)){
		    				Ranks[1].setId(Ranks[0].getId());
		    				Ranks[1].setDomain(Ranks[0].getDomain());
		    				Ranks[2].setId(Ranks[0].getId());
		    				Ranks[2].setDomain(Ranks[0].getDomain());
		    				Ranks[3].setId(Ranks[0].getId());
		    				Ranks[3].setDomain(Ranks[0].getDomain());
		    				Ranks[4].setId(Ranks[0].getId());
		    				Ranks[4].setDomain(Ranks[0].getDomain());
		    				answers=1;
		    			}
		    			else if (Ranks[2].getHits() < ((float)queryLength*1.22)){
		    				
		    				Ranks[2].setDomain(Ranks[0].getDomain());
		    				Ranks[3].setId(Ranks[0].getId());
		    				Ranks[3].setDomain(Ranks[0].getDomain());
		    				Ranks[4].setId(Ranks[0].getId());
		    				Ranks[4].setDomain(Ranks[0].getDomain());
		    				answers=2;
		    			}
		    			else if (Ranks[3].getHits() < ((float)queryLength*1.22)){
		    				Ranks[3].setId(Ranks[0].getId());
		    				Ranks[3].setDomain(Ranks[0].getDomain());
		    				Ranks[4].setId(Ranks[0].getId());
		    				Ranks[4].setDomain(Ranks[0].getDomain());
		    				answers =3;
		    			}
		    			else if (Ranks[4].getHits() < ((float)queryLength*1.22)){
		    				Ranks[4].setId(Ranks[0].getId());
		    				Ranks[4].setDomain(Ranks[0].getDomain());
		    				answers=4;
		    			}
		    			
		    			
		    			
		    			
		    				if (answers == 0)
		    				{
		    					pw.println(s.getId()+","+Ranks[0].getId());
		    					counter ++;
		    				}
		    				else if (answers == 1){
		    					pw.println(s.getId()+","+Ranks[0].getId()+","+(Ranks[0].getHits()/10));
		    					counter++;
		    					correct++;
		    				}
		    				else if (answers == 2){
		    					pw.println(s.getId()+"," + Ranks[0].getId()+","+(Ranks[0].getHits()/10) + ","  + Ranks[1].getId()+","+(Ranks[1].getHits()/10));
		    					counter++;
		    					correct++;
		    				}
		    				else if (answers == 3){
		    					pw.println(s.getId()+"," + Ranks[0].getId()+","+(Ranks[0].getHits()/10) + ","  + Ranks[1].getId()+","+(Ranks[1].getHits()/10) + "," + Ranks[2].getId()+","+(Ranks[2].getHits()/10) );
		    					counter++;
		    					correct++;
		    				}
		    				else if (answers == 4){
		    					pw.println(s.getId()+"," + Ranks[0].getId()+","+(Ranks[0].getHits()/10) + "," + Ranks[1].getId()+","+(Ranks[1].getHits()/10) + "," + Ranks[2].getId()+","+(Ranks[2].getHits()/10) + "," + Ranks[3].getId()+","+(Ranks[3].getHits()/10));
		    					counter++;
		    					correct++;
		    				}
		    				else if (answers == 5){
		    					pw.println(s.getId()+"," + Ranks[0].getId()+","+(Ranks[0].getHits()/10) + "," + Ranks[1].getId()+","+(Ranks[1].getHits()/10) + "," + Ranks[2].getId()+","+(Ranks[2].getHits()/10) + "," + Ranks[3].getId()+","+(Ranks[3].getHits()/10) + "," + Ranks[4].getId()+","+(Ranks[4].getHits()/10));
		    					counter++;
		    					correct++;
		    				}
		    				
		    				
		    			}
			}

	     System.out.println("Total sms :"+counter);
		 pw.flush();
		 
			
	    }
	    catch (IOException e) {
	      e.printStackTrace();
	    }
	    finally {
	      
	      //Close the PrintWriter
	      if (pw != null)
	        pw.close();
	      
	    }
		mySms.toArray(smsList);
		System.out.println("Matched = "+ correct);
		System.out.println("maxScoreChk = " + maxScoreChk);
		
		write_Sms_domain(mySms.size());
		
		System.out.println("EXECUTION COMPLETE!!");
		
	}
	
	
	public void Parsefaq() {
		
			//parse the xml file and get the dom object
			parse_faq();
		
			//get each employee element and create a Employee object
			parseDocument();
		
			
	}
	
	
	private void parse_faq(){
			//get the factory
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
			try {
			
				//Using factory get an instance of document builder
				DocumentBuilder db = dbf.newDocumentBuilder();
			
				//parse using builder to get DOM representation of the XML file
				dom2 = db.parse("eng.xml");
			

			}catch(ParserConfigurationException pce) {
				pce.printStackTrace();
			}catch(SAXException se) {
				se.printStackTrace();
			}catch(IOException ioe) {
				ioe.printStackTrace();
			}
	}

	
	
	private void parseDocument(){
		
			System.out.println("Writing Faqs to List");
			
			myFaq.clear();
			//get the root elememt
			Element docEle = dom2.getDocumentElement();
			
			
			//get a nodelist of <faq> elements
			NodeList nl = docEle.getElementsByTagName("FAQ");
			if(nl != null && nl.getLength() > 0) {
				for(int i = 0 ; i < nl.getLength();i++) {
				
					//get the faq element
					Element sl = (Element)nl.item(i);
				
					//get the faq object
					faq f = getfaq(sl);
				
					//add it to list
					myFaq.add(f);
				}
			}
			System.out.println("Faqs written to list");
	}
	

	static String[] extractTokens (String a){
		
		int chkStopWords =0;
		int i=0;
		String [] tokens = a.split(" ");
		String tokens1[] = new String [2000];

		for (int l =0; l<tokens.length;l++){
			chkStopWords =0;
			try {
				BufferedReader in = new BufferedReader(new FileReader("stopWords.txt"));
				String str;
				while ((str = in.readLine()) != null) {
					if(tokens[l].toLowerCase().equals(str)){
						chkStopWords = 1;
						break;
					}
				}
				in.close();
			} catch (IOException e) {
			}
			if (chkStopWords == 0){
				tokens1[i++]=tokens[l].toLowerCase();
			}
		}
		stringLength = i;
		return tokens1;
	}


	
	private sms getsms(Element smsS1) throws Exception {


		String id = getTextValue(smsS1,"SMS_QUERY_ID");
		String text = getTextValue(smsS1,"SMS_TEXT");
		String matches = getTextValue(smsS1,"ENGLISH");

		String domain;
		
		int i,end;

		//Create a new sms with the value read from the xml nodes
		for ( i = 4 ; i < matches.length() ; i++)
		{
			if (matches.charAt(i) == '_')
				break;
		}
		end = i ;
		domain  = matches.substring(4,end);
		if (matches.equals("NONE"))
			domain = "NONE";
		sms s = new sms(id,text,matches,domain);
		
		return s;
	}
	
	
	private faq getfaq(Element faqF1) {


		String id = getTextValue(faqF1,"FAQID");
		int i  = 0 ;
		int start = 4;
		int end;
		for ( i = 4 ; i < id.length() ; i++)
		{
			if (id.charAt(i) == '_')
				break;
		}
		end = i ;
		
		
		
		String ques = getTextValue(faqF1,"QUESTION");
		String ans = getTextValue(faqF1,"ANSWER");

		String domain = id.substring(start,end);
		

		for (i =0;i<domainsListed;i++){
			if (domainList[i].equals(domain)){
				break;
			}
		}
		if (i == domainsListed){
			domainList[domainsListed]=domain;
			domainsListed++;
		}
		//Create a new faq with the value read from the xml nodes
		faq f = new faq(id,ques,ans,domain);
		
		return f;
	}
	



	private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element sl = (Element)nl.item(0);
			textVal = sl.getFirstChild().getNodeValue();
		}

		return textVal;
	}

	
	
	
	public static void write_Sms_domain (int totalSms){
		
		PrintWriter pw = null;

		try {

		     System.out.println("Writing sms domains to domains1.txt");

	        pw = new PrintWriter(new FileWriter("domains1.txt"));
	        
	    	  for (int j=0;j<domainsListed;j++){
	    		  
	    	  pw.println("'"+domainList[j]+"'"+",");
	      }
	      pw.flush();

	    }
	    catch (IOException e) {
	      e.printStackTrace();
	    }
	    finally {
	      
	      //Close the PrintWriter
	      if (pw != null)
	        pw.close();
	      
	    }
    }
	
	
	
	public static void write_Faq_domain(int totalFaqs) {
        
		PrintWriter pw = null;
	    
	    try {

		     
	    	System.out.println("Total Faqs :"+totalFaqs);
	    	
	    	System.out.println("Writing Faqs domains in domains.txt");
	    	
	        pw = new PrintWriter(new FileWriter("domains.txt"));
	     
	        for (int j=0;j<domainsListed;j++){
	    	  pw.println("'"+domainList[j]+"'"+",");
	      }
	        
	      pw.flush();

	    }
	    catch (IOException e) {
	      e.printStackTrace();
	    }
	    finally {
	      
	      //Close the PrintWriter
	      if (pw != null)
	        pw.close();
	      
	    }
    }
	
	

	
	public static void main(String[] args) throws Exception {
		
		
		//create an instance
		parser dpe = new parser(null);
		
		
		dpe.index();
		
		
		
	}

}
