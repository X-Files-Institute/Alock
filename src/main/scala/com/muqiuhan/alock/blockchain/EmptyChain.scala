package com.muqiuhan.alock.blockchain

case object EmptyChain extends Chain {
  val index: Int                = 0
  val hash: String              = "1"
  val values: List[Transaction] = Nil
  val proof: Long               = 100L
  val timestamp: Long           = System.currentTimeMillis()
}
