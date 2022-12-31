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
