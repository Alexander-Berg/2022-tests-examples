package ru.yandex.realty.searcher.search

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 04.07.16
  */
@RunWith(classOf[JUnitRunner])
class NewFlatSortProcessorTest extends FlatSpec with Matchers {
  "NewFlatSort" should "correct work in simple case" in {
    val p = new NewFlatSortProcessor(10)
    p.setDocBase(0)

    p.addDoc(1, relevance = 1f, siteId = -1L, revenue = 0L)

    val s = p.iterator.toIndexedSeq
    s.size should be(1)
    s(0) should be(1)
  }

  it should "correct work sort site info" in {
    val p = new NewFlatSortProcessor(3)
    p.setDocBase(0)

    p.addDoc(1, relevance = 1f, siteId = 1L, revenue = 0L)
    p.addDoc(2, relevance = 2f, siteId = 1L, revenue = 0L)
    p.addDoc(3, relevance = 3f, siteId = 2L, revenue = 0L)
    p.addDoc(4, relevance = 4f, siteId = 2L, revenue = 0L)

    val s = p.iterator.toIndexedSeq
    s.size should be(3)
    s(0) should be(4)
    s(1) should be(2)
    s(2) should be(3)
  }

  it should "correct work in more complicated situation" in {
    val p = new NewFlatSortProcessor(4)
    p.setDocBase(0)

    p.addDoc(5, relevance = 0f, siteId = 2L, revenue = 200L)
    p.addDoc(6, relevance = 0f, siteId = 2L, revenue = 200L)
    p.addDoc(7, relevance = 0f, siteId = 2L, revenue = 200L)
    p.addDoc(8, relevance = 0f, siteId = 2L, revenue = 200L)
    p.addDoc(1, relevance = 1f, siteId = 1L, revenue = 100L)
    p.addDoc(2, relevance = 4f, siteId = 1L, revenue = 100L)
    p.addDoc(3, relevance = 1.1f, siteId = 2L, revenue = 200L)
    p.addDoc(4, relevance = 1f, siteId = 2L, revenue = 200L)

    val s = p.iterator.toIndexedSeq
    s.size should be(4)
    s(0) should be(3)
    s(1) should be(2)
    s(2) should be(4)
    s(3) should be(8)
  }

  it should "correct work (2)" in {
    val p = new NewFlatSortProcessor(6)
    p.setDocBase(0)

    p.addDoc(1, relevance = 1f, siteId = 1L, revenue = 100L)
    p.addDoc(2, relevance = 2f, siteId = 2L, revenue = 200L)
    p.addDoc(3, relevance = 3f, siteId = 1L, revenue = 100L)
    p.addDoc(4, relevance = 4f, siteId = 2L, revenue = 200L)
    p.addDoc(5, relevance = 5f, siteId = 1L, revenue = 100L)
    p.addDoc(6, relevance = 6f, siteId = 2L, revenue = 200L)
    p.addDoc(7, relevance = 7f, siteId = 1L, revenue = 100L)
    p.addDoc(8, relevance = 8f, siteId = 2L, revenue = 200L)
    p.addDoc(9, relevance = 9f, siteId = 1L, revenue = 100L)
    p.addDoc(10, relevance = 10f, siteId = 2L, revenue = 200L)

    val s = p.iterator.toIndexedSeq
    s.size should be(6)
    s(0) should be(10)
    s(1) should be(9)
    s(2) should be(8)
    s(3) should be(7)
    s(4) should be(6)
    s(5) should be(4)
  }

  it should "correct work with one site" in {
    val p = new NewFlatSortProcessor(2)
    p.setDocBase(10)

    p.addDoc(1, relevance = 1f, siteId = -1L, revenue = 0L)
    p.addDoc(2, relevance = 1f, siteId = -1L, revenue = 0L)
    p.addDoc(3, relevance = 2f, siteId = -1L, revenue = 0L)
    p.addDoc(4, relevance = 4f, siteId = -1L, revenue = 0L)

    val s = p.iterator.toIndexedSeq
    s.size should be(2)
    s(0) should be(14)
    s(1) should be(13)
  }

  it should "correct merge in simple case" in {
    val p = new NewFlatSortProcessor(10)
    p.setDocBase(10)
    p.addDoc(1, relevance = 1f, siteId = -1L, revenue = 0L)

    val p2 = new NewFlatSortProcessor(10)
    p2.setDocBase(0)
    p2.addDoc(2, relevance = 2f, siteId = -1L, revenue = 0L)

    p.merge(p2)

    val s = p.iterator.toIndexedSeq
    s.size should be(2)
    s(0) should be(2)
    s(1) should be(11)
  }

  it should "correct merge in more complicated case" in {
    val p = new NewFlatSortProcessor(2)
    p.setDocBase(0)
    p.addDoc(1, relevance = 1f, siteId = 1L, revenue = 100L)
    p.addDoc(2, relevance = 1f, siteId = 2L, revenue = 200L)

    val p2 = new NewFlatSortProcessor(2)
    p2.setDocBase(0)
    p2.addDoc(3, relevance = 2f, siteId = 1L, revenue = 100L)
    p2.addDoc(4, relevance = 2f, siteId = 2L, revenue = 200L)
    p2.addDoc(5, relevance = 0f, siteId = 1L, revenue = 100L)

    p.merge(p2)

    p.getTotalHits should be(5)
    val s = p.iterator.toIndexedSeq
    s.size should be(2)
    s(0) should be(4)
    s(1) should be(2)
  }

  it should "correct append another offers having same siteId" in {
    val p = new NewFlatSortProcessor(1)
    p.setDocBase(0)

    p.addDoc(1, relevance = 10f, siteId = 42L, revenue = 100L)

    p.getTotalHits should be(1)
    p.getTotalOffers should be(1)

    val s = p.iterator.toIndexedSeq
    s.size should be(1)
    s(0) should be(1)

    p.addDoc(2, relevance = 20f, siteId = 42L, revenue = 100L)
    p.getTotalHits should be(2)
    p.getTotalOffers should be(1)

    val s1 = p.iterator.toIndexedSeq
    s1.size should be(1)
    s1(0) should be(2)
  }

  it should "correct merge offers having same siteId" in {
    val p1 = new NewFlatSortProcessor(1)
    p1.setDocBase(0)

    p1.addDoc(1, relevance = 10f, siteId = 42L, revenue = 100L)

    p1.getTotalHits should be(1)
    p1.getTotalOffers should be(1)

    val s1 = p1.iterator.toIndexedSeq
    s1.size should be(1)
    s1(0) should be(1)

    val p2 = new NewFlatSortProcessor(1)
    p2.addDoc(2, relevance = 20f, siteId = 42L, revenue = 100L)
    p2.getTotalHits should be(1)
    p2.getTotalOffers should be(1)

    val s2 = p2.iterator.toIndexedSeq
    s2.size should be(1)
    s2(0) should be(2)

    p1.merge(p2)

    p1.getTotalHits should be(2)
    p1.getTotalOffers should be(1)

    val s = p1.iterator.toIndexedSeq
    s.size should be(1)
    s(0) should be(2)
  }
}
