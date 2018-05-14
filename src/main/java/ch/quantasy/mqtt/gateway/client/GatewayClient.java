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
package ch.quantasy.mqtt.gateway.client;

import ch.quantasy.mqtt.gateway.client.message.MessageReceiver;
import ch.quantasy.mqtt.gateway.client.contract.AServiceContract;
import ch.quantasy.mqtt.communication.mqtt.MQTTCommunication;
import ch.quantasy.mqtt.communication.mqtt.MQTTCommunicationIntent;
//import ch.quantasy.mqtt.communication.mqtt.MQTTCommunicationIntent;
import ch.quantasy.mqtt.communication.mqtt.Testament;
import ch.quantasy.mqtt.gateway.client.message.Message;
import ch.quantasy.mqtt.gateway.client.message.MessageCollector;
import ch.quantasy.mqtt.gateway.client.message.PublishingMessageCollector;
import com.fasterxml.jackson.core.JsonPointer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;

/**
 *
 * @author reto
 * @param <S>
 */
public class GatewayClient<S extends AServiceContract> implements MqttCallback {

    private final S contract;
    private final MQTTCommunication communication;
    private final Map<String, Set<MessageReceiver>> messageConsumerMap;

    private final HashMap<String, MqttMessage> contractDescriptionMap;

    private final MessageCollector collector;
    private final PublishingMessageCollector<S> publishingCollector;
    //private final MQTTCommunicationIntent intent;

    /**
     * One executorService pool for all implemented Services within a JVM
     */
    private final static ExecutorService EXECUTOR_SERVICE;
    private final static ScheduledExecutorService TIMER_SERVICE;

    static {
        EXECUTOR_SERVICE = Executors.newCachedThreadPool();
        TIMER_SERVICE = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2, (Runnable r) -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public GatewayClient(URI mqttURI, String clientID, S contract) throws MqttException {
        this.contract = contract;
        collector = new MessageCollector();
        publishingCollector = new PublishingMessageCollector(collector, this);
        messageConsumerMap = new HashMap<>();
        contractDescriptionMap = new HashMap<>();
        communication = new MQTTCommunication();
        Testament testament = new Testament();
        testament.isLastWillRetained = true;
        try {
            testament.lastWillMessage = contract.getObjectMapper().writeValueAsBytes(new ConnectionStatus(contract.OFFLINE));
        } catch (JsonProcessingException ex) {
            Logger.getLogger(GatewayClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        testament.lastWillQoS = 1;
        testament.willTopic = contract.STATUS_CONNECTION;
        MQTTCommunicationIntent intent = new MQTTCommunicationIntent();
        intent.clientID = clientID;
        intent.automaticReconnect = true;
        intent.isCleanSession = false;
        intent.serverURIs = new URI[]{mqttURI};
        intent.testament = testament;
        intent.mqttCallback = this;
        communication.setIntent(intent);
        contract.publishContracts(this);
    }

    public PublishingMessageCollector<S> getPublishingCollector() {
        return publishingCollector;
    }

    /**
     * Convenience Method that calls internal PublishingMessageCollector
     *
     * @param topic
     * @param message
     */
    public void readyToPublish(String topic, Message message) {
        publishingCollector.readyToPublish(topic, message);
    }

    /**
     * Convenience method that calls internal PublishingMessageCollector
     *
     * @param topic
     */
    public void clearPublish(String topic) {
        publishingCollector.clearPublish(topic);
    }

    public MessageCollector getCollector() {
        return collector;
    }

    public MQTTCommunicationIntent getIntent() {
        return communication.getIntent();
    }

    public MQTTCommunication getCommunication() {
        return communication;
    }

    public void quit() {
        try {
            this.disconnect();
            this.communication.quit();
        } catch (MqttException ex) {
            Logger.getLogger(GatewayClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setIntent(MQTTCommunicationIntent intent) {
        communication.setIntent(intent);
    }

    public void connect() throws MqttException {
        if (communication.isConnected()) {
            return;
        }
        MQTTCommunicationIntent intent = communication.getIntent();
        intent.connect = true;
        communication.setIntent(intent);
        try {
            communication.publishActualWill(contract.getObjectMapper().writeValueAsBytes(new ConnectionStatus(contract.ONLINE)));
        } catch (JsonProcessingException ex) {
            Logger.getLogger(GatewayClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        messageConsumerMap.keySet().forEach((subscription) -> {
            communication.subscribe(subscription, 1);
        });
    }

    public void disconnect() throws MqttException {
        if (!communication.isConnected()) {
            return;
        }
        try {
            communication.publishActualWill(contract.getObjectMapper().writeValueAsBytes(Boolean.FALSE));
            messageConsumerMap.keySet().forEach((subscription) -> {
                communication.unsubscribe(subscription);
            });
        } catch (JsonProcessingException ex) {
            Logger.getLogger(GatewayClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        MQTTCommunicationIntent intent = communication.getIntent();
        intent.connect = false;
        communication.setIntent(intent);
    }

    /**
     *
     * @param topic
     * @param consumer
     */
    public synchronized void subscribe(String topic, MessageReceiver consumer) {
        if (!messageConsumerMap.containsKey(topic)) {
            messageConsumerMap.put(topic, new HashSet<>());
            communication.subscribe(topic, 1);
        }
        messageConsumerMap.get(topic).add(consumer);
    }

    public synchronized void unsubscribe(String topic, MessageReceiver consumer) {
        if (messageConsumerMap.containsKey(topic)) {
            Set<MessageReceiver> messageConsumers = messageConsumerMap.get(topic);
            messageConsumers.remove(consumer);
            if (messageConsumers.isEmpty()) {
                unsubscribe(topic);
            }
        }
    }

    public synchronized void unsubscribe(String topic) {
        synchronized (messageConsumerMap) {
            messageConsumerMap.remove(topic);
        }
        communication.unsubscribe(topic);
    }

    public Set<String> getSubscriptionTopics() {
        synchronized (messageConsumerMap) {
            return messageConsumerMap.keySet();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        //System.out.println("Delivery is done.");
    }

    public S getContract() {
        return contract;
    }

    private ScheduledFuture connectionFuture;

    @Override
    public void connectionLost(Throwable thrwbl) {
        Logger.getLogger(GatewayClient.class
                .getName()).log(Level.SEVERE, "Connection to subscriptions lost... will try again in 3 seconds", thrwbl);
        if (this.connectionFuture != null) {
            return;
        }
        connectionFuture = TIMER_SERVICE.scheduleAtFixedRate(() -> {
            try {
                if (connectionFuture != null) {
                    MQTTCommunicationIntent intent = communication.getIntent();
                    intent.connect = true;
                    communication.setIntent(intent);
                    connectionFuture.cancel(false);
                    connectionFuture = null;

                    communication.publishActualWill(contract.getObjectMapper().writeValueAsBytes(new ConnectionStatus(contract.ONLINE)));
                    messageConsumerMap.keySet().forEach((topic) -> {
                        communication.subscribe(topic, 1);
                    });
                    Logger.getLogger(GatewayClient.class
                            .getName()).log(Level.INFO, "Connection and topic-subscriptions re-established");

                }

            } catch (Exception ex) {
            }
        }, 0, 3000, TimeUnit.MILLISECONDS);
    }

    public static boolean compareTopic(final String actualTopic, final String subscribedTopic) {
        return actualTopic.matches(subscribedTopic.replaceAll("\\+", "[^/]+").replaceAll("/#", "(|/.*)"));
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) {
        byte[] payload = mm.getPayload();
        if (payload == null || payload.length == 0) {
            return;
        }
        Set<MessageReceiver> messageConsumers = new HashSet<>();
        synchronized (messageConsumerMap) {
            this.messageConsumerMap.keySet().stream().filter((subscribedTopic) -> (compareTopic(topic, subscribedTopic))).forEachOrdered((subscribedTopic) -> {
                messageConsumers.addAll(this.messageConsumerMap.get(subscribedTopic));
            });
        }
        //This way, even if a consumer has been subscribed itself under multiple topic-filters,
        //it is only called once per topic match.
        messageConsumers.forEach((consumer) -> {
            EXECUTOR_SERVICE.submit(() -> {
                try {
                    consumer.messageReceived(topic, payload);
                } catch (Exception ex) {
                    Logger.getLogger(getClass().
                            getName()).log(Level.INFO, null, ex);
                }
            });
        });

    }

    public static ExecutorService getEXECUTOR_SERVICE() {
        return EXECUTOR_SERVICE;
    }

    public static ScheduledExecutorService getTIMER_SERVICE() {
        return TIMER_SERVICE;
    }

    public <M extends Message> SortedSet<M> toMessageSet(byte[] payload, Class<M> messageClass) throws Exception {
        JavaType endType = contract.getObjectMapper().getTypeFactory().constructCollectionType(TreeSet.class, messageClass);
        return contract.getObjectMapper().readValue(payload, endType);
    }

    /**
     *
     * This method allows to map a yaml document into an other yaml document. It
     * accepts the payload and tries its best to translate and fill in the gaps
     * and returning the mapped yaml document..
     *
     * @param messageClass The MessageClass to map into (Target)
     * @param targetSourceMap The translation map target field, source field.
     * @return The sorted set of desired target messages
     * @throws Exception
     *
     * http://proliferay.com/create-json-by-jackson-api/
     * https://cassiomolin.com/2016/07/13/using-jackson-and-json-path-to-query-and-parse-an-arbitrary-json-node/
     *
     * @param payload The serialized set of source messages
     * @param targetSourceMap The translation map target field, source field
     * (written as XTree)
     * @return The translated and serialized yaml structure.
     * @throws Exception
     */
    public byte[] map(byte[] payload, Map<String, String> targetSourceMap) throws Exception {
        final String separator = Character.toString(JsonPointer.SEPARATOR);
        ArrayNode arrayNode = getContract().getObjectMapper().createArrayNode();
        JsonNode sourceTree = (getContract().getObjectMapper().readTree(payload));
        System.out.println("tree: " + sourceTree);

        JsonNode element = sourceTree;
        //Check if the tree (payload) is an array of elements
        Iterator<JsonNode> elements = null;
        if (sourceTree.isArray()) {
            elements = sourceTree.elements();
            if (elements.hasNext()) {
                element = elements.next();
            }
        }

        while (element != null) {

            ObjectNode objectNode = getContract().getObjectMapper().createObjectNode();
            String yamlStructure = "";
            // Create a new yaml structure using the target fields and the source values.
            for (Map.Entry<String, String> entry : targetSourceMap.entrySet()) {
                yamlStructure += entry.getKey();
                int spacer = 0;
                while (yamlStructure.contains(separator)) {
                    String spaces = "  ";
                    for (int i = 0; i < spacer; i++) {
                        spaces += "  ";
                    }
                    yamlStructure = yamlStructure.replaceFirst(separator, ": \n" + spaces);
                    spacer++;

                }
                yamlStructure += ": " + element.at(entry.getValue()) + "\n";
            }
            getContract().getObjectMapper().readerForUpdating(objectNode).readValue(yamlStructure);
            arrayNode.add(objectNode);
            if (elements != null && elements.hasNext()) {
                element = elements.next();
            } else {
                element = null;
            }
        }
        return getContract().getObjectMapper().writeValueAsBytes(arrayNode);
    }

}
