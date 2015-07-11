package com.github.dei_tasks

import android.view.{ View, ViewGroup }
import android.widget.Filter
import android.widget.Filter.FilterResults
import macroid.viewable.{ Listable, ListableListAdapter }
import macroid.{ ActivityContext, AppContext, Ui }
import macroid.contrib.ListTweaks
import java.util.ArrayList

/** A `ListAdapter` based on the `Listable` typeclass */
class FilterableListableListAdapter[A, W <: View](data: Seq[A], queryInterpreter: CharSequence => A => Boolean)(implicit ctx: ActivityContext, appCtx: AppContext, listable: Listable[A, W])
  extends ListableListAdapter[A, W](data) {
  override lazy val getFilter = {
    new CustomFilter
  }
  import scala.collection.JavaConverters._

  class CustomFilter extends Filter {

    protected override def performFiltering(prefix: CharSequence): FilterResults = {
      val filterFunction = queryInterpreter(prefix)
      val results = new FilterResults()
      val newValues = if (prefix == null || prefix.length == 0) {
        data
      } else {
        data.filter(filterFunction)
      }
      results.values = newValues.asJava
      results.count = newValues.size
      results
    }

    protected override def publishResults(constraint: CharSequence, results: FilterResults) {
      FilterableListableListAdapter.this.clear()
      FilterableListableListAdapter.this.addAll(results.values.asInstanceOf[java.util.List[A]])
      if (results.count > 0) {
        notifyDataSetChanged()
      } else {
        notifyDataSetInvalidated()
      }
    }
  }
}

object FilterableListableListAdapter {
  implicit class FilterableListable[A, W <: View](listable: Listable[A, W]) {
    /** An adapter to use with a `ListView` */
    def filterableListAdapter(data: Seq[A], queryInterpreter: CharSequence => A => Boolean)(implicit ctx: ActivityContext, appCtx: AppContext): FilterableListableListAdapter[A, W] =
      new FilterableListableListAdapter[A, W](data, queryInterpreter)(ctx, appCtx, listable)

    /** A tweak to set the adapter of a `ListView` */
    def filterableListAdapterTweak(data: Seq[A], queryInterpreter: CharSequence => A => Boolean)(implicit ctx: ActivityContext, appCtx: AppContext) =
      ListTweaks.adapter(filterableListAdapter(data, queryInterpreter))
  }
}
