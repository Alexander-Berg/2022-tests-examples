package ru.yandex.realty.model.util

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase

@RunWith(classOf[JUnitRunner])
class SliceToPageAdapterSpec extends SpecBase {

  "SliceToPageAdapter" when {
    "toPageForLoad" should {
      "return page 0 for download if length more than from " in {
        val slice: Slice = Range(2, 7)
        val adapter = new SliceToPageAdapter(slice)

        adapter.page.number should be(0)
        adapter.page.size should be(slice.to)
      }

      "return no first page for download if length less than from " in {
        val slice: Slice = Range(10, 13)
        val adapter = new SliceToPageAdapter(slice)

        adapter.page.number should be(3)
        adapter.page.size should be(3)
      }

    }
    "preparePageBySlice" should {
      "return sub sequence from first page" in {
        val page = Seq.range(1, 15)
        val slice = Range(0, 5)
        val adapter = new SliceToPageAdapter(slice)

        val trimResult = adapter.extract(page)

        trimResult should be eq Seq.range(1, 5)
      }

      "return sub sequence from any page in the middle " in {
        val page = Seq.range(1, 5)
        val slice = Range(10, 13)
        val adapter = new SliceToPageAdapter(slice)

        val trimResult = adapter.extract(page)

        trimResult should be eq Seq(1, 2, 3)
      }

      "return sub sequence from page, which not contains full length, because it sould not be" in {
        val page = Seq.range(1, 3)
        val slice = Range(0, 5)
        val adapter = new SliceToPageAdapter(slice)

        val trimResult = adapter.extract(page)

        trimResult should be eq Seq.range(1, 3)
      }

      "full work" in {
        val list = Seq.range(1, 14)
        val slice = Range(10, 13) //I want to take Seq(10, 11, 12)
        val adapter = new SliceToPageAdapter(slice)

        val paging = adapter.page //Take a page, which contains needed numbers
        println("---> page = [" + paging.number + "] size [" + paging.size + "]")

        //build page from list
        val page = list.slice(paging.number * paging.size, paging.number * paging.size + paging.size)
        println(page)

        val fragment = adapter.extract(page)
        fragment should be eq Seq(10, 11, 12)
      }
    }
  }
}
