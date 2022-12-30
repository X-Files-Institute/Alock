import java.security.InvalidParameterException
import java.math.BigInteger
import java.security.MessageDigest

/// The chain is the core of our blockchain: it is a linked list of blocks containing transactions.
sealed trait Chain {
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

case object EmptyChain extends Chain {
  val index     = 0
  val hash      = "1"
  val values    = Nil
  val proof     = 100L
  val timestamp = System.currentTimeMillis()
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
