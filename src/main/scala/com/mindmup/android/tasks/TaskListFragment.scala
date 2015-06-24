package com.mindmup.android.tasks

import android.app.SearchManager
import android.os.Bundle
import android.content.Context
import android.support.v4.app.Fragment
import android.view._
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget._
import com.malinskiy.materialicons.IconDrawable
import com.malinskiy.materialicons.Iconify.IconValue
import com.mindmup.android.tasks.Implicits._
import macroid.FullDsl._
import macroid.{IdGeneration, _}
import macroid.viewable._
import rx._
import rx.ops._
import scala.concurrent._
import ExecutionContext.Implicits.global

class TaskListFragment[T, V <: View](idsWithTaskTrees: Rx[Map[String, T]], queryInterpreter: CharSequence => (List[T] => Boolean))(implicit val listable: Listable[List[T], V], val treeLike: TreeLike[T])
  extends Fragment with Contexts[Fragment] with RxSupport with IdGeneration with TaskUi[T] {
  import FilterableListableListAdapter._
  val taskFilterString = Var[String]("")

  val itemSelections: Var[Option[List[T]]] = Var[Option[List[T]]](None)
  val currentTasks = Rx {
    val parsed = idsWithTaskTrees()
    println(s"Successfully parsed ${parsed.size} Mindmups")
    import TreeLike._
    val tasks = parsed.flatMap { case (_, json) =>
      allDescendantsWithPaths(json)
    }.toList.sortBy(_.map(_.title).mkString("/"))
    println(s"Successfully taskified ${tasks.size} nodes")
    tasks
  }
  lazy val taskListView: Ui[ListView] = w[ListView] <~
    currentTasks.map(t => listable.filterableListAdapterTweak(t, queryInterpreter)) <~
    taskFilterString.map { fs =>
      Tweak[ListView] { lv =>
        val adapter = lv.getAdapter.asInstanceOf[ListableListAdapter[_, _]]
        if (adapter != null) {
          adapter.getFilter.filter(fs)
        }
      }
    } <~
    FuncOn.itemClick[ListView] { (view: AdapterView[_], _: View, index: Int, _: Long) =>
      view.asInstanceOf[ListView].setItemChecked(index, true)
      Ui(true)
    } <~ Tweak[ListView] { listView =>
    listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL)
    listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
      def checkedItems = listView.getCheckedItemPositions
      def checkedIndices = (0 until listView.getCount).filter(checkedItems.get(_))
      def selectedItems = checkedIndices.map(listView.getItemAtPosition(_).asInstanceOf[List[T]].last)

      override def onItemCheckedStateChanged(mode: ActionMode,
                                             position: Int,
                                             id: Long,
                                             checked: Boolean): Unit = {
        mode.setTitle(s"${checkedItems.size} tasks selected")
      }

      override def onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = {
        val result = actionMap.get(item.getItemId).map { action: (T => Unit) =>
          selectedItems.foreach(action)
          mode.finish()
          true
        }.getOrElse({
          item.getItemId match {
            case R.id.add_child =>
              selectedItems.foreach { item =>
                val newNode = treeLike.newNode
                val withChild = treeLike.addChild(item, newNode)
                val child = treeLike.findChildByTitle(withChild, treeLike.title(newNode)).get
                itemSelections() = Some(List(child))
                mode.finish()
                Future { currentTasks.recalc() }
              }
              true
            case R.id.edit =>
              itemSelections() = Some(selectedItems.takeRight(1).toList)
              mode.finish()
              true
            case _ => false
          }
        })
        result
      }

      override def onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = {
        val inflater = mode.getMenuInflater
        inflater.inflate(R.menu.task_cab_menu, menu)
        inflater.inflate(R.menu.progress_menu, menu)
        updateIcons(menu)
        true
      }

      override def onDestroyActionMode(mode: ActionMode) {
      }

      override def onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
    })
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    setHasOptionsMenu(true)
    getUi {
      taskListView
    }
  }
  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater): Unit = {
    inflater.inflate(R.menu.task_list_menu, menu)
    // Associate searchable configuration with the SearchView
    val searchManager = getActivity.getSystemService(Context.SEARCH_SERVICE).asInstanceOf[SearchManager]
    val menuItem = menu.findItem(R.id.search)
    val searchView = menuItem.getActionView().asInstanceOf[android.support.v7.widget.SearchView]
    searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
      def onQueryTextChange(text: String): Boolean = { taskFilterString()=text; false}
      def onQueryTextSubmit(text: String): Boolean = { taskFilterString()=text; false}
    })

    val searchableInfo = searchManager.getSearchableInfo(getActivity.getComponentName)
    searchView.setSearchableInfo(searchableInfo)
    searchView.setIconifiedByDefault(false)
    searchView.setQueryRefinementEnabled(true)
    super.onCreateOptionsMenu(menu, inflater)
  }
}

object TaskListFragment {
  def newInstance[T, V <: View](idsWithTaskTrees: Rx[Map[String, T]], queryInterpreter: CharSequence => (List[T] => Boolean), listable: Listable[List[T], V], treeLike: TreeLike[T]) = {
    implicit val tl = treeLike
    implicit val la = listable
    new TaskListFragment[T, V](idsWithTaskTrees, queryInterpreter)
  }}
