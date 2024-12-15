package io.github.cmeredit.nbt.tags

case class IntTag(name: String, payload: Int) extends Tag {
  type A = Int

  override val typeName: String = Identifiers.TypeNames.int
}
