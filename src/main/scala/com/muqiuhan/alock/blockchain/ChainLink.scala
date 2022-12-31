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
