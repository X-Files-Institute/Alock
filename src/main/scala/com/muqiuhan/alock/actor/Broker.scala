package com.muqiuhan.alock.actor

import akka.actor.{Actor, ActorLogging, Props}
import com.muqiuhan.alock.blockchain.Transaction

/// The manager of the transactions of our blockchain.
/// Its responsibilities are the addition of new transactions, and the retrieval of pending ones.
/// To identify the messages of the actor.Broker Actor, every other message will extend this trait.
object Broker {
  sealed trait BrokerMessage

  /// Adds a new transaction to the list of pending ones
  case class AddTransaction(transaction: Transaction)

  /// Retrieve the pending transactions
  case object GetTransactions extends BrokerMessage

  /// Empties the list
  case object Clear extends BrokerMessage
  /// Initialize the actor when it will be created
  val props: Props = Props(new Broker)
}

/// Contains the business logic to react to the different messages
class Broker extends Actor with ActorLogging {
  import Broker.*

  private val pending: List[Transaction] = List()

  override def receive: Receive = onMessage(pending)

  private def onMessage(pending: List[Any]): Receive = {
    case AddTransaction(transaction) => {
      context.become(onMessage(transaction :: pending))
      log.info(s"Added $transaction to pending transaction")
    }

    case GetTransactions => {
      log.info(s"Getting pending transactions")
      sender() ! pending
    }

    case Clear => {
      context.become(onMessage(List()))
      log.info("Clear pending transaction list")
    }
  }
}
