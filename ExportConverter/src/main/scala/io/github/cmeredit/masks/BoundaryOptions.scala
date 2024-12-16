package io.github.cmeredit.masks

object BoundaryOptions {

  sealed trait BoundaryExtensionMethod

  object AllFalse extends BoundaryExtensionMethod

  object AllTrue extends BoundaryExtensionMethod

  object CopyBoundary extends BoundaryExtensionMethod

}
