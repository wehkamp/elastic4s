package com.sksamuel.elastic4s.http.search.queries.specialized

import com.sksamuel.elastic4s.json.{XContentBuilder, XContentFactory}
import com.sksamuel.elastic4s.searches.queries.funcscorer.FieldValueFactorDefinition

object FieldValueFactorBodyFn {

  def apply(q: FieldValueFactorDefinition): XContentBuilder = {
    val builder = XContentFactory.jsonBuilder()
    builder.field("field", q.fieldName)
    q.factor.map(x => builder.field("factor", x))
    q.modifier.map(x => builder.field("modifier", x.toString))
    q.missing.map(x => builder.field("missing", x))
    builder.endObject()
    builder
  }
}
