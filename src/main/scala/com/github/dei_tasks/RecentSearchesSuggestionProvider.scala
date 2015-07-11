package com.github.dei_tasks

import android.content.SearchRecentSuggestionsProvider
object RecentSearchesSuggestionProvider {
  val AUTHORITY = "com.github.dei_tasks.RecentSearchesSuggestionProvider"
  val MODE = SearchRecentSuggestionsProvider.DATABASE_MODE_QUERIES
}

class RecentSearchesSuggestionProvider extends SearchRecentSuggestionsProvider {
  import RecentSearchesSuggestionProvider._
  setupSuggestions(AUTHORITY, MODE)
}
