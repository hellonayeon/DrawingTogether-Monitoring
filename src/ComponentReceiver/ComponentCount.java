package ComponentReceiver;

/* 회의방에 현재 그려진 컴포넌트 개수를 저장하는 클래스 */
public class ComponentCount {
	
	String topic; // 토픽
    int stroke; // 스트로크 개수
    int rect; // 사각형 개수
    int oval; // 원 개수 
    int text; // 텍스트 
    int image; // 이미지 전송 횟수
    int erase; // 지우개 전송 횟수
    
    
	public String getTopic() {
		return topic;
	}
	
	public int getStroke() {
		return stroke;
	}
	
	public int getRect() {
		return rect;
	}
	
	public int getOval() {
		return oval;
	}
	
	public int getText() {
		return text;
	}
	
	public int getImage() {
		return image;
	}
	
	public int getErase() {
		return erase;
	}
	
	public void changeTopicName(String finishTopic) {
		this.topic = finishTopic;
	}
	
}
