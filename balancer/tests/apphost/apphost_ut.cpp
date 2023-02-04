#include <library/cpp/json/json_reader.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <balancer/serval/tests/helpers.h>
#include <balancer/serval/core/buffer.h>
#include <balancer/serval/core/unistat.h>
#include <balancer/serval/mod/apphost/stream.h>

#include <apphost/lib/compression/compression.h>
#include <apphost/lib/compression/compression_codecs.h>
#include <apphost/lib/proto_answers/http.pb.h>

static NSv::TThreadLocalRoot TLSRoot;

static TVector<size_t> GetRequestCounts(const NSv::TAuxData& aux, const TVector<TString>& names) {
    TVector<size_t> ret(names.size());
    NJson::TJsonValue value;
    NJson::ReadJsonTree(NSv::SerializeSignals(aux.Signals()), &value);
    for (const auto& item : value.GetArray())
        for (size_t i = 0; i < names.size(); i++)
            if (item.GetArray()[0].GetString() == "-" + names[i] + "-visits_dmmm")
                ret[i] = item.GetArray()[1].GetUInteger();
    return ret;
}

static void ExpectResponse(NSv::NAppHost::TResponse& rsp, TStringBuf expectText, bool keepOrder = false) {
    if (!keepOrder)
        std::stable_sort(rsp.MutableAnswers()->begin(), rsp.MutableAnswers()->end(),
            [](auto& a, auto& b) { return a.GetType() < b.GetType(); });
    NJson::TJsonValue expect;
    NJson::ReadJsonFastTree(expectText, &expect);
    NJson::TJsonValue decoded;
    for (const auto& answer : rsp.GetAnswers()) {
        NSv::NAppHost::DecodeJson(answer, [&](NJson::TJsonValue&& value) {
            value["type"] = answer.GetType();
            decoded.AppendValue(std::move(value));
        });
    }
    UNIT_ASSERT_VALUES_EQUAL(decoded, expect);
}

static void ExpectResponse(TStringBuf config, TStringBuf expectText, bool keepOrder = false) {
    ServerClient(ExpectError(EREQDONE, FromConfig(YAML::Load(static_cast<std::string>(config)))), [&](NSv::IConnection& c) {
        auto req = c.Request({"POST", "/"});
        UNIT_ASSERT(req);
        auto data = NSv::ReadFrom(*req);
        UNIT_ASSERT(data);
        NSv::NAppHost::TResponse rsp;
        UNIT_ASSERT(rsp.ParseFromArray(data->begin(), data->size()));
        ExpectResponse(rsp, expectText, keepOrder);
    });
}

SV_DEFINE_ACTION("action", [](const YAML::Node& args, NSv::TAuxData& aux) {
    CHECK_NODE(args, args.IsMap(), "an argument is required");
    return aux.Action(args.begin()->second);
});

Y_UNIT_TEST_SUITE(Apphost) {
    Y_UNIT_TEST(ParseOK) {
        auto mod = FromConfig(YAML::Load(R"(
            apphost:
            - RESPONSE: [A, B]
            - A: [!x {value: y}]
            - B: [!x {value: z}]
        )"));
    }

    Y_UNIT_TEST(Redundant) {
        UNIT_ASSERT_EXCEPTION(FromConfig(YAML::Load(R"(
            apphost:
            - RESPONSE: [A]
            - A: [!x {value: y}]
            - B: [!x {value: z}]
        )")), YAML::Exception); // B not used for RESPONSE
    }

    Y_UNIT_TEST(Cycle) {
        UNIT_ASSERT_EXCEPTION(FromConfig(YAML::Load(R"(
            apphost:
            - RESPONSE: [A, B]
            - A: [!x {value: y}, B]
            - B: [!x {value: z}, A]
        )")), YAML::Exception); // A -> B -> A
    }

    Y_UNIT_TEST(ResponseInput) {
        UNIT_ASSERT_EXCEPTION(FromConfig(YAML::Load(R"(
            apphost:
            - RESPONSE: [A]
            - A: [!x {value: y}]
            - B: [!x {value: z}, RESPONSE]
        )")), YAML::Exception); // B not used for RESPONSE
    }

    Y_UNIT_TEST(Culling) {
        auto mod = FromConfig(YAML::Load(R"(
            ! apphost:
            - RESPONSE:
              - C: A[nonexisting]
              - D: A[x]
            - A: [!a {}]
              apphost:
              - RESPONSE: [!x {value: y}]
            - B: [A]
              apphost:
              - RESPONSE: [!x {value: z}]
            - C: [B]
              apphost:
              - RESPONSE: [!x {value: w}]
            - D: [A]
              apphost:
              - RESPONSE: [!x {value: u}]
        )"));
        ServerClient(ExpectError(EREQDONE, mod), [](NSv::IConnection& c) {
            UNIT_ASSERT(c.Request({"POST", "/"}));
            UNIT_ASSERT(cone::yield() && cone::yield() && cone::yield() && cone::yield());
        });
        auto cs = GetRequestCounts(mod.second, {"A", "B", "C", "D"});
        UNIT_ASSERT_VALUES_EQUAL(cs[0], 1);
        UNIT_ASSERT_VALUES_EQUAL(cs[1], 0); // disabled by edge expressions before being visited
        UNIT_ASSERT_VALUES_EQUAL(cs[2], 0);
        UNIT_ASSERT_VALUES_EQUAL(cs[3], 1);
    }

    Y_UNIT_TEST(SATSolving) {
        auto a = cone::time::clock::now();
        ExpectResponse(R"(
            apphost:
            - RESPONSE:
              - B
              - C: "B[x]"
              - D: "!B[x]"
            - A: [!a {}]
              delay: 1ms
            - B: [!x {value: b}, A]
            - C: [!x {value: c}]
            - D: [!a {}]
              delay: 5s
        )", R"([{"type": "x", "value": "b"}, {"type": "x", "value": "c"}])");
        // A, C, and D are started right away. 1 ms later, A completes and B is started.
        // When B completes (right away), the flags force D->RESPONSE to become false,
        // so RESPONSE is started; when it finishes, D is cancelled.
        UNIT_ASSERT(cone::time::clock::now() - a < std::chrono::seconds(3));
    }

    Y_UNIT_TEST(Skipping) {
        auto a = cone::time::clock::now();
        ExpectResponse(R"(
            apphost:
            - RESPONSE:
              - !app_host_params {srcskip: [B]}
              apphost:
              - RESPONSE: [C, {A: "C[y]"}]
              - A: [!a {}]
                delay: 5s
              - B: [A]
              - C: [B, !x {value: c}]
        )", R"([{"type": "x", "value": "c"}])");
        // A -> B -> C -> RESPONSE, but srcskip disables B, so C is started before A finishes
        // (and then disables the A->RESPONSE edge, terminating the graph; see test above).
        UNIT_ASSERT(cone::time::clock::now() - a < std::chrono::seconds(3));
    }

    Y_UNIT_TEST(TypeFilters) {
        ExpectResponse(R"(
            apphost:
            - RESPONSE:
              - A
              - B@x,x->y
              - C@-x,-y
              - ^D@x
              - $D@x
            - A: [!x {value: ax}, !y {value: ay}]
            - B: [!x {value: bx}, !y {value: by}]
            - C: [!x {value: cx}, !y {value: cy}]
            - D: [!x {value: dx}, !x {value: dy}, !x {value: dz}]
        )", R"([
            {"type": "x", "value": "ax"},
            {"type": "x", "value": "bx"},
            {"type": "x", "value": "dx"},
            {"type": "x", "value": "dz"},
            {"type": "y", "value": "ay"},
            {"type": "y", "value": "bx"}
        ])");
    }

    Y_UNIT_TEST(EdgeExpressions) {
        ExpectResponse(R"(
            apphost:
            - RESPONSE:
              - A: "Z[a]"
              - B: "Z[a] && !Z[b]"
              - C: "(Z[a] && !Z[b]) || Z[c]"
              - D: "Z[b] ? Z[d] : Z[c]"
              - E: "Z[b] ? Z[c] : Z[d]"
              - F: "false"
              - G: "!false"
              - Z@null
            - A: [!a {}]
            - B: [!b {}]
            - C: [!c {}]
            - D: [!d {}]
            - E: [!e {}]
            - F: [!f {}]
            - G: [!g {}]
            - Z: [A, B, C]
        )", R"([{"type": "a"}, {"type": "c"}, {"type": "e"}, {"type": "g"}])");
    }

    Y_UNIT_TEST(Retry503) {
        cone::guard backend = RunBackend(19471, [](NSv::IStreamPtr s) {
            return s->WriteHead(503) && s->Close();
        });
        UNIT_ASSERT(backend);
        ExpectResponse(R"(
            apphost:
            - RESPONSE: [!a {}]
              proxy:
              - inf: http://[::1]:19471
              - apphost: [{RESPONSE: [!a {}]}]
              attempt-delay: inf
        )", R"([{"type": "a"}])");
    }

    UT_ACTION("echo") {
        NSv::NAppHost::TInterface ah = req;
        NSv::NAppHost::TResponse output;
        // TODO support out-streaming and replace this with a loop.
        auto input = ah.ReadAll();
        if (!input MUN_RETHROW)
            return false;
        *output.MutableAnswers() = input->GetAnswers();
        return ah.Write(std::move(output)) && ah.Close();
    }

    Y_UNIT_TEST(StreamingOff) {
        // Without streaming, items with same type are sent in the specified order.
        ExpectResponse(R"(
            apphost:
            - RESPONSE: [C]
            - A: [!a {n: 1}]
              action:
              - delay: 1ms
              - echo
            - B: [!a {n: 2}]
            - C: [A, B]
              echo: .
        )", R"([{"type": "a", "n": 1}, {"type": "a", "n": 2}])");
    }

    Y_UNIT_TEST(StreamingOn) {
        // With streaming, the one that completes first is sent first.
        ExpectResponse(R"(
            apphost:
            - RESPONSE: [C]
            - A: [!a {n: 1}]
              action:
              - delay: 1ms
              - echo
            - B: [!a {n: 2}]
            - C: [A, B]
              echo: .
              streaming: true
        )", R"([{"type": "a", "n": 2}, {"type": "a", "n": 1}])");
    }

    Y_UNIT_TEST(StreamDirectEmbed) {
        // If a streaming node has a direct embed, it is sent when the graph is started.
        ExpectResponse(R"(
            apphost:
            - RESPONSE: [A]
            - A: [!a {}]
              echo: .
              streaming: true
        )", R"([{"type": "a"}])");
    }

    Y_UNIT_TEST(SubgraphStreamingOff) {
        // Same as above, but this time the buffering point is when making a request to a subgraph.
        ExpectResponse(R"(
            apphost:
            - RESPONSE: [C]
            - A: [!a {n: 1}]
              action:
              - delay: 1ms
              - echo
            - B: [!a {n: 2}]
            - C: [A, B]
              apphost:
              - A
              - B
              - RESPONSE: [A, B]
                echo: .
                streaming: true
        )", R"([{"type": "a", "n": 1}, {"type": "a", "n": 2}])");
    }

    Y_UNIT_TEST(SubgraphStreamingOn) {
        ExpectResponse(R"(
            apphost:
            - RESPONSE: [C]
            - A: [!a {n: 1}]
              action:
              - delay: 1ms
              - echo
            - B: [!a {n: 2}]
            - C: [A, B]
              apphost:
              - A
              - B
              - RESPONSE: [A, B]
                echo: .
                streaming: true
              streaming: true
        )", R"([{"type": "a", "n": 2}, {"type": "a", "n": 1}])");
    }

    Y_UNIT_TEST(ResponseStreamingOff) {
        auto mod = FromConfig(YAML::Load(R"(
            apphost:
            - RESPONSE: [A, B]
            - A: [!a {}]
              action:
              - delay: 1ms
              - echo
            - B: [A, !b {}]
              action:
              - delay: 1ms
              - echo
        )"));
        ServerClient(ExpectError(EREQDONE, mod), [](NSv::IConnection& c) {
            auto rsp = c.Request({"POST", "/NAppHostProtocol.TServant/InvokeEx", {
                {":authority", "unknown"},
                {":scheme", "http"},
                {"content-type", "application/grpc"},
                {"te", "trailers"}}});
            auto rsh = rsp->Head();
            UNIT_ASSERT(rsh);
            auto ch1 = NSv::NAppHost::DecodeChunk<NSv::NAppHost::TResponse>(*rsp, true);
            UNIT_ASSERT(ch1);
            ExpectResponse(*ch1, R"([{"type": "a"}, {"type": "a"}, {"type": "b"}])");
            auto ch2 = NSv::NAppHost::DecodeChunk<NSv::NAppHost::TResponse>(*rsp, true);
            UNIT_ASSERT(ch2);
            UNIT_ASSERT(ch2->ByteSizeLong() == 0);
        });
    }

    Y_UNIT_TEST(ResponseStreamingOn) {
        auto mod = FromConfig(YAML::Load(R"(
            apphost:
            - RESPONSE: [A, B]
              streaming: true
            - A: [!a {}]
              action:
              - delay: 1ms
              - echo
            - B: [A, !b {}]
              action:
              - delay: 1ms
              - echo
        )"));
        ServerClient(ExpectError(EREQDONE, mod), [](NSv::IConnection& c) {
            auto rsp = c.Request({"POST", "/NAppHostProtocol.TServant/InvokeEx", {
                {":authority", "unknown"},
                {":scheme", "http"},
                {"content-type", "application/grpc"},
                {"te", "trailers"}}});
            auto rsh = rsp->Head();
            UNIT_ASSERT(rsh);
            auto ch1 = NSv::NAppHost::DecodeChunk<NSv::NAppHost::TResponse>(*rsp, true);
            UNIT_ASSERT(ch1);
            ExpectResponse(*ch1, R"([{"type": "a"}])");
            auto ch2 = NSv::NAppHost::DecodeChunk<NSv::NAppHost::TResponse>(*rsp, true);
            UNIT_ASSERT(ch2);
            ExpectResponse(*ch2, R"([{"type": "a"}, {"type": "b"}])");
            auto ch3 = NSv::NAppHost::DecodeChunk<NSv::NAppHost::TResponse>(*rsp, true);
            UNIT_ASSERT(ch3);
            UNIT_ASSERT(ch3->ByteSizeLong() == 0);
        });
    }

#if 0
    Y_UNIT_TEST(StreamingRenamed) {
        // Request to C arrives in two chunks: the first contains A, the second B.
        // But B is renamed to A, so they both end up added to the same input node.
        // Completion of A must not be marked in the first chunk.
        ExpectResponse(R"(
            apphost:
            - RESPONSE: [C]
            - A: [!a {}]
            - B: [!b {}]
              action:
              - delay: 1ms
              - echo
            - C: [A, B->A]
              apphost:
              - A
              - RESPONSE: [A]
              streaming: true
        )", R"([{"type": "a"}, {"type": "b"}])");
    }
#endif

    Y_UNIT_TEST(Hashing) {
        auto mod = FromConfig(YAML::Load(R"(
            ! apphost:
            - RESPONSE: [B]
            - A: [!a {"x": "y"}, !b {"x": [{"y": 1}]}]
              apphost-hasher:
                B: [[a, x], [b, x, 0, y]]
            - B: [A, !b {}]
              ! proxy:
              - ! apphost:
                - RESPONSE: [X]
                - X: [!c {}]
              - ! apphost:
                - RESPONSE: [Y]
                - Y: [!d {}]
              hash-by: x-apphost-hint
        )"));
        ServerClient(ExpectError(EREQDONE, mod), [](NSv::IConnection& c) {
            for (size_t i = 0; i < 100; i++)
                UNIT_ASSERT(c.Request({"POST", "/"}));
            UNIT_ASSERT(cone::yield() && cone::yield() && cone::yield() && cone::yield());
        });
        auto cs = GetRequestCounts(mod.second, {"B-X", "B-Y", "B"});
        UNIT_ASSERT_C(cs[0] + cs[1] == cs[2] && (cs[0] == 0 || cs[1] == 0), cs[0] << "/" << cs[1] << "/" << cs[2]);
    }

    UT_ACTION("apphost-to-http-backends") {
        auto rqh = req->Head();
        if (!rqh MUN_RETHROW)
            return false;
        auto payload = NSv::ReadFrom(*req);
        if (!payload MUN_RETHROW)
            return false;
        UNIT_ASSERT_VALUES_EQUAL(rqh->Method, "POST");
        UNIT_ASSERT_VALUES_EQUAL(rqh->PathWithQuery, "/path?query");
        UNIT_ASSERT_VALUES_EQUAL(payload, "payload");
        auto header = rqh->find("x-req-header");
        UNIT_ASSERT(header != rqh->end());
        UNIT_ASSERT_VALUES_EQUAL(header->second, "X-Header-Value");
        return req->WriteHead({200, {{"x-rsp-header", "RESPONSE"}}}) && req->Write("rsp-payload") && req->Close();
    }

    UT_ACTION("http-response-as-json") {
        NSv::NAppHost::TInterface ah = req;
        NSv::NAppHost::TResponse output;
        NAppHostHttp::THttpResponse hrsp;
        auto input = ah.ReadAll();
        if (!input MUN_RETHROW)
            return false;
        for (const auto& answer : input->GetAnswers()) {
            UNIT_ASSERT_VALUES_EQUAL(answer.GetType(), "http_response");
            TString data = NAppHost::NCompression::Decode(answer.GetData());
            TStringBuf view = data;
            UNIT_ASSERT(view.SkipPrefix("p_"));
            UNIT_ASSERT(hrsp.ParseFromArray(view.data(), view.size()));
            NJson::TJsonValue val;
            val["code"] = hrsp.GetStatusCode();
            val["data"] = hrsp.GetContent();
            for (const auto& header : hrsp.GetHeaders()) {
                NJson::TJsonValue hdr;
                hdr["k"] = header.GetName();
                hdr["v"] = header.GetValue();
                val["headers"].AppendValue(std::move(hdr));
            }
            auto item = output.AddAnswers();
            item->SetType("json_response");
            item->SetData(NAppHost::NCompression::Encode(ToString(val), NAppHost::NCompression::TCodecs::Default));
        }
        return ah.Write(std::move(output)) && ah.Close();
    }

    Y_UNIT_TEST(ApphostToHTTP) {
        ExpectResponse(R"(
            apphost:
            - RESPONSE: [B]
            - A:
              - !proto!http_request
                1: 1 # method: post
                2: 1 # scheme: http
                3: "query"
                4: [{1: X-Req-Header, 2: X-Header-Value}]
                5: "payload"
              apphost-to-http: apphost-to-http-backends
              path: /path?
            - B: [A]
              http-response-as-json: .
        )", R"([{"type": "json_response", "code": 200, "data": "rsp-payload", "headers": [{"k": "x-rsp-header", "v": "RESPONSE"}]}])");
    }

    Y_UNIT_TEST(HTTPToApphost) {
        auto mod = FromConfig(YAML::Load(R"(
            - http-to-apphost
            - apphost:
              - HTTP_REQUEST
              - RESPONSE: [A, B]
                streaming: true
              - A: [!http_response {status_code: 201}, HTTP_REQUEST@http_request->http_response]
                action:
                - delay: 1ms
                - echo
              - B: [A@nothing, !http_response {content: "Response chunk 2."}]
                action:
                - delay: 2ms
                - echo
        )"));
        ServerClient(ExpectError(EREQDONE, mod), [](NSv::IConnection& c) {
            NSv::THeaderVector headers = {
                {":authority", "unknown"},
                {":scheme", "http"},
                {"x-header", "X-Header-Value"}};
            auto rsp = c.Request({"POST", "/test", headers}, true);
            UNIT_ASSERT(rsp);
            UNIT_ASSERT(rsp->Write("Hello, World!"));
            UNIT_ASSERT(rsp->Close());
            EXPECT_HEAD(rsp->Head(), 201, headers);
            UNIT_ASSERT_VALUES_EQUAL(rsp->Read(), "Hello, World!");
            UNIT_ASSERT(rsp->Consume(13) && !rsp->AtEnd());
            UNIT_ASSERT_VALUES_EQUAL(rsp->Read(), "Response chunk 2.");
            UNIT_ASSERT(rsp->Consume(17) && rsp->AtEnd());
        });
    }

    Y_UNIT_TEST(HTTPToApphostInApphost) {
        auto mod = FromConfig(YAML::Load(R"(
            - http-to-apphost
            - apphost:
              - HTTP_REQUEST
              - RESPONSE: [HTTP_REQUEST]
                apphost:
                - HTTP_REQUEST
                - RESPONSE: [A, B]
                  streaming: true
                - A: [!http_response {status_code: 201}, HTTP_REQUEST@http_request->http_response]
                  action:
                  - delay: 1ms
                  - echo
                - B: [A@nothing, !http_response {content: "Response chunk 2."}]
                  action:
                  - delay: 2ms
                  - echo
        )"));
        ServerClient(ExpectError(EREQDONE, mod), [](NSv::IConnection& c) {
            NSv::THeaderVector headers = {
                {":authority", "unknown"},
                {":scheme", "http"},
                {"x-header", "X-Header-Value"}};
            auto rsp = c.Request({"POST", "/test", headers}, true);
            UNIT_ASSERT(rsp);
            UNIT_ASSERT(rsp->Write("Hello, World!"));
            UNIT_ASSERT(rsp->Close());
            EXPECT_HEAD(rsp->Head(), 201, headers);
            UNIT_ASSERT_VALUES_EQUAL(rsp->Read(), "Hello, World!");
            UNIT_ASSERT(rsp->Consume(13) && !rsp->AtEnd());
            UNIT_ASSERT_VALUES_EQUAL(rsp->Read(), "Response chunk 2.");
            UNIT_ASSERT(rsp->Consume(17) && rsp->AtEnd());
        });
    }
}
