package io.github.cmeredit.mesh

import io.github.cmeredit.Orientations._
import io.github.cmeredit.masks.Mask

import java.io.{BufferedWriter, FileWriter}

case class Mesh(faces: Vector[Face]) {

  def saveToObj(filename: String, scale: Double): Unit = {


    val orderedVertices: Vector[Vertex] = faces.flatMap(_.vertices).distinct
    val vertexIndexMap: Map[Vertex, Int] = orderedVertices.zipWithIndex.toMap

    val outFile: BufferedWriter = new BufferedWriter(new FileWriter(filename))

    orderedVertices.foreach({case Vertex(x, y, z) =>
      outFile.write(f"v ${x.toDouble * scale} ${y.toDouble * scale} ${z.toDouble * scale}\n")
    })

    faces.foreach({case Face(vertices) =>
      val vertexLabels = vertices.map(v => (vertexIndexMap(v) + 1).toString)
      val vertexLabelLine = vertexLabels.reduce(_ + " " + _)
      outFile.write(f"f $vertexLabelLine\n")
    })

    outFile.close()

  }

}

object Mesh {

  // Puts a unit cube at each set bit in the cubeMask. You probably
  // don't want to use this function. In most cases, this will produce a mesh
  // with a lot of redundant geometry (e.g., internal faces and vertices that
  // could be dissolved).
  //
  // This is a great function for debugging and testing, but it's almost
  // certainly not what you want to be using.
  def fromCubeLocations(cubeMask: Mask): Mesh = {
    def getCubeAt(x: Int, y: Int, z: Int): Vector[Face] = {
      val v000 = Vertex(x, y, z)
      val v001 = Vertex(x, y, z+1)
      val v010 = Vertex(x, y+1, z)
      val v011 = Vertex(x, y+1, z+1)
      val v100 = Vertex(x+1, y, z)
      val v101 = Vertex(x+1, y, z+1)
      val v110 = Vertex(x+1, y+1, z)
      val v111 = Vertex(x+1, y+1, z+1)

      Vector(
        Face(Vector(v000, v010, v110, v100)), // Bottom
        Face(Vector(v001, v101, v111, v011)), // Top
        Face(Vector(v000, v100, v101, v001)), // Left
        Face(Vector(v010, v011, v111, v110)), // Right
        Face(Vector(v000, v001, v011, v010)), // Back
        Face(Vector(v100, v110, v111, v101)) // Front
      )
    }

    val faces: Vector[Face] = (0 until cubeMask.zDim).flatMap(z => {

      val zLayerFaces = (0 until cubeMask.yDim).flatMap(y => {
        val xRowFaces = (0 until cubeMask.xDim).flatMap(x => {
          if (cubeMask.getBoolean(x, y, z)) Some(getCubeAt(x, y, z)) else None
        }).flatten

        xRowFaces
      })

      zLayerFaces
    }).toVector

    Mesh(faces)
  }


  // Returns a mesh just containing the interface between flagged and un-flagged locations in the mesh.
  // I.e., a coordinate that is flagged that is adjacent to an un-flagged coordinate.
  // Considers off-mask coordinates as un-flagged / non-solid
  def surfaceMeshFromSolidityMask(solidityMask: Mask): Mesh = {

    import io.github.cmeredit.masks.BoundaryOptions.AllFalse

    val axes: Vector[Axis] = Vector(X, Y, Z)
    val directions: Vector[Boolean] = Vector(false, true)

    val orientations: Vector[Orientation] = for (
      axis <- axes;
      direction <- directions
    ) yield Orientation(axis, direction)


    val orientationMasks: Vector[(Orientation, Mask)] = orientations.map(orientation => {
      // E.g., if making *right*-hand surfaces, we need to shift the mask to the *left*
      val shiftingOrientation = orientation.copy(upwards = !orientation.upwards)
      val surfaceMask = solidityMask and solidityMask.shifted(shiftingOrientation, AllFalse).neg()
      (orientation, surfaceMask)
    })

    val faces: Vector[Face] = orientationMasks.flatMap({case (orientation, mask) =>
      val faceCoordinates: Vector[(Int, Int, Int)] = mask.getTrueFlagCoordinates
      faceCoordinates.map({case (x, y, z) =>
        Face.squareFromAxisWithUpwardsOffset(orientation, Vertex(x, y, z))
      })
    })

    Mesh(faces)

  }

}