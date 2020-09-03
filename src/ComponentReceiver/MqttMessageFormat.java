package ComponentReceiver;

/* 모바일 앱으로부터 수신한 MQTT 메시지 형식 */
public class MqttMessageFormat {

	private ComponentCount componentCount;

	public MqttMessageFormat(ComponentCount componentCount) {
		this.componentCount = componentCount;
	}
	
	public ComponentCount getComponentCount() {
		return this.componentCount;
	}

}
