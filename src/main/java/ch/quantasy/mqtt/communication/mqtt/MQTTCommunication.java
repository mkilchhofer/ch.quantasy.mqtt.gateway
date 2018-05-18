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
package ch.quantasy.mqtt.communication.mqtt;

import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 *
 * @author reto
 */
public class MQTTCommunication implements IMqttActionListener {

    private static final Logger LOG = LogManager.getLogger(MQTTCommunication.class);
    private final MQTTCommunicationIntent intent;
    private final MqttConnectOptions connectOptions;
    private IMqttAsyncClient mqttClient;
    private Thread publisherThread;
    private final Publisher publisher;
    private boolean timeToQuit;

    public MQTTCommunication() {
        this.connectOptions = new MqttConnectOptions();
        this.publisher = new Publisher();
        this.intent = new MQTTCommunicationIntent();
    }

    public synchronized void setIntent(MQTTCommunicationIntent intent) {
        if (!intent.isValid()) {
            return;
        }
        //If communication is established (connected) nothing can be changed. It only accepts changes when not connected
        if (this.intent.connect != null && this.intent.connect) {
            return;
        }

        if (intent.isCleanSession != null) {
            this.connectOptions.setCleanSession(intent.isCleanSession);
            this.intent.isCleanSession = this.connectOptions.isCleanSession();
        }
        if (intent.clientID != null) {
            this.intent.clientID = intent.clientID;
        }
        if (intent.mqttCallback != null) {
            this.intent.mqttCallback = intent.mqttCallback;
        }
        if (intent.serverURIs != null) {
            this.connectOptions.setServerURIs(intent.getServerURIsAsString());
            this.intent.setServerURIs(this.connectOptions.getServerURIs());
        }
        if (intent.testament != null) {
            this.intent.testament = new Testament(intent.testament);
            connectOptions.setWill(intent.testament.willTopic, intent.testament.lastWillMessage, intent.testament.lastWillQoS, intent.testament.isLastWillRetained);

        }
        if (intent.authentication != null) {
            this.intent.authentication = new Authentication(intent.authentication);
        }
        if (intent.automaticReconnect != null) {
            this.connectOptions.setAutomaticReconnect(intent.automaticReconnect);
            this.intent.automaticReconnect = this.connectOptions.isAutomaticReconnect();
        }

        if (intent.connect != null && !intent.connect.equals(this.intent.connect)) {
            this.intent.connect = intent.connect;
            if (this.intent.connect && this.intent.isConnectable()) {
                try {
                    connect();
                } catch (MqttException ex) {
                    LOG.error("", ex);
                }
            }
            if (!this.intent.connect) {
                try {
                    disconnect();
                } catch (MqttException ex) {
                    LOG.error("", ex);
                }
            }

        }
    }

    public MQTTCommunicationIntent getIntent() {
        return new MQTTCommunicationIntent(intent);
    }
    
    

    private synchronized void connect() throws MqttException {
        if (!intent.isConnectable()) {
            return;
        }
        if (mqttClient != null && mqttClient.isConnected()) {
            return;
        }
        if (mqttClient == null) {
            mqttClient = new MqttAsyncClient(intent.serverURIs[0].toString(), intent.clientID, new MemoryPersistence());
        }
        mqttClient.setCallback(intent.mqttCallback);

        mqttClient.setManualAcks(false);
        mqttClient.connect(connectOptions).waitForCompletion();
        if (publisherThread == null) {
            publisherThread = new Thread(publisher);
            publisherThread.setDaemon(true);
            publisherThread.start();
        }
        notifyAll();
    }

    public synchronized IMqttDeliveryToken publishActualWill(byte[] actualWill) {
        MqttMessage message = new MqttMessage(actualWill);
        message.setQos(intent.testament.lastWillQoS);
        message.setRetained(intent.testament.isLastWillRetained);
        return this.publish(intent.testament.willTopic, message);
    }

    public void readyToPublish(MQTTMessageManager publisherCallback, String topic) {
        publisher.readyToPublish(publisherCallback, topic);
    }

    private synchronized IMqttDeliveryToken publish(String topic, MqttMessage message) {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                return null;
            }
            return mqttClient.publish(topic, message);
        } catch (Exception ex) {
            return null;
        }
    }

    public void quit() {
        try {
            this.timeToQuit = true;
            this.publisherThread.interrupt();
            this.disconnect();
        } catch (MqttException ex) {
            //That is ok
        }
    }

    public synchronized IMqttToken subscribe(String topic, int qualityOfService) {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                return null;
            }
            return mqttClient.subscribe(topic, qualityOfService, null, this);
        } catch (Exception ex) {
            return null;
        }
    }

    public synchronized IMqttToken unsubscribe(String topic) {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                return null;
            }
            return mqttClient.unsubscribe(topic);
        } catch (Exception ex) {
            return null;
        }
    }

    private synchronized void disconnect() throws MqttException {
        if (mqttClient == null) {
            return;
        }
        mqttClient.disconnect();
    }

//    public synchronized void disconnectForcibly() throws MqttException {
//        if (mqttClient == null) {
//            return;
//        }
//        mqttClient.disconnectForcibly();
//        connectionParameters.setInUse(false);
//    }

   
    public synchronized boolean isConnected() {
        if (mqttClient == null) {
            return false;
        }
        return mqttClient.isConnected();
    }

    @Override
    public void onSuccess(IMqttToken imt) {
        LOG.info("success");
    }

    @Override
    public void onFailure(IMqttToken imt, Throwable thrwbl) {
        LOG.error("", thrwbl);
    }

    class Publisher implements Runnable {

        private final BlockingDeque<PublishRequest> publishingQueue;

        public Publisher() {
            this.publishingQueue = new LinkedBlockingDeque<>();
        }

        public void readyToPublish(MQTTMessageManager callback, String topic) {
            PublishRequest publishRequest = new PublishRequest(callback, topic);
            synchronized (publishingQueue) {
                if (this.publishingQueue.contains(publishRequest)) {
                    return;
                }
                this.publishingQueue.add(publishRequest);
            }
            return;
        }

        @Override
        public void run() {
            while (!timeToQuit) {
                try {

                    PublishRequest publishRequest = null;
                    publishRequest = publishingQueue.take();

                    MqttMessage message = publishRequest.getMessage();

                    while (message != null) {
                        synchronized (MQTTCommunication.this) {
                            while (!MQTTCommunication.this.isConnected()) {
                                if (timeToQuit) {
                                    return;
                                }
                                MQTTCommunication.this.wait(1000);
                            }
                        }
                        IMqttDeliveryToken token = publish(publishRequest.topic, message);
                        if (token == null) {
                            LOG.error("Message for {} lost... Will try again", publishRequest);
                        } else {
                            token.waitForCompletion();
                            message = null;
                        }

                    }
                } catch (InterruptedException | MqttException ex) {

                    if (timeToQuit) {
                        return;
                    } else {
                        LOG.error("", ex);
                    }
                }
            }
        }
    }

    class PublishRequest {

        public final String topic;
        public final MQTTMessageManager publisherCallback;

        public PublishRequest(MQTTMessageManager publisherCallback, String topic) {
            this.topic = topic;
            this.publisherCallback = publisherCallback;
        }

        public MqttMessage getMessage() {
            return this.publisherCallback.getMessageFor(topic);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 59 * hash + Objects.hashCode(this.topic);
            hash = 59 * hash + Objects.hashCode(this.publisherCallback);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PublishRequest other = (PublishRequest) obj;
            if (!Objects.equals(this.topic, other.topic)) {
                return false;
            }
            if (!Objects.equals(this.publisherCallback, other.publisherCallback)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "PublishRequest{" + "publisherCallback=" + publisherCallback + ", topic=" + topic + '}';
        }
    }
}
