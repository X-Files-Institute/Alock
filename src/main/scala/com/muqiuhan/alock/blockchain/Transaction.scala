package com.muqiuhan.alock.blockchain

/// The transaction is a very simple object:
/// it has a sender, a recipient and a value. We can implement it as a simple case class.
case class Transaction(sender: String, recipient: String, value: Long)
