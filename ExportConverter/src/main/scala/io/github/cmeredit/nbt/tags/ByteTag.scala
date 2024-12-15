package io.github.cmeredit.nbt.tags

case class ByteTag(name: String, payload: Byte) extends Tag {
  type A = Byte

  override val typeName: String = Identifiers.TypeNames.byte
}
