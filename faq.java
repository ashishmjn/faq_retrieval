
public class faq{

	
	private String id;

	private String question;
	
	private String answer;
	
	private String domain;
	
	public faq(){
		
	}
	
	public faq(String id, String question,String answer,String domain) {
		this.id = id;
		this.question = question;
		this.answer=answer;
		this.domain=domain;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String name) {
		this.question = name;
	}
	
	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String name) {
		this.answer = name;
	}
	
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

}
