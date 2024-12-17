package io.github.cmeredit

object Orientations {

  sealed trait Axis
  case object X extends Axis
  case object Y extends Axis
  case object Z extends Axis

  case class Orientation(axis: Axis, upwards: Boolean)

}
