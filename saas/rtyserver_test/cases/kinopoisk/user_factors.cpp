#include <saas/rtyserver_test/cases/indexer/ann.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

namespace {

    constexpr TStringBuf KPUSERDATA = "ChMIARIPCAEQARgJJTMzMz8oATABCgQIAxIACggIAhIEEAEoAA==";

    using TExpectedFactors = THashMap<TString, float>;

    const static TVector<TExpectedFactors> EXPECTED_FACTORS = {
        {
            {"KpAwaiting", 1},
            {"KpWatched", 1},
            {"KpVote", 9},
            {"KpRecency", 0.7},
            {"KpToWatch", 1},
            {"KpWillWatch", 1}
        },
        {
            {"KpAwaiting", 0},
            {"KpWatched", 1},
            {"KpVote", 0},
            {"KpRecency", 0},
            {"KpToWatch", 0},
            {"KpWillWatch", 0}
        },
        {
            {"KpAwaiting", 0},
            {"KpWatched", 0},
            {"KpVote", 0},
            {"KpRecency", 0},
            {"KpToWatch", 0},
            {"KpWillWatch", 0}
        },
        {
            {"KpAwaiting", 0},
            {"KpWatched", 0},
            {"KpVote", 0},
            {"KpRecency", 0},
            {"KpToWatch", 0},
            {"KpWillWatch", 0}
        }
    };

}
    
START_TEST_DEFINE(TestKinopoiskUserFactors)
private:
    using TFactors = NSaas::TDocFactorsView;
public:
    TVector<TFactors> ReadFactors(const TString& query, int line) {
        DEBUG_LOG << "L" << line <<" Query: " << query << Endl;
        return ReadFactors(query);
    }

    TVector<TFactors> ReadFactors(const TString& query) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;
        QuerySearch(query, results, &resultProps);
        TVector<TFactors> factors;
        for (const auto& props : resultProps) {
            auto& docFactors = factors.emplace_back();
            docFactors.AssignFromSearchResult(*props);
            docFactors.DebugPrint();
        }
        return factors;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        bool prefixed = GetIsPrefixed();
        {
            NSaas::TAction action;
            action.SetPrefix(prefixed ? 1 : 0);
            action.SetActionType(NSaas::TAction::atModify);
            auto& doc = action.GetDocument();
            doc.SetUrl("kinopoisk.ru/film/1");
            doc.AddFactor("static_factor", 4.0);
            doc.AddZone("z_title").SetText("мстители");
            doc.AddZone("z_original_title").SetText("the avengers");
            doc.AddAttribute("i_film_id").AddValue(1).AddType(NSaas::TAttributeValue::avtGrp);
            messages.push_back(action.ToProtobuf());
        }
        {
            NSaas::TAction action;
            action.SetPrefix(prefixed ? 1 : 0);
            action.SetActionType(NSaas::TAction::atModify);
            auto& doc = action.GetDocument();
            doc.SetUrl("kinopoisk.ru/film/2");
            doc.AddFactor("static_factor", 3.0);
            doc.AddZone("z_title").SetText("мстители финал");
            doc.AddZone("z_original_title").SetText("avengers endgame");
            doc.AddAttribute("i_film_id").AddValue(2).AddType(NSaas::TAttributeValue::avtGrp);
            messages.push_back(action.ToProtobuf());
        }
        {
            NSaas::TAction action;
            action.SetPrefix(prefixed ? 1 : 0);
            action.SetActionType(NSaas::TAction::atModify);
            auto& doc = action.GetDocument();
            doc.SetUrl("kinopoisk.ru/film/3");
            doc.AddFactor("static_factor", 2.0);
            doc.AddZone("z_title").SetText("мстители война бесконечности");
            doc.AddZone("z_original_title").SetText("avengers infinite war");
            doc.AddAttribute("i_film_id").AddValue(3).AddType(NSaas::TAttributeValue::avtGrp);
            messages.push_back(action.ToProtobuf());
        }
        {
            NSaas::TAction action;
            action.SetPrefix(prefixed ? 1 : 0);
            action.SetActionType(NSaas::TAction::atModify);
            auto& doc = action.GetDocument();
            doc.SetUrl("kinopoisk.ru/film/4");
            doc.AddZone("z_title").SetText("мстители эра альтрона");
            doc.AddZone("z_original_title").SetText("avengers age of ultron");
            doc.AddAttribute("i_film_id").AddValue(4).AddType(NSaas::TAttributeValue::avtGrp);
            messages.push_back(action.ToProtobuf());
        }

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        auto factors = ReadFactors(
            "мстители&relev=relevgeo%3D225&dbgrlv=da&relev=all_factors&fsgta=_JsonFactors"
                + KpUserData()
                + GetAllKps(messages),
            __LINE__
        );
        CHECK_TEST_FAILED(factors.size() != EXPECTED_FACTORS.size(), "Did not find enough documents");
        for (size_t i = 0; i < factors.size(); i++) {
            DEBUG_LOG << "Checking document #" << i << Endl;
            for (const auto& [factorName, expectedValue]: EXPECTED_FACTORS[i]) {
                factors[i].CheckFactor(factorName, expectedValue);
            }
        }
            
        return true;
    }
private:
    TString KpUserData() {
        return Sprintf(
            "&relev=kp_user_data=%s;store=KpUserData:kp_parse_userdata(base64_decode(get_relev(\"kp_user_data\")))"
            ";calc=KpWatched:kp_watched(#group_i_film_id,\"KpUserData\")"
            ";calc=KpAwaiting:kp_awaiting(#group_i_film_id,\"KpUserData\")"
            ";calc=KpRecency:kp_recency(#group_i_film_id,\"KpUserData\")"
            ";calc=KpVote:kp_vote(#group_i_film_id,\"KpUserData\")"
            ";calc=KpToWatch:kp_to_watch(#group_i_film_id,\"KpUserData\")"
            ";calc=KpWillWatch:kp_will_watch(#group_i_film_id,\"KpUserData\")",
            KPUSERDATA.data());
    }
};
