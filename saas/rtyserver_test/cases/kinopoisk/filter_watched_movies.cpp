#include "util/generic/strbuf.h"
#include <ostream>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

namespace {
    const TStringBuf MovieDocs[] = {
        TStringBuf(R"(
            {
            "docs": [
                {
                "url": "kinopoisk.ru/film/666",
                "s_poster_mds_base_url": {
                    "value": "//avatars.mdst.yandex.net/get-kinopoisk-image/65710/f3bcd219-5e3d-41d5-9d77-41608b9bbf8d",
                    "type": "#plh"
                },
                "i_available_online": {
                    "value": 1,
                    "type": "#pig"
                },
                "s_document_type": {
                    "value": "FILM",
                    "type": "#plh"
                },
                "s_title": {
                    "value": "Форсаж",
                    "type": "#plh"
                },
                "i_has_image": {
                    "value": 1,
                    "type": "#pig"
                },
                "i_year": {
                    "value": 2001,
                    "type": "#pig"
                },
                "i_kp_top_rating": {
                    "value": 0,
                    "type": "#pig"
                },
                "z_alternate_titles": [
                    {
                    "value": "Furios si iute",
                    "type": "#z"
                    },
                    {
                    "value": "Velozes E Furiosos",
                    "type": "#z"
                    }
                ],
                "s_age_restriction": {
                    "value": "under_16",
                    "type": "#plh"
                },
                "s_service_region_exclregion": [
                    {
                    "value": "10_169_",
                    "type": "#plh"
                    },
                    {
                    "value": "167_159_",
                    "type": "#plh"
                    },
                    {
                    "value": "200_171_",
                    "type": "#plh"
                    }
                ],
                "z_year": {
                    "value": "2001",
                    "type": "#z"
                },
                "i_votes": {
                    "value": 170962,
                    "type": "#pig"
                },
                "s_original_title": {
                    "value": "The Fast and the Furious",
                    "type": "#plh"
                },
                "i_view": {
                    "value": 1,
                    "type": "#pig"
                },
                "i_film_id": {
                    "value": 666,
                    "type": "#pig"
                },
                "s_service_region_subscription": [
                    {
                    "value": "10_169_",
                    "type": "#plh"
                    },
                    {
                    "value": "200_171_",
                    "type": "#plh"
                    },
                    {
                    "value": "167_159_",
                    "type": "#plh"
                    }
                ],
                "z_original_title": {
                    "value": "The Fast and the Furious",
                    "type": "#z"
                },
                "s_type": {
                    "value": "film",
                    "type": "#plh"
                },
                "i_searchable": {
                    "value": 1,
                    "type": "#pig"
                },
                "z_title": {
                    "value": "Форсаж",
                    "type": "#z"
                },
                "i_kp_rating": {
                    "value": 7731,
                    "type": "#pig"
                },
                "options": {
                    "modification_timestamp": "1617802262"
                }
                }
            ],
            "action": "modify",
            "prefix": 1
            }
        )"),

        TStringBuf(R"(
            {
            "docs": [
                {
                "url": "kinopoisk.ru/film/301",
                "i_available_online": {
                    "value": 1,
                    "type": "#pig"
                },
                "s_original_title": {
                    "value": "The Matrix",
                    "type": "#plh"
                },
                "s_document_type": {
                    "value": "FILM",
                    "type": "#plh"
                },
                "s_service_region_subscription": [
                    {
                    "value": "77_159_YA_PLUS_3M",
                    "type": "#plh"
                    },
                    {
                    "value": "3_225_YA_PLUS",
                    "type": "#plh"
                    },
                    {
                    "value": "10_225_YA_PLUS",
                    "type": "#plh"
                    }
                ],
                "i_has_image": {
                    "value": 1,
                    "type": "#pig"
                },
                "z_original_title": {
                    "value": "The Matrix",
                    "type": "#z"
                },
                "z_title": {
                    "value": "Матрица",
                    "type": "#z"
                },
                "s_age_restriction": {
                    "value": "under_16",
                    "type": "#plh"
                },
                "z_year": {
                    "value": "1999",
                    "type": "#z"
                },
                "i_view": {
                    "value": 1,
                    "type": "#pig"
                },
                "i_kp_rating": {
                    "value": 8489,
                    "type": "#pig"
                },
                "i_film_id": {
                    "value": 301,
                    "type": "#pig"
                },
                "z_alternate_titles": [
                    {
                    "value": "Matrikss",
                    "type": "#z"
                    },
                    {
                    "value": "Matrice, La",
                    "type": "#z"
                    },
                    {
                    "value": "The Matrix",
                    "type": "#z"
                    }
                ],
                "i_kp_top_rating": {
                    "value": 23,
                    "type": "#pig"
                },
                "i_votes": {
                    "value": 505811,
                    "type": "#pig"
                },
                "s_service_region_exclregion": [
                    {
                    "value": "167_159_",
                    "type": "#plh"
                    }
                ],
                "s_type": {
                    "value": "film",
                    "type": "#plh"
                },
                "i_year": {
                    "value": 1999,
                    "type": "#pig"
                },
                "i_searchable": {
                    "value": 1,
                    "type": "#pig"
                },
                "s_poster_mds_base_url": {
                    "value": "https://avatars.mds.yandex.net/get-kinopoisk-image/1704946/eed1de3a-5400-43b3-839e-22490389bf54",
                    "type": "#plh"
                },
                "s_title": {
                    "value": "Матрица",
                    "type": "#plh"
                },
                "options": {
                    "modification_timestamp": "1617804267"
                }
                }
            ],
            "action": "modify",
            "prefix": 1
            }
        )"),
        
        TStringBuf(R"(
            {
            "docs": [
                {
                "url": "kinopoisk.ru/film/333",
                "z_title": {
                    "value": "Звёздные войны: Эпизод 4 — Новая надежда",
                    "type": "#z"
                },
                "i_available_online": {
                    "value": 1,
                    "type": "#pig"
                },
                "s_document_type": {
                    "value": "FILM",
                    "type": "#plh"
                },
                "i_has_image": {
                    "value": 1,
                    "type": "#pig"
                },
                "z_original_title": {
                    "value": "Star Wars",
                    "type": "#z"
                },
                "i_kp_rating": {
                    "value": 8107,
                    "type": "#pig"
                },
                "s_service_region_subscription": [
                    {
                    "value": "10_169_",
                    "type": "#plh"
                    },
                    {
                    "value": "200_171_",
                    "type": "#plh"
                    },
                    {
                    "value": "167_159_",
                    "type": "#plh"
                    }
                ],
                "s_service_region_exclregion": [
                    {
                    "value": "10_169_",
                    "type": "#plh"
                    },
                    {
                    "value": "167_159_",
                    "type": "#plh"
                    },
                    {
                    "value": "200_171_",
                    "type": "#plh"
                    }
                ],
                "z_year": {
                    "value": "1977",
                    "type": "#z"
                },
                "i_year": {
                    "value": 1977,
                    "type": "#pig"
                },
                "z_alternate_titles": [
                    {
                    "value": "звездные войны: эпизод четыре - новая надежда",
                    "type": "#z"
                    },
                    {
                    "value": "Ratovi zvezda - Nova nada",
                    "type": "#z"
                    },
                    {
                    "value": "Star Wars: Epizoda 4 - Nova nadeje",
                    "type": "#z"
                    }
                ],
                "i_kp_top_rating": {
                    "value": 286,
                    "type": "#pig"
                },
                "i_view": {
                    "value": 1,
                    "type": "#pig"
                },
                "i_film_id": {
                    "value": 333,
                    "type": "#pig"
                },
                "s_type": {
                    "value": "film",
                    "type": "#plh"
                },
                "s_poster_mds_base_url": {
                    "value": "https://avatars.mds.yandex.net/get-kinopoisk-image/1600647/9bdc6690-de82-4a8c-a114-aa3a353bc1da",
                    "type": "#plh"
                },
                "s_title": {
                    "value": "Звёздные войны: Эпизод 4 — Новая надежда",
                    "type": "#plh"
                },
                "i_votes": {
                    "value": 222589,
                    "type": "#pig"
                },
                "i_searchable": {
                    "value": 1,
                    "type": "#pig"
                },
                "s_original_title": {
                    "value": "Star Wars",
                    "type": "#plh"
                },
                "s_age_restriction": {
                    "value": "none",
                    "type": "#plh"
                },
                "options": {
                    "modification_timestamp": "1617804326"
                }
                }
            ],
            "action": "modify",
            "prefix": 1
            }

        )")
    };
    constexpr int MovieDocsCount = 3;
    constexpr int MovieDocsKeyPrefix = 0;
}

START_TEST_DEFINE(TestKinopoiskFilterOutWatchedMovies)
private:

    TVector<NRTYServer::TMessage> PrepareMessages(const TStringBuf* sourceDocs, const int numDocs, const int keyPrefix) {
        TVector<NRTYServer::TMessage> messages;
        messages.reserve(numDocs);
        for (int i = 0; i < numDocs; i++) {
            const auto& doc = sourceDocs[i];
            messages.push_back(NSaas::TAction().ParseFromJson(NJson::ReadJsonFastTree(doc)).ToProtobuf());
            messages.back().MutableDocument()->SetKeyPrefix(keyPrefix);
        }
        return messages;
    };

public:

    bool Run() override {
        CHECK_TEST_TRUE(HasSearchproxy());

        auto movieMessages = PrepareMessages(MovieDocs, MovieDocsCount, MovieDocsKeyPrefix);
        IndexMessages(movieMessages, DISK, 1);
        ReopenIndexers();
        CheckSearchResults(movieMessages);

        TVector<TDocSearchInfo> results;
        const TString kps = GetAllKps(movieMessages);

        QuerySearch("s_document_type%3AFILM&kps=0", results);
        CHECK_TEST_FAILED(results.size() != MovieDocsCount, "Results size must be the same as indexed messages");

        // case 1
        results.clear();
        QuerySearch("s_document_type%3AFILM&kps=0&bu=kinopoisk.ru/film/666", results);
        CHECK_TEST_FAILED(results.size() != MovieDocsCount - 1, "[1] Results size must be one less than the count of indexed messages");

        // case 2
        // kinopoisk.ru/film/666
        results.clear();
        QuerySearch("s_document_type%3AFILM&kps=0&buproto=CAASKBcAAAAAAAAAKLUv/SAXuQAAChVraW5vcG9pc2sucnUvZmlsbS82NjY=", results);
        CHECK_TEST_FAILED(results.size() != MovieDocsCount - 1, "[2] Results size must be one less than the count of indexed messages");

        // case 3
        // kinopoisk.ru/film/666,kinopoisk.ru/film/777
        results.clear();
        QuerySearch("s_document_type%3AFILM&kps=0&buproto=CAASMS4AAAAAAAAAKLUv/SAuBQEA0AoVa2lub3BvaXNrLnJ1L2ZpbG0vNjY2Nzc3AQCVnk0=", results);
        CHECK_TEST_FAILED(results.size() != MovieDocsCount - 1, "[3] Results size must be one less than the count of indexed messages");

        // case 4
        // kinopoisk.ru/film/666,kinopoisk.ru/film/301
        results.clear();
        QuerySearch("s_document_type%3AFILM&kps=0&buproto=CAASMS4AAAAAAAAAKLUv/SAuBQEA0AoVa2lub3BvaXNrLnJ1L2ZpbG0vNjY2MzAxAQCVnk0=", results);
        CHECK_TEST_FAILED(results.size() != MovieDocsCount - 2, "[4] Results size must be two less than the count of indexed messages");

        return true;
    }
};
