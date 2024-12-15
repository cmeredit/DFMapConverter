package io.github.cmeredit.nbt.tags

case class IntArrayTag(name: String, payload: Vector[Int]) extends Tag {
  type A = Vector[Int]

  override val typeName: String = Identifiers.TypeNames.intArray
}
