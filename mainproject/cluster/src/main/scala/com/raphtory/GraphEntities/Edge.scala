package com.raphtory.GraphEntities

import scala.collection.mutable

/**
  * Created by Mirate on 01/03/2017.
  */
class Edge(msgID: Int, srcId: Int, dstId: Int, initialValue: Boolean, addOnly:Boolean)
    extends Entity(msgID, initialValue,addOnly) {

  /*override def printProperties: String =
    s"Edge between $srcId and $dstId: with properties: " + System
      .lineSeparator() +
      super.printProperties()
  */

  def killList(vKills: mutable.TreeMap[Int, Boolean]): Unit = {
    removeList ++= vKills
    if (!addOnly)
      previousState ++= vKills
  }
}
