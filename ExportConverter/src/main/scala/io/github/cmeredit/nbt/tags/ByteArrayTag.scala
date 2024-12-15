package io.github.cmeredit.nbt.tags

case class ByteArrayTag(name: String, payload: Vector[Byte]) extends Tag {
  type A = Vector[Byte]

  override val typeName: String = MagicNumbers.TypeNames.byteArray

  override def toPrettyStrings: Vector[String] = {
    val maxDisplayLength: Int = 10
    val header = f"$name ($typeName):"

    val displayedChildStrings = payload.take(maxDisplayLength).map(byte => f"  |- $byte")
    val truncationMessage = if (payload.length > maxDisplayLength) Vector(f"  |- (${payload.length - maxDisplayLength} elements omitted)") else Vector()

    Vector(header) ++ displayedChildStrings ++ truncationMessage
  }
}
