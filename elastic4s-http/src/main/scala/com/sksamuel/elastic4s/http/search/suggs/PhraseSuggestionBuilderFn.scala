package com.sksamuel.elastic4s.http.search.suggs

import com.sksamuel.elastic4s.http.SourceAsContentBuilder
import com.sksamuel.elastic4s.json.{XContentBuilder, XContentFactory}
import com.sksamuel.elastic4s.searches.suggestion.{Generator, PhraseSuggestionDefinition}

object PhraseSuggestionBuilderFn {
  def apply(phrase: PhraseSuggestionDefinition): XContentBuilder = {
    val builder = XContentFactory.obj()

    def buildGenerator(gen: Generator) = {
      builder.startObject()
      builder.field("field", gen.field)
      builder.field("suggest_mode", gen.suggestMode)
      if (gen.preFilter.getOrElse("").nonEmpty) builder.field("pre_filter", gen.preFilter.get)
      if (gen.postFilter.getOrElse("").nonEmpty) builder.field("post_filter", gen.postFilter.get)
      builder.endObject()
    }

    builder.startObject("phrase")

//    phrase.text.foreach(builder.field("text", _))
    builder.field("field", phrase.fieldname)
    phrase.analyzer.foreach(builder.field("analyzer", _))

    phrase.confidence.foreach(builder.field("confidence", _))
    phrase.forceUnigrams.foreach(builder.field("force_unigrams", _))
    phrase.gramSize.foreach(builder.field("gram_size", _))
    phrase.maxErrors.foreach(builder.field("max_errors", _))
    phrase.realWordErrorLikelihood.foreach(builder.field("real_word_error_likelihood", _))
    phrase.separator.foreach(builder.field("separator", _))
    phrase.tokenLimit.foreach(builder.field("token_limit", _))
    phrase.size.foreach(builder.field("size", _))
    phrase.shardSize.foreach(builder.field("shard_size", _))

    phrase.collateQuery match {
      case _ => // do nothing
      case Some(query) => {
        //COLLATE
        builder.startObject("collate")

        builder.startObject("query")
        phrase.collateQuery.foreach(t => builder.rawField("inline", t.script))
        builder.endObject()

        phrase.collatePrune.foreach(builder.field("prune", _))
        builder.rawField("params", SourceAsContentBuilder(phrase.collateParams))

        builder.endObject()
        //END COLLATE
      }
    }

    phrase.preTag match {
      case _ => // do nothing
      case Some(str) => {
        builder.startObject("highlight")
        phrase.preTag.foreach(builder.field("pre_tag", _))
        phrase.postTag.foreach(builder.field("post_tag", _))
        builder.endObject()
      }
    }

    // CANDIDATE GENERATOR
    builder.startArray("direct_generator")
    phrase.candidateGenerator.map(x => x.generators.map(buildGenerator))
    builder.endArray()

    // END CANDIDATE GENERATOR
    builder
  }
}
