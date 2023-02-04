package ru.yandex.realty.searcher.nrt

import java.nio.file.{Files, Path}

import org.apache.commons.io.FileUtils
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index._
import org.apache.lucene.store.{NIOFSDirectory, SingleInstanceLockFactory}
import org.apache.lucene.util.InfoStream
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.application.OperationalSupportImpl
import ru.yandex.realty.lucene.OfferDocumentFields.ID
import ru.yandex.realty.lucene.QueryUtil._
import ru.yandex.realty.lucene.readable.ReadableTermsQuery
import ru.yandex.realty.util.TestPropertiesSetup
import ru.yandex.realty.util.lucene.{DocumentReaderUtils, DocumentWriterUtils}

import scala.collection.JavaConverters._

/**
  * author: rmuzhikov
  */
@RunWith(classOf[JUnitRunner])
class NRTIndexTest extends FlatSpec with TestPropertiesSetup {
  val numberOfDocs = 100L
  val sizeOfBatch = 10

  behavior.of("NRTIndex")

  it should "sequentially upsert documents" in {
    val tmpDir = Files.createTempDirectory("nrtIndex")
    val index = createNRTIndex(tmpDir)
    for (id <- 1L to numberOfDocs) {
      val doc = generateDocument(id)
      index.upsert(newTerm(ID, id), doc)
      index.softCommit()
      val query = newTermQuery(ID, id)
      index.doWithContext(c => {
        val topDocs = c.indexSearcher.search(query, sizeOfBatch)
        assert(topDocs.totalHits == 1)
        val doc = c.indexSearcher.doc(topDocs.scoreDocs(0).doc)
        assert(DocumentReaderUtils.longValue(doc, ID) == id)
      })
    }
    index.close()
    FileUtils.deleteDirectory(tmpDir.toFile)
  }

  it should "batch upsert documents" in {
    val tmpDir = Files.createTempDirectory("nrtIndex")
    val index = createNRTIndex(tmpDir)
    for (id <- 1L to numberOfDocs) {
      val doc = generateDocument(id)
      index.upsert(newTerm(ID, id), doc)
    }
    index.softCommit()
    for (id <- 1L to numberOfDocs) {
      val query = newTermQuery(ID, id)
      index.doWithContext(c => {
        val topDocs = c.indexSearcher.search(query, sizeOfBatch)
        assert(topDocs.totalHits == 1)
        val doc = c.indexSearcher.doc(topDocs.scoreDocs(0).doc)
        assert(DocumentReaderUtils.longValue(doc, ID) == id.toLong)
      })
    }
    index.close()
    FileUtils.deleteDirectory(tmpDir.toFile)
  }

  it should "sequentially delete documents" in {
    val tmpDir = Files.createTempDirectory("nrtIndex")
    val index = createNRTIndex(tmpDir)
    for (id <- 1L to numberOfDocs) {
      val doc = generateDocument(id)
      index.upsert(newTerm(ID, id), doc)
    }
    index.softCommit()
    for (id <- 1L to numberOfDocs) {
      index.delete(newTerm(ID, id))
      index.softCommit()
      val query = newTermQuery(ID, id)
      index.doWithContext(c => {
        val topDocs = c.indexSearcher.search(query, sizeOfBatch)
        assert(topDocs.totalHits == 0)
      })
    }
    index.close()
    FileUtils.deleteDirectory(tmpDir.toFile)
  }

  it should "delete documents after upsert without commit" in {
    val tmpDir = Files.createTempDirectory("nrtIndex")
    val index = createNRTIndex(tmpDir)
    for (id <- 1L to numberOfDocs) {
      val doc = generateDocument(id)
      index.upsert(newTerm(ID, id), doc)
      index.delete(newTerm(ID, id))
    }

    index.hardCommit()
    for (id <- 1L to numberOfDocs) {
      val query = newTermQuery(ID, id)
      index.doWithContext(c => {
        val topDocs = c.indexSearcher.search(query, sizeOfBatch)
        assert(topDocs.totalHits == 0)
      })
    }
    index.close()
    FileUtils.deleteDirectory(tmpDir.toFile)
  }

  it should "batch delete documents" in {
    val tmpDir = Files.createTempDirectory("nrtIndex")
    val index = createNRTIndex(tmpDir)
    for (id <- 1L to numberOfDocs) {
      val doc = generateDocument(id)
      index.upsert(newTerm(ID, id), doc)
    }
    index.softCommit()
    for (ids <- (1L to numberOfDocs).grouped(sizeOfBatch)) {
      val terms = ids.map(newTerm(ID, _)).toList
      index.delete(ReadableTermsQuery.build(terms.asJava))
      index.softCommit()
      for (id <- ids) {
        val query = newTermQuery(ID, id)
        index.doWithContext(c => {
          val topDocs = c.indexSearcher.search(query, sizeOfBatch)
          assert(topDocs.totalHits == 0)
        })
      }
    }
    index.close()
    FileUtils.deleteDirectory(tmpDir.toFile)
  }

  it should "make index snapshot at last commit point" in {
    val tmpDir = Files.createTempDirectory("nrtIndex")
    val index = createNRTIndex(tmpDir)
    for (id <- 1L to numberOfDocs) {
      val doc = generateDocument(id)
      index.upsert(newTerm(ID, id), doc)
    }

    index.hardCommit()

    val terms = (1L to numberOfDocs).map(newTerm(ID, _)).toList
    index.delete(ReadableTermsQuery.build(terms.asJava))

    index.softCommit()

    for (id <- 1L to numberOfDocs) {
      val query = newTermQuery(ID, id)
      index.doWithContext(c => {
        val topDocs = c.indexSearcher.search(query, numberOfDocs.intValue())
        assert(topDocs.totalHits == 0)
      })
    }

    val indexSnapshotDir = Files.createTempDirectory("nrtIndexSnapshot")
    IndexSnapshotBuilder.fromLastCommit(index, indexSnapshotDir.toFile, None)
    val indexSnapshot = createNRTIndex(indexSnapshotDir)
    for (id <- 1L to numberOfDocs) {
      val query = newTermQuery(ID, id)
      indexSnapshot.doWithContext(c => {
        val topDocs = c.indexSearcher.search(query, numberOfDocs.intValue())
        assert(topDocs.totalHits == 1)
        val doc = c.indexSearcher.doc(topDocs.scoreDocs(0).doc)
        assert(DocumentReaderUtils.longValue(doc, ID) == id)
      })
    }
    index.close()
    indexSnapshot.close()
    FileUtils.deleteDirectory(tmpDir.toFile)
    FileUtils.deleteDirectory(indexSnapshotDir.toFile)
  }

  private def generateDocument(id: Long): Document = {
    val doc = new Document
    DocumentWriterUtils.addField(doc, ID, id, true, true)
    doc
  }

  def createNRTIndex(indexDir: Path): NRTIndex = {
    val snapshotDeletionPolicy = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy)
    new NRTIndex(createIndexWriter(indexDir, snapshotDeletionPolicy), snapshotDeletionPolicy)(OperationalSupportImpl)
  }

  def createIndexWriter(indexDir: Path, indexDeletionPolicy: IndexDeletionPolicy): IndexWriter = {
    val dir = new NIOFSDirectory(indexDir, new SingleInstanceLockFactory)
    val analyzer: Analyzer = new KeywordAnalyzer
    val limitTokenCountAnalyzer: LimitTokenCountAnalyzer = new LimitTokenCountAnalyzer(analyzer, 10000)
    val config = new IndexWriterConfig(limitTokenCountAnalyzer)
    config.setIndexDeletionPolicy(indexDeletionPolicy)
    config.setOpenMode(OpenMode.CREATE_OR_APPEND)
    config.setMergedSegmentWarmer(new SimpleMergedSegmentWarmer(InfoStream.getDefault))
    new IndexWriter(dir, config)
  }

}
