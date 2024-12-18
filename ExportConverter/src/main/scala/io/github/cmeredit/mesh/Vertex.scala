package io.github.cmeredit.mesh

import io.github.cmeredit.Orientations._

case class Vertex(x: Int, y: Int, z: Int) {

  def perturbed(perturbationDirections: Vector[Axis]): Vertex = {
    def offset(axis: Axis): Int = if (perturbationDirections.contains(axis)) 1 else 0
    Vertex(x + offset(X), y + offset(Y), z + offset(Z))
  }

}
