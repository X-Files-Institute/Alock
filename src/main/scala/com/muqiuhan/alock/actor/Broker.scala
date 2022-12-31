/// MIT License
/// 
/// Copyright (c) 2022 Muqiu Han
/// 
/// Permission is hereby granted, free of charge, to any person obtaining a copy
/// of this software and associated documentation files (the "Software"), to deal
/// in the Software without restriction, including without limitation the rights
/// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
/// copies of the Software, and to permit persons to whom the Software is
/// furnished to do so, subject to the following conditions:
/// 
/// The above copyright notice and this permission notice shall be included in all
/// copies or substantial portions of the Software.
/// 
/// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
/// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
/// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
/// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
/// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
/// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
/// SOFTWARE.

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
