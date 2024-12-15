package io.github.cmeredit.nbt.tags

case class LongArrayTag(name: String, payload: Vector[Long]) extends Tag {
  type A = Vector[Long]

  override val typeName: String = MagicNumbers.TypeNames.longArray

  override def toPrettyStrings: Vector[String] = {
    val maxDisplayLength: Int = 10
    val header = f"$name ($typeName):"

    val displayedChildStrings = payload.take(maxDisplayLength).map(byte => f"  |- $byte")
    val truncationMessage = if (payload.length > maxDisplayLength) Vector(f"  |- (${payload.length - maxDisplayLength} elements omitted)") else Vector()

    Vector(header) ++ displayedChildStrings ++ truncationMessage
  }
}
