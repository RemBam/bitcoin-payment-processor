package com.wlangiewicz.bitcoinpaymentprocessor.actors

import akka.actor.{Props, ActorSystem, Actor}
import akka.actor.Actor.Receive
import com.wlangiewicz.bitcoinpaymentprocessor.{JsonFormats, PaymentCompletedResponse}
import com.wlangiewicz.bitcoinpaymentprocessor.actors.PaymentNotificationActor.{DelayedNotifyPaid, NotifyPaid}
import spray.http._
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Success, Failure}
import scala.concurrent.duration._

object PaymentNotificationActor {

  case class NotifyPaid(notificationUrl: String, payload: PaymentCompletedResponse)
  case class DelayedNotifyPaid(notificationUrl: String, payload: PaymentCompletedResponse, delay: Int)
}

class PaymentNotificationActor extends Actor with JsonFormats {

  import scala.concurrent.ExecutionContext.Implicits.global

  def notify(url: String, payload: PaymentCompletedResponse, delay: Int = 1): Unit = {
    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    val response: Future[HttpResponse] = pipeline(Post(url, payload))
    response.onComplete{
      case Failure(ex) => {
        implicit val system = ActorSystem("payment-processor")

        val paymentActor = system.actorOf(Props[PaymentNotificationActor], "notify")

        system.scheduler.scheduleOnce(delay seconds, paymentActor, DelayedNotifyPaid(url,payload,delay*2))
      }
    }
  }


  override def receive = {
    case NotifyPaid(n, p) =>
      notify(n, p)
    case DelayedNotifyPaid(n, p, t) =>
      notify(n, p, t)
  }
}
