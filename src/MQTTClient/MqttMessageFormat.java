package MQTTClient;

public class MqttMessageFormat {

	private ComponentCount componentCount;

	public MqttMessageFormat(ComponentCount componentCount) {
		this.componentCount = componentCount;
	}
	
	public ComponentCount getComponentCount() {
		return this.componentCount;
	}

}
