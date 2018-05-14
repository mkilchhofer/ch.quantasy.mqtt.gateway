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
package ch.quantasy.mqtt.gateway.client.reference;

import ch.quantasy.mqtt.communication.mqtt.MQTTCommunicationIntent;
import ch.quantasy.mqtt.communication.mqtt.MQTTCommunicationStatus;
import ch.quantasy.mqtt.gateway.client.GatewayClient;
import ch.quantasy.mqtt.gateway.client.contract.AServiceContract;
import ch.quantasy.mqtt.gateway.client.message.Message;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.util.JSONPObject;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 *
 * @author reto
 */
public class Referencer<M extends Message, S extends AServiceContract> {

    private GatewayClient<S> gatewayClient;
    private Class<M> targetMessageClass;
    private Map<String, Reference> referenceMap;

    public Referencer(GatewayClient<S> gatewayClient, Class<M> targetMessageClass) {
        this.gatewayClient = gatewayClient;
        referenceMap = new TreeMap<>();
        this.targetMessageClass = targetMessageClass;
    }

    public void doIt(Reference reference) {
        referenceMap.put(reference.sourceTopic, reference);
        if (reference.targetSourceMap.isEmpty()) {
            gatewayClient.unsubscribe(reference.sourceTopic);
        } else {
            gatewayClient.subscribe(reference.sourceTopic, (topic, payload) -> {
                byte[] o=gatewayClient.map(payload, reference.targetSourceMap);
                System.out.println(new String(o));
                //System.out.println(Arrays.toString(messages.toArray()));
            });
        }
    }

    public static void main(String[] args) throws Exception {
        GatewayClient gc = new GatewayClient(URI.create("tcp://127.0.0.1:1883"), "REFERENCER", new ReferencerContract("", "Referencer"));
        MQTTCommunicationIntent intent= gc.getIntent();
                intent.isCleanSession=true;
                gc.setIntent(intent);
        gc.connect();
        Referencer referencer = new Referencer(gc, XYZIntent.class);
        Reference reference = new Reference();
        reference.sourceTopic = gc.getContract().INTENT;
        PositionIntent posIntent=new PositionIntent();
        posIntent.xyz=new XYZIntent();
        posIntent.xyz.x=2;
        posIntent.xyz.y=4;
        posIntent.xyz.z=6;
        
        
        for(int i=0;i<3;i++)gc.readyToPublish(reference.sourceTopic, posIntent);
        reference.targetSourceMap.put("position/a", "/xyz/x");
        reference.targetSourceMap.put("position/b", "/xyz/y");
        reference.targetSourceMap.put("timeStamp", "/timeStamp");
        referencer.doIt(reference);
        System.in.read();
    }
}
