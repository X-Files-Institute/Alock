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

import com.muqiuhan.alock.blockchain.Transaction
import com.muqiuhan.alock.actor.Miner.Validate
import com.muqiuhan.alock.actor.Broker.Clear
import com.muqiuhan.alock.actor.Blockchain.AddBlockCommand
import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.SECONDS
import com.muqiuhan.alock.blockchain.EmptyChain
import com.muqiuhan.alock.actor.Miner.Ready
import com.muqiuhan.alock.actor.Blockchain.GetChain
import com.muqiuhan.alock.actor.Blockchain.GetLastIndex
import com.muqiuhan.alock.actor.Blockchain.GetLastHash
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future

/// The Node Actor is the backbone of our Alock node.
/// It is the supervisor of all the other actors (Broker, Miner, and Blockchain),
/// and the one communicating with the outside world through the REST API.
object Node {
  sealed trait NodeMessage

  /// The Node Actor has to handle all the high level messages that coming from the REST API.
  /// This is the reason why we find in the companion object more or less the same messages we implemented in the children actors.
  case class AddTransaction(transaction: Transaction) extends NodeMessage
  case class CheckPowSolution(solution: Long)         extends NodeMessage
  case class AddBlock(proof: Long)                    extends NodeMessage
  case object GetTransactions                         extends NodeMessage
  case object Mine                                    extends NodeMessage
  case object StopMining                              extends NodeMessage
  case object GetStatus                               extends NodeMessage
  case object GetLastBlockIndex                       extends NodeMessage
  case object GetLastBlockHash                        extends NodeMessage

  /// Takes a nodeId as an argument to create our Node Actor.
  /// This will be the one used for the initialization of Blockchain Actor.
  def props(nodeId: String): Props = Props(new Node(nodeId))

  /// Creates a transaction assigning a predefined coin amount to the node itself.
  /// This will be the reward for the successful mining of a new block of the blockchain.
  def createCoinbaseTransaction(nodeId: String) = Transaction("coinbase", nodeId, 100)
}

class Node(nodeId: String) extends Actor with ActorLogging {
  import Node._

  implicit lazy val timeout: Timeout = Timeout(FiniteDuration(5L, SECONDS))

  val broker     = context.actorOf(Broker.props)
  val miner      = context.actorOf(Miner.props)
  val blockchain = context.actorOf(Blockchain.props(EmptyChain, nodeId))

  miner ! Ready

  override def receive: Receive = {

    /// Triggers the logic to store a new transaction in the list of pending ones of our blockchain.
    /// The Node Actor responds with the index of the block that will contain the transaction.
    case AddTransaction(transaction) => {
      val node = sender()

      broker ! Broker.AddTransaction(transaction)

      (blockchain ? GetLastIndex).mapTo[Int] onComplete {
        case Success(index)     => node ! (index + 1)
        case Failure(exception) => node ! akka.actor.Status.Failure(exception)
      }
    }

    /// Check if a solution to the PoW algorithm is correct.
    /// Ask to the Blockchain Actor the hash of the last block, and we tell the Miner Actor to validate the solution against the hash.
    /// In the tell function we pass to the Miner the Validate message along with the address of the sender, so that the miner can respond directly to it.
    case CheckPowSolution(solution) => {
      val node = sender()
      (blockchain ? GetLastHash).mapTo[String] onComplete {
        case Success(hash: String) => miner.tell(Validate(hash, solution), node)
        case Failure(exception)    => node ! akka.actor.Status.Failure(exception)
      }
    }

    /// Other nodes can mine blocks, so we may receive a request to add a block that we didn’t mine.
    /// The proof is enough to add the new block, since we assume that all the nodes share the same list of pending transactions.
    case AddBlock(proof) => {
      val node = sender()
      (self ? CheckPowSolution(proof)) onComplete {
        case Success(_) => {
          (broker ? Broker.GetTransactions).mapTo[List[Transaction]] onComplete {
            case Success(transactions) =>
              blockchain.tell(AddBlockCommand(transactions, proof), node)
            case Failure(e) => node ! akka.actor.Status.Failure(e)
          }
          broker ! Clear
        }
        case Failure(exception) => node ! akka.actor.Status.Failure(exception)
      }
    }

    /// This is a simplification, in the Bitcoin network there cannot be such assumption.
    /// First of all we should check if the solution is valid. We do this sending a message to the node itself: self ? CheckPowSolution(proof).
    /// If the proof is valid, we get the list of pending transaction from the Broker Actor,
    /// then we tell to the Blockchain Actor to add to the chain a new block containing the transactions and the validated proof.
    /// The last thing to do is to command the Broker Actor to clear the list of pending transactions.
    case Mine => {
      def waitForSolution(solution: Future[Long]) = Future {
        solution onComplete {
          case Success(proof) => {
            broker ! Broker.AddTransaction(createCoinbaseTransaction(nodeId))
            self ! AddBlock(proof)
            miner ! Ready
          }
          case Failure(e) => log.error(s"Error finding PoW solution: ${e.getMessage}")
        }
      }

      val node = sender()
      (blockchain ? GetLastHash).mapTo[String] onComplete {
        case Success(hash) =>
          (miner ? Miner.Mine(hash)).mapTo[Future[Long]] onComplete {
            case Success(solution) => waitForSolution(solution)
            case Failure(e)        => log.error(s"Error finding PoW solution: ${e.getMessage}")
          }
        case Failure(e) => node ! akka.actor.Status.Failure(e)
      }
    }

    /// Forward the messages to the Blockchain Actor, since it isn’t required any processing.
    /// Using the forward operator the sender() of the message will be the one that originated the message, not the Node Actor.
    /// In this way the Blockchain Actor will respond to the original sender of the message (the REST API layer).
    case GetTransactions   => broker forward Broker.GetTransactions
    case GetStatus         => blockchain forward GetChain
    case GetLastBlockIndex => blockchain forward GetLastIndex
    case GetLastBlockHash  => blockchain forward GetLastHash
  }
}
