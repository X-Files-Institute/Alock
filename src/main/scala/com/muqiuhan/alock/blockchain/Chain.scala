package com.muqiuhan.alock.blockchain

import java.security.InvalidParameterException
import scala.annotation.targetName

/// The chain is the core of our blockchain: it is a linked list of blocks containing transactions.
trait Chain {
  val index: Int
  val hash: String
  val values: List[Transaction]
  val proof: Long
  val timestamp: Long

  def ::(link: Chain): Chain = link match {
    case l: ChainLink => ChainLink(l.index, l.proof, l.values, this.hash, this)
    case _            => throw new InvalidParameterException("Cannot add invalid link to chain")
  }
}

/// Create a new chain passing it a list of blocks
object Chain {
  def apply(b: Chain*): Chain = {
    if (b.isEmpty) EmptyChain
    else {
      val link = b.head.asInstanceOf[ChainLink]
      ChainLink(link.index, link.proof, link.values, link.previousHash, apply(b.tail: _*))
    }
  }
}
