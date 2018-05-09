package com.sksamuel.elastic4s.search.suggestions

import com.sksamuel.elastic4s.analyzers.{CustomAnalyzerDefinition, StandardAnalyzerDefinition}
import com.sksamuel.elastic4s.json.XContentFactory
import com.sksamuel.elastic4s.searches.suggestion.Generator
import com.sksamuel.elastic4s.{ElasticDsl, Indexable}
import com.sksamuel.elastic4s.testkit.{DiscoveryLocalNodeProvider, ElasticSugar}
import org.elasticsearch.search.suggest.phrase.DirectCandidateGeneratorBuilder
import org.scalatest.{Matchers, WordSpec}

class PhraseSuggestionsTest extends WordSpec with Matchers with ElasticSugar with DiscoveryLocalNodeProvider with ElasticDsl {

  implicit object SongIndexable extends Indexable[Song] {
    override def json(t: Song): String = s"""{"name":"${t.name}", "artist":"${t.artist}"}"""
  }

  private val Index = "phrasesuggest"
  private val indexType = Index / "music"

//  http.execute {
//    createIndex(Index).mappings(
//      mapping("music").fields(
//        textField("name")
//      )
//    ).indexSetting("number_of_shards", 1) // suggestions may be distributed among multiple shards and not be returned
//  }.await

//  http.execute(
//    bulk(
//      indexInto(indexType) doc Song("Rocket Man", "Kate Bush"),
//      indexInto(indexType) doc Song("Rubberband Girl", "Kate Bush"),
//      indexInto(indexType) doc Song("Running Up that Hill", "Kate Bush"),
//      indexInto(indexType) doc Song("The Fog", "Kate Bush"),
//      indexInto(indexType) doc Song("The Red Shoes", "Kate Bush"),
//      indexInto(indexType) doc Song("The Dreaming", "Kate Bush"),
//      indexInto(indexType) doc Song("The Big Sky", "Kate Bush")
//    ).immediateRefresh()
//  ).await

  "phrase suggestions" should {
    "support maxErrors" in {
      val directCandidateGenerators =
        Seq(
          Generator("test", "whoot")
//          new DirectCandidateGeneratorBuilder("didyoumeansearchables.trigram").suggestMode("always"),
//          new DirectCandidateGeneratorBuilder("didyoumeansearchables.reverse")
//            .suggestMode("always")
//            .preFilter("dym_phrase_reverse")
//            .postFilter("dym_phrase_reverse")
        )
      println(
        http.show(search(indexType).suggestions {
          phraseSuggestion("phrase_dym_suggestion")
            .on("didyoumeansearchables.trigram")
            .text("miss atam")
            .size(10)
            .gramSize(3)
            .realWordErrorLikelihood(.9f)
            .confidence(1.0f)
            .addCandidateGenerator(directCandidateGenerators)
        })
      )


//      val resp = http.execute {
//        search(indexType).suggestions {
//          phraseSuggestion("a").on("name").text("Rebberband Gril").maxErrors(2.0f)
//        }
//      }.await
//
//      val entry = resp.suggestion("a").entries.head
//      entry.optionsText shouldBe List("rubberband girl", "rubberband gril", "rebberband girl")
    }

    "support directCandidateGenerator with minWordLength" ignore {

      // minWordLength = 3 allows suggestions for words with < 4 chars
      val directCandidateGenerator = new DirectCandidateGeneratorBuilder("name").minWordLength(3)

      // todo add back in candidate generators
      val resp = client.execute {
        search(indexType).suggestions {
          phraseSuggestion("a").on("name").text("Thx Dreaming") //.addCandidateGenerator(directCandidateGenerator)
        }
      }.await

      val entry = resp.suggestion("a").entries.head
      entry.optionsText shouldBe List("the dreaming")
    }

    "support directCandidateGenerator with prefixLength" ignore {

      // prefixLength = 0 allows misspellings at the beginning of a word
      val directCandidateGenerator = new DirectCandidateGeneratorBuilder("name").prefixLength(0)

      // todo add back in candidate generators
      val resp = client.execute {
        search(indexType).suggestions {
          phraseSuggestion("a").on("name").text("Socket Man") //.addCandidateGenerator(directCandidateGenerator)
        }
      }.await

      val entry = resp.suggestion("a").entries.head
      entry.optionsText shouldBe List("rocket man")
    }

    "support collateQuery and collateParams" ignore {
      // Add a collate query to the PhraseSuggestionDefinition to
      // ensure that suggestions which don't yield results
      // are not part of the suggestion results.
      // You will notice that only if you increase maxErrors to at least 2.
      // For "Rebberband Gril" the nonsense suggestions "rubberband gril", "rebberband girl" will be filtered out

      val resp = client.execute {
        search(indexType).suggestions {
          phraseSuggestion("a").on("name").text("Rebberband Gril").
            maxErrors(2.0f).
            collateQuery("match_phrase", "field_name", "suggestion").
            collateParams(Map("field_name" -> "name"))
        }
      }.await

      val entry = resp.suggestion("a").entries.head
      entry.optionsText shouldBe List("rubberband girl")
    }
  }
}
