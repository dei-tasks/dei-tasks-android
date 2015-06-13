package com.mindmup.android.tasks

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.Matchers
import org.scalatest.prop.PropertyChecks

import rapture.json.{JsonBuffer, jsonBackends, jsonStringContext, jsonBufferStringContext}
import jsonBackends.json4s._
import scala.reflect._

@RunWith(classOf[JUnitRunner])
class MindmupJsonTreeTest extends PropSpec with PropertyChecks with Matchers with GivenWhenThen {
  type JSON = JsonBuffer
  val treeLike = MindmupJsonTree.mindmupJsonTreeLike
  def mindmups(highestId: Int = 1, level: Int = 0): Gen[(JSON, Int)] = for {
    noChildren <- Gen.const((jsonBuffer"{}", highestId))
    (children, childrenHighestId) <- if (level > 5) Gen.const(noChildren)
    else {
      val numChildren = Gen.chooseNum(0, 5)
      val someChildren = numChildren.map { nc =>
        (0 until nc).foldLeft((jsonBuffer"{}".as[JSON], highestId)) { case ((childObject, highestChildId), i) =>
          val (js, hcid) = mindmups(highestChildId + 1, level + 1).map { case (child, id) =>
            val j = jsonBuffer"""{}"""
            j.updateDynamic(i.toString)(child)
            (j, id)
          }.sample.get
          (childObject ++ js, math.max(highestChildId, hcid))
        }
      }.map { case(children, highestId) =>
        (jsonBuffer"""{"ideas": $children}""", highestId)
      }
      Gen.frequency(1 -> Gen.const(noChildren), 3 -> someChildren)
    }
  } yield (((json"""{"id": ${highestId}}""" ++ children).as[JSON], childrenHighestId))
  val children = Gen.const( json"""{"title": "foo"}""").map(_.as[JSON])
  property("finds the highest id correctly") {
    forAll(mindmups()) { case (mindmup, highestId) =>
      treeLike.findMaxId(mindmup) should equal(highestId)
    }
  }

  import rapture.data.Extractor.{mapExtractor, optionExtractor}

  def ideas(node: JSON) = node match {
    case json"""{"ideas": $ideas}""" => ideas.asInstanceOf[JSON]
    case _ => json"{}".as[JSON]
  }
  val addChildProperties: Map[String, (JSON, JSON) => Unit] = Map(
    "the parent's key-value pairs remain untouched except for the ideas" -> { case (parent, child) =>
      val previousKeyValues = parent.as[Map[String, JSON]] - "ideas"
      val withChild = treeLike.addChild(parent, child)
      (withChild.as[Map[String, JSON]] - "ideas") should equal(previousKeyValues)
    },
    "the ideas' keys are still unique" -> { case (parent, child) =>
      val withChild = treeLike.addChild(parent, child)
      val ideasMap = ideas(withChild).as[Map[String, JSON]]
      ideasMap.keys.toList.distinct.sorted should equal(ideasMap.keys.toList.sorted)
    },
    "the key-value pairs of the child are transferred to ideas under a unique key" -> { case (parent, child) =>
      val withChild = treeLike.addChild(parent, child)
      val childMap = child.as[Map[String, JSON]]
      val ideasMap = ideas(withChild).as[Map[String, Map[String, JSON]]]
      exactly(1, ideasMap.values.map(_ - "id")) should equal(childMap)
    },
    "the other ideas remain untouched" -> { case (parent, child) =>
      val ideasBefore = ideas(parent).as[Map[String, JSON]]
      val withChild = treeLike.addChild(parent, child)
      val ideasAfter = ideas(withChild).as[Map[String, JSON]]
      def findChild(child: JSON, ideas: JSON) = {
        ideas.as[Map[String, Map[String, JSON]]].find(i => (i._2 - "id") == child.as[Map[String, JSON]])
      }
      val addedChildKey = findChild(child, withChild.ideas).get._1

      ideasBefore should equal(ideasAfter - addedChildKey)
    },
    "when the parent does not have ideas yet, the ideas element will be created" -> { case (parent, child) => () },
    "the new child node has the highest ID in the document" -> { case (parent, child) => () }
  )
  addChildProperties.foreach { case (propertyName, check) =>
    property(propertyName) {
      forAll(mindmups().map(_._1), children)(check)
    }
  }
}
