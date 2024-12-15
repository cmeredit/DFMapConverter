package io.github.cmeredit.nbt.tags

case class CompoundTag(name: String, payload: Vector[Tag]) extends Tag {
  type A = Vector[Tag]

  override val typeName: String = Identifiers.TypeNames.compound

  override def toPrettyStrings: Vector[String] = {

    val headerString: String = f"$typeName Tag $name"

    // Gets the pretty strings from the child, then adjusts them with prefixes
    def adjustChildStrings(child: Tag, isLastChild: Boolean = false): Vector[String] = {

      // Some nice prefixes. This will make sub-children with siblings look pretty.
      val headPrefix: String = "  |- "
      val internalContinuationPrefix: String = "  |  "
      val endChildPrefix: String = "     "

      // Simple function, but I like clear names
      def prefixed(prefix: String, string: String): String = prefix + string

      // Get the pretty child strings, adjust the head, then adjust the tail with the appropriate prefix
      val childStrings = child.toPrettyStrings
      val adjustedHead = prefixed(headPrefix, childStrings.head)
      // Determine tail adjustment
      val tailAdjustment = if (!isLastChild) internalContinuationPrefix else endChildPrefix
      val adjustedTail = childStrings.tail.map(prefixed(tailAdjustment, _))

      adjustedHead +: adjustedTail

    }

    // Adjusts the pretty strings from all but the last child. Such strings need a continuation prefix
    // in order to be pretty!
    val coTailStrings = payload.dropRight(1).foldLeft[Vector[String]](Vector())({ case (curStrings, nextChild) =>

      // We might need to add some buffer lines. It's a bit hard to read when compound tags are smooshed together.
      val bufferPrefix: String = "  |  "
      val maybeBufferLine = nextChild match {
        case CompoundTag(_, _) => Vector(bufferPrefix)
        case _ => Vector()
      }

      curStrings ++ maybeBufferLine ++ adjustChildStrings(nextChild) ++ maybeBufferLine

    })

    // The last child does not need the internal continuation prefixes. To be pretty, it just needs empty prefixes
    val coHeadStrings = adjustChildStrings(payload.last, isLastChild = true)


    headerString +: (coTailStrings ++ coHeadStrings)
  }
}
