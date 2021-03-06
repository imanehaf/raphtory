package com.raphtory.examples.blockchain.litecoin.actors

import akka.cluster.pubsub.DistributedPubSubMediator
import com.raphtory.core.components.Router.TraditionalRouter.Helpers.RouterSlave
import com.raphtory.core.components.Router.TraditionalRouter.RaphtoryRouter
import com.raphtory.core.model.communication.{EdgeAdd, EdgeAddWithProperties, RaphWriteClass, VertexAddWithProperties}
import com.raphtory.core.utils.Utils.getManager
import com.raphtory.examples.blockchain.{BitcoinTransaction, LitecoinTransaction}
import spray.json.JsArray

import scala.util.hashing.MurmurHash3

class LitecoinRouter(val routerId:Int, val initialManagerCount:Int) extends RouterSlave{


  def parseRecord(record: Any): Unit = {
    val value = record.asInstanceOf[LitecoinTransaction]
    val transaction = value.transaction
    val time = value.time
    val blockID = value.blockID
    val block = value.block
    val timeAsString = time.toString
    val timeAsLong = (timeAsString.toLong)*1000

    val txid = transaction.asJsObject.fields("txid").toString()
    val vins = transaction.asJsObject.fields("vin")
    val vouts = transaction.asJsObject.fields("vout")
    val locktime = transaction.asJsObject.fields("locktime")
    val version = transaction.asJsObject.fields("version")
    var total:Double = 0

    for (vout <- vouts.asInstanceOf[JsArray].elements) {
      val voutOBJ = vout.asJsObject()
      var value = voutOBJ.fields("value").toString
      total+= value.toDouble
      val n = voutOBJ.fields("n").toString
      val scriptpubkey = voutOBJ.fields("scriptPubKey").asJsObject()

      var address = "nulldata"
      val outputType = scriptpubkey.fields("type").toString

      if(scriptpubkey.fields.contains("addresses"))
        address = scriptpubkey.fields("addresses").asInstanceOf[JsArray].elements(0).toString
      else value = "0" //TODO deal with people burning money

      //creates vertex for the receiving wallet
      toPartitionManager(VertexAddWithProperties(msgTime = timeAsLong, srcID = MurmurHash3.stringHash(address), properties = Map[String,String](("type","address"),("address",address),("outputType",outputType))))
      //creates edge between the transaction and the wallet
      toPartitionManager(EdgeAddWithProperties( msgTime = timeAsLong, srcID = MurmurHash3.stringHash(txid), dstID = MurmurHash3.stringHash(address), properties = Map[String,String](("n",n),("value",value))))

    }
    toPartitionManager(VertexAddWithProperties(msgTime = timeAsLong, srcID = MurmurHash3.stringHash(txid), properties = Map[String,String](
      ("type","transaction"),
      ("time",timeAsString),
      ("id",txid),
      ("total",total.toString),
      ("lockTime",locktime.toString),
      ("version",version.toString),
      ("blockhash",blockID.toString),
      ("block",block.toString))))

    if(vins.toString().contains("coinbase")){
      //creates the coingen node
      toPartitionManager(VertexAddWithProperties(msgTime = timeAsLong, srcID = MurmurHash3.stringHash("coingen"), properties = Map[String,String](("type","coingen"))))

      //creates edge between coingen and the transaction
      toPartitionManager(EdgeAdd(msgTime = timeAsLong, srcID = MurmurHash3.stringHash("coingen"), dstID =  MurmurHash3.stringHash(txid)))
    }
    else{
      for(vin <- vins.asInstanceOf[JsArray].elements){
        val vinOBJ = vin.asJsObject()
        val prevVout = vinOBJ.fields("vout").toString
        val prevtxid = vinOBJ.fields("txid").toString
        val sequence = vinOBJ.fields("sequence").toString
        //no need to create node for prevtxid as should already exist
        //creates edge between the prev transaction and current transaction
        toPartitionManager(EdgeAddWithProperties(msgTime = timeAsLong, srcID =  MurmurHash3.stringHash(prevtxid), dstID =  MurmurHash3.stringHash(txid), properties = Map[String,String](("vout",prevVout),("sequence",sequence))))
      }
    }
  }


}