package io.github.cmeredit.masks

import BoundaryOptions._
import io.github.cmeredit.Orientations._

// Provides convenience methods for mask data stored as a 1-Dim container of numerical values
case class Mask(data: Vector[Short], xDim: Int, yDim: Int, zDim: Int) {

  // Adding this in case I want to change what numeric type is used later, but I doubt it.
  // At a minimum, it gives a name to a random number.
  //
  // The reason I'm using shorts is that DF maps are composed of chunks of 16x16 tiles, and each
  // map can have 3 or more chunks in any direction. I.e., 16 is the gcd of all possible map
  // sizes (xDim's and yDim's). I would really like each x-row to be a whole number of
  // data carriers, and shorts are the largest numeric type to meet this requirement.
  private val shortSizeBits = 16

  // I keep getting "map size in tiles" mixed up with "map size when compressed into shorts" mixed
  // up while coding. Adding in some clearly named variables to make this easier.
  //
  // Number of shorts that encode a complete row of tiles on the map x-axis.
  val xRowSizeShorts: Int = xDim / 16
  // Number of shorts that encode a complete grid of tiles on the map xy-plane.
  // I.e., number of shorts per z layer
  val xyLayerSizeShorts: Int = xRowSizeShorts * yDim
  // Number of bits that encode a complete row of tiles on the map x-axis.
  // Since we have one bit per tile, this is just an alias for xDim
  val xRowSizeBits: Int = xDim
  // Number of bits that encode a complete grid of tiles on the map xy-plane.
  // Since we have one bit per tile, this is just the number of tiles per zLayer, i.e., xDim*yDim.
  val xyLayerSizeBits: Int = xRowSizeBits * yDim


  // Gets the mask value from the specified map coordinates.
  def getBit(x: Int, y: Int, z: Int): Int = {

    // Conceptually, the data is as a collection of xy layers, from lowest to highest z index.
    // Each xy layer is stored as a collection of x rows, from lowest to highest y index.
    //
    // I.e., the data array was filled using a loop like this:
    //
    // for z = 0 until zDim
    //   for y = 0 until yDim
    //     for x = 0 until xDim
    //       [Append mask bit from coordinate x, y, z to data buffer]
    //     end for
    //   end for
    // end for
    //
    // Convert tile coordinates to bit index based on the aforementioned storage scheme
    val bitIndex: Int = z * xyLayerSizeBits + y * xRowSizeBits + x
    // Convert bit index to an index of the data array (i.e., index of the short that contains the desired bit)
    val shortIndex: Int = bitIndex / shortSizeBits
    val targetShort: Short = data(shortIndex)

    // We've already passed by shortIndex * shortSizeBits bits. How much further do we need to go?
    val bitIndexWithinShort: Int = bitIndex % shortSizeBits

    // Shift the target bit all the way to the right, then mask to it. Signed-right-bit-shift smears the sign bit. E.g., (0b1000 >> 3) == 0b1111, not 0b0001.
    // Also, the bitshift operators are only defined for ints, so there's an implicit conversion happening here too. Thankfully, we don't need to deal with that (high bit stays the same)
    // Note that the bitIndexWithinShort is between 0 and 15 (shortSizeBits-1).
    // For example, if we're retrieving bit 15, then it's already at the least significant digit, so don't need to shift
    // Most significant digit (index 0) would need to be shifted 15 bits.
    val targetBit = (targetShort >> ((shortSizeBits - 1) - bitIndexWithinShort)) & 1

    targetBit
  }

  // Convenience function for interpreting mask entries as booleans instead of bits.
  def getBoolean(x: Int, y: Int, z: Int): Boolean = {
    getBit(x, y, z) == 1
  }

  // Convenience functions for applying element-wise operations between masks.
  // I only define a single unary operation, which just flips each mask bit.
  def neg(): Mask = Mask(data.map(s => (~s).toShort), xDim, yDim, zDim)

  // Standard logical operations and applyOp, which makes it easier to define nonstandard ops as well.
  // For example, you could write something like m1.implies(m2) as m1.applyOp(m2, (s1, s2) => (~s1 | s2).toShort).
  def applyOp(other: Mask, op: (Short, Short) => Short): Mask = Mask(data.zip(other.data).map({ case (s1, s2) => op(s1, s2) }), xDim, yDim, zDim)

  def or(other: Mask): Mask = applyOp(other, (s1, s2) => (s1 | s2).toShort)

  def and(other: Mask): Mask = applyOp(other, (s1, s2) => (s1 & s2).toShort)

  def nand(other: Mask): Mask = applyOp(other, (s1, s2) => (~(s1 & s2)).toShort)

  def xor(other: Mask): Mask = applyOp(other, (s1, s2) => (s1 ^ s2).toShort)

  def iff(other: Mask): Mask = (this xor other).neg()

  // Returns a copy of the current mask that has been shifted up/down one xy / yz / xz layer, depending on [direction]
  // The copy does not contain the portion of the original mesh that gets shifted out of bounds
  // extensionMethod determines how to fill the coordinates that got shifted away from. E.g., if shifting up
  // in the Z direction, this determines how the bottom xy layer in the new mask should be filled
  // For example, with method "AllTrue", the bottom xy layer of the returned mask will have all its bits set.
  // As another example, with the method "CopyBoundary", the bottom xy layer of the returned mask is the same as in the original mask.
  def shifted(direction: Orientation, extensionMethod: BoundaryExtensionMethod = AllFalse): Mask = direction match {
    case Orientation(X, upwards) => {

      // This is going to be the worst of all the shifting methods.
      // The reason is that no individual short needs to be changed in the y or z shifting methods.
      // However, since each short is a collection of x-aligned mask values, we need to shift bits
      // within each short, and also carry bits between successive shorts (not passing xRow boundaries).

      // I'm not sure if this is the best implementation, but I think this is going to be fairly slow
      // no matter what I do.

      // Unlike in the other cases, we're not going to make a clear "retained" and "replacement".
      // Instead, we're going to group by x row, operate on that row, and flatten.

      val xRows: Iterator[Vector[Short]] = data.grouped(xRowSizeShorts)
      val updatedXRows: Iterator[Vector[Short]] = xRows.map(xRow => {

        // To shift this x row up, we need to shift every bit up, carrying bits between successive shorts.
        // We'll replace the very first bit depending on the extension method, and lose the very last bit of the last short no matter what.
        // This seems like the work of a fold!
        //
        // If shifting downwards, we'll fold right instead, carrying highest bits to lowest
        val firstShortToProcess: Short = if (upwards) xRow.head else xRow.last

        val emptyMaskInt = 0x0000
        val fullMaskInt = 0xFFFF
        val topBitMaskInt = 0x8000
        val bottomBitMaskInt = 0x0001
        val setCarryBitMaskInt = if (upwards) topBitMaskInt else bottomBitMaskInt

        val firstBitSettingMask: Int = extensionMethod match {
          case BoundaryOptions.AllFalse => emptyMaskInt
          case BoundaryOptions.AllTrue => if (upwards) topBitMaskInt else bottomBitMaskInt
          case BoundaryOptions.CopyBoundary => if (upwards) topBitMaskInt & firstShortToProcess else firstShortToProcess & bottomBitMaskInt
        }

        // Converts a short to the corresponding unsigned int.
        def getUInt(short: Short): Int = short & fullMaskInt

//        // Right shifts a short without dragging the sign bit
//        // Note that this is always used with further bitwise operations, so don't convert back to a short quite yet.
//        def rightshiftUShort(short: Short): Int = getUInt(short) >> 1

        // If shifting the mask upwards, we'll need to shift individual bits upwards within
        // and between shorts. In order to prevent weirdness with sign bits, when shifting right,
        // first convert to an unsigned int
        val shiftUShort: Short => Int = {
          if (upwards) {
            getUInt(_) >> 1
          } else {
            _ << 1
          }
        }

        // If shifting upwards, we'll take the least significant bit to the next short
        // Otherwise, we're taking the most significant bit to the preceding short
        val carryMask: Int = if (upwards) bottomBitMaskInt else topBitMaskInt

        def getCarry(short: Short): Boolean = (short & carryMask) == carryMask

        // Get first updated short by shifting in the previously decided direction and setting the appropriate bit
        val firstUpdatedShort: Short = (shiftUShort(firstShortToProcess) | firstBitSettingMask).toShort
        val firstCarry: Boolean = getCarry(firstShortToProcess)

        // Prepare for the fold.
        // We'll start with the first updated short and a flag for whether we need to carry
        // In the fold, we'll take each subsequent short, shift it, incorporate previous carry, and set the next carry flag.
        val initUpdatedRow: Vector[(Short, Boolean)] = Vector((firstUpdatedShort, firstCarry))
        val remainingSectionToUpdate: Vector[Short] = if (upwards) xRow.tail else xRow.dropRight(1)

        val foldingOp: (Vector[(Short, Boolean)], Short) => Vector[(Short, Boolean)] = {
          case (curUpdatedRow, nextShort) =>

            val previousCarryFlag: Boolean = if (upwards) curUpdatedRow.last._2 else curUpdatedRow.head._2
            val newReplacementBit: Int = if (previousCarryFlag) setCarryBitMaskInt else emptyMaskInt

            val updatedNextShort: Short = (shiftUShort(nextShort) | newReplacementBit).toShort
            val nextCarry: Boolean = getCarry(nextShort)

            if (upwards) curUpdatedRow.appended((updatedNextShort, nextCarry))
            else curUpdatedRow.prepended((updatedNextShort, nextCarry))

        }

        val shiftedXRowWithCarries: Vector[(Short, Boolean)] = if (upwards) {
          remainingSectionToUpdate.foldLeft(initUpdatedRow)(foldingOp)
        } else {
          remainingSectionToUpdate.foldRight(initUpdatedRow)({ case (nextShort, curUpdatedRow) =>
            foldingOp(curUpdatedRow, nextShort)
          })
        }

        val shiftedXRow: Vector[Short] = shiftedXRowWithCarries.map({ case (updatedShort, _ /*Carry info (which can now be forgotten)*/ ) => updatedShort })

        shiftedXRow

      })

      // Traverse the iterator and put the results in a vector
      val updatedData = updatedXRows.flatten.toVector

      // New mask has the same dimensions as the original.
      Mask(updatedData, xDim, yDim, zDim)
    }

    case Orientation(Y, upwards) => {
      // This is the portion of the data that gets directly copied to the new mask.
      //
      // If shifting upwards:
      //
      // The bottom xz layer is not retained. Our data container is arranged by xy layer, not xz layer!
      // Therefore, we need to think about what data is retained from each xy layer.
      // Shifting up by y will make us lose the top x row from each xy layer, shift all other x rows up.
      // This means that from each block of xyLayerSizeShorts, we need to take the shorts that represent the bottom (yDim-1) x rows.
      // This is exactly the first xyLayerSizeShorts - xRowSizeShorts of the xy layer.
      //
      // This is equivalent to something like:
      // data.grouped(xyLayerSizeShorts).map(xyLayer => xyLayer.dropRight(xRowSizeShorts))
      //
      // Unlike in the z shift case, we can't just tack the replacement data on the beginning or end of the new data array.
      // Instead, it needs to be interleaved with the retained values. For this reason, we won't flatten things quite yet.
      //
      // Each element of this iterator is the retained data from the corresponding xy layer.
      //
      // If shifting downwards:
      // Similar, except we want to start our slide one x-row worth of data later.
      val retainedData: Iterator[Vector[Short]] = if (upwards) {
        data.sliding(xyLayerSizeShorts - xRowSizeShorts, xyLayerSizeShorts)
      } else {
        data.drop(xRowSizeShorts).sliding(xyLayerSizeShorts - xRowSizeShorts, xyLayerSizeShorts)
      }

      // Like before, reason about what to take from each xy layer.
      val replacementData: Iterator[Vector[Short]] = extensionMethod match {
        case AllTrue =>
          val replacementRow: Vector[Short] = Vector.fill(xRowSizeShorts)(0xFFFF.toShort)
          Iterator.fill(zDim)(replacementRow)
        case AllFalse =>
          val replacementRow: Vector[Short] = Vector.fill(xRowSizeShorts)(0x0000.toShort)
          Iterator.fill(zDim)(replacementRow)
        case CopyBoundary => if (upwards) {
          data.sliding(xRowSizeShorts, xyLayerSizeShorts) // Take the bottom x row from each xy layer
        } else {
          data.drop(xyLayerSizeShorts - xRowSizeShorts).sliding(xRowSizeShorts, xyLayerSizeShorts) // Take the top x row from each xy layer
        }
      }

      // If shifting up, the replacement data should be at the bottom of each xy layer. Otherwise, it should be at the top.
      val combinedData: Vector[Short] = if (upwards) {
        retainedData.zip(replacementData).flatMap({ case (retained, replacement) => replacement ++ retained }).toVector
      } else {
        retainedData.zip(replacementData).flatMap({ case (retained, replacement) => retained ++ replacement }).toVector
      }

      // New mask has the same dimensions as the original.
      Mask(combinedData, xDim, yDim, zDim)
    }

    case Orientation(Z, upwards) => {

      // If we're shifting upwards, then drop the top xy layer of shorts. Otherwise, drop the bottom xy later.
      val retainedData: Vector[Short] = if (upwards) data.dropRight(xyLayerSizeShorts) else data.drop(xyLayerSizeShorts)

      // We also need to determine what data makes up the bottom xy layer of the new mask.
      // Whatever method we use, it should consist of a vector of xyLayerSizeShorts many shorts.
      val replacementData: Vector[Short] = extensionMethod match {
        case AllTrue => Vector.fill(xyLayerSizeShorts)(0xFFFF.toShort)
        case AllFalse => Vector.fill(xyLayerSizeShorts)(0x0000.toShort)
        case CopyBoundary => if (upwards) data.take(xyLayerSizeShorts) else data.takeRight(xyLayerSizeShorts)
      }


      // If shifting upwards, we want the replacement data to be the *bottom* xy layer of the new mask, so it should
      // be at the *front* of combinedData. Putting retainedData at the end is the same as shifting it upwards.
      // If shifting downwards, we want the opposite
      val combinedData: Vector[Short] = if (upwards) replacementData ++ retainedData else retainedData ++ replacementData

      // New mask has the same dimensions as the original.
      Mask(combinedData, xDim, yDim, zDim)
    }

  }

  // Extracts the bit indices of set bits in vecShort.
  private def shortsToIndices(vecShort: Vector[Short]): Vector[Int] = {

    // Use the index of each short to get the number of previous bits.
    // Within each short, extract the bit indices of the set bits.
    // Then combine the resulting info into overall bit/flag index.
    vecShort.zipWithIndex.flatMap({case (short, shortIndex) =>

      // shortIndex previous shorts, each with shortSizeBits bits
      val numPreviousBits: Int = shortIndex * shortSizeBits

      // Bit-shifting a short widens the value anyway - Scala only supports bit-shifting ints.
      // Instead of repeating the widening 8 times, just do it once.
      val widenedVal: Int = short & 0xFFFF

      //      // You know, maybe this is the simplest way, but it's probably not the fastest.
      //      // We're doing a lot of unnecessary shifting.
      //      // Instead of going through a pseudo loop like this, let's just
      //      // unroll the entire loop and skip the shifting step entirely.
      //      val setBitIndices = (0 until shortSizeBits).filter(k => {
      //        ((widenedVal << k) & 0x8000) == 0x8000
      //      }).toVector

      // Yes, I know how ugly an unrolled loop is, but can you argue it's not fast?
      // If so, please let me know!
      val isolatedBits = Vector(
        widenedVal & 0x8000, // 1000 0000 ...
        widenedVal & 0x4000, // 0100 0000 ...
        widenedVal & 0x2000, // 0010 0000 ...
        widenedVal & 0x1000, // 0001 0000 ...
        widenedVal & 0x800, // 0000 1000 ...
        widenedVal & 0x400, // 0000 0100 ...
        widenedVal & 0x200, // 0000 0010 ...
        widenedVal & 0x100, // 0000 0001 ...
        widenedVal & 0x80, // ... 1000 0000
        widenedVal & 0x40, // ... 0100 0000
        widenedVal & 0x20, // ... 0010 0000
        widenedVal & 0x10, // ... 0001 0000
        widenedVal & 0x8, // 1000
        widenedVal & 0x4, // 0100
        widenedVal & 0x2, // 0010
        widenedVal & 0x1, // 0001
      )

      val setBitIndices = isolatedBits.zipWithIndex.filter(_._1 != 0).map(_._2)

      setBitIndices.map(_ + numPreviousBits)

    })
  }

  // Returns the bit/flag index of each true flag.
  // E.g., if the first few shorts are:
  // 0000 1000 0000 0000
  // 0000 0000 0010 0000
  // 0000 1000 0000 0001
  // ...
  // Then the returned vector will be
  // Vector(4, 26, 36, 47, ...)
  //
  // Extracting individual flag values is slow, so try to minimize your use of this function.
  def getTrueFlagIndices: Vector[Int] = shortsToIndices(data)

  private def coordinatesFromBitIndex(index: Int): (Int, Int, Int) = {
    (index % xRowSizeBits, (index / xRowSizeBits) % yDim, index / xyLayerSizeBits)
  }
  def getTrueFlagCoordinates: Vector[(Int, Int, Int)] = {

    getTrueFlagIndices.map(coordinatesFromBitIndex)

  }

  // Returns the bit/flag index of each true flag relative to its x-row.
  // For example, suppose an x-row is exactly 16 tiles / 1 short long. Then the first bit of
  // the second short has absolute bit index 17, but bit index 0 within its row.
  def getTrueFlagIndicesWithinXRows: Vector[Vector[Int]] = {
    data.grouped(xRowSizeShorts).map(shortsToIndices).toVector
  }


}
