package io.github.cmeredit.game_map

case class Mask(data: Vector[Short], xDim: Int, yDim: Int, zDim: Int) {

  val xRowSizeShorts: Int = xDim / 16
  val xyLayerSizeShorts: Int = xRowSizeShorts * yDim

  val xRowSizeBits: Int = xDim
  val xyLayerSizeBits: Int = xRowSizeBits * yDim


  def getBit(x: Int, y: Int, z: Int): Int = {

    val bitIndex: Int = z * xyLayerSizeBits + y * xRowSizeBits + x
    val shortIndex: Int = bitIndex / 16

    val targetShort: Short = data(shortIndex)

    val bitIndexWithinShort: Int = bitIndex % 16

    // Shift the target bit all the way to the right, then mask to it.
    // Note that the bitIndexWithinShort is between 0 and 15.
    // For example, if we're retrieving bit 15, then it's already at the least significant digit, so don't need to shift
    // Most significant digit (index 0) would need to be shifted 15 bits.
    val targetBit = (targetShort >> (15 - bitIndexWithinShort)) & 1

    targetBit
  }

  def getBoolean(x: Int, y: Int, z: Int): Boolean = {
    getBit(x, y, z) == 1
  }

  def neg(): Mask = Mask(data.map(s => (~s).toShort), xDim, yDim, zDim)
  def applyOp(other: Mask, op: (Short, Short) => Short): Mask = Mask(data.zip(other.data).map({case (s1, s2) => op(s1, s2)}), xDim, yDim, zDim)
  def or(other: Mask): Mask = applyOp(other, (s1, s2) => (s1 | s2).toShort)
  def and(other: Mask): Mask = applyOp(other, (s1, s2) => (s1 & s2).toShort)
  def xor(other: Mask): Mask = applyOp(other, (s1, s2) => (s1 ^ s2).toShort)
  def iff(other: Mask): Mask = (this xor other).neg()


  def shiftedUpZ(extensionMethod: BoundaryOptions.BoundaryExtensionMethod): Mask = {
    import BoundaryOptions._
    extensionMethod match {
      case AllTrue =>
        val replacement: Short = 0xFFFF.toShort
        Mask(Vector.fill(xyLayerSizeShorts)(replacement) ++ data.dropRight(xyLayerSizeShorts), xDim, yDim, zDim)
      case AllFalse =>
        val replacement: Short = 0x0000.toShort
        Mask(Vector.fill(xyLayerSizeShorts)(replacement) ++ data.dropRight(xyLayerSizeShorts), xDim, yDim, zDim)
      case CopyBoundary =>
        Mask(data.take(xyLayerSizeShorts) ++ data.dropRight(xyLayerSizeShorts), xDim, yDim, zDim)
    }
  }

  def shiftedDownZ(extensionMethod: BoundaryOptions.BoundaryExtensionMethod): Mask = {
    import BoundaryOptions._
    extensionMethod match {
      case AllTrue =>
        val replacement: Short = 0xFFFF.toShort
        Mask(data.drop(xyLayerSizeShorts) ++ Vector.fill(xyLayerSizeShorts)(replacement), xDim, yDim, zDim)
      case AllFalse =>
        val replacement: Short = 0x0000.toShort
        Mask(data.drop(xyLayerSizeShorts) ++ Vector.fill(xyLayerSizeShorts)(replacement), xDim, yDim, zDim)
      case CopyBoundary =>
        Mask(data.drop(xyLayerSizeShorts) ++ data.takeRight(xyLayerSizeShorts), xDim, yDim, zDim)
    }
  }

  def shiftedUpY(extensionMethod: BoundaryOptions.BoundaryExtensionMethod): Mask = {
    import BoundaryOptions._


    val retainedData: Vector[Vector[Short]] = data.sliding(xyLayerSizeShorts - xRowSizeShorts, xyLayerSizeShorts).toVector
    val replacementData: Vector[Vector[Short]] = extensionMethod match {
      case AllTrue =>
        val replacementRow: Vector[Short] = Vector.fill(xRowSizeShorts)(0xFFFF.toShort)
        Vector.fill(zDim)(replacementRow)
      case AllFalse =>
        val replacementRow: Vector[Short] = Vector.fill(xRowSizeShorts)(0x0000.toShort)
        Vector.fill(zDim)(replacementRow)
      case CopyBoundary => data.sliding(xRowSizeShorts, xyLayerSizeShorts).toVector
    }

    Mask(replacementData.zip(retainedData).flatMap({case (replacement, retained) => replacement ++ retained}), xDim, yDim, zDim)

  }

  def shiftedDownY(extensionMethod: BoundaryOptions.BoundaryExtensionMethod): Mask = {
    import BoundaryOptions._


    val retainedData: Vector[Vector[Short]] = data.drop(xRowSizeShorts).sliding(xyLayerSizeShorts - xRowSizeShorts, xyLayerSizeShorts).toVector
    val replacementData: Vector[Vector[Short]] = extensionMethod match {
      case AllTrue =>
        val replacementRow: Vector[Short] = Vector.fill(xRowSizeShorts)(0xFFFF.toShort)
        Vector.fill(zDim)(replacementRow)
      case AllFalse =>
        val replacementRow: Vector[Short] = Vector.fill(xRowSizeShorts)(0x0000.toShort)
        Vector.fill(zDim)(replacementRow)
      case CopyBoundary => data.drop(xyLayerSizeShorts - xRowSizeShorts).sliding(xRowSizeShorts, xyLayerSizeShorts).toVector
    }

    Mask(replacementData.zip(retainedData).flatMap({case (replacement, retained) => replacement ++ retained}), xDim, yDim, zDim)

  }

}
