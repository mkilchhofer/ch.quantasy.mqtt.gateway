/*
 *   "SeMqWay"
 *
 *    SeMqWay(tm): A gateway to provide an MQTT-View for any micro-service (Service MQTT-Gateway).
 *
 *    Copyright (c) 2016 Bern University of Applied Sciences (BFH),
 *    Research Institute for Security in the Information Society (RISIS), Wireless Communications & Secure Internet of Things (WiCom & SIoT),
 *    Quellgasse 21, CH-2501 Biel, Switzerland
 *
 *    Licensed under Dual License consisting of:
 *    1. GNU Affero General Public License (AGPL) v3
 *    and
 *    2. Commercial license
 *
 *
 *    1. This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *    2. Licensees holding valid commercial licenses for TiMqWay may use this file in
 *     accordance with the commercial license agreement provided with the
 *     Software or, alternatively, in accordance with the terms contained in
 *     a written agreement between you and Bern University of Applied Sciences (BFH),
 *     Research Institute for Security in the Information Society (RISIS), Wireless Communications & Secure Internet of Things (WiCom & SIoT),
 *     Quellgasse 21, CH-2501 Biel, Switzerland.
 *
 *
 *     For further information contact <e-mail: reto.koenig@bfh.ch>
 *
 *
 */
package ch.quantasy.mqtt.gateway.client.message;

import ch.quantasy.mqtt.communication.mqtt.PublisherCallback;
import ch.quantasy.mqtt.gateway.client.GatewayClient;
import ch.quantasy.mqtt.gateway.client.contract.AServiceContract;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author reto
 */
public class PublishingMessageCollector<S extends AServiceContract> implements PublisherCallback {

    private final MessageCollector messageCollector;
    private final GatewayClient<S> gatewayClient;

    public PublishingMessageCollector(MessageCollector messageCollector, GatewayClient<S> gatewayClient) {
        this.messageCollector = messageCollector;
        this.gatewayClient = gatewayClient;
    }

    public MessageCollector getMessageCollector() {
        return messageCollector;
    }

    public GatewayClient getGatewayClient() {
        return gatewayClient;
    }

    public void readyToPublish() {
        for (String topic : this.getMessageCollector().getTopics()) {
            readyToPublish(topic);
        }
    }

    public void readyToPublish(String topic, Message message) {
        this.getMessageCollector().add(topic, message);
        this.readyToPublish(topic);
    }

    public void readyToPublish(String topic) {
        this.gatewayClient.getCommunication().readyToPublish(this, topic);
    }

    @Override
    public MqttMessage manageMessageToPublish(String topic) {
        MqttMessage message = null;
        if (topic == null) {
            return message;
        }
        SortedSet<Message> messages = getMessageCollector().clearMessages(topic);
        if (messages == null) {
            return message;
        }
        try {
            message = new MqttMessage(gatewayClient.getContract().getObjectMapper().writeValueAsBytes(messages));
            message.setQos(1);
            message.setRetained(true);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(PublishingMessageCollector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return message;
    }

}
