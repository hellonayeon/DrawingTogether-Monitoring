package ComponentReceiver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.gson.Gson;


public class ComponentReceiver implements MqttCallback, Runnable {

	private int interval;
	
	private String ip;
	private String port;

	private String topic;
	private int qos;

	private MqttClient client;
	private String clientId;

	private String driver;
	private String url;
	private String user;
	private String pw;
	

	private double duration;
	
	private static ConcurrentHashMap<String, ComponentCount> componentMap = new ConcurrentHashMap<>();
	
	private Gson gson = new Gson();
	
	public ComponentReceiver(int interval) {

		this.interval = interval;
		
		this.ip = "192.168.200.102";
		this.port = "1883";

		this.topic = "monitoring";
		this.qos = 2;
		this.clientId = "Monitoring System Admin";

		try {
			client = new MqttClient("tcp://" + this.ip + ":" + this.port, clientId, new MemoryPersistence());
			client.setCallback(this);
		} catch (MqttException e) {
			e.printStackTrace();
		}

		try {
			client.connect();
		} catch (MqttException e) {
			e.printStackTrace();
		}

		try {
			client.subscribe(this.topic, this.qos); // 시스템 토픽을 구독 - 모바일 앱에서			
		} catch (MqttException e) {
			e.printStackTrace();
		}

		readProperties();
		
		// 모든 DB 테이블 비우기
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, pw);

			pstmt = conn.prepareStatement("DELETE FROM component");
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("all table delete (mqtt client class) query error");
			e.printStackTrace();
		} catch (ClassNotFoundException e1) {
			System.out.println("driver error");
		}

	}

	@Override
	public void connectionLost(Throwable cause) {
		System.out.println(cause.getCause());

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {	
		
		MqttMessageFormat mmf = gson.fromJson(new String(msg.getPayload()), MqttMessageFormat.class);
		
		ComponentCount componentCount = mmf.getComponentCount();
		
		if(componentMap.contains(componentCount.getTopic())) // 이미 컴포넌트 정보가 저장된 토픽인 경우 값을 대체
			componentMap.replace(componentCount.getTopic(), componentCount);
		else
			componentMap.put(componentCount.getTopic(), componentCount);
	
	}
	
	// db.properties 를 읽어오는 함수
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
	}

	// timer 함수
	public void startTimer() {
		System.out.println("MQTTClient component update starts at " + getCurrentTime());

		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				updateComponentTable();
			}
		};
		timer.schedule(task, 0, interval * 1000);
	}

	/* 컴포넌트 테이블을 업데이트하는 함수 */
	public void updateComponentTable() {
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, pw);

			pstmt = conn.prepareStatement(
					"INSERT INTO component(stroke, rect, oval, text, image, topic) VALUES(?, ?, ?, ?, ?, ?)"
							+ " ON DUPLICATE KEY UPDATE" + " stroke=VALUES(stroke)," + " rect=VALUES(rect),"
							+ " oval=VALUES(oval)," + " text=VALUES(text)," + " image=VALUES(image),"
							+ " topic=VALUES(topic)");

			for (ComponentCount cc : componentMap.values()) {

				pstmt.setInt(1, cc.getStroke());
				pstmt.setInt(2, cc.getRect());
				pstmt.setInt(3, cc.getOval());
				pstmt.setInt(4, cc.getText());
				pstmt.setInt(5, cc.getImage());
				pstmt.setNString(6, cc.getTopic());

				pstmt.executeUpdate();

			}
		} catch (SQLException e) {
			System.out.println("sql update component query error");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("driver error");
			e.printStackTrace();
		} finally {
			if (pstmt != null)
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if (conn != null)
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
	}

	public static void removeComponent(String topic) {
		componentMap.remove(topic);
	}

	// 현재 시간 리턴하는 함수
	public String getCurrentTime() {
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		return sdf.format(d);
	}

	public void print() {
		System.out.println("-----------MQTTClient-----------");
	}

}
