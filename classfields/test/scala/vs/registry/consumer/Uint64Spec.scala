package vs.registry.consumer

import strict.Uint64
import zio.test.*
import zio.test.Assertion.*
import zio.test.ZIOSpecDefault

object Uint64Spec extends ZIOSpecDefault {

  val suites =
    suite("Uint64")(
      test("MaxUint64") {
        val a = Uint64(1L)
        val b = Uint64(2L)
        val c = Uint64(Long.MaxValue + 1L)
        val d = Uint64(Long.MaxValue + 2L)
        val e = Uint64.MaxValue
        assert(a.max(a))(equalTo(a)) && assert(d.max(d))(equalTo(d)) &&
        assert(a.max(b))(equalTo(b)) && assert(b.max(a))(equalTo(b)) &&
        assert(a.max(c))(equalTo(c)) && assert(c.max(a))(equalTo(c)) &&
        assert(c.max(d))(equalTo(d)) && assert(d.max(c))(equalTo(d)) &&
        assert(d.max(e))(equalTo(e)) && assert(e.max(d))(equalTo(e))
      },
    )

  override def spec = suites @@ TestAspect.sequential

}
