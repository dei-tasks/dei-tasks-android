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
import macroid.FullDsl._
import macroid.{IdGeneration, _}
import macroid.viewable._
import rx._
import rx.ops._


class TaskListFragment[T, V <: View](currentTasks: Rx[Seq[T]], queryInterpreter: CharSequence => (T => Boolean))(implicit val listable: Listable[T, V], val treeLike: TreeLike[T])
  extends Fragment with Contexts[Fragment] with RxSupport with IdGeneration with TaskUi[T] {
  import FilterableListableListAdapter._
  val taskFilterString = Var[String]("")

  val itemSelections: Var[Option[T]] = Var[Option[T]](None)

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
    FuncOn.itemClick[ListView] { (_: AdapterView[_], _: View, index: Int, _: Long) =>
      val selectedTask = getUi(taskListView).getItemAtPosition(index).asInstanceOf[T]
      itemSelections() = Some(selectedTask)
      Ui(true)
    } <~ Tweak[ListView] { listView =>
    listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL)
    listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

      override def onItemCheckedStateChanged(mode: ActionMode,
                                             position: Int,
                                             id: Long,
                                             checked: Boolean) {
      }

      override def onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = {
        val checkedItems = listView.getCheckedItemPositions
        println(s"Checked items $checkedItems, size ${checkedItems.size}")
        val checkedIndices = (0 until listView.getCount).filter(checkedItems.get(_))
        val items = checkedIndices.map(listView.getItemAtPosition(_).asInstanceOf[T])
        println(s"Marking items $checkedIndices as done: $items")

        actionMap.get(item.getItemId).map { action: (T => Unit) =>
          items.foreach(action)
          mode.finish()
          true
        }.getOrElse(false)
      }

      override def onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = {
        val inflater = mode.getMenuInflater
        inflater.inflate(R.menu.task_cab_menu, menu)
        inflater.inflate(R.menu.progress_menu, menu)

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
      def onQueryTextChange(text: String): Boolean = { taskFilterString()=text; true}
      def onQueryTextSubmit(text: String): Boolean = { taskFilterString()=text; true}
    })

    val searchableInfo = searchManager.getSearchableInfo(getActivity.getComponentName)
    searchView.setSearchableInfo(searchableInfo)
    super.onCreateOptionsMenu(menu, inflater)
  }
}

object TaskListFragment {
  def newInstance[T, V <: View](currentTasks: Rx[Seq[T]], queryInterpreter: CharSequence => (T => Boolean), listable: Listable[T, V], treeLike: TreeLike[T]) = {
    implicit val tl = treeLike
    implicit val la = listable
    new TaskListFragment[T, V](currentTasks, queryInterpreter)
  }}
