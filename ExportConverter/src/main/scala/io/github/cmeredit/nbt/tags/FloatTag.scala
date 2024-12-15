package io.github.cmeredit.nbt.tags

case class FloatTag(name: String, payload: Float) extends Tag {
  type A = Float

  override val typeName: String = MagicNumbers.TypeNames.float
}
