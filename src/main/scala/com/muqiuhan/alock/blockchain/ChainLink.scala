package com.muqiuhan.alock.blockchain

import java.math.BigInteger
import java.security.MessageDigest

/// The Chain can have two types: it can be an EmptyChain or a ChainLink.
/// The former is our block zero (the genesis block)
/// and it is implemented as a singleton (it is a case object), while the latter is a regular mined block.
case class ChainLink(
    index: Int,
    proof: Long,
    values: List[Transaction],
    previousHash: String = "",
    tail: Chain = EmptyChain,
    timestamp: Long = System.currentTimeMillis()
) extends Chain {
  val hash = String.format(
    "%032x",
    new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(this.toString.getBytes("UTF-8")))
  )
}
