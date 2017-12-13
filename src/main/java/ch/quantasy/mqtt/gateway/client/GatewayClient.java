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
import ch.quantasy.mqtt.communication.mqtt.MQTTCommunicationCallback;
import ch.quantasy.mqtt.communication.mqtt.MQTTParameters;
import ch.quantasy.mqtt.gateway.client.message.Message;
import ch.quantasy.mqtt.gateway.client.message.MessageCollector;
import ch.quantasy.mqtt.gateway.client.message.PublishingMessageCollector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
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

/**
 *
 * @author reto
 * @param <S>
 */
public class GatewayClient<S extends AServiceContract> implements MQTTCommunicationCallback {

    private final MQTTParameters parameters;
    private final S contract;
    private final MQTTCommunication communication;
    private final Map<String, Set<MessageReceiver>> messageConsumerMap;

    private final HashMap<String, MqttMessage> contractDescriptionMap;
    
    private final MessageCollector collector;
    private final PublishingMessageCollector<S> publishingCollector;


    /**
     * One executorService pool for all implemented Services within a JVM
     */
    private final static ExecutorService EXECUTOR_SERVICE;
    private final static ScheduledExecutorService TIMER_SERVICE;

    static {
        EXECUTOR_SERVICE = Executors.newCachedThreadPool();
        TIMER_SERVICE = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), (Runnable r) -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public GatewayClient(URI mqttURI, String clientID, S contract) throws MqttException {
        this.contract = contract;
        collector=new MessageCollector();
        publishingCollector=new PublishingMessageCollector(collector,this);
        messageConsumerMap = new HashMap<>();
        contractDescriptionMap = new HashMap<>();
        communication = new MQTTCommunication();
        parameters = new MQTTParameters();
        parameters.setClientID(clientID);
        parameters.setIsCleanSession(false);
        parameters.setIsLastWillRetained(true);
        parameters.setLastWillMessage(contract.OFFLINE.getBytes());
        parameters.setLastWillQoS(1);
        parameters.setServerURIs(mqttURI);
        parameters.setWillTopic(contract.STATUS_CONNECTION);
        parameters.setMqttCallback(this);
        //communication.connect(parameters);
        //communication.publishActualWill(contract.ONLINE.getBytes());
        //publishDescription(getContract().STATUS_CONNECTION, "[" + getContract().ONLINE + "|" + getContract().OFFLINE + "]");
        contract.publishContracts(this);
    }

    public PublishingMessageCollector<S> getPublishingCollector() {
        return publishingCollector;
    }

    public MessageCollector getCollector() {
        return collector;
    }
    
    

    /**
     *
     * @return
     */
    public MQTTParameters getParameters() {
        return parameters;
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

    public void connect() throws MqttException {
        if (communication.isConnected()) {
            return;
        }
        if (getSubscriptionTopics().isEmpty()) {
            MQTTParameters params = new MQTTParameters(parameters);
            communication.connect(params);
        }
        communication.connect(parameters);

        communication.publishActualWill(contract.ONLINE.getBytes());
        messageConsumerMap.keySet().forEach((subscription) -> {
            communication.subscribe(subscription, 1);
        });
    }

    public void disconnect() throws MqttException {
        if (!communication.isConnected()) {
            return;
        }
        try {
            communication.publishActualWill(getMapper().writeValueAsBytes(Boolean.FALSE));
            messageConsumerMap.keySet().forEach((subscription) -> {
                communication.unsubscribe(subscription);
            });
        } catch (JsonProcessingException ex) {
            Logger.getLogger(GatewayClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        communication.disconnect();
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

    public ObjectMapper getMapper() {
        return contract.getObjectMapper();
    }

    /**
     * This method has to become obsolete...
     *
     * @param topic
     * @return
     */
    @Override
    public MqttMessage manageMessageToPublish(String topic) {

        //For the contract, only the latest one per topic is of interest.
        synchronized (contractDescriptionMap) {
            MqttMessage message = contractDescriptionMap.get(topic);
            if (message != null) {
                return message;
            }
        }

        //For the intent, each of which per topic is of interest. Hence one after the other is called.
//        synchronized (intentMap) {
//            Deque<MqttMessage> intents = intentMap.get(topic);
//            if (intents != null) {
//                MqttMessage intent = intents.pollFirst();
//                if (!intents.isEmpty()) {
//                    communication.readyToPublish(this, topic);
//                }
//                return intent;
//            }
//        }
        return null;
    }


    public void publishDescription(String topic, Object description) {
        try {
            MqttMessage message = new MqttMessage(getMapper().writeValueAsBytes(description));
            message.setQos(1);
            message.setRetained(true);
            topic = topic.replaceFirst(getContract().CANONICAL_TOPIC, "");
            String descriptionTopic = getContract().DESCRIPTION + topic;
            synchronized (contractDescriptionMap) {
                contractDescriptionMap.put(descriptionTopic, message);
            }
            communication.readyToPublish(this, descriptionTopic);

        } catch (JsonProcessingException ex) {
            Logger.getLogger(GatewayClient.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
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
                    communication.connect(parameters);
                    connectionFuture.cancel(false);
                    connectionFuture = null;

                    communication.publishActualWill(getMapper().writeValueAsBytes(contract.ONLINE));
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

    //This works if the other one is too slow! Test its speed.
    //GatewayClientEvent<PhysicalMemory>[] events = getMapper().readValue(payload, new TypeReference<GatewayClientEvent<PhysicalMemory>[]>() { });
//    public <T> T toMessageSet(byte[] payload, Class<?> eventValue) throws Exception {
//        JavaType javaType = getMapper().getTypeFactory().constructParametricType(GCEvent.class, eventValue);
//        JavaType endType = getMapper().getTypeFactory().constructArrayType(javaType);
//        return getMapper().readValue(payload, endType);
//    }
    public <M extends Message> Set<M> toMessageSet(byte[] payload, Class<M> messageClass) throws Exception {
        JavaType javaType = getMapper().getTypeFactory().constructArrayType(messageClass);
        JavaType endType = getMapper().getTypeFactory().constructCollectionType(HashSet.class, messageClass);
        return getMapper().readValue(payload, endType);
    }

}
