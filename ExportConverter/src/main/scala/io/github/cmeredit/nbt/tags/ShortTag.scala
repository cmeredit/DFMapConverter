package io.github.cmeredit.nbt.tags

case class ShortTag(name: String, payload: Short) extends Tag {
  type A = Short

  override val typeName: String = MagicNumbers.TypeNames.short
}
