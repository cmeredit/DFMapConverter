package io.github.cmeredit.nbt.tags

case class IntArrayTag(name: String, payload: Vector[Int]) extends Tag {
  type A = Vector[Int]

  override val typeName: String = MagicNumbers.TypeNames.intArray

  override def toPrettyStrings: Vector[String] = {
    val maxDisplayLength: Int = 10
    val header = f"$name ($typeName):"

    val displayedChildStrings = payload.take(maxDisplayLength).map(byte => f"  |- $byte")
    val truncationMessage = if (payload.length > maxDisplayLength) Vector(f"  |- (${payload.length - maxDisplayLength} elements omitted)") else Vector()

    Vector(header) ++ displayedChildStrings ++ truncationMessage
  }
}
