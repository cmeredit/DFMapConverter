package io.github.cmeredit.game_map

object BoundaryOptions {

  sealed trait BoundaryExtensionMethod
  object AllFalse extends BoundaryExtensionMethod
  object AllTrue extends BoundaryExtensionMethod
  object CopyBoundary extends BoundaryExtensionMethod

}
