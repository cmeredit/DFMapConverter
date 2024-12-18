package io.github.cmeredit.mesh

import io.github.cmeredit.Orientations._

case class Face(vertices: Vector[Vertex])

object Face {
  def squareFromAxisWithUpwardsOffset(orientation: Orientation, basePoint: Vertex): Face = {
    // Order in which to perturb the base point to achieve a CC-oriented face
    val standardPerturbationAxes = orientation.axis match {
      case X => Vector(Y, Z)
      case Y => Vector(Z, X)
      case Z => Vector(X, Y)
    }

    val ourAxes = if (orientation.upwards) standardPerturbationAxes else standardPerturbationAxes.reverse

    val faceVerts: Vector[Vertex] = Vector(
      basePoint,
      basePoint.perturbed(ourAxes.take(1)),
      basePoint.perturbed(ourAxes),
      basePoint.perturbed(ourAxes.takeRight(1))
    )

    if (orientation.upwards)
      Face(faceVerts.map(_.perturbed(Vector(orientation.axis))))
    else
      Face(faceVerts)

  }
}
