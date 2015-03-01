package org.serviceHub.domain

import org.json4s.JsonAST
import org.json4s.JsonAST.{JField, JObject, JArray, JString}

case class Endpoint(url: String, env: String = "*")

object Service {
  def jsonTransformer: PartialFunction[(String, JsonAST.JValue), (String, JsonAST.JValue)] = {
    // "endpoints": "http://localhost/"
    //        ====>
    // "endpoints": [
    //   { env: "*", url: "http://localhost/" }
    // ]
    case ("endpoints", JString(url)) => (
      "endpoints",
      JArray(List(JObject(
        ("url", JString(url)),
        ("env", JString("*"))
      )))
      )
    // "endpoints": {
    //   "*": "http://server.com/"
    //   "dev": "http://localhost/"
    // }
    //      =====>
    // "endpoints: [
    //   { env: "*", url: "http://server.com/" },
    //   { env: "dev", url: "http://localhost/" }
    // ]
    case ("endpoints", JObject(x)) => ("endpoints", JArray(
      x.map{
        case JField(env, JString(uri)) => JObject(
          ("url", JString(uri)),
          ("env", JString(env))
        )
      }
    ))
  }
}

case class Service(name: String,
                   publishes: Seq[String] = Seq.empty,
                   subscribes: Seq[String] = Seq.empty,
                   endpoints: Seq[Endpoint] = Seq(Endpoint("http://localhost:8080/events/:message_type"))) {
  def isSubscriberOf(msg: Message) = subscribes.contains(msg.messageType) || subscribes.contains("*")
  def isPublisherOf(msg: Message) = publishes.contains(msg.messageType)
}
