import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class efficiency {
	
	static org.w3c.dom.Document dom;
	BufferedReader in;
	int matched=8482;
	int count=0;
	
	efficiency()
	{
		parse_db();
		//System.out.println("1");
		read_file();
		//System.out.println("2");
		calculate();
	}
	
	private void parse_db(){
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				
		try{
			
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse("eng-mono-masked.xml");
			
			
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
	}
	
	private void read_file(){
		
		try{
			in = new BufferedReader(new FileReader("ANS.txt"));
			
		}catch (IOException e) {
		}
		
	}
	
	private void calculate(){
		
		try{
			System.out.println("Here");
		
			Element docEle = dom.getDocumentElement();
			NodeList nl = docEle.getElementsByTagName("SMS");
		
			String str,match;
			int flag=0;
			str = in.readLine();
		
			if(nl != null && nl.getLength() > 0 && !str.equals("")) {
				for(int i = 0 ; i < nl.getLength();i++) {
					
					Element sl = (Element)nl.item(i);
					match = get_matches(sl);
					String m[]=match.split(",");
					String [] list=str.split(",");
					
					/*if(match.equals(list[1]))
						count++;*/
					
					for (int k = 0; k<m.length;k++)
					{
						for (int l =1; l<list.length;l++){
					
							if(m[k].equals(list[l])){
								count++;
								flag++;
								break;
							}
					
						}
						if(flag==1)
						{
							flag=0;
							break;
						}
					}
					
					str=in.readLine();
					
					System.out.println(i+list[0]);
					
					
					
				}
			}
			
			System.out.println("Result "+count+" "+((float)count/matched));
		}catch(Exception e){
		}
		
	}
	
	private String get_matches(Element s){
		
		String matches="";
		
		NodeList nl=s.getElementsByTagName("MATCHES");
		
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength();i++) {
				
				Element sl = (Element)nl.item(i);

				matches = getTextValue(sl,"ENGLISH");
				
			}
		}
		
		return matches;
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
	
	public static void main(String[] args) throws Exception {
		
		new efficiency();		
		
	}
	
	
	

}
