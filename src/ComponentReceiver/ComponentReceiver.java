package ComponentReceiver;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.gson.Gson;


public class ComponentReceiver implements MqttCallback {
	
	private String ip;
	private String port;

	private String topic;
	private int qos;

	private MqttClient client;
	private String clientId;

	/* [key] topic name, [value] ComponentCount */
	public static ConcurrentHashMap<String, ComponentCount> componentMap = new ConcurrentHashMap<>();
	
	private Gson gson = new Gson();
	
	public ComponentReceiver() {
		
		this.ip = "192.168.0.36";
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
		
		

	}

	@Override
	public void connectionLost(Throwable cause) {
		System.out.println(cause.getCause());
		cause.printStackTrace();
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

	public static void removeComponent(String topic) {
		componentMap.remove(topic);
	}


}
