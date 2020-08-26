package MQTTClient;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTTClient implements MqttCallback {

	private String ip;
	private String port;
	private String clientId;
	
	private MqttClient client;

	
	
	
	public MQTTClient(String ip, String port) {
		this.ip = ip;
		this.port = port;
		this.clientId = "Admin";
		
		try {
			client = new MqttClient("tcp://" + ip + ":" + port, clientId, new MemoryPersistence());
		} catch (MqttException e) {
			e.printStackTrace();
		}
		
		try { 
			client.connect();
		} 
		catch (MqttException e) { 
			e.printStackTrace(); 
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
		System.out.println("topic = " + topic);
		System.out.println("msg = " + msg);
	}

	

}
