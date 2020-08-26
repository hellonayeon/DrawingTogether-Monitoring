package DBReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import LogReader.Topic;

/*DB에 저장된 데이터들을 모두 읽어와 통계 데이터로 가공하는 클래스*/
public class DBReader implements Runnable {

   private String driver;
   private String url;
   private String user;
   private String pw;
   
   private ConcurrentHashMap<String, RealtimeInfo> realtimeMap = new ConcurrentHashMap<>(); //date가 key, RealtimeInfo가 value인 map
   private ConcurrentHashMap<String, TopicInfo> topicMap = new ConcurrentHashMap<>(); //topic 이름이 key, TopicInfo 가 value인 map

   public DBReader() {
      readProperties();
   }
   
   @Override
   public void run() {
      startTimer();
   }

   public void startTimer() {
      System.out.println("DBReader starts at " + getCurrentTime());

      Timer timer = new Timer();
      TimerTask task = new TimerTask() {
         @Override
         public void run() {
            print();
            getRealtimeData();
            getTopicData();
            sortRealtimeData();
            sortTopicData();
            updateRealTimeInfoTable();
            updateTopicInfoTable();
            deleteRealtimeTables();
         }

      };
      timer.schedule(task, 0, (long) 8.64e+7);
   }

   // db.properties를 읽어오는 함수
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

   //db에서 realtime table의 데이터를 가져와 realtimeMap에 저장하는 함수 
   public void getRealtimeData() {
      Connection conn = null;
      PreparedStatement pstmt = null;
      ResultSet rs = null;

      try {
         Class.forName(driver);
         conn = DriverManager.getConnection(url, user, pw);
         
         //시간 별로 데이터를 모으기 위해 0~23시의 데이터를 따로 수집 
         for (int i = 0; i < 23; i++) {

            pstmt = conn.prepareStatement("SELECT * FROM realtime where date = ?");
            pstmt.setString(1, "*-*-* " +String.format("%02d", i)+ ":*:*"); 
            rs = pstmt.executeQuery();
            RealtimeInfo info = new RealtimeInfo();
            
            while (rs.next()) {
               info.setDate(rs.getString("date")); //시간은 계속해서 다시 써져도 어쩔 수 없음.. 분과 초는 중요하지 않기 때문에!
               info.increaseTotalConnections(rs.getInt("connections"));
               info.increaseTotalMsgSize(rs.getInt("accumulated_msg_size"));
               info.increaseTotalMsgNums(rs.getInt("number_of_messages"));
               info.recordCnt++;
               realtimeMap.put(info.getDate(), info);//시간 별로 하나의 RealtimeInfo 객체를 저장 
            }
         }

      } catch (SQLException e) {
         System.out.println("sql connection error");
      } catch (ClassNotFoundException e1) {
         System.out.println("driver error");
      } finally {
         if( rs != null)
            try {
               rs.close();
            } catch (SQLException e) {}
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
   
   //db에서 받아온 realtime table data를 24시간으로 나누어서 시간 별 평균을 계산하는 함수  
   public void sortRealtimeData() {
      for(String   key: realtimeMap.keySet()) {
         RealtimeInfo info = realtimeMap.get(key);
         String date = info.getDate().split(":")[0]; //시까지 저장하기 위해 분과 초는 잘라내기
         
         int averageMsgSize = info.getTotalMsgNums() / info.recordCnt;
         int averageConnections = info.getTotalConnections() / info.recordCnt;
         int averageMsgNums = info.getTotalMsgNums() / info.recordCnt;
         
         info.setDate(date);
         info.setTotalMsgSize(averageMsgSize);
         info.setTotalConnections(averageConnections);
         info.setTotalMsgNums(averageMsgNums);
      }
   }
   
   
   //sortRealtimeData()를 통해 가공한 데이터를 average_info table에 저장 
   public void updateRealTimeInfoTable() {
      Connection conn = null;
      PreparedStatement pstmt = null;
      
      try {
         Class.forName(driver);
         conn = DriverManager.getConnection(url, user, pw);
         
         pstmt = conn.prepareStatement("INSERT INTO realtime_info(date, connections, number_of_messages, accumulated_msg_size) VALUES");
         
         for (RealtimeInfo ri : realtimeMap.values()) {
         
         pstmt.setString(1, ri.getDate());
         pstmt.setInt(2, ri.getTotalConnections());
         pstmt.setInt(3, ri.getTotalMsgNums());
         pstmt.setInt(4, ri.getTotalMsgSize());

         
         pstmt.addBatch();
         pstmt.clearParameters();
         }
      } catch (SQLException e) {
         System.out.println("sql connection error");
      } catch (ClassNotFoundException e1) {
         System.out.println("driver error");
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
   
   //db에서 topic table의 데이터를 가져와 topicMap에 저장하는 함수 
   public void getTopicData() {
      Connection conn = null;
      PreparedStatement pstmt = null;
      ResultSet rs = null;

      try {
         Class.forName(driver);
         conn = DriverManager.getConnection(url, user, pw);
         for (int i = 00; i < 23; i++) {

            pstmt = conn.prepareStatement("SELECT * FROM topic where start_date = ?");
            pstmt.setString(1, "*-*-* " +String.format("%02d", i) + ":*:*");
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
               TopicInfo ti = new TopicInfo(rs.getString("name"));
               ti.setStartTime(rs.getString("start_time"));
               ti.setFinishTime(rs.getString("finish_time"));
               ti.setParticipants(rs.getInt("participants"));
               topicMap.put(ti.getName(),ti);
            }
         }

      } catch (SQLException e) {
         System.out.println("sql connection error");
      } catch (ClassNotFoundException e1) {
         System.out.println("driver error");
      } finally {
         if( rs != null)
            try {
               rs.close();
            } catch (SQLException e) {}
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
   
   //db에서 받아온 topic table data에서 토픽 시작시간과 종료시간의 차이를 계산하여 지속시간을 알아내서 저장하는 함수  
   public void sortTopicData(){
      for(String   key: topicMap.keySet()) {
         TopicInfo info = topicMap.get(key);
         String date = info.getStartTime().split(":")[0];
         
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
         Date start = null;
         Date finish = null;
         
         //
         try {
            start = sdf.parse(info.getStartTime());
            finish = sdf.parse(info.getFinishTime());

         } catch (ParseException e) {
            System.out.println("parse exception error");
         }
         
         long diff = finish.getTime() - start.getTime(); //topic 종료 시간과 시작 시간의 차이 
         long min = diff/60000;
         info.setDuration(min);
         info.setStartTime(date);
      }
   }
   
   //sortTopicInfo()를 통해 가공한 데이터를 topic_info table에 저장 
   public void updateTopicInfoTable() {
      Connection conn = null;
      PreparedStatement pstmt = null;
      
      try {
         Class.forName(driver);
         conn = DriverManager.getConnection(url, user, pw);
         
         pstmt = conn.prepareStatement("INSERT INTO topic_info(topic, startTime, duration, participants) VALUES");
         
         for(TopicInfo ti: topicMap.values()) {
            pstmt.setString(1, ti.getName());
            pstmt.setString(2, ti.getStartTime());
            pstmt.setLong(3, ti.getDuration());
            pstmt.setLong(4, ti.getParticipants());
            
            pstmt.addBatch();
            pstmt.clearParameters();
            
         }
      } catch (SQLException e) {
         System.out.println("sql connection error");
      } catch (ClassNotFoundException e1) {
         System.out.println("driver error");
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
   
   //데이터베이스로부터 모든 데이터를 가져와 가공한 뒤 실시간 테이블을 비워주는 작업을 하는 함수 
   public void deleteRealtimeTables() {
      // TODO Auto-generated method stub
      
   }

   public void print() {
      System.out.println("-------------DBReader-------------");
      System.out.println("current time: " + getCurrentTime());
   }

   //현제 시간을 return하는 함수 
   public String getCurrentTime() {
      Date d = new Date();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

      return sdf.format(d);
   }
}