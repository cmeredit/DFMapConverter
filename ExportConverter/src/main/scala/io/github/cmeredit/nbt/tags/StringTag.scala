package io.github.cmeredit.nbt.tags

case class StringTag(name: String, payload: String) extends Tag {
  type A = String

  override val typeName: String = MagicNumbers.TypeNames.string
}
