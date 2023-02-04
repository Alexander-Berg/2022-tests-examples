package ru.yandex.vertis.parsing.realty.scheduler.workers.meta

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.{JsObject, Json}
import ru.yandex.realty.proto.unified.offer.images.OriginalSize

@RunWith(classOf[JUnitRunner])
class HolocronRealtyPhotoBuilderTest extends FlatSpec with Matchers {

  private val MetaExample: String =
    """
      |{
      |    "AppTimestamp": "Feb  5 2019 15:11:06",
      |    "AutoRuContacts": {
      |        "data": {
      |            "aggregated_stat": 0,
      |            "blocks": null,
      |            "fulltext": null,
      |            "imgsize": {
      |                "h": 1032,
      |                "w": 774
      |            },
      |            "max_line_confidence": null,
      |            "rotate": 0,
      |            "timeLimit": {
      |                "percent": 100,
      |                "stopped_by_timeout": false
      |            }
      |        },
      |        "status": "success"
      |    },
      |    "GlobalSemidupDescriptor64": "MED948FDEE1D0EF40",
      |    "NNetClassifiersTopClasses": {
      |        "pool7_imagenet": [
      |            [
      |                3805,
      |                60
      |            ],
      |            [
      |                5141,
      |                30
      |            ],
      |            [
      |                3310,
      |                24
      |            ],
      |            [
      |                3732,
      |                14
      |            ],
      |            [
      |                3217,
      |                12
      |            ],
      |            [
      |                4701,
      |                12
      |            ],
      |            [
      |                2998,
      |                11
      |            ],
      |            [
      |                3007,
      |                10
      |            ],
      |            [
      |                4357,
      |                8
      |            ],
      |            [
      |                4657,
      |                8
      |            ]
      |        ]
      |    },
      |    "NeuralNetClasses": {
      |        "aesthetic": 116,
      |        "gruesome": 25,
      |        "multiclass_photos_porn": 5,
      |        "ocr_text": 2,
      |        "perversion": 0,
      |        "porno": 0,
      |        "realty_docs_with_plans": 0,
      |        "realty_docs_wo_plans": 0,
      |        "realty_entrance_stairs": 0,
      |        "realty_interior": 252,
      |        "realty_kitchen": 0,
      |        "realty_maps": 0,
      |        "realty_other": 1,
      |        "realty_outside": 0,
      |        "realty_spam": 0,
      |        "realty_wc": 0,
      |        "wallpaper": 90
      |    },
      |    "SmartCrop": {
      |        "1200x630": {
      |            "FitLevel": 0.6971356272697449,
      |            "Rect": [
      |                0,
      |                574,
      |                774,
      |                406
      |            ]
      |        }
      |    },
      |    "SmartCropCompactSaliency": "AgAAAEjhej-amRk-BgMAAAgEAAAAAAAAAAAAAAAAAAAAAAAAxQkAAP_Y_-AAEEpGSUYAAQEAAAEAAQAA_9sAQwAoHB4jHhkoIyEjLSsoMDxkQTw3Nzx7WF1JZJGAmZaPgIyKoLTmw6Cq2q2KjMj_y9ru9f___5vB____-v_m_f_4_8AACwgCQQGxAQERAP_EABgAAQEBAQEAAAAAAAAAAAAAAAABAgMF_8QAGBABAQEBAQAAAAAAAAAAAAAAAAERAhL_2gAIAQEAAD8A8YAAUVWosjUjcjUjcjUjUjUi4uLhhYzYzYljFjFYrNYqVmoiCAAAAAoACiqsakakbkakdJGpGpy3OWpy15PJ5XEsZsZsZsYsc6xWKzWazUqIIAAAACgAoLGosakbkbkdOY3zG5y3OWpy3OV8nlfJ5ZsZsYsZsY6jn1HOsVis1mpUQQAAAAFABRVjUajUjcjpzHTmOnPLpOWpy3OW5yvk8nk8s2MWMWMWMdRy6jn0xWKzWazUQQAAAAFAFBWosbkbkb5jrzHTmO3PLc5bnLc5dJyvlPJ5Tyz1HOxixzsc-o59Ry6YrnWazWaiCAAAAAoAoo1Go3G5HXmOvMdeY7cx0kbnLpOW5yvlMTExnqOXUc-ox1HLpy6cunPpisVms1EEAAAABQBRRqNRuNx05rrzXXmu3PTc6bnTc6bnS-k9J6T0x1059dMddOd6c-unLqufVc7WLWazWaiCAAAAAoAoo1Go1G43zXTmuvNdOa6TpqdNzpqdL6T0npPTN6c70xenO9MdVz6rnaxazWazURBAAAAAUAUUWNRqNRuV0ldOa6c1qdNTpqdNzo9J6T0npm9MXpi9MWsWsWsWsWs1KjIggAAAAKAKCrGosajcrcrcrcrUrU6anSzpfSek9J6ZvTF6ZtZtYtYtZtZtZqIiAgAAAAKAKCqsaixqVuVqVuVqVZVnTU6PSek9J6S9MWs2s2s2s2s2s1EEQEAAAABQBQVVWLK1KsrcrUq6ur6X0ejU1NS1m1m1m1LWbUrNQQQEAAAAAUAUFVV1ZVlalWVrV1dNXU1NNS1m1LU1m1KiIggCAAAAAKACgqi6urrWrpq6ummppqampqaiIiAgIAAAAACgAoKKumrq6umrpppqaamppqIiAgCAAAAAAoAAoKLpq6aumrqaaaammoIggAIAAAAAAoAACgppq6aaaaaaCCCAAIAAAAAAKAAAKCgAACCAAIAAAAAAAoAAAoCgAgCAAIAAAAAAAAAoAAoAAAgCAAAAAAAAAAACgAKACAAgAAAAAAAAAAAAoAAAACAAAAAAAAAAAAAKAAAAIAAAAAAAAAAAAAKAAAAgAAAAAAAAAAAAAAKAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAACAAAAAAAAAAACgAAAAAAAgAAAAAAAAAKAAKAAAgAAIAAAAAAAACgAoCgACAIAAgAAAAAAACgKApi4YYuGGGJhhiYIAgAIAAAAAACgKKC4YuGLhi4YYmGGJhiYgIAgAIAAAAKACiimLhi4uGLhi4YYYmGJiYYmIICAgACAACgACiqLi4YuLi4uLhi4YYYmJhjOJiYYmIICAgACACgAKKKuLi4YuLi4uNSGLi4YYYmJiWJYljOJiYYggggIACAKAKKLi4uLhi4uLjUiyLI1IuGLi4YmJiYliWM2JYziYYmJiYYmCCCAAgKCgqmLi41iyLi4sjUiyLI1Is5XF8ri4YmJiYljNjNiWM2JiYmJiYmGJiYIIIAIKoKKuLiyNSLI1IsjUiyNTlZy1OVnLU5XyeV8r5PKeU8p5S8s3lmxmxLGbEsTExMTExMTExEEBAEaBVVcXFkakWRqRqRqRZy1OWpy1OVnLU5Wcr5Xyvk8nlPKeU8peWbyzeWbyzeUsZsSxmxMTExMTExMREEEAUVVWRZGpFkakakakakanLU5bnLU5WctTlqcrOV8r5PK-TynlPKeUvLN5ZvLN5ZvLN5ZsZsZsSxLGbEsSxmxMTEREBAVVVYsjUjUjUjUjUjcjU5bnLU5anLc5anKzlqcrOV8r5XyeTynlPKeUvLN5ZvLF5ZvLN5ZvLNjNjNiWM2JYzYzYljNiIiICDSqqxqLGpG5GpGpG5G5Go1G41GlixqVdNNXTU1NTUrNZqVmsVms1ms2JYzYljNjNjNSs1KiIgI0qtRY1GpGpGpG5GpG5GpGo1Go3FUXV001dTTU01KzUqVms1ms1mpYzYljNiWM2M2M1mpUrIiA0qxqLGpGpG5GpGpG5GpG5FjUajUUF00000001NERGalZrNSxmxLEsZsZsZsZsZsSs1mpUEQbWNRY1I1I1I1I3I1I1I3I1I1FjUWAGmmmmmmmmoIJWalSs2JYljNiWM2M2M2M2M1ms1KiCDaxqLGpGpGpG5G5GpGpGpG5FVVBDTU0000000ARERMTGcTEsZsZsYsZsZsZrNSoiCOixqLGo1G41G41GpWpWpV1dXTTU01NTTTU01dNNXTTTTU1NTU1NRErNZrNZrFZrNSsiINrGosajUajUrUqytSrK1Ol9HpfR6PSej0no9HpPR6PS-j0el9Ho9Ho9JpqaamppqazazWazWazWalREQbVqLFjUWNStSrq6ur6PS-j0ej0no9HpPR6PRpq6aaaummmmpppqaaampWalZrNZrNSoiINqrSqsWVrTV01dNPR6X0no9HpPRpq6aaaaummmmmmmmmmmmmpWalSs1ms1KiII2qqqqurppppq6aaammmmmmmmmrpppq6aaaaaaaaummogjNZrNSogg2Kqirppppqaaaaaaammmrpppq6aaaaummmmmrpq6auoIiVms1KiCDSirpq6aaaammmmmpppppppq6aaummmmmmrppppq6umrqaaampazUqVEEFUUXTTV1NNNNTTTU00001dNNXTTTTTTTV000001dXTV1NNNTUtREQQRVFBdNNNNTTTTU00000NNXTTTV0000000001dNXTV01NNNREQQQFFAF1NAEE0UAUA0NNNNNXTTTV001dNNNTREEEABQAAEBAFAUAAADV001dNNNAQQQBBQAABAQFAFAAAAFFAAQQAQAAAAEAAFAAAAFAUAAQBAQUAAEAQBQAUAAAAFAAAQAQAAABAAAUAAAABQAUEAAGVAAEAARQABQAAAAFAAABBAAAAAQBQAAAAAFAABQEAQAAAAQAFAAAAAAFAABUAH__Z",
      |    "cbirdaemon": {
      |        "cbirdaemon": {
      |            "info": "774x1032",
      |            "info_orig": "774x1032",
      |            "similarnn": {
      |                "ImageFeatures": [
      |                    {
      |                        "Features": [
      |                            -0.1318143308,
      |                            0.1403168887,
      |                            -0.1522182673,
      |                            -0.008895006962,
      |                            -0.002994248411,
      |                            0.07275622338,
      |                            0.04962832108,
      |                            -0.1122755185,
      |                            0.1344784051,
      |                            -0.2516216934,
      |                            0.0709400028,
      |                            0.128210023,
      |                            -0.07859010249,
      |                            -0.01006792299,
      |                            -0.0327532962,
      |                            -0.02012322471,
      |                            0.0777195245,
      |                            -0.1056786478,
      |                            -0.07371180505,
      |                            -0.21505858,
      |                            -0.00290714833,
      |                            0.08138815314,
      |                            0.06558935344,
      |                            -0.04865885153,
      |                            -0.08767291903,
      |                            -0.03263534233,
      |                            0.116766952,
      |                            0.1060671508,
      |                            -0.2091316283,
      |                            -0.06362788379,
      |                            -0.06633866578,
      |                            -0.237084493,
      |                            -0.08759687096,
      |                            0.03355151787,
      |                            0.1307696849,
      |                            0.03461098298,
      |                            -0.0587683171,
      |                            -0.2246658355,
      |                            0.1384114921,
      |                            -0.04932020605,
      |                            0.038831871,
      |                            -0.04057937488,
      |                            -0.058645885439999999,
      |                            -0.0785221979,
      |                            0.1359129846,
      |                            -0.0180724524,
      |                            -0.07002644241,
      |                            -0.1028375551,
      |                            -0.1925652474,
      |                            -0.004641811363,
      |                            0.07685406506,
      |                            -0.01639235206,
      |                            0.09659406543,
      |                            0.04617168009,
      |                            0.08162722737,
      |                            0.0324655287,
      |                            -0.1144959703,
      |                            -0.1467055231,
      |                            -0.06112627685,
      |                            -0.004028744996,
      |                            0.08900462091,
      |                            -0.01215494517,
      |                            0.04526535049,
      |                            -0.01471640263,
      |                            -0.081777744,
      |                            -0.1072611883,
      |                            -0.03407890722,
      |                            0.09624387324,
      |                            0.09043543786,
      |                            -0.1706464589,
      |                            0.08753157407,
      |                            0.01891871169,
      |                            -0.03796821833,
      |                            -0.07460840046,
      |                            -0.03484823182,
      |                            -0.05299314111,
      |                            0.06004130095,
      |                            0.2102721035,
      |                            -0.009294332936,
      |                            -0.02804001048,
      |                            -0.1798692197,
      |                            0.159914434,
      |                            -0.03917828202,
      |                            0.1136076376,
      |                            0.05127402395,
      |                            0.1336796284,
      |                            -0.1409919411,
      |                            0.1361580342,
      |                            0.144623369,
      |                            0.01711837575,
      |                            0.00102055748,
      |                            0.0678460449,
      |                            0.06205057353,
      |                            -0.1429952383,
      |                            -0.02933611535,
      |                            0.08787488192
      |                        ],
      |                        "LayerName": "prod_v8_enc_toloka_96",
      |                        "Version": "8"
      |                    }
      |                ],
      |                "Predictions": [
      |                    {
      |                        "LabelName": "docs_with_plans",
      |                        "Probability": 7.4655361e-8,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "docs_wo_plans",
      |                        "Probability": 2.77982326e-9,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "entrance_stairs",
      |                        "Probability": 7.333119925e-7,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "interior",
      |                        "Probability": 0.9964697361,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "kitchen",
      |                        "Probability": 0.000001705850423,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "maps",
      |                        "Probability": 6.011887876e-8,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "other",
      |                        "Probability": 0.003419877728,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "outside",
      |                        "Probability": 0.000004009569238,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "spam",
      |                        "Probability": 0.0001026473619,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "wc",
      |                        "Probability": 0.000001042522854,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "bad_quality_v3",
      |                        "Probability": 0.03709795699,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "good_quality_v3",
      |                        "Probability": 0.1554287672,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "ok_quality_v3",
      |                        "Probability": 0.8074732423,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "bad_quality_v2",
      |                        "Probability": 0.01448962372,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "good_quality_v2",
      |                        "Probability": 0.8702962399,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "ok_quality_v2",
      |                        "Probability": 0.1151949465,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "500px_0",
      |                        "Probability": 0.5543541312,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "2_of_6_0",
      |                        "Probability": 0.2687864304,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "aadb_aesthetic_0",
      |                        "Probability": 0.4770888686,
      |                        "Version": "8"
      |                    },
      |                    {
      |                        "LabelName": "pairwise_attr_0",
      |                        "Probability": 0.4646453261,
      |                        "Version": "8"
      |                    }
      |                ]
      |            }
      |        }
      |    },
      |    "crc64": "BC40D2CF1770C85B",
      |    "md5": "391c3b29d76bc96e3d270588a1aae58e",
      |    "modification-time": 1574947455,
      |    "orig-animated": false,
      |    "orig-format": "JPEG",
      |    "orig-orientation": "",
      |    "orig-size": {
      |        "x": 774,
      |        "y": 1032
      |    },
      |    "orig-size-bytes": 83802,
      |    "processed_by_AutoRuContacts": true,
      |    "processed_by_cbirdaemon": true,
      |    "processed_by_computer_vision": true,
      |    "processed_by_realty_repair_quality_by_image_v1": true,
      |    "processed_by_realty_repair_quality_v1": true,
      |    "processing": "finished",
      |    "r-orig-size": {
      |        "x": 774,
      |        "y": 1032
      |    },
      |    "realty_repair_quality_by_image_v1": {
      |        "AppTimestamp": "Oct 15 2018 12:17:18",
      |        "NeuralNetClasses": {
      |            "realty_cosmetic": 106,
      |            "realty_euro": 144,
      |            "realty_need_repair": 1,
      |            "realty_other": 1
      |        }
      |    },
      |    "realty_repair_quality_v1": {
      |        "categories": [
      |            {
      |                "name": "евроремонт",
      |                "similarity": 0.331372
      |            },
      |            {
      |                "name": "свежий ремонт",
      |                "similarity": 0.296343
      |            },
      |            {
      |                "name": "квартира требует ремонта",
      |                "similarity": 0.378914
      |            },
      |            {
      |                "name": "убитая квартира",
      |                "similarity": 0.316739
      |            }
      |        ],
      |        "features": "prod_v5_enc_i2t_v7_200_img",
      |        "score": -0.0266527
      |    }
      |}
      |""".stripMargin

  "extractMetaFromJson" should "work correctly" in {
    val json = Json.parse(MetaExample).asInstanceOf[JsObject]
    val meta = HolocronRealtyPhotoBuilder.extractMetaFromJson(json)
    meta.getSimilarityVector.getFeatureCount shouldBe 96
    meta.getCbirPredictionCount shouldBe 20
    meta.getCbirPrediction(0).getName shouldBe "docs_with_plans"
    meta.getCbirPrediction(0).getPrediction shouldEqual 7.4655361e-8 +- 1e-8
    meta.getCbirPrediction(0).getClassifierVersion shouldBe "8"
    meta.getCvHash shouldBe "MED948FDEE1D0EF40"
    meta.getNnPredictionCount shouldBe 17
    meta.getNnPrediction(0).getName shouldBe "aesthetic"
    meta.getNnPrediction(0).getPrediction shouldEqual 116.0 +- 1e-8
    meta.getNnPrediction(0).getClassifierVersion shouldBe ""
    meta.getRepairQualityV1.getScore shouldEqual -0.0266527 +- 1e-6
    meta.getRepairQualityV1.getVersion shouldBe "prod_v5_enc_i2t_v7_200_img"
    meta.getRepairQualityV1.getPredictionCount shouldBe 4
    meta.getRepairQualityV1.getPrediction(0).getName shouldBe "евроремонт"
    meta.getRepairQualityV1.getPrediction(0).getPrediction shouldEqual 0.331372 +- 1e-6
    meta.getRepairQualityByImageV1Count shouldBe 4
    meta.getRepairQualityByImageV1(0).getName shouldBe "realty_cosmetic"
    meta.getRepairQualityByImageV1(0).getPrediction shouldBe 106.0
    meta.getOriginalSize shouldBe OriginalSize.newBuilder().setX(774).setY(1032).build()
  }

}
