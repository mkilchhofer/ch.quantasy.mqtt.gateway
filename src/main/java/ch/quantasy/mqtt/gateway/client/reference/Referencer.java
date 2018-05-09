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

import ch.quantasy.mqtt.gateway.client.GatewayClient;
import ch.quantasy.mqtt.gateway.client.contract.AServiceContract;
import ch.quantasy.mqtt.gateway.client.message.Message;
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
        referenceMap.put(reference.topic, reference);
        gatewayClient.subscribe(reference.topic, (topic, payload) -> {
            SortedSet<M> messages = gatewayClient.map(payload, targetMessageClass, reference.TargetSourceMap);
            System.out.println(Arrays.toString(messages.toArray()));
        });

    }

    public static void main(String[] args) throws Exception {
        GatewayClient gc = new GatewayClient(URI.create("tcp://127.0.0.1:1883"), "REFERENCER", new ReferencerContract("", "Referencer"));
        gc.connect();
        Referencer referencer = new Referencer(gc, XYZIntent.class);
        Reference reference = new Reference();
        reference.topic = "TF/OutdoorWeather/U/DYp/E/humidity/+";
        reference.TargetSourceMap.put("x", "value");
        reference.TargetSourceMap.put("y", "id");
        reference.TargetSourceMap.put("timeStamp", "timeStamp");
        referencer.doIt(reference);
        System.in.read();
    }
}
