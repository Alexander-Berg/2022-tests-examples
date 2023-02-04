package vasgen.core.saas

object DssmHelperSpec extends ZIOSpecDefault with Logging {

  private val vector: Array[Float] = Array(
    -0.0821619183f, 0.0895022079f, -0.0941065177f, 0.0536232777f, 0.0255993772f,
    -0.00346656679f, 0.0552949384f, -0.0330919661f, 0.173802957f, 0.161270753f,
    0.0677589923f, -0.0766444504f, -0.00230164803f, -0.0220243689f,
    -0.0620158277f, 0.0865136608f, -0.294350892f, 0.0134364162f, -0.164030969f,
    -0.0254515577f, 0.0767327398f, 0.0097791478f, -0.147331014f, 0.1342085f,
    0.11273475f, 0.0343590416f, -0.042685207f, 0.0676408038f, -0.0665865466f,
    -0.410292476f, 0.0485725738f, 0.102063715f, 0.132022202f, -0.159374952f,
    0.142058149f, 0.163174376f, -0.3195692f, -0.0309698284f, 0.0514705181f,
    -0.126454517f, 0.340103149f, 0.0916257054f, -0.0844670013f, 0.343217731f,
    0.0895128548f, -0.122139916f, -0.112703644f, 0.0106707374f, -0.161860213f,
    -0.0443313755f,
  )

  private val encodedVectorAsBase64String =
    "gkSove9Mtz3rusC9FaRbPcm10TxXL2O78nxiPXGLB71n+TE+KSQlPjrFij3E95y9P9cWu3NstLxPBH69Ey6xPSm1lr5qJFw8vPcnvsl/0LwOJp09uDggPPHdFr70bQk+euHmPRG8DD2v1i69Q4eKPYdeiL3bEdK+CfRGPcgG0T3UMAc+MDMjvrF3ET4vFyc+k56jvnC0/bzA0lI9S30BvgAirj5Cprs9Cf2svTy6rz6EUrc9fiT6vSvR5r1R1C48r74lvtGUNb0="

  override def spec: Spec[TestEnvironment, Any] = {
    suite("encode dssm vector")(
      test("encode") {
        assert(toBase64String(Array(1f, 2f, 3f)))(
          equalTo("AACAPwAAAEAAAEBA"),
        ) &&
        assert(toBase64String(vector))(equalTo(encodedVectorAsBase64String))
      },
    ) @@ TestAspect.ignore
  }

}
