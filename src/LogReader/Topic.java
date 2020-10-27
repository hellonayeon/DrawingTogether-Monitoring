package LogReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/*토픽과 관련된 정보를 저장하는 클래스*/ 
public class Topic {
   private String name; //topic 이름을 저장하는 변수 
   private int participants; //topic에 참여한 인원을 저장하는 변수
   private int msgPublishCount; //해당 topic으로 보내는 메시지 개수를 저장하는 변수
   private int accumulatedMsgSize; // 해당 topic으로 오는 메세지의 크기를 누적하여 저장하는 변수
   private String startDate;//topic이 처음 생성되는 시간을 저장하는 변수
   
   private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   
   public Topic(String name) {
      this.name = name;
      
      participants = 1;
      msgPublishCount = 0;
      accumulatedMsgSize = 0;
      
      setStartDate();
   }
   
   public void clearMsgData() {
	   if(this.name.contains(":")) return; // 사용이 종료된 토픽은 0으로 초기화 X
	   
	   this.msgPublishCount = 0;
	   this.accumulatedMsgSize = 0;
   }

   public String getName() {
      return name;
   }

   public int getMsgPublishCount() {
      return msgPublishCount;
   }

   public int getParticipants() {
      return participants;
   }

   public int getAccumulatedMsgSize() {
      return accumulatedMsgSize;
   }

   public String getStartDate() {
      return startDate;
   }

   public void increaseParticipants() {
      participants++;
   }
   
   public void decreaseParticipants() {
	   participants--;
   }

   public void increaseMsgPublishCount() {
	   msgPublishCount++;
   }

   public void increaseAccumulatedMsgSize(int size) {
	   accumulatedMsgSize += size;
   }

   public void setStartDate() {
      Date d = new Date();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

      this.startDate = sdf.format(d);
   }
   
   public String changeTopicName() {
	   this.name = this.name + "(" + sdf.format(new Date()) + ")"; // finish date
	   return this.name;
   }

	@Override
	public String toString() {
		return "Topic [name=" + name + ", participants=" + participants + ", msgPublishCount=" + msgPublishCount
				+ ", accumulatedMsgSize=" + accumulatedMsgSize + ", startDate=" + startDate + "]";
	}
   

}