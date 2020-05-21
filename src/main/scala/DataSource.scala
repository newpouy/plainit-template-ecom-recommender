package com.plainit.sniipong

import org.apache.predictionio.controller.PDataSource
import org.apache.predictionio.controller.EmptyEvaluationInfo
import org.apache.predictionio.controller.EmptyActualResult
import org.apache.predictionio.controller.Params
import org.apache.predictionio.data.storage.Event
import org.apache.predictionio.data.store.PEventStore

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import grizzled.slf4j.Logger

case class DataSourceParams(appName: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {

    // create a RDD of (entityID, User)
    val usersRDD: RDD[(String, User)] = PEventStore.aggregateProperties(
      appName = dsp.appName,
      entityType = "user"
    )(sc).map { case (entityId, properties) =>
      val user = try {
        User()
      } catch {
        case e: Exception => {
          logger.error(s"Failed to get properties ${properties} of" +
            s" user ${entityId}. Exception: ${e}.")
          throw e
        }
      }
      (entityId, user)
    }.cache()

    // create a RDD of (entityID, Item)
    val itemsRDD: RDD[(String, Item)] = PEventStore.aggregateProperties(
      appName = dsp.appName,
      entityType = "item"
    )(sc).map { case (entityId, properties) =>
      val item = try {
        // Assume categories is optional property of item.
        Item(categories = properties.getOpt[List[String]]("categories"))
      } catch {
        case e: Exception => {
          logger.error(s"Failed to get properties ${properties} of" +
            s" item ${entityId}. Exception: ${e}.")
          throw e
        }
      }
      (entityId, item)
    }.cache()

    val eventsRDD: RDD[Event] = PEventStore.find(
      appName = dsp.appName,
      entityType = Some("user"),
      eventNames = Some(List("view", "buy", "basket", "like", "share")),
      // targetEntityType is optional field of an event.
      targetEntityType = Some(Some("item")))(sc)
      .cache()

    val viewEventsRDD: RDD[ViewEvent] = eventsRDD
      .filter { event => event.event == "view" }
      .map { event =>
        try {
          ViewEvent(
            user = event.entityId,
            item = event.targetEntityId.get,
            t = event.eventTime.getMillis
          )
        } catch {
          case e: Exception =>
            logger.error(s"Cannot convert ${event} to ViewEvent." +
              s" Exception: ${e}.")
            throw e
        }
      }

    val buyEventsRDD: RDD[BuyEvent] = eventsRDD
      .filter { event => event.event == "buy" }
      .map { event =>
        try {
          BuyEvent(
            user = event.entityId,
            item = event.targetEntityId.get,
            t = event.eventTime.getMillis
          )
        } catch {
          case e: Exception =>
            logger.error(s"Cannot convert ${event} to BuyEvent." +
              s" Exception: ${e}.")
            throw e
        }
      }

    val basketEventsRDD: RDD[BasketEvent] = eventsRDD
      .filter { event => event.event == "basket" }
      .map { event =>
        try {
          BasketEvent(
            user = event.entityId,
            item = event.targetEntityId.get,
            t = event.eventTime.getMillis
          )
        } catch {
          case e: Exception =>
            logger.error(s"Cannot convert ${event} to BasketEvent." +
              s" Exception: ${e}.")
            throw e
        }
      }

    val likeEventsRDD: RDD[LikeEvent] = eventsRDD
      .filter { event => event.event == "like" }
      .map { event =>
        try {
          LikeEvent(
            user = event.entityId,
            item = event.targetEntityId.get,
            t = event.eventTime.getMillis
          )
        } catch {
          case e: Exception =>
            logger.error(s"Cannot convert ${event} to LikeEvent." +
              s" Exception: ${e}.")
            throw e
        }
      }

    val shareEventsRDD: RDD[ShareEvent] = eventsRDD
      .filter { event => event.event == "share" }
      .map { event =>
        try {
          ShareEvent(
            user = event.entityId,
            item = event.targetEntityId.get,
            t = event.eventTime.getMillis
          )
        } catch {
          case e: Exception =>
            logger.error(s"Cannot convert ${event} to ShareEvent." +
              s" Exception: ${e}.")
            throw e
        }
      }           

    new TrainingData(
      users = usersRDD,
      items = itemsRDD,
      viewEvents = viewEventsRDD,
      buyEvents = buyEventsRDD,
      basketEvents = basketEventsRDD,
      likeEvents = likeEventsRDD,
      shareEvents = shareEventsRDD
    )
  }
}

case class User()

case class Item(categories: Option[List[String]])

case class ViewEvent(user: String, item: String, t: Long)

case class BuyEvent(user: String, item: String, t: Long)

case class BasketEvent(user: String, item: String, t: Long)

case class LikeEvent(user: String, item: String, t: Long)

case class ShareEvent(user: String, item: String, t: Long)

class TrainingData(
  val users: RDD[(String, User)],
  val items: RDD[(String, Item)],
  val viewEvents: RDD[ViewEvent],
  val buyEvents: RDD[BuyEvent],
  val basketEvents: RDD[BasketEvent],
  val likeEvents: RDD[LikeEvent],
  val shareEvents: RDD[ShareEvent]
) extends Serializable {
  override def toString = {
    s"users: [${users.count()} (${users.take(2).toList}...)]" +
    s"items: [${items.count()} (${items.take(2).toList}...)]" +
    s"viewEvents: [${viewEvents.count()}] (${viewEvents.take(2).toList}...)" +
    s"buyEvents: [${buyEvents.count()}] (${buyEvents.take(2).toList}...)" +
    s"basketEvents: [${basketEvents.count()}] (${basketEvents.take(2).toList}...)" +
    s"likeEvents: [${likeEvents.count()}] (${likeEvents.take(2).toList}...)" +
    s"shareEvents: [${shareEvents.count()}] (${shareEvents.take(2).toList}...)"
  }
}
