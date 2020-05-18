package org.example.ecommercerecommendation

import org.apache.predictionio.controller.PPreparator

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class Preparator
  extends PPreparator[TrainingData, PreparedData] {

  override
  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(
      users = trainingData.users,
      items = trainingData.items,
      viewEvents = trainingData.viewEvents,
      buyEvents = trainingData.buyEvents,
      basketEvents = trainingData.basketEvents,
      likeEvents = trainingData.likeEvents,
      shareEvents = trainingData.shareEvents)
  }
}

class PreparedData(
  val users: RDD[(String, User)],
  val items: RDD[(String, Item)],
  val viewEvents: RDD[ViewEvent],
  val buyEvents: RDD[BuyEvent],
  val baksetEvents: RDD[BasketEvent],
  val likeEvents: RDD[LikeEvent],
  val shareEvents: RDD[ShareEvent]
) extends Serializable
