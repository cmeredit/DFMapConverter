package io.github.cmeredit.nbt.tags

trait Tag {
  // Core NBT content ----------------------------------------------------------
  type A
  val name: String
  val payload: A

  // Utility content -----------------------------------------------------------
  val typeName: String

  // By default, just return one string. Compound tags will return more.
  def toPrettyStrings: Vector[String] = Vector(f"$typeName Tag $name: $payload")

  def printPretty(): Unit = toPrettyStrings foreach println
}
