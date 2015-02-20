package com.wlangiewicz.bitcoinpaymentprocessor.actors

import akka.actor.{ActorSystem, Actor}
import akka.actor.Actor.Receive
import com.wlangiewicz.bitcoinpaymentprocessor.{JsonFormats, PaymentCompletedResponse}
import com.wlangiewicz.bitcoinpaymentprocessor.actors.PaymentNotificationActor.NotifyPaid
import spray.http._
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._

import scala.concurrent.Future

object PaymentNotificationActor extends JsonFormats{

  case class NotifyPaid(notificationUrl: String, payload: PaymentCompletedResponse)

  def notify(url: String, payload: PaymentCompletedResponse): Unit = {
    implicit val system = ActorSystem()
    import system.dispatcher  // execution context for futures

    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    val response: Future[HttpResponse] = pipeline(Post(url, payload))
    Console.println(response)
  }
}

class PaymentNotificationActor extends Actor with JsonFormats{

  override def receive = {
    case NotifyPaid(n, p) => {
      PaymentNotificationActor.notify(n,p)
    }

  }
}
