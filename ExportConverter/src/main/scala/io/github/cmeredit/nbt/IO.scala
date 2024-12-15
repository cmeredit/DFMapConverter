package io.github.cmeredit.nbt

import io.github.cmeredit.nbt.tags._
import MagicNumbers._

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

object IO {

  // Loads CompoundTag from uncompressed .nbt file
  def fromUncompressedFile(path: String): CompoundTag = {

    val loadedTag = fromByteBuffer(ByteBuffer.wrap(Files.readAllBytes(Paths.get(path))))

    loadedTag match {
      case x: CompoundTag => x
      case _ =>
        println("Loaded tag from file, but it was not a compound tag.")
        loadedTag.printPretty()
        assert(false)
        CompoundTag("", Vector())
    }

  }


  // Reads a tag from the byte buffer, mutating the buffer along the way
  //
  // I really dislike the TAG_List tag. Apparently .nbt should actually be .[named-in-all-of-13-cases-except-for-tag_list-and-tag_none]bt
  // To deal with the quirkiness of list tags, there are optional override fields enabled. You SHOULD NOT set these unless you're absolutely certain.
  // If they are set, then we won't read the corresponding bytes from the byte buffer. If the name and id bytes ARE present and you've set an
  // override, then everything will get out of alignment.
  // Alternatively, think of it this way: When the overrides are set, you're assuming the byte buffer starts directly with the payload and is missing the
  // 3+name_length tagID and name bytes.
  private def fromByteBuffer(bb: ByteBuffer, nameOverride: Option[String] = None, tagIDOverride: Option[Int] = None): Tag = {

    val tagID: Int = tagIDOverride.getOrElse(bb.get & 0xFF)
    val name: String = nameOverride.getOrElse({
      val nameLength: Int = bb.getShort & 0xFFFF

      val nameBytes = new Array[Byte](nameLength)
      bb.get(nameBytes)
      new String(nameBytes, StandardCharsets.UTF_8)
    })

    // Read data based on the tagID. In most cases, we can just use the
    // byte buffer to convert a few bytes into the appropriate type.
    // For array tags (including string), we can still just rely on the byte buffer
    // However, for container tags, we need to use recursion.
    tagID match {
      case TagIDs.byte => ByteTag(name, bb.get)
      case TagIDs.short => ShortTag(name, bb.getShort)
      case TagIDs.int => IntTag(name, bb.getInt)
      case TagIDs.long => LongTag(name, bb.getLong)
      case TagIDs.float => FloatTag(name, bb.getFloat)
      case TagIDs.double => DoubleTag(name, bb.getDouble)
      case TagIDs.byteArray =>
        val payloadLength: Int = bb.getInt
        val payloadBytes = new Array[Byte](payloadLength)
        bb.get(payloadBytes)
        ByteArrayTag(name, payloadBytes.toVector)
      case TagIDs.string =>
        val payloadLength: Int = bb.getShort() & 0xFFFF
        val payloadBytes = new Array[Byte](payloadLength)
        bb.get(payloadBytes)
        val payload: String = new String(payloadBytes, StandardCharsets.UTF_8)
        StringTag(name, payload)
      case TagIDs.list =>
        val payloadTagID: Int = bb.get & 0xFF
        val payloadNumEntries: Int = bb.getInt
        val payload = (0 until payloadNumEntries).toVector.map(payloadIndex => fromByteBuffer(bb, Some(f"$name $payloadIndex"), Some(payloadTagID)))
        ListTag(name, payload)
      case TagIDs.compound =>
        // I know, I know. This isn't purely functional. But it's so dang annoying sometimes to try to
        // write in a purely FP style when you're dealing with a crazy file format and
        // a mutable buffer anyway.
        var payload: Vector[Tag] = Vector()
        var nextTagID: Int = bb.get & 0xFF
        // Read tags until we hit the TAG_End tag
        while (nextTagID != 0) {

          // We've already consumed the tag ID, so make sure to pass it down
          payload = payload :+ fromByteBuffer(bb, None, Some(nextTagID))

          // Read and record the next tag ID
          nextTagID = bb.get & 0xFF

        }
        CompoundTag(name, payload)
      // Not sure why TAG_Short_Array doesn't exist lol
      case TagIDs.intArray =>
        // The payload length is recorded in nbt as the number of ints, not the number of bytes
        val payloadLength: Int = bb.getInt & 0xFF
        val payload = Vector.fill(payloadLength)(bb.getInt)
        IntArrayTag(name, payload)
      case TagIDs.longArray =>
        // The payload length is recorded in nbt as the number of longs, not the number of bytes
        val payloadLength: Int = bb.getInt & 0xFF
        val payload = Vector.fill(payloadLength)(bb.getLong)
        LongArrayTag(name, payload)
    }
  }

}
