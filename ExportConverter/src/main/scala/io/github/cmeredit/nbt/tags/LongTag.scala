package io.github.cmeredit.nbt.tags

case class LongTag(name: String, payload: Long) extends Tag {
  type A = Long

  override val typeName: String = MagicNumbers.TypeNames.long
}
