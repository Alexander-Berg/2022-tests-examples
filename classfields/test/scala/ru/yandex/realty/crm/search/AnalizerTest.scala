package ru.yandex.realty.crm.search

import org.apache.lucene.document.{Document, Field, FieldType}
import org.apache.lucene.index.{DirectoryReader, IndexOptions, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.{BooleanClause, IndexSearcher}
import org.apache.lucene.store.RAMDirectory
import org.apache.lucene.util.QueryBuilder
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * @author rmuzhikov
  */
@RunWith(classOf[JUnitRunner])
class AnalizerTest extends FlatSpec with Matchers {

  "Analizer" should "find document by prefix" in {
    val directory = new RAMDirectory
    val minNGram = 2
    val maxNGram = 10
    val analyzer = new SuggestWriteAnalyzer(minNGram, maxNGram)
    val config = new IndexWriterConfig(analyzer)
    val writer = new IndexWriter(directory, config)

    val fieldType = new FieldType
    fieldType.setOmitNorms(true)
    fieldType.setIndexOptions(IndexOptions.DOCS)
    fieldType.setStored(false)
    fieldType.setTokenized(true)
    fieldType.freeze()

    val testData = List("+79523991438", "rmuzhikov@gmail.com", "Русская недвига", "123456789", "1120000000000334")

    val doc = new Document
    doc.add(new Field("suggest", testData.mkString(" "), fieldType))

    writer.addDocument(doc)
    writer.close()

    val searcher = new IndexSearcher(DirectoryReader.open(directory))

    val queryBuilder = new QueryBuilder(new SuggestReadAnalyzer(minNGram, maxNGram))
    var query = queryBuilder.createBooleanQuery("suggest", "нед РУСС ъ", BooleanClause.Occur.MUST)
    var hits = searcher.search(query, 1)
    hits.totalHits should be(1)

    query = queryBuilder.createBooleanQuery("suggest", "1120000000000334", BooleanClause.Occur.MUST)
    hits = searcher.search(query, 1)
    hits.totalHits should be(1)
  }
}
