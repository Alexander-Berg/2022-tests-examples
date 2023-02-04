#include <saas/rtyserver_test/cases/indexer/ann.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

namespace {

    using TExpectedFactors = THashMap<TString, float>;

    const static TVector<TExpectedFactors> EXPECTED_FACTORS = {
        {
            {"KpPrefixMatch", 1}
        },
        {
            {"KpPrefixMatch", 0}
        },
        {
            {"KpPrefixMatch", 1}
        },
        {
            {"KpPrefixMatch", 0}
        },
        {
            {"KpPrefixMatch", 1}
        },
        {
            {"KpPrefixMatch", 0}
        }
    };

}
    
START_TEST_DEFINE(TestKinopoiskPrefixMatchFactor)
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
            doc.AddZone("z_title").SetText("Мстители Вовремя скрыться");
            doc.AddAttribute("s_title").AddValue("Мстители Вовремя скрыться").AddType(NSaas::TAttributeValue::avtGrpLit);
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
            doc.AddZone("z_misc").SetText("вот");
            doc.AddAttribute("s_title").AddValue("мстители финал").AddType(NSaas::TAttributeValue::avtGrpLit);
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
            doc.AddAttribute("s_title").AddValue("мстители война бесконечности").AddType(NSaas::TAttributeValue::avtGrpLit);
            messages.push_back(action.ToProtobuf());
        }
        {
            NSaas::TAction action;
            action.SetPrefix(prefixed ? 1 : 0);
            action.SetActionType(NSaas::TAction::atModify);
            auto& doc = action.GetDocument();
            doc.SetUrl("kinopoisk.ru/film/4");
            doc.AddFactor("static_factor", 1.0);
            doc.AddZone("z_title").SetText("мстители эра альтрона");
            doc.AddZone("z_misc").SetText("во");
            messages.push_back(action.ToProtobuf());
        }
        {
            NSaas::TAction action;
            action.SetPrefix(prefixed ? 1 : 0);
            action.SetActionType(NSaas::TAction::atModify);
            auto& doc = action.GetDocument();
            doc.SetUrl("kinopoisk.ru/film/5");
            doc.AddZone("z_title").SetText("мстители во");
            doc.AddAttribute("s_title").AddValue("мстители во").AddType(NSaas::TAttributeValue::avtGrpLit);
            messages.push_back(action.ToProtobuf());
        }
        {
            NSaas::TAction action;
            action.SetPrefix(prefixed ? 1 : 0);
            action.SetActionType(NSaas::TAction::atModify);
            auto& doc = action.GetDocument();
            doc.SetUrl("kinopoisk.ru/film/6");
            doc.AddFactor("static_factor", -1.0);
            doc.AddZone("z_title").SetText("мстители");
            doc.AddAttribute("s_title").AddValue("мстители").AddType(NSaas::TAttributeValue::avtGrpLit);
            doc.AddZone("z_misc").SetText("во");
            messages.push_back(action.ToProtobuf());
        }

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        auto factors = ReadFactors(
            "мстители во&template=%request%*&relev=relevgeo%3D225&dbgrlv=da&relev=all_factors&fsgta=_JsonFactors"
            "&relev=calc=KpPrefixMatch:is_prefix_lowercase(#group_s_title)",
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
};
