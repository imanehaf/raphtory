package com.raphtory.examples.blockchain.ethereum.analysis

import com.raphtory.core.analysis.API.Analyser
import com.raphtory.core.analysis.API.GraphRepositoryProxies.WindowProxy
import com.raphtory.core.storage.EntityStorage
import com.raphtory.core.utils.{HistoryOrdering, Utils}

import scala.collection.mutable.ArrayBuffer
import scala.collection.parallel.immutable

class DegreeRanking extends Analyser {
  object sortOrdering extends Ordering[Int]{
    def compare(key1:Int, key2:Int) = key2.compareTo(key1)
  }
  override def analyse(): Unit = {
    val degree =proxy.getVerticesSet().map(vert=>{
      val vertex = proxy.getVertex(vert._2)
      val outDegree = vertex.getOutgoingNeighbors.size
      val inDegree = vertex.getIngoingNeighbors.size
      val walletID = vertex.getPropertyCurrentValue("id").get
      (walletID, outDegree,inDegree)
    })
    val totalV = degree.size
    val totalOut = degree.map(x=>x._2).sum
    val totalIn = degree.map(x=>x._3).sum
    val topUsers =degree.toArray.sortBy(x=> x._3)(sortOrdering).take(20)
    (totalV,totalOut,totalIn,topUsers)
  }

  override def setup(): Unit = {}

  override def defineMaxSteps(): Int = 1

  override def processResults(results: ArrayBuffer[Any], timeStamp: Long, viewCompleteTime: Long): Unit = {

  }

  override def processViewResults(results: ArrayBuffer[Any], timestamp: Long, viewCompleteTime: Long): Unit = {
  }

  override def processWindowResults(results: ArrayBuffer[Any], timestamp: Long, windowSize: Long, viewCompleteTime: Long): Unit = {
    val endResults = results.asInstanceOf[ArrayBuffer[(Int,Int,Int,Array[(Int,Int,Int)])]]
    var output_file = System.getenv().getOrDefault("GAB_PROJECT_OUTPUT", "/app/defout.csv").trim
    val startTime = System.currentTimeMillis()
    val totalVert = endResults.map(x=>x._1).sum
    val totalEdge = endResults.map(x=>x._3).sum

    val degree = try{totalEdge/totalVert}catch {case e:ArithmeticException => 0}
    var bestUserArray = "["
    val bestUsers = endResults.map(x=>x._4).flatten.sortBy(x=> x._3)(sortOrdering).take(20).map(x=> s"""{"id":${x._1},"indegree":${x._3},"outdegree":${x._2}}""").foreach(x=> bestUserArray+=x+",")
    bestUserArray = if(bestUserArray.length>1) bestUserArray.dropRight(1)+"]" else bestUserArray+"]"
    val text = s"""{"time":$timestamp,"windowsize":$windowSize,"vertices":$totalVert,"edges":$totalEdge,"degree":$degree,"bestusers":${bestUserArray},"viewTime":$viewCompleteTime,"concatTime":${System.currentTimeMillis()-startTime}},"""
    Utils.writeLines(output_file,text,"{\"views\":[")
    println(text)
  }

  override def processBatchWindowResults(results: ArrayBuffer[Any], timestamp: Long, windowSet: Array[Long], viewCompleteTime: Long): Unit = {
    for(i <- results.indices) {
      val window = results(i).asInstanceOf[ArrayBuffer[Any]]
      val windowSize = windowSet(i)
      processWindowResults(window, timestamp, windowSize, viewCompleteTime)
    }
  }

  override def returnResults(): Any = ???
}

