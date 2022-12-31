package com.muqiuhan.alock.blockchain

import java.math.BigInteger
import scala.annotation.tailrec
import java.security.MessageDigest

object ProofOfWork {
  def proofOfWork(lastHash: String): Long = {
    @tailrec
    def powHelper(lastHash: String, proof: Long): Long = {
      if (validProof(lastHash, proof))
        proof
      else
        powHelper(lastHash, proof + 1)
    }

    val proof = 0
    powHelper(lastHash, proof)
  }

  def validProof(lastHash: String, proof: Long): Boolean = {
    val guess = (lastHash ++ proof.toString).toString
    val guessHash = String.format(
      "%032x",
      new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(guess.getBytes("UTF-8")))
    )

    (guessHash take 4) == "0000"
  }
}
