package ru.yandex.direct.web.api5.request

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.direct.domain.FundsAmount

@RunWith(Parameterized::class)
class BidsSetAutoBuilderTest(val expected: String, val actual: BidsSetAuto.Builder) {
    @Test
    fun runBidsSetAutoBuilderTest() {
        assertThat(actual.build()).isEqualToComparingFieldByFieldRecursively(
                mGson.fromJson(expected, BidsSetAuto::class.java)
        )
    }

    companion object {
        private val mGson = Gson()

        @JvmStatic
        @Parameterized.Parameters
        fun provideParameters(): Collection<Array<Any>> = listOf(
                arrayOf(
                        SIMPLE_NETWORK_JSON,
                        BidsSetAuto.forNetworkByCoverage(0)
                ),
                arrayOf(
                        BID_CEILING_NETWORK_JSON,
                        BidsSetAuto.forNetworkByCoverage(0).setBidCeiling(FundsAmount.zero())
                ),
                arrayOf(
                        INCREASE_PERCENT_NETWORK_JSON,
                        BidsSetAuto.forNetworkByCoverage(0).setIncreasePercent(0)
                ),
                arrayOf(
                        SINGLE_PHRASE_NETWORK_JSON,
                        BidsSetAuto.forNetworkByCoverage(0).withKeywordIds(listOf(0))
                ),
                arrayOf(
                        MULTIPLE_PHRASE_NETWORK_JSON,
                        BidsSetAuto.forNetworkByCoverage(0).withKeywordIds(listOf(0, 0))
                ),
                arrayOf(
                        MULTIPLE_PHRASE_BID_CEILING_NETWORK_JSON,
                        BidsSetAuto.forNetworkByCoverage(0).withKeywordIds(listOf(0, 0))
                                .setBidCeiling(FundsAmount.zero())
                ),
                arrayOf(
                        MULTIPLE_PHRASE_BID_CEILING_NETWORK_JSON,
                        BidsSetAuto.forNetworkByCoverage(0).setBidCeiling(FundsAmount.zero())
                                .withKeywordIds(listOf(0, 0))
                ),
                arrayOf(
                        MULTIPLE_PHRASE_INCREASE_PERCENT_NETWORK_JSON,
                        BidsSetAuto.forNetworkByCoverage(0).withKeywordIds(listOf(0, 0)).setIncreasePercent(0)
                ),
                arrayOf(
                        MULTIPLE_PHRASE_INCREASE_PERCENT_NETWORK_JSON,
                        BidsSetAuto.forNetworkByCoverage(0).setIncreasePercent(0).withKeywordIds(listOf(0, 0))
                ),
                arrayOf(
                        SIMPLE_SEARCH_JSON,
                        BidsSetAuto.forSearchByTrafficVolume(0)
                )
        )

        /*  Request structure:
         *  {
         *    "method": "setAuto",
         *    "params": {
         *      "KeywordBids": [{
         *        "CampaignId": (long),
         *        "AdGroupId": (long),
         *        "KeywordId": (long),
         *        "BiddingRule": {
         *          "SearchByTrafficVolume": {
         *            "TargetTrafficVolume": (int),
         *            "IncreasePercent": (int),
         *            "BidCeiling": (long)
         *          },
         *          "NetworkByCoverage": {
         *            "TargetCoverage": (int),
         *            "IncreasePercent": (int),
         *            "BidCeiling": (long)
         *          }
         *        }
         *      }, ... ]
         *    }
         *  }
         */

        private const val SIMPLE_NETWORK_JSON = """
                    {
                        "method": "setAuto",
                        "params": {
                            "KeywordBids": [{
                                "BiddingRule": {
                                    "NetworkByCoverage": {
                                        "TargetCoverage": 0
                                    }
                                }
                            }]
                        }
                    }
        """

        private const val BID_CEILING_NETWORK_JSON = """
                    {
                        "method": "setAuto",
                        "params": {
                            "KeywordBids": [{
                                "BiddingRule": {
                                    "NetworkByCoverage": {
                                        "TargetCoverage": 0,
                                        "BidCeiling": 0
                                    }
                                }
                            }]
                        }
                    }
        """

        private const val INCREASE_PERCENT_NETWORK_JSON = """
                    {
                        "method": "setAuto",
                        "params": {
                            "KeywordBids": [{
                                "BiddingRule": {
                                    "NetworkByCoverage": {
                                        "TargetCoverage": 0,
                                        "IncreasePercent": 0
                                    }
                                }
                            }]
                        }
                    }
        """

        private const val SINGLE_PHRASE_NETWORK_JSON = """
                    {
                        "method": "setAuto",
                        "params": {
                            "KeywordBids": [{
                                "KeywordId": 0,
                                "BiddingRule": {
                                    "NetworkByCoverage": {
                                        "TargetCoverage": 0
                                    }
                                }
                            }]
                        }
                    }
        """

        private const val MULTIPLE_PHRASE_NETWORK_JSON = """
                    {
                        "method": "setAuto",
                        "params": {
                            "KeywordBids": [{
                                "KeywordId": 0,
                                "BiddingRule": {
                                    "NetworkByCoverage": {
                                        "TargetCoverage": 0
                                    }
                                }
                            }, {
                                "KeywordId": 0,
                                "BiddingRule": {
                                    "NetworkByCoverage": {
                                        "TargetCoverage": 0
                                    }
                                }
                            }]
                        }
                    }
        """

        private const val MULTIPLE_PHRASE_BID_CEILING_NETWORK_JSON = """
                    {
                        "method": "setAuto",
                        "params": {
                            "KeywordBids": [{
                                "KeywordId": 0,
                                "BiddingRule": {
                                    "NetworkByCoverage": {
                                        "TargetCoverage": 0,
                                        "BidCeiling": 0
                                    }
                                }
                            }, {
                                "KeywordId": 0,
                                "BiddingRule": {
                                    "NetworkByCoverage": {
                                        "TargetCoverage": 0,
                                        "BidCeiling": 0
                                    }
                                }
                            }]
                        }
                    }
        """

        private const val MULTIPLE_PHRASE_INCREASE_PERCENT_NETWORK_JSON = """
                    {
                        "method": "setAuto",
                        "params": {
                            "KeywordBids": [{
                                "KeywordId": 0,
                                "BiddingRule": {
                                    "NetworkByCoverage": {
                                        "TargetCoverage": 0,
                                        "IncreasePercent": 0
                                    }
                                }
                            }, {
                                "KeywordId": 0,
                                "BiddingRule": {
                                    "NetworkByCoverage": {
                                        "TargetCoverage": 0,
                                        "IncreasePercent": 0
                                    }
                                }
                            }]
                        }
                    }
        """

        private const val SIMPLE_SEARCH_JSON = """
                    {
                        "method": "setAuto",
                        "params": {
                            "KeywordBids": [{
                                "BiddingRule": {
                                    "SearchByTrafficVolume": {
                                        "TargetTrafficVolume": 0
                                    }
                                }
                            }]
                        }
                    }
        """
    }
}