#include <library/cpp/json/json_reader.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <balancer/serval/core/config.h>
#include <balancer/serval/core/unistat.h>

Y_UNIT_TEST_SUITE(Config) {
    Y_UNIT_TEST(Cycle) {
        NSv::TAuxData aux;
        aux.AddAction("a", YAML::Load("[b]"));
        aux.AddAction("b", YAML::Load("[a]"));
        aux.AddAction("c", YAML::Load("[a]"));
        UNIT_ASSERT_EXCEPTION(aux.Action(YAML::Load("c")), YAML::Exception);
    }

    Y_UNIT_TEST(SignalAggregation) {
        NSv::TAuxData aux;
        aux.AddAction("a", YAML::Load("[]"));
        aux.AddAction("b", YAML::Load("[!x a, !x a]"));
        NSv::IStreamPtr ctx;
        aux.Action(YAML::Load("b"))(ctx);
        NJson::TJsonValue value;
        NJson::ReadJsonTree(NSv::SerializeSignals(aux.Signals()), &value);
        size_t aRequests = -1;
        size_t xRequests = -1;
        UNIT_ASSERT(value.IsArray());
        for (const auto& item : value.GetArray()) {
            UNIT_ASSERT(item.IsArray());
            UNIT_ASSERT(item.GetArray().size() == 2);
            UNIT_ASSERT(item.GetArray()[0].IsString());
            if (item.GetArray()[0].GetString() == "a-requests_dmmm")
                aRequests = item.GetArray()[1].GetUInteger();
            if (item.GetArray()[0].GetString() == "b-x-requests_dmmm")
                xRequests = item.GetArray()[1].GetUInteger();
        }
        UNIT_ASSERT_VALUES_EQUAL(aRequests, 2);
        UNIT_ASSERT_VALUES_EQUAL(xRequests, 2);
    }

#define PATCH_TEST(name, a, p, b) Y_UNIT_TEST(Patch##name) { \
    auto __a = NSv::PatchYAML(YAML::Load(a), YAML::Load(p)); \
    auto __b = YAML::Load(b); \
    UNIT_ASSERT_C(NSv::EqYAML(__a, __b), __a << " != " << __b); \
}

    PATCH_TEST(MapAdd, "{a: b, c: d}", "!add e: f", "{a: b, c: d, e: f}");
    PATCH_TEST(MapSet, "{a: b, c: d}", "!set a: q", "{a: q, c: d}");
    PATCH_TEST(MapDel, "{a: b, c: d}", "!del c", "{a: b}");
    PATCH_TEST(MapMod, "{a: {b: c}}", "!mod a: {!set b: d}", "{a: {b: d}}");
    PATCH_TEST(SeqAdd, "[a]", "!add b", "[a, b]");
    PATCH_TEST(MapSeqAdd, "[{a: b}]", "!add c: d", "[{a: b}, {c: d}]");
    PATCH_TEST(MapSeqSet, "[{a: b, c: d}]", "!set a: e", "[{a: e}]");
    PATCH_TEST(MapSeqSetArg, "[{a: b, c: d}]", "!set-arg a: e", "[{a: e, c: d}]");
    PATCH_TEST(MapSeqMod, "[{a: b, c: d}]", "!mod a: {!add e: f}", "[{a: b, c: d, e: f}]");
    PATCH_TEST(MapSeqModArg, "[{a: [b], c: d}]", "!mod-arg a: [!add e]", "[{a: [b, e], c: d}]");
    // TODO !del in lists
}
