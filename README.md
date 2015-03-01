# ServiceHub

Lightweight, scalable, high-performance Event Driven Architecture (`EDA`) Enabler

## Requirements

- Java 7+
- RabbitMQ/SQS
- MongoDB
- Redis

## Installation

### Prerequisites for Mac OS X (Contribution of instructions for Linux/Windows is welcome)

```sh
  brew install scala
  brew install sbt
```

### Build

```sh
  git clone git@github.com:igorshapiro/serviceHub
  cd serviceHub
  sbt assembly
```

### Run

```sh
  java -cp target/scala-2.11/hub-assembly-1.0.jar Boot
```

## Getting started

### Create the services manifest file:

Create `services.json` in the same directory you placed hub-assembly-1.0.jar.

Example of the manifest:

```json
{
  "services": [{
      "name": "orders",
      "publishes": ["order_created"],
      "subscribes": ["order_paid"],
      "endpoints": "http://server.com/:message_type"
    }, {
      "name": "billing",
      "publishes": ["order_paid"],
      "subscribes": ["order_created"],
      "endpoints": "http://localhost/:message_type"
    }
  ]
}
```

### Run the hub

```sh
  java -cp target/scala-2.11/hub-assembly-1.0.jar Boot
```

### Sending messages:
Now from the orders service you can publish your messages:

```ruby
  require 'rest_client'
  
  RestClient.post 'http://your_service_hub_host:8080/api/v1/messages', {
    message_type: "order_created", 
    content: {  
      id: "order_1",
      user: {
        id: "user_2",
        email: "me@gmail.com"
      }
    }
  }.to_json, content_type: :json
```
