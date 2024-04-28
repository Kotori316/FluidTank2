package com.kotori316.fluidtank.contents

import cats.data.Chain

import java.util.SequencedCollection

trait Reversible[F[_]] {
  def reverse[A](seq: F[A]): F[A]
}

object Reversible {
  implicit val listReversible: Reversible[List] = new Reversible[List] {
    override def reverse[A](seq: List[A]): List[A] = seq.reverse
  }
  implicit val chainReversible: Reversible[Chain] = new Reversible[Chain] {
    override def reverse[A](seq: Chain[A]): Chain[A] = seq.reverse
  }
  implicit val javaSequencedReversible: Reversible[SequencedCollection] = new Reversible[SequencedCollection] {
    override def reverse[A](sequencedCollection: SequencedCollection[A]): SequencedCollection[A] = sequencedCollection.reversed()
  }
}
