/*
 * /*
 *  *   "SeMqWay"
 *  *
 *  *    SeMqWay(tm): A gateway to provide an MQTT-View for any micro-service (Service MQTT-Gateway).
 *  *
 *  *    Copyright (c) 2016 Bern University of Applied Sciences (BFH),
 *  *    Research Institute for Security in the Information Society (RISIS), Wireless Communications & Secure Internet of Things (WiCom & SIoT),
 *  *    Quellgasse 21, CH-2501 Biel, Switzerland
 *  *
 *  *    Licensed under Dual License consisting of:
 *  *    1. GNU Affero General Public License (AGPL) v3
 *  *    and
 *  *    2. Commercial license
 *  *
 *  *
 *  *    1. This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Affero General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Affero General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Affero General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  *
 *  *    2. Licensees holding valid commercial licenses for TiMqWay may use this file in
 *  *     accordance with the commercial license agreement provided with the
 *  *     Software or, alternatively, in accordance with the terms contained in
 *  *     a written agreement between you and Bern University of Applied Sciences (BFH),
 *  *     Research Institute for Security in the Information Society (RISIS), Wireless Communications & Secure Internet of Things (WiCom & SIoT),
 *  *     Quellgasse 21, CH-2501 Biel, Switzerland.
 *  *
 *  *
 *  *     For further information contact <e-mail: reto.koenig@bfh.ch>
 *  *
 *  *
 */
package ch.quantasy.mqtt.gateway.service;

import ch.quantasy.mqtt.communication.mqtt.MQTTCommunication;
import ch.quantasy.mqtt.communication.mqtt.MQTTParameters;
import ch.quantasy.mqtt.gateway.client.ClientContract;
import ch.quantasy.mqtt.gateway.client.GatewayClient;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.net.URI;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import ch.quantasy.mqtt.gateway.client.MessageReceiver;

/**
 *
 * @author reto
 * @param <S>
 */
public abstract class AbstractService<S extends ServiceContract> implements MessageReceiver {

    
    private final GatewayClient<S> gatewayClient;

    public AbstractService(URI mqttURI, String clientID, S contract) throws MqttException {
        gatewayClient=new GatewayClient(mqttURI, clientID, contract);
        gatewayClient.subscribe(contract.INTENT + "/#", this);
        gatewayClient.connect();
    }
    
    public void addDescription(String topic,Object value){
        gatewayClient.addDescription(topic, value);
    }
    
    public void addEvent(String topic, Object value){
        gatewayClient.addEvent(topic, value);
    }
    
    public void addIntent(String topic, Object value){
        gatewayClient.addIntent(topic, value);
    }
    
    public void addStatus(String topic, Object value){
        gatewayClient.addStatus(topic, value);
    }

    public S getContract() {
        return gatewayClient.getContract();
    }

    public ObjectMapper getMapper() {
        return gatewayClient.getMapper();
    }

    public GatewayClient<S> getGatewayClient() {
        return gatewayClient;
    }
    
    

    /**
     * This is called within a new runnable! Be sure this method is programmed
     * thread safe!
     *
     * @param topic This String is never null and contains the topic of the mqtt
     * message.
     * @param payload This byte[] is never null and contains the payload of the
     * mqtt message.
     * @throws Exception Any exception is handled 'gracefully' within
     * AbstractService.
     */
    public abstract void messageReceived(String topic, byte[] payload) throws Exception;
}
