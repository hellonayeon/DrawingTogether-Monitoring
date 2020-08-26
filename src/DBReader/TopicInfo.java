package DBReader;

/*토픽 이름과 지속 시간, 참여 인원을 저장하는 클래스 */
public class TopicInfo {
   private String name;
   private String startTime;
   private String finishTime;
   private long duration;
   private long participants;
   
   public TopicInfo(String name) {
      this.setName(name);
      setDuration(0);
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getFinishTime() {
      return finishTime;
   }

   public void setFinishTime(String finishTime) {
      this.finishTime = finishTime;
   }

   public String getStartTime() {
      return startTime;
   }

   public void setStartTime(String startTime) {
      this.startTime = startTime;
   }

   public long getDuration() {
      return duration;
   }

   public void setDuration(long duration) {
      this.duration = duration;
   }

   public long getParticipants() {
      return participants;
   }

   public void setParticipants(long participants) {
      this.participants = participants;
   }


   

}