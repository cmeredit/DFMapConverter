package io.github.cmeredit.examples

import io.github.cmeredit.game_map.GameMap
import io.github.cmeredit.masks.Mask
import io.github.cmeredit.mesh.Mesh

object ExportToMeshApp extends App {

  // Load map wrapper from .nbt file
  val map: GameMap = GameMap.fromUncompressedNBT("Resources/NBT/df map export - 2024-12-19 20-17-8.nbt")

  // Grab some useful masks
  val walkable: Mask = map.masks("walkableMask")
  val passableFlowDown: Mask = map.masks("passableFlowDownMask")

  // Define our own mask
  val openSpaces: Mask = walkable or passableFlowDown

  // Build a mesh from our open spaces mask.
  // "from solidity mask" is a bad name that will be changed.
  val openSpacesMesh: Mesh = Mesh.surfaceMeshFromSolidityMask(openSpaces)

  // I like the generated mesh to have xy coordinates between 0 and 1, but you can use whatever scale you want.
  val scale: Double = 1.0/map.xTileSize.toDouble

  openSpacesMesh.saveToObj("Resources/OBJ/example mesh.obj", scale)

}