package com.sksamuel.elastic4s.http.search

import com.sksamuel.elastic4s.http.{EnumConversions, ScriptBuilderFn}
import com.sksamuel.elastic4s.http.search.aggs.AggregationBuilderFn
import com.sksamuel.elastic4s.http.search.collapse.CollapseBuilderFn
import com.sksamuel.elastic4s.http.search.queries.{QueryBuilderFn, SortContentBuilder}
import com.sksamuel.elastic4s.http.search.suggs.{PhraseSuggestionBuilderFn, TermSuggestionBuilderFn}
import com.sksamuel.elastic4s.json.{XContentBuilder, XContentFactory}
import com.sksamuel.elastic4s.searches.SearchDefinition
import com.sksamuel.elastic4s.searches.suggestion.{PhraseSuggestionDefinition, TermSuggestionDefinition}

object SearchBodyBuilderFn {

  def apply(request: SearchDefinition): XContentBuilder = {

    val builder = XContentFactory.jsonBuilder()

    request.query.map(QueryBuilderFn.apply).foreach(x => builder.rawField("query", x.string))
    request.postFilter.map(QueryBuilderFn.apply).foreach(x => builder.rawField("post_filter", x.string))
    request.collapse.map(CollapseBuilderFn.apply).foreach(x => builder.rawField("collapse", x.string))

    request.from.foreach(builder.field("from", _))
    request.size.foreach(builder.field("size", _))

    if (request.explain.contains(true)) {
      builder.field("explain", true)
    }

    request.minScore.foreach(builder.field("min_score", _))
    if (request.searchAfter.nonEmpty) {
      builder.autoarray("search_after", request.searchAfter)
    }

    if (request.scriptFields.nonEmpty) {
      builder.startObject("script_fields")
      request.scriptFields.foreach { field =>
        builder.startObject(field.field)
        builder.rawField("script", ScriptBuilderFn(field.script))
        builder.endObject()
      }
      builder.endObject()
    }

    if (request.rescorers.nonEmpty) {
      builder.startArray("rescore")
      request.rescorers.foreach { rescore =>
        builder.startObject()
        rescore.windowSize.foreach(builder.field("window_size", _))
        builder.startObject("query")
        builder.rawField("rescore_query", QueryBuilderFn(rescore.query))
        rescore.rescoreQueryWeight.foreach(builder.field("rescore_query_weight", _))
        rescore.originalQueryWeight.foreach(builder.field("query_weight", _))
        rescore.scoreMode.map(EnumConversions.queryRescoreMode).foreach(builder.field("score_mode", _))
        builder.endObject().endObject()
      }
      builder.endArray()
    }

    if (request.sorts.nonEmpty) {
			builder.startArray("sort")
			// Workaround for bug where separator is not added with rawValues
      val arrayBody = request.sorts.map(s => SortContentBuilder(s).string).mkString(",")
      builder.rawValue(arrayBody)
			builder.endArray()
    }

    request.trackScores.map(builder.field("track_scores", _))

    request.highlight.foreach { highlight =>
      builder.rawField("highlight", HighlightBuilderFn(highlight))
    }

    if (request.suggs.nonEmpty) {
      builder.startObject("suggest")
      request.globalSuggestionText.foreach(builder.field("text", _))
      request.suggs.foreach {
        case term: TermSuggestionDefinition => builder.rawField(term.name, TermSuggestionBuilderFn(term))
        case phrase: PhraseSuggestionDefinition => {
          builder.field("text", phrase.text.getOrElse(""))
          builder.rawField(phrase.name, PhraseSuggestionBuilderFn(phrase))
        }
      }
      builder.endObject()
    }

    if (request.storedFields.nonEmpty) {
      builder.array("stored_fields", request.storedFields.toArray)
    }

    if (request.indexBoosts.nonEmpty) {
      builder.startArray("indices_boost")
      request.indexBoosts.foreach { case (name, double) =>
        builder.startObject()
        builder.field(name, double)
        builder.endObject()
      }
      builder.endArray()
    }

    // source filtering
    request.fetchContext foreach { context =>
      if (context.fetchSource) {
        if (context.includes.nonEmpty || context.excludes.nonEmpty) {
          builder.startObject("_source")
          builder.array("includes", context.includes)
          builder.array("excludes", context.excludes)
          builder.endObject()
        }
      } else {
        builder.field("_source", false)
      }
    }

    if (request.docValues.nonEmpty)
      builder.array("docvalue_fields", request.docValues.toArray)

    // aggregations
    if (request.aggs.nonEmpty) {
      builder.startObject("aggs")
      request.aggs.foreach { agg =>
        builder.rawField(agg.name, AggregationBuilderFn(agg))
      }
      builder.endObject()
    }

    builder.endObject()
  }
}
