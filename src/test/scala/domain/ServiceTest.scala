package domain

import helpers.SpecBase
import org.serviceHub.domain.{NoEndpointForMessageExeption, Message, Endpoint, Service}

class ServiceTest extends SpecBase {
  "getEndpointUrlFor" should "find a wildcard endpoint" in {
    val svc = Service("svc", Seq.empty, Seq("done"), Seq(Endpoint("http://localhost")))
    svc.getEndpointUrlFor(Message("done")) should be ("http://localhost")
  }

  "getEndpointUrlFor" should "find env-specific endpoint" in {
    val svc = Service("svc", Seq.empty, Seq("done"), Seq(Endpoint("http://localhost", "qa")))
    svc.getEndpointUrlFor(Message("done", env = "qa")) should be ("http://localhost")
  }

  "getEndpointUrlFor" should "throw exception for missing endpoint" in {
    val svc = Service("svc", Seq.empty, Seq("done"), Seq(Endpoint("http://localhost", "qa")))
    an [NoEndpointForMessageExeption] should be thrownBy svc.getEndpointUrlFor(Message("done", env = "default"))
  }

  "getEndpointUrlFor" should "fill all placeholders" in {
    val svc = Service("svc", Seq.empty, Seq("done"), Seq(Endpoint("http://:env.com/:type")))
    svc.getEndpointUrlFor(Message("done", env = "qa")) should be ("http://qa.com/done")
  }
}
