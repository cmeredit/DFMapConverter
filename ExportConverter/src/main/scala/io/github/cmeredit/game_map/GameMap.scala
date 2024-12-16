package io.github.cmeredit.game_map

import io.github.cmeredit.nbt._
import tags._

import java.nio.ByteBuffer

class GameMap(
             val xTileSize: Int,
             val yTileSize: Int,
             val zTileSize: Int,
             val masks: Map[String, Mask]
             ) {

}

object GameMap {

  def fromUncompressedNBT(path: String): GameMap = {

    val loadedTag: CompoundTag = IO.fromUncompressedFile(path)

    val payloadMap: Map[String, Tag] = loadedTag.payload.map(tag => (tag.name, tag)).toMap

    def getIntFromPayloadMap(name: String): Int = {
      assert(payloadMap.contains(name), f".nbt file successfully loaded, but tag '$name' not found.")
      payloadMap(name) match {
        case ByteTag(_, byte) => byte & 0xFF
        case _ =>
          assert(assertion = false, f"'$name' tag loaded from .nbt file was not a byte.")
          0
      }
    }

    val xTileSize: Int = getIntFromPayloadMap("x")
    val yTileSize: Int = getIntFromPayloadMap("y")
    val zTileSize: Int = getIntFromPayloadMap("z")

    def getMaskFromPayloadMap(name: String): Option[Mask] = {

      def byteVecToShortVec(byteVec: Vector[Byte]): Vector[Short] = {
        assert(byteVec.length % 2 == 0, "Byte vector cannot be converted to vector of shorts - wrong parity.")

        byteVec.grouped(2).toVector.map(bytePair => ByteBuffer.wrap(bytePair.toArray).getShort)
      }

      if (payloadMap.contains(name)) {
        Some({
          payloadMap(name) match {
            case ByteArrayTag(_, payload) => Mask(byteVecToShortVec(payload), xTileSize, yTileSize, zTileSize)
            case _ =>
              assert(assertion = false, f"'$name' tag loaded from .nbt file was not a byte array.")
              Mask(Vector(), xTileSize, yTileSize, zTileSize)
          }
        })
      } else None
    }

    val validMaskNames = Vector("walkableMask", "passableFlowDownMask", "magmaMask", "openTileMask")

    val retrievedMasks: Map[String, Mask] = validMaskNames.flatMap(maskName => getMaskFromPayloadMap(maskName).map(mask => (maskName, mask))).toMap

    assert(retrievedMasks.nonEmpty, ".nbt file contained no masks.")

    new GameMap(xTileSize, yTileSize, zTileSize, retrievedMasks)

  }

}