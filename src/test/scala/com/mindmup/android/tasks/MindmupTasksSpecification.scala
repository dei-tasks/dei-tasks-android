package com.mindmup.android.tasks

import com.google.android.gms.drive.{Drive, DriveId}
import com.mindmup.android.tasks.TreeLike._
import com.mindmup.android.tasks.MindmupJsonTree._
import org.robolectric.{ShadowsAdapter, Shadows, RuntimeEnvironment, Robolectric}
import org.robolectric.Shadows._
import org.robolectric.annotation.Config
import org.robolectric.shadows.{ShadowPreferenceManager, ShadowLog}
import org.robolectric.util.{ActivityController, ReflectionHelpers}
import org.scalacheck.{Shrink, Gen, Prop}
import Prop._
import org.scalacheck.commands.Commands
import org.scalatest.prop.Checkers
import org.scalatest.{PropSpec, RobolectricSuite, Matchers, FeatureSpec}

import scala.collection.JavaConverters._
import scala.io.Source
import util.{Try, Success, Failure}

@Config(sdk = Array(21), manifest = "src/main/AndroidManifest.xml")
class CommandsMindmupTasks extends FeatureSpec with Checkers with RobolectricSuite {
  override val skipInstrumentationPkg = Seq("org.scalacheck")
  //ShadowLog.stream = System.out

  feature("Mindmup Tasks") {
    scenario("works") {
      check((new MindmupTasksSpecification).property(), minSuccessful(3))

    }
  }

  class MindmupTasksSpecification extends Commands {
    implicit def dontShrinkAnything[T]: Shrink[T] = Shrink(s => Stream.empty)
    val mindmupJsonString = Source.fromURL(getClass.getResource("/mindmup_tasks.mup")).getLines.mkString("\n")
    val mindmupJsonStrings = Gen.const(mindmupJsonString)
    val driveIdStrings = Gen.const("DriveId:CAESHDBCMmh0cDdjdkdMdVZlRVJqWmpRd1QyaDRYMk0YhgIgyMuOlLVTKAA=")
    val driveIds = driveIdStrings.map(DriveId.decodeFromString)
    val existingFiles = for {
      drIds <- Gen.containerOf[List, DriveId](driveIds)
      contents <- Gen.containerOfN[List, String](drIds.size, mindmupJsonStrings)
    } yield((drIds.zip(contents).toMap))
    val possibleQueries = existingFiles.flatMap { fileMap =>
      Gen.oneOf(fileMap.values.flatMap { str =>
        val json = MindmupModel.parseMindmup(str)
        // all nodes' titles' words
        allDescendants(json).flatMap(_.title.split(" "))
      }.toSeq)
    }
    // This is our system under test. All commands run against this instance.
    type Sut = MainActivity

    // This is our state type that encodes the abstract state. The abstract state
    // should model all the features we need from the real state, the system
    // under test. We should leave out all details that aren't needed for
    // specifying our pre- and postconditions. The state type must be called
    // State and be immutable.
    case class State(query: String, files: Map[DriveId, String] = Map.empty) {
      val suffix = "...)"

      def ellipse(original: String, maxLength: Int) =
        if(original.length <= maxLength)
          original
        else
          original.substring(0, maxLength - suffix.length) + suffix

      override def toString: String = s"""State(query = "$query", files = ${ellipse(files.toString, 20)},\nshownTasks = ${shownTasks})"""
      lazy val parsedFiles = files.mapValues(MindmupModel.parseMindmup)
      lazy val shownTasks = parsedFiles.flatMap { case(_, mindmupJson) =>
        val tasksInFile = allDescendantsWithPaths(mindmupJson)
        tasksInFile.filter(_.exists(_.title.toLowerCase.contains(query.toLowerCase))).map(_.last.title)
      }.toSeq
    }

    case class SetQuery(query: String) extends SuccessCommand {
      type Result = String
      def run(s: Sut) = { s.setQuery(query); s.getQuery.toString }
      def nextState(s: State) = s.copy(query = query)
      def preCondition(s: State) = true
      def postCondition(s: State, result: String) = result == query
    }

    case object ClearQuery extends SuccessCommand {
      type Result = String
      def run(s: Sut) = { s.setQuery(""); s.getQuery.toString }
      def nextState(s: State) = s.copy(query = "")
      def preCondition(s: State) = true
      def postCondition(s: State, result: String) = result.isEmpty()
    }

    case object GetShownTasks extends SuccessCommand {
      type Result = Seq[String]
      def run(s: Sut) = {
        shadowOf(s.taskListFragment.taskListView).populateItems()
        s.displayedTasks.map(_.split(" / ").last)
      }
      def nextState(s: State) = s
      override def preCondition(state: State): Boolean = true
      def postCondition(s: State, result: Result) = {
        result.sorted == s.shownTasks.sorted
      }
    }

    case class AddSubTask(parentPath: List[String], title: String) extends UnitCommand {
      def run(s: Sut) = {
        val view = s.findTaskByPath(parentPath)
        view.get.performClick()
        shadowOf(s).clickMenuItem(R.id.add_child)
        ActivityController.of(null, s)
        s.taskDetailsFragment.titleEditor.setText(title)
        s.getSupportFragmentManager.popBackStack()
      }
      def nextState(s: State) = s
      override def preCondition(state: State): Boolean = state.shownTasks.contains(parentPath.last)
      def postCondition(s: State, success: Boolean) = success
    }
    // This is our command generator. Given an abstract state, the generator
    // should return a command that is allowed to run in that state. Note that
    // it is still neccessary to define preconditions on the commands if there
    // are any. The generator is just giving a hint of which commands that are
    // suitable for a given state, the preconditions will still be checked before
    // a command runs. Sometimes you maybe want to adjust the distribution of
    // your command generator according to the state, or do other calculations
    // based on the state.
    def genCommand(s: State): Gen[Command] = Gen.oneOf(possibleQueries.map(SetQuery(_)), Gen.const(ClearQuery), Gen.const(GetShownTasks))
    override def canCreateNewSut(newState: State, initSuts: Traversable[State], runningSuts: Traversable[Sut]): Boolean = initSuts.isEmpty && runningSuts.isEmpty
    override def destroySut(sut: Sut): Unit = ()
    override def initialPreCondition(state: State): Boolean = true
    override def genInitialState: Gen[State] = existingFiles.map(f => State("", f))
    override def newSut(state: State): Sut = {
      val shadowDriveApi = new ShadowDriveApi(state.files)
      ReflectionHelpers.setStaticField(classOf[Drive], "DriveApi", shadowDriveApi)
      val sharedPreferences = ShadowPreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application.getApplicationContext())
      sharedPreferences.edit().putStringSet("selected_mindmups", state.files.keySet.map(_.encodeToString()).asJava).commit()
      Robolectric.setupActivity(classOf[MainActivity])
    }
  }

}
