/*
 *   "TiMqWay"
 *
 *    TiMqWay(tm): A gateway to provide an MQTT-View for the Tinkerforge(tm) world (Tinkerforge-MQTT-Gateway).
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

import java.util.TreeMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.Comparator;

/**
 *
 * @author reto
 */
public class MessageCollector {

    private final SortedMap<String, SortedSet<Message>> messageMap;
    private final Comparator<Message> messageComparator;

    public MessageCollector(Comparator<Message> messageComparator) {
        this.messageComparator = messageComparator;
        this.messageMap = new TreeMap();
    }

    public MessageCollector() {
        this(new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return o1.compareTo(o2);
            }
        });
    }

    public void add(String topic, Message message) {
        if (message == null) {
            return;
        }
        synchronized (messageMap) {
            SortedSet<Message> messageSet = messageMap.get(topic);
            if (messageSet == null) {
                messageSet = new TreeSet<>(messageComparator);
                messageMap.put(topic, messageSet);
            }
            messageSet.add(message);
        }
    }

    public void add(String topic, Set<Message> messages) {
        if (messages == null) {
            return;
        }
        synchronized (messageMap) {
            SortedSet<Message> messageSet = messageMap.get(topic);
            if (messageSet == null) {
                messageSet = new TreeSet<>(messageComparator);
                messageMap.put(topic, messageSet);
            }
            messageSet.addAll(messages);
        }
    }

    public Set<String> getTopics() {
        synchronized (messageMap) {
            return messageMap.keySet();
        }
    }

    public SortedSet<Message> getMessages(String topic) {
        return this.getMessages(topic, this.messageComparator);
    }

    public SortedSet<Message> getMessages(String topic, Comparator<Message> comparator) {
        SortedSet<Message> messages = new TreeSet(comparator);
        synchronized (messageMap) {
            SortedSet<Message> messageSet = messageMap.get(topic);
            if (messageSet != null) {
                messages.addAll(messageSet);
            }
            return messages;
        }
    }

    public Message retrieveFirstMessage(String topic) {
        Message message = null;
        synchronized (messageMap) {
            SortedSet<Message> messageSet = messageMap.get(topic);
            if (messageSet != null && !messageSet.isEmpty()) {
                message = messageSet.first();
                messageSet.remove(message);
            }
            return message;
        }
    }

    public Message retrieveLastMessage(String topic) {
        Message message = null;
        synchronized (messageMap) {
            SortedSet<Message> messageSet = messageMap.get(topic);
            if (messageSet != null && !messageSet.isEmpty()) {
                message = messageSet.last();
                messageSet.remove(message);
            }
            return message;
        }
    }
    public SortedSet<Message> clearMessages(String topic) {
        if (topic == null) {
            return null;
        }
        synchronized (messageMap) {
            return messageMap.remove(topic);
        }
    }
}
