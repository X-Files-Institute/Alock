package com.muqiuhan.alock.actor

import akka.actor.Status.{Failure, Success}
import akka.actor.{Actor, ActorLogging, Props}
import com.muqiuhan.alock.actor.Miner.{Ready, Validate}
import com.muqiuhan.alock.blockchain.ProofOfWork
import com.muqiuhan.alock.exception.{InvalidProofException, MinerBusyException}
import com.muqiuhan.alock.actor.Miner.*

import scala.concurrent.Future

/// Miner Actor is the one mining new blocks for our blockchain.
/// Miner Actor will have two states: ready, when it is ready to mine a new block, and busy, when it is mining a block.
object Miner {
  sealed trait MinerMessage

  /// Asks for a validation of a proof, and pass to the Miner the hash and the proof to check.
  case class Validate(hash: String, proof: Long) extends MinerMessage

  /// Asks for the mining starting from a specified hash
  case class Mine(hash: String) extends MinerMessage

  /// Triggers a state transaction
  case object Ready extends MinerMessage

  val props: Props = Props(new Miner)
}

class Miner extends Actor with ActorLogging {
  import context._

  def validate: Receive = {
    case Validate(hash, proof) => {
      log.info(s"Validating proof $proof")
      if (ProofOfWork.validProof(hash, proof)) {
        log.info("Proof is valid!")
        sender() ! Success
      } else {
        log.info("Proof is not valid!")
        sender() ! Failure(new InvalidProofException(hash, proof))
      }
    }
  }

  def ready: Receive = validate orElse {
    case Mine(hash) => {
      log.info(s"Mine hash $hash...")
      val proof = Future {
        ProofOfWork.proofOfWork(hash)
      }
      sender() ! proof

      become(busy)
    }
    case Ready => {
      log.info("Miner is ready!")
      sender() ! Success("Ok")
    }
  }

  def busy: Receive = validate orElse {
    case Mine(_) => {
      log.info("Mine is busy!")
      sender() ! Failure(new MinerBusyException("Miner is busy"))
    }

    case Ready => {
      log.info("Miner is ready!")
      become(ready)
    }
  }

  /// Start waiting for a Ready message. when it comes, start the Miner.
  override def receive: Receive = { case Ready =>
    become(ready)
  }
}
