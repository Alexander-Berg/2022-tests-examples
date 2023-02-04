package vasgen.indexer.saas.worker

object LogBrokerWorkerTest extends ZIOSpecDefault {

  private lazy val originalDoc = RawDocument()
    .withVersion(5)
    .withEpoch(5)
    .withModifiedAt(Timestamp().withSeconds(1000))
    .withAction(UPSERT)

  override def spec: Spec[TestEnvironment, Any] =
    suite("docOrdering")(
      test("lower epoch should lose") {
        val another = originalDoc.update(_.epoch.modify(_ - 1))
        assert(docOrdering.max(originalDoc, another))(equalTo(originalDoc))
      },
      test("lower version should lose") {
        val another = originalDoc.update(_.version.modify(_ - 1))
        assert(docOrdering.max(originalDoc, another))(equalTo(originalDoc))
      },
      test("lower modified date should lose") {
        val another = originalDoc.update(_.modifiedAt.seconds.modify(_ - 1))
        assert(docOrdering.max(originalDoc, another))(equalTo(originalDoc))
      },
      test("REMOVE should win over UPSERT") {
        val another = originalDoc.update(_.action := DELETE)
        assert(docOrdering.max(originalDoc, another))(equalTo(another))
      },
      test("higher epoch should win") {
        val another = originalDoc.update(_.epoch.modify(_ + 1))
        assert(docOrdering.max(originalDoc, another))(equalTo(another))
      },
      test("higher version should win") {
        val another = originalDoc.update(_.version.modify(_ + 1))
        assert(docOrdering.max(originalDoc, another))(equalTo(another))
      },
      test("higher modified date should win") {
        val another = originalDoc.update(_.modifiedAt.seconds.modify(_ + 1))
        assert(docOrdering.max(originalDoc, another))(equalTo(another))
      },
    )

}
