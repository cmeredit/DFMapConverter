package io.github.cmeredit.game_map

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
    // Note that the bitIndexWithinShort is between 0 and 15 (shortSizeBits-1).
    // For example, if we're retrieving bit 15, then it's already at the least significant digit, so don't need to shift
    // Most significant digit (index 0) would need to be shifted 15 bits.
    val targetBit = (targetShort >> ((shortSizeBits-1) - bitIndexWithinShort)) & 1

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
  def applyOp(other: Mask, op: (Short, Short) => Short): Mask = Mask(data.zip(other.data).map({case (s1, s2) => op(s1, s2)}), xDim, yDim, zDim)
  def or(other: Mask): Mask = applyOp(other, (s1, s2) => (s1 | s2).toShort)
  def and(other: Mask): Mask = applyOp(other, (s1, s2) => (s1 & s2).toShort)
  def xor(other: Mask): Mask = applyOp(other, (s1, s2) => (s1 ^ s2).toShort)
  def iff(other: Mask): Mask = (this xor other).neg()

  // Returns a copy of the current mask that has been shifted up one z coordinate / xy layer.
  // The copy does not contain the highest xy layer of the original mask.
  // The bottom xy layer in the copy is determined by the extension method argument.
  // For example, with method "AllTrue", the bottom xy layer of the returned mask will have all its bits set.
  // As another example, with the method "CopyBoundary", the bottom xy layer of the returned mask is the same as in the original mask.
  def shiftedUpZ(extensionMethod: BoundaryOptions.BoundaryExtensionMethod): Mask = {
    import BoundaryOptions._

    // This is the portion of the data that gets directly copied to the new mask.
    // The top xy layer is not retained, so we drop that number of shorts from the end of the current data (which is arranged by xy layers anyway)
    val retainedData: Vector[Short] = data.dropRight(xyLayerSizeShorts)

    // We also need to determine what data makes up the bottom xy layer of the new mask.
    // Whatever method we use, it should consist of a vector of xyLayerSizeShorts many shorts.
    val replacementData: Vector[Short] = extensionMethod match {
      case AllTrue => Vector.fill(xyLayerSizeShorts)(0xFFFF.toShort)
      case AllFalse => Vector.fill(xyLayerSizeShorts)(0x0000.toShort)
      case CopyBoundary => data.take(xyLayerSizeShorts)
    }

    // We want the replacement data to be the *bottom* xy layer of the new mask, so it should
    // be at the *front* of combinedData. Putting retainedData at the end is the same as shifting it upwards.
    val combinedData: Vector[Short] = replacementData ++ retainedData

    // New mask has the same dimensions as the original.
    Mask(combinedData, xDim, yDim, zDim)

  }

  // Returns a copy of the current mask that has been shifted down one z coordinate / xy layer.
  // The copy does not contain the lowest xy layer of the original mask.
  // The highest xy layer in the copy is determined by the extension method argument.
  def shiftedDownZ(extensionMethod: BoundaryOptions.BoundaryExtensionMethod): Mask = {
    import BoundaryOptions._

    // This is the portion of the data that gets directly copied to the new mask.
    // The bottom xy layer is not retained, so we drop that number of shorts from the beginning of the current data (which is arranged by xy layers anyway)
    val retainedData: Vector[Short] = data.drop(xyLayerSizeShorts)

    // We also need to determine what data makes up the top xy layer of the new mask.
    // Whatever method we use, it should consist of a vector of xyLayerSizeShorts many shorts.
    val replacementData: Vector[Short] = extensionMethod match {
      case AllTrue => Vector.fill(xyLayerSizeShorts)(0xFFFF.toShort)
      case AllFalse => Vector.fill(xyLayerSizeShorts)(0x0000.toShort)
      case CopyBoundary => data.takeRight(xyLayerSizeShorts)
    }

    // We want the replacement data to be the *top* xy layer of the new mask, so it should
    // be at the *end* of combinedData. Putting retainedData at the front is the same as shifting it downwards.
    val combinedData: Vector[Short] = retainedData ++ replacementData

    // New mask has the same dimensions as the original.
    Mask(combinedData, xDim, yDim, zDim)
  }

  // Returns a copy of the current mask that has been shifted up one y coordinate / xz layer.
  // The copy does not contain the highest xz layer of the original mask.
  // The lowest xz layer in the copy is determined by the extension method argument.
  def shiftedUpY(extensionMethod: BoundaryOptions.BoundaryExtensionMethod): Mask = {
    import BoundaryOptions._

    // This is the portion of the data that gets directly copied to the new mask.
    // The bottom xz layer is not retained. Our data container is arranged by xy layer, not xz layer.
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
    val retainedData: Iterator[Vector[Short]] = data.sliding(xyLayerSizeShorts - xRowSizeShorts, xyLayerSizeShorts)

    // Like before, reason about what to take from each xy layer.
    val replacementData: Iterator[Vector[Short]] = extensionMethod match {
      case AllTrue =>
        val replacementRow: Vector[Short] = Vector.fill(xRowSizeShorts)(0xFFFF.toShort)
        Iterator.fill(zDim)(replacementRow)
      case AllFalse =>
        val replacementRow: Vector[Short] = Vector.fill(xRowSizeShorts)(0x0000.toShort)
        Iterator.fill(zDim)(replacementRow)
      case CopyBoundary => data.sliding(xRowSizeShorts, xyLayerSizeShorts) // Take the bottom x row from each xy layer
    }

    // Shifting *up* in y, so each replacement row should be placed at the *bottom* of its xy layer
    val combinedData: Vector[Short] = retainedData.zip(replacementData).flatMap({case (retained, replacement) => replacement ++ retained}).toVector

    // New mask has the same dimensions as the original.
    Mask(combinedData, xDim, yDim, zDim)

  }

  // Returns a copy of the current mask that has been shifted down one y coordinate / xz layer.
  // The copy does not contain the lowest xz layer of the original mask.
  // The highest xz layer in the copy is determined by the extension method argument.
  def shiftedDownY(extensionMethod: BoundaryOptions.BoundaryExtensionMethod): Mask = {
    import BoundaryOptions._

    // See shiftedUpY comments for our general approach.
    // As for the details of shifting down, we want to lose the bottom x row from each xy layer.
    // The sliding function doesn't appear to have a version with the start index specified, so we achieve
    // the same results by dropping the first x row.
    //
    // This is equivalent to something like:
    // data.grouped(xyLayerSizeShorts).map(xyLayer => xyLayer.drop(xRowSizeShorts))
    //
    // Each element of this iterator is the retained data from the corresponding xy layer.
    val retainedData: Iterator[Vector[Short]] = data.drop(xRowSizeShorts).sliding(xyLayerSizeShorts - xRowSizeShorts, xyLayerSizeShorts)

    val replacementData: Vector[Vector[Short]] = extensionMethod match {
      case AllTrue =>
        val replacementRow: Vector[Short] = Vector.fill(xRowSizeShorts)(0xFFFF.toShort)
        Vector.fill(zDim)(replacementRow)
      case AllFalse =>
        val replacementRow: Vector[Short] = Vector.fill(xRowSizeShorts)(0x0000.toShort)
        Vector.fill(zDim)(replacementRow)
      case CopyBoundary => data.drop(xyLayerSizeShorts - xRowSizeShorts).sliding(xRowSizeShorts, xyLayerSizeShorts).toVector // Take the top x row from each xy layer
    }

    // Shifting *down* in y, so each replacement row should be placed at the *top* of its xy layer
    val combinedData: Vector[Short] = retainedData.zip(replacementData).flatMap({case (retained, replacement) => retained ++ replacement}).toVector

    // New mask has the same dimensions as the original.
    Mask(combinedData, xDim, yDim, zDim)

  }

}
