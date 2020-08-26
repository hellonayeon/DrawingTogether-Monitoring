package LogReader;

/*mqtt broker에 연결된 client의 id와 플랫폼을 저장하는 클래스*/  
public class Client {
   private String name; //client name를 저장하는 변수 
   private int msgSendingCount; // client가 메시지를 전송한 횟수를 저장하는 변수
   private int accumulatedMsgSize; // client가 보낸 메시지의 크기를 누적하여 저장하는 변수
   private String platform; //client의 플랫폼이 안드로이드인지 iOS인지 확인하는 변수 
   private String topic; // client가 구독한 토픽을 저장하는 변수
   
   public Client(String name, String topic, String platform) {
      this.name = name;
      this.topic = topic;
      this.platform = platform;
      
      this.msgSendingCount = 1;
   }
   
   public void increaseMsgSendingCount() {
	   msgSendingCount++;
   }
   
   public void increaseAccumulatedMsgSize(int size) {
	   accumulatedMsgSize += size;
   }

   public String getClientName() {
      return name;
   }
   
   public int getMsgSendingCount() {
	   return msgSendingCount;
   }
   
   public int getAccumulatedMsgSize() {
	   return accumulatedMsgSize;
   }

   public String getPlatform() {
      return platform;
   }
   
   public String getTopic() {
	   return topic;
   }
   

}