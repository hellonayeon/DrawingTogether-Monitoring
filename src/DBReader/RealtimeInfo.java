package DBReader;

/*실시간 메세지 크기, 커넥션 수, 메세지 개수를 시간 단위로 저장하는 클래스*/
public class RealtimeInfo {
   private String date; 
   private int totalMsgSize;
   private int totalConnections;
   private int totalMsgNums;
   public int recordCnt; //한 시간 동안 db에 저장된 레코드의 개수 
   
   public RealtimeInfo() {
      totalConnections = 0;
   }
   
   public String getDate() {
      return date;
   }
   public int getTotalMsgSize() {
      return totalMsgSize;
   }
   public int getTotalConnections() {
      return totalConnections;
   }
   public int getTotalMsgNums() {
      return totalMsgNums;
   }
   
   public void setDate(String date) {
      this.date = date;
   }
   
   public void setTotalMsgSize(int totalMsgSize) {
      this.totalMsgSize = totalMsgSize;
   }
   public void setTotalConnections(int totalConnections) {
      this.totalConnections = totalConnections;
   }
   public void setTotalMsgNums(int totalMsgNums) {
      this.totalMsgNums = totalMsgNums;
   }
   
   public void increaseTotalMsgSize(int msgSize) {
      totalMsgSize+=msgSize;
   }
   
   public void increaseTotalConnections(int connections) {
      totalConnections+=connections;
   }
   
   public void increaseTotalMsgNums(int msgNum) {
      totalMsgNums+=msgNum;
   }

}