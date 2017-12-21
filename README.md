

# MQTT-Gateway
ch.quantasy.mqtt.gateway

This is a wrapper to [paho]'s [MQTT] library and allows to design data driven programs e.g. micro-services supporting the following general API:
<a href="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/MqttGatewayClient.svg">
    <img src="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/MqttGatewayClient.svg.png" alt="Interface-Diagram" />
</a>

## Ideology

This project provides a the messaging extension to reactive programming.
 Due to mqtt as the underlying message bus, the messaging is agnostic to the programming language.
This allows the implementation of (micro-)services as described by [martinFowler] promoting their capabilities in form of promises as described
 in [promiseLinux],[promise]. 
Thus, each (micro-)service can provide a document based API, which is not bound to any programming language. This allows the API to be [tolerant].

A message broker (publish subscribe) is used to handle the flow of documents between the micro-services. The broker does not provide any domain specific business logic.

This concept foresees the following structure:
### One Service Class multiple Service Instances
Every Service class provides a unique API that is valid for any working unit of it (=U), whereas each unit has a distinct identifier `<id>`.


## API towards MQTT
The idea of this MQTT-Gateway is to provide some very generic but common structure. There is nothing new, the following ideas are all borrowed from different design ideologies. The idea
behind this structure is to provide a simple and light-weight data-flow for the communication.

Per default, the implemented MQTT-Gateway expects [YAML] as data in- and output. However, this can be changed to any text- or binary or even hybrid solution.
### Unit
Each working unit (=U) represents an instance of a Service-Class and uses an identifier `<id>` within its topic in order to be discriminated.

### Intent
The intent (=I) is the way, a working unit can be allow to be controlled / configured.
The designer of the (micro-)service defines the contracts on what data is accepted as input. The MQTT-gateway-client will subscribes to this topic.
and thus will promote each publish to this topic to the service. A good pattern for the intent is to define only one intent topic where a very versatile document is accepted.

### Status
The status (=S) is the way, a working unit can express its actual status.
The designer of the (micro-)service defines the contracts on what internal state shall be expressed. The MQTT-gateway-client will publish to this topic(s).
This way, every one interested in a specific status can subscribe to it. A good pattern for the status is to define fine granulated topics per status attribute.

### Event
The event (=E) is the way, a working unit can express expected changes as events. The MQTT-gateway-client will publish to this topic(s)
The designer of the (micro-)service defines the contracts on what events shall be expressed. A good pattern for the event is to define fine granulated topics per event attribute.
This way, every one interested in a specific event can subscribe to it.

### Description
The description (=D) is the way for the designer to express the abilities of a (micro-)service. The MQTT-gateway-client will publish to this topics. A good pattern for the description is
to provide the abilities in the form of a data-definition language readable to humans and machines.

### Bursts of Messages
Due to the nature of the underlying MQTT implementation, it is important to allow Intent / Status / Event to occur as bursts of messages. It is a good pattern to
expect each message as an array of messages!

        <a href="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/Full-Micro-service.svg">
            <img src="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/Full-Micro-service.svg.png" alt="Interface-Diagram" />
        </a>

## API towards Java
### Construction
For construction of a GatewayClient. The mqttURI and the clientID is requested as parameters. 
Furthermore, a so called ClientContract is required. This contract defines the MQTT-Topics for I,S,E and D

### connect()
Only after calling the connect method, the connection will be established and the topic for the availability is set to online

### disconnect()
When this method is called, the topic for the availability is set to offline and the connection is closed.

### subscribe()
When the GatewayClient serves a service, the subscriptions should point to the according intent-topics.
Per subscription a MessageReceiver has to be provided as callback.

### getCollector(): MessageCollector
The MessageCollector allows to collect messages (i.e. Intent, Event, Status)

### getPublishingCollector(): PublishingMessageCollector
The PublishingMessageCollector allows to publish collected messages. They will be sent as arrays of messages per topic.

### publishDescription()
This method sends out each message as a single publish... i.e. if the network is slow, the publishes are queued.

This convenience method is used, in order to send the description of the service abilities i.e. the contract(s). As a rule of thumb:
This method should be used in the very beginning only and should not change during life-time... It describes the abilities of the Service / Servant.


## Full Micro-Service
With the GatewayClient towards MQTT and the API towards the native programming language (Java), now the following generic composition can be used, in order to
provide micro-service capabilities to native programs, using a MVP (Model View Presenter) pattern, where the native program serves as 'model' (or source) and the MQTT side serves as 'view'. 
        <a href="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/Micro-service.svg">
            <img src="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/Micro-service.svg.png" alt="Micro-service-Diagram" />
        </a>



[paho]: <https://github.com/eclipse/paho.mqtt.java>
[YAML]: <https://en.wikipedia.org/wiki/YAML>
[MQTT]: <http://mqtt.org/>
[TiMqWay.jar]: <https://prof.hti.bfh.ch/knr1/TiMqWay.jar>
[d3Viewer]: <https://github.com/hardillb/d3-MQTT-Topic-Tree>
[micro-service]: <https://en.wikipedia.org/wiki/Microservices>
[martinFowler]: <https://martinfowler.com/articles/microservices.html>
[promiseLinux]: <http://www.linuxjournal.com/content/promise-theory%E2%80%94what-it>
[promise]: <http://markburgess.org/BookOfPromises.pdf>
[contract]: <https://en.wikipedia.org/wiki/Design_by_contract>
[tolerant]: <https://martinfowler.com/bliki/TolerantReader.html>


