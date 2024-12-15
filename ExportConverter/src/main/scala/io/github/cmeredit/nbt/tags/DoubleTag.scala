package io.github.cmeredit.nbt.tags

case class DoubleTag(name: String, payload: Double) extends Tag {
  type A = Double

  override val typeName: String = MagicNumbers.TypeNames.double
}
