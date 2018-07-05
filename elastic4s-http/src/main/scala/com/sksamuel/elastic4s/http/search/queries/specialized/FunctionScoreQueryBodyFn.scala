package com.sksamuel.elastic4s.http.search.queries.specialized


import com.sksamuel.elastic4s.http.EnumConversions
import com.sksamuel.elastic4s.http.search.queries.QueryBuilderFn
import com.sksamuel.elastic4s.json.{XContentBuilder, XContentFactory}
import com.sksamuel.elastic4s.searches.queries.funcscorer.{FieldValueFactorDefinition, FunctionScoreQueryDefinition}

object FunctionScoreQueryBodyFn {

  def apply(q: FunctionScoreQueryDefinition): XContentBuilder = {
    val builder = XContentFactory.jsonBuilder()

    def buildFunctions(defs: Seq[FieldValueFactorDefinition]) = {
      builder.startArray("functions")
      defs.foreach(definition => {
        builder.startObject()
        builder.rawField("field_value_factor", FieldValueFactorBodyFn(definition))
        builder.endObject()
      })
      builder.endArray()
    }

    builder.startObject("function_score")

    q.query.map(qDef => builder.rawField("query", QueryBuilderFn(qDef)))
    q.scriptScore.map(sDef => builder.rawField("script_score", ScriptScoreQueryBodyFn(sDef)))
    q.fieldValueFactor.map(defs => buildFunctions(defs))
    q.minScore.map(builder.field("min_score", _))
    q.boost.map(builder.field("boost", _))
    q.maxBoost.map(builder.field("max_boost", _))
    q.scoreMode.map(sm => builder.field("score_mode", EnumConversions.scoreMode(sm)))
    q.boostMode.map(bm => builder.field("boost_mode", EnumConversions.boostMode(bm)))

    builder.endObject()
    builder
  }

}
