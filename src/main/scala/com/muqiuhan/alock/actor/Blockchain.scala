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
import com.muqiuhan.alock.blockchain.Chain
import com.muqiuhan.alock.blockchain.ChainLink
import akka.actor.Props
import akka.persistence.PersistentActor
import akka.actor.ActorLogging
import akka.persistence.RecoveryCompleted
import akka.persistence.SnapshotOffer
import akka.persistence.SaveSnapshotSuccess
import akka.persistence.SaveSnapshotFailure

/// The Blockchain Actor interacts with the business logic of the blockchain.
/// It can add a new block to the blockchain, and it can retrieve information about the state of the blockchain.
/// This actor has another superpower: it can persist and recover the state of the blockchain.
/// This is possible implementing the PersistentActor trait provided by the Akka Framework.
object Blockchain {

  /// Handle the events that will trigger the persistence logic.
  sealed trait BlockchainEvent

  /// Will update state
  case class AddBlockEvent(transactions: List[Transaction], proof: Long) extends BlockchainEvent

  /// Send direct commands to the actor.
  sealed trait BlockchainCommand

  /// Interact with the underlying blockchain.
  case class AddBlockCommand(transactions: List[Transaction], proof: Long) extends BlockchainCommand
  case object GetChain                                                     extends BlockchainCommand
  case object GetLastHash                                                  extends BlockchainCommand
  case object GetLastIndex                                                 extends BlockchainCommand

  /// Store the state of our blockchain, that is its Chain.
  /// Update the state every time a new block is created.
  case class State(chain: Chain)

  /// Initializes the Blockchain Actor with the initial Chain and the nodeId of the Alock node.
  def props(chain: Chain, nodeID: String): Props = Props(new Blockchain(chain, nodeID))
}

/// The Blockchain Actor extends the trait PersistentActor provided by the Akka framework.
/// In this way we have out-of-the-box all the logic required to persist and recover our state.
/// Initialize the state using the Chain provided as an argument upon creation.
class Blockchain(chain: Chain, nodeID: String) extends PersistentActor with ActorLogging {
  import Blockchain._

  var state = State(chain)

  /// The nodeId is part of the persistenceId, persistence logic will use it to identify the persisted state.
  /// Since we can have multiple Alock nodes running in the same machine, we need this value to correctly persist and recover the state of each node.
  override def persistenceId: String = s"chainer-$nodeID"

  /// Executes the update of the Actor state when the AddBlockEvent is received.
  def updateState(event: BlockchainEvent) = event match {
    case AddBlockEvent(transactions, proof) => {
      state = State(ChainLink(state.chain.index + 1, proof, transactions) :: state.chain)
      log.info(s"Added block ${state.chain.index} containing ${transactions.size} transactions")
    }
  }

  /// Reacts to the recovery messages sent by the persistence logic.
  /// During the creation of an actor a persisted state (snapshot) may be offered to it using the SnapshotOffer message.
  /// In this case the current state becomes the one provided by the snapshot.
  override def receiveRecover: Receive = {
    case SnapshotOffer(metadata, snapshot: State) => {
      log.info(s"Recovering from snapshot ${metadata.sequenceNr} at block ${snapshot.chain.index}")
      state = snapshot
    }

    case RecoveryCompleted  => log.info("Recovery completed!")
    case evt: AddBlockEvent => updateState(evt)
  }

  /// React to the direct commands sent to the actor
  override def receiveCommand: Receive = {
    case SaveSnapshotSuccess(metadata) => log.info(s"Snapshot ${metadata.sequenceNr}")
    case SaveSnapshotFailure(metadata, reason) =>
      log.error(s"Error saving snapshot ${metadata.sequenceNr}: ${reason.getMessage}")
    case AddBlockCommand(transactions, proof) => {
      // creates and fires an AddBlock event, that is persisted in the event journal of the Actor.
      // In this way events can be replayed in case of recovery.
      persist(AddBlockEvent(transactions, proof)) { event =>
        updateState(event)
      }

      deferAsync(Nil) { _ =>
        saveSnapshot(state)
        sender() ! state.chain.index
      }
    }
    case AddBlockCommand(_, _) => log.error("Invalid add block command!")
    case GetChain              => sender() ! state.chain
    case GetLastHash           => sender() ! state.chain.hash
    case GetLastIndex          => sender() ! state.chain.index
  }
}
