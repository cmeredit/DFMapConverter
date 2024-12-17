package io.github.cmeredit.mesh

import java.io.{BufferedWriter, FileWriter}

case class Mesh(faces: Vector[Face]) {

  def saveToObj(filename: String, scale: Double): Unit = {

    val vertexIndexMap: Map[Vertex, Int] = faces.flatMap(_.vertices).distinct.zipWithIndex.toMap

    val outFile: BufferedWriter = new BufferedWriter(new FileWriter(filename))

    vertexIndexMap.foreach({case (Vertex(x, y, z), _) =>
      outFile.write(f"v ${x.toDouble * scale} ${y.toDouble * scale} ${z.toDouble * scale}")
    })

    faces.foreach({case Face(vertices) =>
      val vertexLabels = vertices.map(v => (vertexIndexMap(v) + 1).toString)
      val vertexLabelLine = vertexLabels.reduce(_ + " " + _)
      outFile.write(f"f $vertexLabelLine")
    })

    outFile.close()

  }

}