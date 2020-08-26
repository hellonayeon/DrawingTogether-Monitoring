package LogReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


/*mqtt broker 로그의 정보를 읽어와 트래픽 데이터를 수집하여 db에 저장하는 클래스 */
public class LogReader implements Runnable {

   private int interval;// 초 간격으로 DB 업데이트
   
   //client id 가 key, client가 value인 Map
   private ConcurrentHashMap<String, Client> clientMap = new ConcurrentHashMap<>();
   
   //topic name이 key, topic이 value인 map
   private ConcurrentHashMap<String, Topic> topicMap = new ConcurrentHashMap<>();


   private String driver;
   private String url;
   private String user;
   private String pw;

   public LogReader(int interval) {
      this.interval = interval;

      readProperties();
   }

   //db.properties 를 읽어오는 함수 
   public void readProperties() {
      Properties props = new Properties();
      InputStream is = null;
      try {
         is = new FileInputStream("db.properties");

         props.load(is);
      } catch (IOException e) {
         e.printStackTrace();
         return;
      } finally {
         if (is != null)
            try {
               is.close();
            } catch (IOException e) {
               e.printStackTrace();
            }

      }
      driver = props.getProperty("jdbc.driver");
      url = props.getProperty("jdbc.url");
      user = props.getProperty("jdbc.username");
      pw = props.getProperty("jdbc.password");
   }

   @Override
   public void run() {
      startTimer();
      read();
   }
   
   //timer 함수
   public void startTimer() {
      System.out.println("LogReader starts at " + getCurrentTime());

      Timer timer = new Timer();
      TimerTask task = new TimerTask() {
         @Override
         public void run() {
            print();
            updateTopicTable();
            updateRealtimeTable();
            updateClientTable();
         }
      };
      timer.schedule(task, 0, interval * 1000);
   }

   //client table을 업데이트하는 함수 
   public void updateClientTable() {
      
      Connection conn = null;
      PreparedStatement pstmt = null;

      try {
         Class.forName(driver);
         conn = DriverManager.getConnection(url, user, pw);
         
         //client table에 client map에 삽입되어 있는 데이터 올리기. 만약 중복 있으면 패스
//         pstmt = conn.prepareStatement(
//               "INSERT INTO client(client_id, isAndroid) select ?, ? from dual where not exists ( select * from client where client_id=? and isAndroid=?)");
         
         pstmt = conn.prepareStatement("INSERT INTO client(name, msg_sending_count, accumulated_msg_size, platform, topic) VALUES(?, ?, ?, ?, ?)"
        		 + " ON DUPLICATE KEY UPDATE"
        		 + " msg_sending_count=VALUES(msg_sending_count),"
        		 + " accumulated_msg_size=VALUES(accumulated_msg_size),"
        		 + " platform=VALUES(platform),"
        		 + " topic=VALUES(topic)");
         
         for (Client c : clientMap.values()) {        	 
        	 
            pstmt.setString(1, c.getClientName());
            pstmt.setInt(2, c.getMsgSendingCount());
            pstmt.setInt(3, c.getAccumulatedMsgSize());
            pstmt.setString(4, c.getPlatform());
            pstmt.setNString(5, c.getTopic());

            pstmt.addBatch();
            pstmt.clearParameters();
         }
      } catch (SQLException e) {
         System.out.println("sql update client query error");
         e.printStackTrace();
      } catch (ClassNotFoundException e) {
         System.out.println("driver error");
         e.printStackTrace();
      } finally {
         if (pstmt != null)
            try {
               pstmt.executeBatch();
               pstmt.clearBatch();
               pstmt.close();
            } catch (SQLException e) {}
         if (conn != null)
            try {
               conn.close();
            } catch (SQLException e) {}
      }
   }


   //topic table을 업데이트하는 함수 
   public void updateTopicTable() {

      Connection conn = null;
      PreparedStatement pstmt = null;

      try {
         Class.forName(driver);
         conn = DriverManager.getConnection(url, user, pw);         
         pstmt = conn.prepareStatement("INSERT INTO topic(topic, msg_sending_count, accumulated_msg_size, start_date, finish_date, participants) VALUES(?, ?, ?, ?, ?, ?)"
         		+ " ON DUPLICATE KEY UPDATE"
         		+ " msg_sending_count=VALUES(msg_sending_count),"
         		+ " accumulated_msg_size=VALUES(accumulated_msg_size),"
         		+ " finish_date=VALUES(finish_date),"
         		+ " participants=VALUES(participants)"); //만약 topic이 이미 테이블에 올라가 있으면 해당 레코드를 업데이트 
      
         for (Topic t : topicMap.values()) {
	         pstmt.setString(1, t.getName());
	         pstmt.setInt(2, t.getMsgSendingCount());
	         pstmt.setInt(3, t.getAccumulatedMsgSize());
	         pstmt.setString(4, t.getStartDate());
	         pstmt.setString(5, t.getFinishDate());
	         pstmt.setInt(6, t.getParticipants());
	         
	         pstmt.addBatch();
	         pstmt.clearParameters();
         }
      
      } catch (SQLException e) {
         System.out.println("sql query error");
         e.printStackTrace();
      } catch (ClassNotFoundException e) {
         System.out.println("driver error");
         e.printStackTrace();
      } finally {
         if (pstmt != null)
            try {
               pstmt.executeBatch();
               pstmt.clearBatch();
               pstmt.close();
            } catch (SQLException e) {}

         if (conn != null)
            try {
               conn.close();
            } catch (SQLException e) {}
      }
   }
   
   //realtime table을 업데이트하는 함수 
   public void updateRealtimeTable() {

      int numberOfCurrentConnections = 0; // 현재 연결된 클라이언트 수를 저장하는 변수
      int accumulatedMsgSize = 0;
      int numberOfMsgs = 0;

      numberOfCurrentConnections = clientMap.size();

      for (Map.Entry<String, Topic> elem : topicMap.entrySet()) {
         numberOfMsgs += elem.getValue().getMsgSendingCount();
         accumulatedMsgSize += elem.getValue().getAccumulatedMsgSize();
      }

      Connection conn = null;
      PreparedStatement pstmt = null;

      try {
         Class.forName(driver);
         conn = DriverManager.getConnection(url, user, pw);

         pstmt = conn.prepareStatement(
               "INSERT INTO realtime(date, number_of_connections, accumulated_msg_size, number_of_msgs) VALUES(?, ?, ?, ?)"
        		 + " ON DUPLICATE KEY UPDATE"
        		 + " number_of_connections=VALUES(number_of_connections),"
        		 + " accumulated_msg_size=VALUES(accumulated_msg_size),"
        		 + " number_of_msgs=VALUES(number_of_msgs)");

         pstmt.setString(1, getCurrentTime());
         pstmt.setInt(2, numberOfCurrentConnections);
         pstmt.setInt(3, accumulatedMsgSize);
         pstmt.setInt(4, numberOfMsgs);

         int r = pstmt.executeUpdate();

      } catch (SQLException e) {
         System.out.println("realtime table sql query error");
         e.printStackTrace();
      } catch (ClassNotFoundException e) {
         System.out.println("driver error");
         e.printStackTrace();
      } finally {
         if (pstmt != null)
            try {
               pstmt.close();
            } catch (SQLException e) {}
         if (conn != null)
            try {
               conn.close();
            } catch (SQLException e) {}
      }
   }
   
   /* topic 테이블 레코드 삭제 */
	public void deleteFromTopicTable(String topic) {
		Connection conn=null;
		PreparedStatement pstmt=null;

		try {
			Class.forName(driver);
			conn=DriverManager.getConnection(url, user, pw);

			//연결 해제 된 클라이언트 정보를 client 테이블에서 삭제
			pstmt=conn.prepareStatement("DELETE FROM topic where topic=?");

			pstmt.setString(1, topic);

			pstmt.executeUpdate();

		} catch (SQLException e) {
			System.out.println("sql delete query error(client id : " + topic);
			e.printStackTrace();
		} catch (ClassNotFoundException e1) {
			System.out.println("driver error");
		} finally {
			if(pstmt!=null) try { pstmt.close(); } catch (SQLException e) { }
			if(conn!=null) try { conn.close(); } catch (SQLException e) { }
		}
	}
   
   // mqtt broker log를 읽고 데이터를 파싱하는 함수 
   public void read() {
      InputStreamReader inputStreamReader = new InputStreamReader(System.in);
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      
      String inputStr;
      String[] words;
      boolean isContainTopic = false;

      String clientId = "", topic;
      String[] clientInfo; // { "name", "topic", "platform" }
      int messageSize;
      
      try {
         while (true) {
            inputStr = null;
            if ((inputStr = bufferedReader.readLine()) != null) {    
            	
            	if (inputStr.contains(MessageType.CONNECT)) {//connect 시 client 객체 생성하여 clientMap과 clientList에 추가 
            		words = inputStr.split(" ");
            		clientId = words[4];
            		
            		// clientId = "*name_topic_platform"
            		if (clientId.contains("*")) {
            			if (!clientMap.containsKey(clientId)) {
            				clientInfo = clientId.substring(1).split("_");
            				System.out.println(clientInfo);
            				
                			Client c = new Client(clientInfo[0], clientInfo[1], clientInfo[2]);
                			clientMap.put(clientId, c);
                		}	
            		}
            		
            		
            	} else if (inputStr.contains(MessageType.DISCONNECT)) {// disconnect 시 clientList에서 삭제
            		words = inputStr.split(" ");
            		clientId = words[4];

            		if (clientMap.containsKey(clientId))
            			clientMap.remove(clientId);

            	} else if (inputStr.contains(MessageType.SOCKET_ERROR)) {// socket error 발생 시 clientList에서 삭제
            		words = inputStr.split(" ");
            		clientId = words[5].replace(",", "");
            		
            		if (clientMap.containsKey(clientId))
            			clientMap.remove(clientId);

            	} else if (inputStr.contains(MessageType.UNSUBSCRIBE)) {
            		
            	} else if (inputStr.contains(MessageType.SUBSCRIBE) || isContainTopic) { /* 클라이언트가 토픽을 구독(subscribe)할 때 */

            		if (isContainTopic) { 
            			words = inputStr.split("\\s+");
            			topic = words[1];

            			if (topic.contains("_join")) {
            				String pre = topic.replace("_join", "");
            				if (topicMap.containsKey(pre)) { // topicMap에 이미 존재하는 토픽이면 참가자만 증가시킴 // TODO: 이미 있는 사용자인지 확인이 필요?
            					topicMap.get(pre).increaseParticipants();
            				}
            				else { // 새로운 topic일 경우 저장
            					topicMap.put(pre, new Topic(pre));
            				}
            			}

            			isContainTopic = false;

            		} else {
            			words = inputStr.split(" ");
            			clientId = words[4]; // subscriber(클라이언트) id를 저장하는 변수
            			
            			isContainTopic = true; // 구독하는 토픽 이름이 써 있는 다음 줄을 필수적으로 읽어야 하기 때문에 관련 변수의 값을 변환시킴
            		}

            	} else if (inputStr.contains(MessageType.RECEIVED_PUBLISH)) { /* publisher가 메시지를 보냈을 때 */
            		words = inputStr.split(" ");
            		
            		clientId = words[4];
            		clientInfo = clientId.substring(1).split("_"); // name, topic, platform
            		
            		topic = words[9].replaceAll("\'|,", "");
            		messageSize = Integer.parseInt(words[11].replace("(", ""));

            		if (topic.contains("_data")) {
            			String pre = topic.replace("_data", "");
            			if (topicMap.containsKey(pre)) {
            				topicMap.get(pre).increaseAccumulatedMsgSize(messageSize);
            				topicMap.get(pre).increaseMsgSendingCount();
            			}
            			if (clientMap.containsKey(clientId)) {
            				clientMap.get(clientId).increaseAccumulatedMsgSize(messageSize);
            				clientMap.get(clientId).increaseMsgSendingCount();
            			}
            		} else if (topic.contains("_delete")) {// _delete 포함 시 토픽이 종료되었다는 의미임으로 토픽이 끝난 시간을 저장하고 토픽 이름 변경 
            			String pre = topic.replace("_delete", "");
            			if (topicMap.containsKey(pre)) {
            				topicMap.get(pre).setFinishDate();
            				
            				// topic 중복 허용을 위해 토픽이 종료되면 토픽 뒤에 랜덤 숫자를 붙여서 저장 
            				topicMap.get(pre).changeTopicName();

            				deleteFromTopicTable(pre); // 사용이 종료된 토픽 삭제
            				// topicMap.remove(pre);
            			}
            		}
            	}
            }
         }
      } catch (Exception e) {
    	  e.printStackTrace();
      } finally {
    	  if(bufferedReader != null) try { bufferedReader.close(); } catch (IOException e) { }
    	  if(inputStreamReader != null) try { inputStreamReader.close(); } catch (IOException e) { }
      }
   }

   //현재 시간 리턴하는 함수
   public String getCurrentTime() {
	   Date d = new Date();
	   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	   return sdf.format(d);
   }

   public void print() {
	   System.out.println("-----------LogReader-----------");
	   System.out.println("current time : " + getCurrentTime());
	   System.out.println("number of clients : " + clientMap.size());
	   System.out.println("number of topics : " + topicMap.size());

   }
}