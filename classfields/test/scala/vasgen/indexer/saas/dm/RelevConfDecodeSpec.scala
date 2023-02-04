package vasgen.indexer.saas.dm

object RelevConfDecodeSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment, Any] = {
    suite("decodeRelev")(
      test("complete conf") {
        assert(Relev(Samples.completeRelevConf))(
          isRight(
            equalTo(
              Relev.Relev(
                dynamicFactors = Some(
                  Map(
                    "LongQuery"    -> Relev._Factor(0, None),
                    "InvWordCount" -> Relev._Factor(1, None),
                  ),
                ),
                staticFactors = Some(
                  Map("f_general_no_photo" -> Relev._Factor(50, Some(1.0))),
                ),
                userFactors = Some(
                  Map(
                    "sort_hash"        -> Relev._Factor(21, None),
                    "dssm_dot_product" -> Relev._Factor(24, Some(0.0)),
                  ),
                ),
                rtyDynamicFactors = Some(
                  Map(
                    "_Time__f_f_offer_publish_date_g" -> Relev._Factor(31, None),
                  ),
                ),
                zoneFactors = Some(
                  Map("_BM25F_Sy_z_offer_title_g" -> Relev._Factor(61, None)),
                ),
                formulas = Some(
                  Map(
                    "pruning" ->
                      Relev._Formula(
                        Some(
                          s"$${WorkDir and WorkDir or _BIN_DIRECTORY}/configs/formula-2415-matrixnet_remap.info",
                        ),
                        Some("O10F30070SE08000AOL72U00000V30000SF1"),
                      ),
                  ),
                ),
              ),
            ),
          ),
        )
      },
      test("incomplete conf") {
        assert(Relev(Samples.incompleteRelevConf))(
          isRight(
            equalTo(
              Relev.Relev(
                dynamicFactors = Some(
                  Map(
                    "LongQuery"    -> Relev._Factor(0, None),
                    "InvWordCount" -> Relev._Factor(1, None),
                  ),
                ),
                staticFactors = Some(
                  Map("f_general_no_photo" -> Relev._Factor(50, Some(1.0))),
                ),
                userFactors = None,
                rtyDynamicFactors = None,
                zoneFactors = None,
                formulas = None,
              ),
            ),
          ),
        )
      },
    )
  }

}
