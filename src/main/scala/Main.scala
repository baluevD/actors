import akka.actor._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Constants {
  val touristsPerCook = 3
  val touristsAmount = 10
  val cooksAmount = 2
}
case object IAmHungry
case object StartCooking
case object FoodIsReady
case object Eat

class Cook(kitchen: ActorRef) extends Actor {
  val r = scala.util.Random
  def receive = {
    case StartCooking => context.system.scheduler.scheduleOnce(r.nextInt(10).seconds) {
      println(self.path.name + " cooked some food")
      kitchen ! FoodIsReady
    }
    case other => println("Something strange " + other)
  }
  self ! StartCooking
}

class Tourist(kitchen: ActorRef) extends Actor {
  val r = scala.util.Random

  def receive = {
    case Eat =>
      println(self.path.name + " got food")
      context.system.scheduler.scheduleOnce(r.nextInt(10).seconds) {
        println(self.path.name + " is hungry")
        kitchen ! IAmHungry
      }
    case other => println("Something strange " + other)
  }

  kitchen ! IAmHungry
  println(self.path.name + " is hungry")
}

class Kitchen extends Actor {
  def kitchenBehaviour(tourists: mutable.Queue[ActorRef], cooks: mutable.Queue[ActorRef]): Receive = {
    case IAmHungry =>
      tourists.enqueue(sender)
      kitchenRoutine(tourists, cooks)
      context.become(kitchenBehaviour(tourists, cooks), discardOld = true)
    case FoodIsReady =>
      cooks.enqueue(sender)
      kitchenRoutine(tourists, cooks)
      context.become(kitchenBehaviour(tourists, cooks), discardOld = true)
    case other => println("Something strange " + other)
  }

  def kitchenRoutine(tourists: mutable.Queue[ActorRef], cooks: mutable.Queue[ActorRef]) = {
    if (tourists.size >= Constants.touristsPerCook && cooks.nonEmpty) {
      (1 to Constants.touristsPerCook) foreach (_ => tourists.dequeue ! Eat)
      cooks.dequeue ! StartCooking
    }
  }

  def receive = kitchenBehaviour(mutable.Queue.empty, mutable.Queue.empty)

}

object CooksAndTourists extends App {
  val system = ActorSystem()
  val nest = system.actorOf(Props[Kitchen], "kitchen")
  (1 to Constants.touristsAmount).foreach(i => system.actorOf(Props(new Tourist(nest)), "tourist" + i))
  (1 to Constants.cooksAmount).foreach(i => system.actorOf(Props(new Cook(nest)), "cook" + i))
}
