package io.github.cmeredit.nbt.tags

case class LongArrayTag(name: String, payload: Vector[Long]) extends Tag {
  type A = Vector[Long]

  override val typeName: String = Identifiers.TypeNames.longArray
}
