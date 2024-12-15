package io.github.cmeredit.game_map

case class Mask(data: Vector[Short]) {
  def getBit(x: Int, y: Int, z: Int, xDim: Int, yDim: Int, zDim: Int): Int = {

    assert(xDim * yDim * zDim<= data.length * 16 , s"Cannot get data: Mask size smaller than requested map size. ${data.length * 16} vs ${xDim * yDim * zDim}")

    val bitIndex: Int = z * (xDim * yDim) + y * xDim + x
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

  def getBoolean(x: Int, y: Int, z: Int, xDim: Int, yDim: Int, zDim: Int): Boolean = {
    getBit(x, y, z, xDim, yDim, zDim) == 1
  }


}
