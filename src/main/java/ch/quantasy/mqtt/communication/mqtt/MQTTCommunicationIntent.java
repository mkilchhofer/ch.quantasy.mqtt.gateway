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
package ch.quantasy.mqtt.communication.mqtt;

import ch.quantasy.mqtt.gateway.client.message.AMessage;
import ch.quantasy.mqtt.gateway.client.message.annotations.Nullable;
import ch.quantasy.mqtt.gateway.client.message.annotations.Range;
import java.net.URI;
import org.eclipse.paho.client.mqttv3.MqttCallback;

/**
 *
 * @author reto
 */
public class MQTTCommunicationIntent extends AMessage {

    public URI[] serverURIs;
    public String clientID;
    public Boolean isCleanSession;
    public Boolean automaticReconnect;
    public Authentication authentication;
    public Testament testament;
    public MqttCallback mqttCallback;
    public Boolean connect;
    @Nullable
    @Range(from = 0, to = Integer.MAX_VALUE)
    public Integer keepAliveInterval;
    @Nullable
    @Range(from = 0, to = Integer.MAX_VALUE)
    public Integer connectionTimeout;

    public MQTTCommunicationIntent(String clientID, Boolean isCleanSession, Boolean automaticReconnect, Authentication authentication, Testament testament, MqttCallback mqttCallback, Boolean connect, Integer keepAliveInterval, Integer connectionTimeout, URI... serverURIs) {
        this.serverURIs = serverURIs.clone();
        this.clientID = clientID;
        this.isCleanSession = isCleanSession;
        this.automaticReconnect = automaticReconnect;
        this.authentication = new Authentication(authentication);
        this.testament = new Testament(testament);
        this.mqttCallback = mqttCallback;
        this.connect = connect;
        this.keepAliveInterval = keepAliveInterval;
        this.connectionTimeout = connectionTimeout;
    }

    public MQTTCommunicationIntent(MQTTCommunicationIntent intent) {
        this.serverURIs = intent.serverURIs.clone();
        this.clientID = intent.clientID;
        this.isCleanSession = intent.isCleanSession;
        this.automaticReconnect = intent.automaticReconnect;
        if (intent.authentication != null) {
            this.authentication = new Authentication(intent.authentication);
        }
        if (intent.testament != null) {
            this.testament = new Testament(intent.testament);
        }
        this.mqttCallback = intent.mqttCallback;
        this.connect = intent.connect;
        this.keepAliveInterval = intent.keepAliveInterval;
        this.connectionTimeout = intent.connectionTimeout;
    }

    public String[] getServerURIsAsString() {
        String[] urisAsStrings = new String[serverURIs.length];
        for (int i = 0; i < serverURIs.length; i++) {
            urisAsStrings[i] = serverURIs[i].toString();
        }
        return urisAsStrings;
    }

    public void setServerURIs(String... serverURIStrings) {
        this.serverURIs = new URI[serverURIStrings.length];
        for (int i = 0; i < serverURIStrings.length; i++) {
            this.serverURIs[i] = URI.create(serverURIStrings[i]);
        }
    }

    public boolean isConnectable() {
        if (!isValid()) {
            return false;
        }
        if (this.serverURIs == null || this.serverURIs.length < 1) {
            return false;
        }
        if (this.clientID == null || this.clientID.length() < 1) {
            return false;
        }
        if (this.isCleanSession == null) {
            return false;
        }
        if (this.mqttCallback == null) {
            return false;
        }
        return true;
    }

    public MQTTCommunicationIntent() {
    }
}
