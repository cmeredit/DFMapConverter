package io.github.cmeredit.nbt.tags

case class ByteArrayTag(name: String, payload: Vector[Byte]) extends Tag {
  type A = Vector[Byte]

  override val typeName: String = Identifiers.TypeNames.byteArray
}
