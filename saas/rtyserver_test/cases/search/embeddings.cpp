#include <saas/api/search_client/client.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <kernel/dssm_applier/embeddings_transfer/embeddings_transfer.h>
#include <kernel/dssm_applier/utils/utils.h>
#include <kernel/dssm_applier/begemot/production_data.h>

#include <library/cpp/string_utils/base64/base64.h>
#include <library/cpp/string_utils/quote/quote.h>

#include <util/system/tempfile.h>
#include <util/generic/ymath.h>

namespace {
    struct TQueryResult {
        TVector<TDocSearchInfo> Docs;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> DocProps;

    public:
        size_t Count() const {
            Y_ENSURE(Docs.size() == DocProps.size());
            return Docs.size();
        }

        TMaybe<TString> GetGta(size_t docPos, const TStringBuf gta) const {
            Y_ENSURE(docPos < Count());
            const auto& prop = DocProps[docPos];
            auto i = prop->find(gta);
            if (i == prop->end())
                return Nothing();
            return i->second;
        }
    };

    THashMap<TString, NJson::TJsonValue> ParseEmbeddingsJson(TStringBuf in) {
        NJson::TJsonValue resultJson;
        NJson::ReadJsonTree(in, &resultJson, true /*throwOnError*/);

        THashMap<TString, NJson::TJsonValue> result;
        for (const auto& item : resultJson.GetArray()) {
            result[item["name"].GetString()] = item["values"];
        }
        return result;
    }
}

SERVICE_TEST_RTYSERVER_DEFINE(TestDssmHelper)
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate());
        (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME "," FULL_ARCHIVE_COMPONENT_NAME ",DSSM";
        (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "base,full";
        (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".LightLayers"] = "DSSM";
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        return true;
    }

    static NJson::TJsonValue CreateFactorsConfigTemplate() {
        static const TStringBuf configBody = R"({
                "user_factors": {
                    "user1": {
                        "index": 0,
                        "default_value": 2.1
                    },
                    "user2": 1
                },
                "user_functions": {
                    "EmbeddingsJson": "DSSM",
                    "doc_dssm_decompress": "DSSM",
                    "doc_dssm_array_decompress": "DSSM",
                    "doc_dssm_array_decompress_with_tags": "DSSM",
                    "doc_dssm_array_decompress_with_versions": "DSSM"
                },
                "formulas": {
                    "default": {
                        "polynom": "10010000000V3"
                    }
                }
            })";
        NJson::TJsonValue result;
        NJson::ReadJsonTree(configBody, &result, true /*throwOnError*/);
        return result;
    }

    static TString PackFloatVectorRaw(const TVector<float>& v) {
        TString compressed(v.size() * sizeof(v[0]), 0);
        memcpy((void *)compressed.data(), v.data(), compressed.size());

        return compressed;
    }

    static TString PackFloatVectorDefault(const TVector<float>& v) {
        const TVector<ui8> compressed = NDssmApplier::NUtils::TFloat2UI8Compressor::Compress(v);
        return TString{reinterpret_cast<const TString::char_type*>(compressed.data()), compressed.size()};
    }

    static TVector<float> GenerateSparseVector(size_t count, float firstElement) {
        TVector<float> result(count, 0.0f);
        result[0] = firstElement;
        return result;
    }

    TQueryResult QueryOneUrlForGta(const TString& url, const TStringBuf gta, const TString& allKps) {
        TQueryResult result;
        QuerySearch("url:\"" + url + "\"&fsgta=" + gta + allKps, result.Docs, &result.DocProps, 0, true);
        return result;
    }

    TString BuildQuery(const TString& relevValue, const TString& kps,
                        const THashMap<TString, TString>& calcDirectives,
                        const THashMap<TString, TString>& storeDirectives,
                        const TVector<TString>& fsgtaToRequest = TVector<TString>()) {

        TString query = "body&dbgrlv=da&fsgta=_JsonFactors&relev=" + relevValue + kps;
        for (const auto& [factorName, factorExpression] : calcDirectives) {
            query += TStringBuilder{} << "&relev=calc=" << factorName << ":" << factorExpression;
        }
        for (const auto& [valueName, valueExpression] : storeDirectives) {
            query += TStringBuilder{} << "&relev=store=" << valueName << ":" << valueExpression;
        }
        for (const auto& fsgta : fsgtaToRequest) {
            query += "&fsgta=" + fsgta;
        }
        query += "&relev=invalidate_caches=da";
        return query;
    }


    void CheckQueryResult(const TDocSearchInfo& results, const THashMultiMap<TString, TString>& resultProps,
                    const THashMap<TString, float>& correctFactorsValues, const THashMap<TString, TString>& fsgtaToCheck = THashMap<TString, TString>()) {

        THashMap<TString, double> factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
        for (const auto& [factorName, factorValue] : correctFactorsValues) {
            auto it = factors.find(factorName);
            Y_ENSURE(it != factors.end(), TStringBuilder{} << "can't find factor " << factorName << " in doc " << results.GetUrl());

            if (fabs(it->second - factorValue) > 1e-6 * Max(1.0f, fabs(factorValue))) {
                ythrow yexception() << "document " << results.GetUrl() <<  " incorrect value for factor " << factorName << ": " << it->second << " != " << factorValue;
            }
        }
        for (const auto& [attrName, attrValue] : fsgtaToCheck) {
            auto it = resultProps.find(attrName);
            Y_ENSURE(it != resultProps.end(), TStringBuilder{} << "can't find attribute " << attrName << " in doc " << results.GetUrl());

            if (attrValue != it->second) {
                ythrow yexception() << "document " << results.GetUrl() <<  " incorrect value for fsgta " << attrName << ": " << it->second << " != " << attrValue;
            }
        }
    }

            

    void CheckUserFactor(const TString& comment, const TString& kps, const TString& userFactorFormula, const TString& relev, const TVector<TString>& storeDirectives, double correctValue, const TMaybe<TString>& attrToCheck = Nothing(), const TMaybe<TString>& correctAttrValue = Nothing()) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;
        TString query = "body&dbgrlv=da&relev=" + relev + "&fsgta=_JsonFactors&relev=calc=user1:" + userFactorFormula + kps;
        for (const auto& directive : storeDirectives) {
            query += "&relev=store=" + directive;
        }
        if (attrToCheck.Defined() && correctAttrValue.Defined()) {
            query += "&fsgta=" + attrToCheck.GetRef();
        }
        QuerySearch(query, results, &resultProps);
        if (results.size() != 1) {
            ythrow yexception() << comment << "(" << userFactorFormula << "): incorrect count: " << results.size() << " != 1";
        }

        THashMap<TString, double> factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[0];
        THashMap<TString, double>::const_iterator i = factors.find("user1");
        if (i == factors.end())
            ythrow yexception() << "there is no user1 factor";
        if (fabs(i->second - correctValue) > 1e-6 * Max(1.0, fabs(correctValue)))
            ythrow yexception() << "incorrect value: " << i->second << " != " << correctValue;
        DEBUG_LOG << userFactorFormula << " == " << correctValue << "... OK" << Endl;

        if (attrToCheck.Defined() && correctAttrValue.Defined()) {
            auto desiredGta = resultProps[0]->find(attrToCheck.GetRef());
            Y_ENSURE(desiredGta != resultProps[0]->end(), TStringBuilder{} <<"There is no desired gta " << attrToCheck);
            Y_ENSURE((desiredGta->second == correctAttrValue), TStringBuilder{} << "Returned value: " <<  desiredGta->second << " not equals to correct value: " << correctAttrValue);
        }
    }
    void CheckUserFactor(const TString& comment, const TString& kps, const TString& userFactorFormula, const TString& relev, const TMaybe<TString>& singleDirective, double correctValue) {
        if (singleDirective.Empty()) {
            CheckUserFactor(comment, kps, userFactorFormula, relev, TVector<TString>(), correctValue);
        } else {
            CheckUserFactor(comment, kps, userFactorFormula, relev, TVector<TString>(1, singleDirective.GetRef()), correctValue);
        }
    }
};

START_TEST_DEFINE_PARENT(TestDssmBasics, TestDssmHelper)
    static TString PackFloatVector(const TVector<float>& v, const NNeuralNetApplier::EDssmModel modelType = NNeuralNetApplier::EDssmModel::BoostingCtr) {
        NNeuralNetApplier::IBegemotDssmDataPtr serializer = NNeuralNetApplier::GetDssmDataSerializer(modelType);

        NNeuralNetApplier::TVersionRange versions(0);
        serializer->AddData(versions, v);

        TStringStream out;
        serializer->Save(&out);

        return out.Str();
    }

    void GenMessages(TVector<NRTYServer::TMessage>& messages) {
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

        const std::pair<TStringBuf, TStringBuf> placedEmbeddings[] = {
            {"existing_embedding1", "some_value1"},
            {"existing_embedding2", "some_value2"},
        };

        for (const auto& [name, value] : placedEmbeddings) {
            //add embeddings to message
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);

            embedding->SetName(name.data(), name.length());
            embedding->SetValue(value.data(), value.length());
        }

        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("crafted_embedding_nn");
            embedding->SetValue(Base64Decode("AQAAAAAAAAAAAAAAGQAAAKR+a4l4b5OFi0hfhFdcmK1XlZFmnKkAaWEJAAAA"));
        }

        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("crafted_embedding_nn");
            embedding->SetVersion("1234");
            embedding->SetValue(Base64Decode("AQAAAAAAAAAAAAAAGQAAAM6luzDX/4Rjg2MjctiZtcvON4NG9NKcjzkJAAAA"));
        }

        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("crafted_embedding_nn2");
            embedding->SetValue(Base64Decode("AQAAAAAAAAAAAAAAGQAAAM6luzDX/4Rjg2MjctiZtcvON4NG9NKcjzkJAAAA"));
        }

        {
            const TVector<float> someValues = {
                0.284171, 0.0882163, 0.140837, -0.187266, 0.205838, 0.301793, 0.0108336, -0.0665491, 0.00773827, -0.0665491, -0.218219,
                -0.0325007, 0.408933, 0.0603585, 0.12536, 0.17798, 0.184171, -0.17179, 0.00773827, -0.134646, 0.273935, 0.193457, 0.0665491,
                0.035596, -0.765599
            };
            NDssmApplier::NUtils::TFloat2UI8Compressor::Compress(someValues);

            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("crafted_embedding_nn3");
            embedding->SetValue(PackFloatVectorDefault(someValues));
        }

        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(GenerateSparseVector(25, -1.0f), NNeuralNetApplier::EDssmModel::BoostingCtr));
        }

        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(GenerateSparseVector(25, 1.0f), NNeuralNetApplier::EDssmModel::BoostingCtr));
        }
        // first three embeddings (in sorted order) have tags, and last has no tag, want to test behaviour without tags too
        {
            // that embedding has no tag
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_for_top");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(GenerateSparseVector(25, -1.0f), NNeuralNetApplier::EDssmModel::BoostingCtr));
        }

        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_for_top");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(GenerateSparseVector(25, 1.0f), NNeuralNetApplier::EDssmModel::BoostingCtr));
            embedding->SetTag("first");
        }

        {
            auto midVector = GenerateSparseVector(25, 0.1f);
            midVector[1] = 0.9;
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_for_top");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(midVector, NNeuralNetApplier::EDssmModel::BoostingCtr));
            embedding->SetTag("middle");
        }
        {
            auto mid2Vector = GenerateSparseVector(25, 0.5f);
            mid2Vector[1] = -0.5;
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_for_top");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(mid2Vector, NNeuralNetApplier::EDssmModel::BoostingCtr));
            embedding->SetTag("last");
        }
        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_float32");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVectorRaw({1.0f, 1.0f, 1.0f}));
            embedding->SetTag("first");
        }
        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_float32");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVectorRaw({0.0f, 1.0f, 0.0f}));
            embedding->SetTag("middle");
        }
        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_float32");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVectorRaw({-1.0f, -1.0f, -1.0f}));
            embedding->SetTag("last");
        }
    }

    void Check(const TString& comment, const TString& kps) {
        const TString RelevVectorValue = "query_embedding_boosting_ctr%3DAQAAAAAAAAAAAAAAGQAAAKR%2Ba4l4b5OFi0hfhFdcmK1XlZFmnKkAaWEJAAAA";
        // should give 1.0 because we should multiple the same normalized vectors
        CheckUserFactor(comment, kps, "dot_product(doc_dssm_decompress(\"crafted_embedding_nn\",\"BoostingCtr\"),"
            "dssm_decode(base64_decode(get_relev(\"query_embedding_boosting_ctr\")),\"BoostingCtr\"))", RelevVectorValue, Nothing(), 1.0f);

        // should get some value
        CheckUserFactor(comment, kps, "dot_product(doc_dssm_decompress(\"crafted_embedding_nn2\",\"BoostingCtr\"),"
            "dssm_decode(base64_decode(get_relev(\"query_embedding_boosting_ctr\")),\"BoostingCtr\"))", RelevVectorValue, Nothing(), 0.0722879f);

         // should get some value
        CheckUserFactor(comment, kps, "dot_product(doc_dssm_decompress(\"crafted_embedding_nn3\",\"AutoMaxCoordRenorm\"),"
            "dssm_decode(base64_decode(get_relev(\"query_embedding_boosting_ctr\")),\"BoostingCtr\"))", RelevVectorValue, Nothing(), 0.110041f);

        // should give 2.1 because we should get default factor value
        CheckUserFactor(comment, kps, "dot_product(doc_dssm_decompress(\"unknown_embedding\",\"BoostingCtr\"),"
            "dssm_decode(base64_decode(get_relev(\"query_embedding_boosting_ctr\")),\"BoostingCtr\"))", RelevVectorValue, Nothing(), 2.1f);

        // case with begemot relev param
        {
            const TVector<float> dummyValues = {
                0.184171, 0.0882163, 0.140837, -0.187266, 0.205838, 0.301793, 0.0108336, -0.0665491, 0.00773827, -0.0665491, -0.218219,
                -0.0325007, 0.208933, 0.0603585, 0.12536, 0.17798, 0.184171, -0.17179, 0.00773827, -0.134646, 0.273935, 0.193457, 0.0665491,
                0.035596, -0.165599
            };
            NEmbeddingsTransfer::TEmbedding relevEmbedding("1234", dummyValues);

            NEmbeddingsTransfer::NProto::TModelEmbedding modelEmbedding;
            NEmbeddingsTransfer::SetEmbeddingData(modelEmbedding, "begemot_model_XXX", relevEmbedding);

            TString relevValue;
            Y_PROTOBUF_SUPPRESS_NODISCARD modelEmbedding.SerializeToString(&relevValue);
            relevValue = "query_embedding_boosting_ctr=" + Base64Encode(relevValue);
            CGIEscape(relevValue);

            const TVector<float> smallModel = {0.8f, 0.6f, 0.f}; 
            TString smallRelevValue = PackFloatVectorRaw(smallModel);
            smallRelevValue = "query_embedding_boosting_ctr=" + Base64Encode(smallRelevValue);
            CGIEscape(smallRelevValue);

            CheckUserFactor(
                comment,
                kps,
                "dot_product("\
                    "doc_dssm_decompress(\"crafted_embedding_nn\",\"BoostingCtr\"),"\
                    "bg_model_embed_value(\"bg_boostingctr_embed\")"\
                ")",
                relevValue,
                "bg_boostingctr_embed:parse_bg_model_embed(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))",
                0.0722879f
            );

            CheckUserFactor(
                comment,
                kps,
                "dot_product("\
                    "doc_dssm_decompress(\"crafted_embedding_nn\",\"BoostingCtr\",bg_model_embed_version(\"bg_boostingctr_embed\")),"\
                    "bg_model_embed_value(\"bg_boostingctr_embed\")"\
                ")",
                relevValue,
                "bg_boostingctr_embed:parse_bg_model_embed(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))",
                1.0f
            );

            CheckUserFactor(
                comment,
                kps,
                "vect_max_element(matr_X_vect("\
                    "doc_dssm_array_decompress(\"mult_embedding\",\"BoostingCtr\",bg_model_embed_version(\"bg_boostingctr_embed\")),"\
                    "bg_model_embed_value(\"bg_boostingctr_embed\")"\
                "))",
                relevValue,
                "bg_boostingctr_embed:parse_bg_model_embed(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))",
                0.231466f
            );

            CheckUserFactor(
                comment,
                kps,
                "vect_max_element(matr_X_vect("\
                    "doc_dssm_array_decompress(\"mult_embedding_float32\",\"Float32\", \"1234\"),"\
                    "dssm_decode(base64_decode(get_relev(\"query_embedding_boosting_ctr\")), \"Float32\")"\
                "))",
                smallRelevValue, // setting small model, checking precision
                Nothing(),
                1.4f
            );

            // third maximum
            CheckUserFactor(
                comment,
                kps,
                "element_at(\
                    vect_top_n_elements(\
                        matr_X_vect("\
                            "load_embeds_values(\"array_with_tags\"),"\
                            "bg_model_embed_value(\"bg_boostingctr_embed\") \
                        ), 4, -1, load_embeds_tags(\"array_with_tags\"), \"top_4_tags\")"\
                    ", 2)"\
                "))",
                relevValue,
                {
                    "bg_boostingctr_embed:parse_bg_model_embed(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))",
                    "array_with_tags:save_embeds_and_tags(doc_dssm_array_decompress_with_tags(\"mult_embedding_for_top\",\"BoostingCtr\",bg_model_embed_version(\"bg_boostingctr_embed\")))"
                },
                0.0840283f,
                "top_4_tags",
                "first;middle;last;" // last tag is empty
            );

            // check default value behavior
            CheckUserFactor(
                comment,
                kps,
                "element_at(\
                    vect_top_n_elements(\
                        matr_X_vect("\
                            "load_embeds_values(\"array_with_tags\"),"\
                            "bg_model_embed_value(\"bg_boostingctr_embed\") \
                        ), 5, -1, load_embeds_tags(\"array_with_tags\"), \"top_5_tags\")"\
                    ", 4)"\
                "))",
                relevValue,
                {
                    "bg_boostingctr_embed:parse_bg_model_embed(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))",
                    "array_with_tags:save_embeds_and_tags(doc_dssm_array_decompress_with_tags(\"mult_embedding_for_top\",\"BoostingCtr\",bg_model_embed_version(\"bg_boostingctr_embed\")))"
                },
                -1.f,
                "top_5_tags",
                "first;middle;last;;" // last tag is empty and one value is absent
            );

            CheckUserFactor( // 4th max + 1st max
                comment,
                kps,
                "sum(element_at(load_float_vector(\"sorted_top\"), 0), element_at(load_float_vector(\"sorted_top\"), 3))",
                relevValue,
                {
                    "bg_boostingctr_embed:parse_bg_model_embed(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))",
                    "array_with_tags:save_embeds_and_tags(doc_dssm_array_decompress_with_tags(\"mult_embedding_for_top\",\"BoostingCtr\",bg_model_embed_version(\"bg_boostingctr_embed\")))",
                    "sorted_top:save_float_vector(vect_top_n_elements(\
                        matr_X_vect("\
                            "load_embeds_values(\"array_with_tags\"),"\
                            "bg_model_embed_value(\"bg_boostingctr_embed\") \
                        ), 4, -1, load_embeds_tags(\"array_with_tags\"), \"top_4_tags\"))"
                },
                0.231466f - 0.241991f
            );

            CheckUserFactor(
                comment,
                kps,
                "vect_mean(matr_X_vect("\
                    "doc_dssm_array_decompress(\"mult_embedding\",\"BoostingCtr\",bg_model_embed_version(\"bg_boostingctr_embed\")),"\
                    "bg_model_embed_value(\"bg_boostingctr_embed\")"\
                "))",
                relevValue,
                "bg_boostingctr_embed:parse_bg_model_embed(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))",
                -0.0052626f // due to imprecise compression we do not have 0 here
            );

            CheckUserFactor(
                comment,
                kps,
                "vect_harmonic_mean(matr_X_vect("\
                    "doc_dssm_array_decompress(\"mult_embedding\",\"BoostingCtr\",bg_model_embed_version(\"bg_boostingctr_embed\")),"\
                    "bg_model_embed_value(\"bg_boostingctr_embed\")"\
                "), 0)",
                relevValue,
                "bg_boostingctr_embed:parse_bg_model_embed(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))",
                10.6436f
            );
        }
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenMessages(messages);

        IndexMessages(messages, DISK, 1);
        const TString kps(GetAllKps(messages));
        ReopenIndexers();
        Check("disk", kps);

        const TStringBuf gta("_EmbeddingsJson");
        auto queryResults = QueryOneUrlForGta(messages[0].GetDocument().GetUrl(), gta, GetAllKps(messages));
        CHECK_TEST_EQ(1u, queryResults.Count());

        const auto& result = queryResults.GetGta(0, gta);
        CHECK_TEST_TRUE(result.Defined());

        THashMap<TString, NJson::TJsonValue> parsedGta = ParseEmbeddingsJson(result.GetRef());
        CHECK_TEST_EQ(9u, parsedGta.size());

        auto checkSingleArrayElement = [&parsedGta](TStringBuf gtaParameter, TStringBuf expectedValue) -> bool {
            const auto& a = parsedGta[gtaParameter].GetArray();
            CHECK_TEST_EQ(1UL, a.size());
            return expectedValue == a[0].GetString();
        };

        auto checkArrayElements = [&parsedGta](TStringBuf gtaParameter, const std::initializer_list<TStringBuf>& expectedValue) -> bool {
            const auto& a = parsedGta[gtaParameter].GetArray();

            TVector<TString> expected(expectedValue.begin(), expectedValue.end());
            std::sort(expected.begin(), expected.end());

            TVector<TString> actual;
            for (const auto& s : a) {
                actual.push_back(s.GetString());
            }
            std::sort(actual.begin(), actual.end());

            CHECK_TEST_EQ(expectedValue.size(), actual.size());
            return std::equal(actual.begin(), actual.end(), expected.begin());
        };

        CHECK_TEST_TRUE(checkSingleArrayElement("existing_embedding1", Base64Encode("some_value1")));
        CHECK_TEST_TRUE(checkSingleArrayElement("existing_embedding2", Base64Encode("some_value2")));
        CHECK_TEST_TRUE(checkSingleArrayElement("crafted_embedding_nn", "AQAAAAAAAAAAAAAAGQAAAKR+a4l4b5OFi0hfhFdcmK1XlZFmnKkAaWEJAAAA"));
        CHECK_TEST_TRUE(checkSingleArrayElement("crafted_embedding_nn_v_1234", "AQAAAAAAAAAAAAAAGQAAAM6luzDX/4Rjg2MjctiZtcvON4NG9NKcjzkJAAAA"));
        CHECK_TEST_TRUE(checkSingleArrayElement("crafted_embedding_nn2", "AQAAAAAAAAAAAAAAGQAAAM6luzDX/4Rjg2MjctiZtcvON4NG9NKcjzkJAAAA"));
        CHECK_TEST_TRUE(checkArrayElements("mult_embedding_v_1234", {
            Base64Encode(PackFloatVector(GenerateSparseVector(25, -1.0f))),
            Base64Encode(PackFloatVector(GenerateSparseVector(25, 1.0f)))
        }));
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDssmMultiEmbeddings, TestDssmHelper)
    static TString PackFloatVector(const TVector<float>& v, const NNeuralNetApplier::EDssmModel modelType = NNeuralNetApplier::EDssmModel::BoostingCtr) {
        NNeuralNetApplier::IBegemotDssmDataPtr serializer = NNeuralNetApplier::GetDssmDataSerializer(modelType);

        NNeuralNetApplier::TVersionRange versions(0);
        serializer->AddData(versions, v);

        TStringStream out;
        serializer->Save(&out);

        return out.Str();
    }

    static TString PackFloatVectorDefault(const TVector<float>& v) {
        const TVector<ui8> compressed = NDssmApplier::NUtils::TFloat2UI8Compressor::Compress(v);
        return TString{reinterpret_cast<const TString::char_type*>(compressed.data()), compressed.size()};
    }

    static TVector<float> GenerateSparseVector(size_t count, float firstElement) {
        TVector<float> result(count, 0.0f);
        result[0] = firstElement;
        return result;
    }

    void GenMessages(TVector<NRTYServer::TMessage>& messages) {
        GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        // first three embeddings (in sorted order) have tags, and last has no tag, want to test behaviour without tags too
        {
            // that embedding has no tag
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_for_top");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(GenerateSparseVector(25, -1.0f), NNeuralNetApplier::EDssmModel::BoostingCtr));
        }

        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_for_top");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(GenerateSparseVector(25, 1.0f), NNeuralNetApplier::EDssmModel::BoostingCtr));
            embedding->SetTag("first");
        }

        {
            auto midVector = GenerateSparseVector(25, 0.1f);
            midVector[1] = 0.9;
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_for_top");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(midVector, NNeuralNetApplier::EDssmModel::BoostingCtr));
            embedding->SetTag("middle");
        }
        {
            auto mid2Vector = GenerateSparseVector(25, 0.5f);
            mid2Vector[1] = -0.5;
            NRTYServer::TMessage::TEmbedding* embedding = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_for_top");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(mid2Vector, NNeuralNetApplier::EDssmModel::BoostingCtr));
            embedding->SetTag("last");
        }

        {
            // that embedding for second message, an attempt to break attributes behavior
            NRTYServer::TMessage::TEmbedding* embedding = messages[1].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("mult_embedding_for_top");
            embedding->SetVersion("1234");
            embedding->SetValue(PackFloatVector(GenerateSparseVector(25, 1.0f), NNeuralNetApplier::EDssmModel::BoostingCtr));
            embedding->SetTag("second message");
        }
    }

    void Check(const TString& comment, const TString& kps) {
        Cerr << comment << Endl;
        const TString RelevVectorValue = "query_embedding_boosting_ctr%3DAQAAAAAAAAAAAAAAGQAAAKR%2Ba4l4b5OFi0hfhFdcmK1XlZFmnKkAaWEJAAAA";
        // case with begemot relev param
        {
            const TVector<float> dummyValues = {
                0.184171, 0.0882163, 0.140837, -0.187266, 0.205838, 0.301793, 0.0108336, -0.0665491, 0.00773827, -0.0665491, -0.218219,
                -0.0325007, 0.208933, 0.0603585, 0.12536, 0.17798, 0.184171, -0.17179, 0.00773827, -0.134646, 0.273935, 0.193457, 0.0665491,
                0.035596, -0.165599
            };
            NEmbeddingsTransfer::TEmbedding relevEmbedding("1234", dummyValues);

            NEmbeddingsTransfer::NProto::TModelEmbedding modelEmbedding;
            NEmbeddingsTransfer::SetEmbeddingData(modelEmbedding, "begemot_model_XXX", relevEmbedding);

            TString relevValue;
            Y_PROTOBUF_SUPPRESS_NODISCARD modelEmbedding.SerializeToString(&relevValue);
            relevValue = "query_embedding_boosting_ctr=" + Base64Encode(relevValue);
            CGIEscape(relevValue);

            {
                TString query = BuildQuery(relevValue, kps,
                { // calc directives
                    {
                    "user1", "element_at("\
                        "vect_top_n_elements("\
                            "matr_X_vect("\
                                "load_embeds_values(\"array_with_tags\"),"\
                                "bg_model_embed_value(\"bg_boostingctr_embed\")"\
                            "), 4, -1, load_embeds_tags(\"array_with_tags\"), \"top_4_tags\")"\
                        ", 2)"\
                    "))",
                    }
                },
                { // store directives
                    {
                        "bg_boostingctr_embed",
                            "parse_bg_model_embed(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))"
                    },
                    {
                        "array_with_tags",
                        "save_embeds_and_tags(doc_dssm_array_decompress_with_tags("\
                            "\"mult_embedding_for_top\",\"BoostingCtr\",bg_model_embed_version(\"bg_boostingctr_embed\")))"
                    }
                },
                { // fsgta to request
                    "top_4_tags"
                });

                TVector<TDocSearchInfo> results;
                TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;
                QuerySearch(query, results, &resultProps);
                CheckQueryResult(results[0], *resultProps[0], {{"user1", 0.0840283f}}, {{"top_4_tags", "first;middle;last;"}});
                CheckQueryResult(results[1], *resultProps[1], {{"user1", /*default value is -1*/-1.f}}, {{"top_4_tags", "second message;;;"}});
            }

            { // 4th max + 1st max
                TString query = BuildQuery(relevValue, kps,
                { // calc directives
                    {
                        "user1", "sum(element_at(load_float_vector(\"sorted_top\"), 0), element_at(load_float_vector(\"sorted_top\"), 3))"
                    }
                },
                { // store directives
                    {
                        "bg_boostingctr_embed",
                            "parse_bg_model_embed(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))"
                    },
                    {
                        "array_with_tags",
                        "save_embeds_and_tags(doc_dssm_array_decompress_with_tags("\
                            "\"mult_embedding_for_top\",\"BoostingCtr\",bg_model_embed_version(\"bg_boostingctr_embed\")))"
                    },
                    {
                        "sorted_top",
                        "save_float_vector(vect_top_n_elements(\
                            matr_X_vect("\
                                "load_embeds_values(\"array_with_tags\"),"\
                                "bg_model_embed_value(\"bg_boostingctr_embed\") \
                            ), 4, -1, load_embeds_tags(\"array_with_tags\"), \"top_4_tags\"))"
                    }
                }
                );

                TVector<TDocSearchInfo> results;
                TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;
                QuerySearch(query, results, &resultProps);

                CheckQueryResult(results[0], *resultProps[0], {{"user1", 0.231466f - 0.241991f}});
                CheckQueryResult(results[1], *resultProps[1], {{"user1", 0.231466f - 1.f}});
            }
        }
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenMessages(messages);

        IndexMessages(messages, DISK, 1);
        const TString kps(GetAllKps(messages));
        ReopenIndexers();
        Check("disk", kps);

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSparseEmbeddings, TestDssmHelper)

    /**
     * 5 documents:
     * 1) have both Version1 and Version2 of model embedding -> if + dp
     * 2) have only Version1  -> Version1 should be used -> if + dp
     * 3) have only Version2 -> Fallback to Version2 -> if + if + dp
     * 4) have model embedding but with missmatched version ->  if + if + fallback
     * 5) dont have embeddings -> if + if + fallback
     *
     * And 0 expections thrown.
     */
    void GenMessages(TVector<NRTYServer::TMessage>& messages) {
        GenerateInput(messages, 5, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        {
            NRTYServer::TMessage::TEmbedding* embedding1 = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding1);
            embedding1->SetName("ModelName");
            embedding1->SetVersion("Version1");
            embedding1->SetValue(PackFloatVectorRaw(GenerateSparseVector(50, 0.1f)));

            NRTYServer::TMessage::TEmbedding* embedding2 = messages[0].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding2);
            embedding2->SetName("ModelName");
            embedding2->SetVersion("Version2");
            embedding2->SetValue(PackFloatVectorRaw(GenerateSparseVector(50, 0.2f)));
        }
        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[1].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("ModelName");
            embedding->SetVersion("Version1");
            embedding->SetValue(PackFloatVectorRaw(GenerateSparseVector(50, 0.3f)));
        }
        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[2].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("ModelName");
            embedding->SetVersion("Version2");
            embedding->SetValue(PackFloatVectorRaw(GenerateSparseVector(50, 0.4f)));
        }
        {
            NRTYServer::TMessage::TEmbedding* embedding = messages[3].MutableDocument()->AddEmbeddings();
            Y_ASSERT(embedding);
            embedding->SetName("ModelName");
            embedding->SetVersion("AnotherVersion");
            embedding->SetValue(PackFloatVectorRaw(GenerateSparseVector(50, 0.5f)));
        }
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenMessages(messages);

        IndexMessages(messages, DISK, 1);
        const TString kps(GetAllKps(messages));
        ReopenIndexers();

        // 0.28941 0.03216 -0.20366 0.15364 0.12505 -0.14649 -0.00357 0.00357 -0.00357 -0.11791 -0.16793 -0.13934 0.13934 0.15364 0.07503 -0.32514 0.06789 -0.03216 -0.9111 -0.05359 -0.0393 0.17507 -0.36801 0.37516 -0.28941 -0.06074 0.2108 0.08932 -0.11076 -0.36801 0.02501 0.11791 -0.02501 0.00357 -0.17507 -0.1322 0.12505 -0.05359 -0.28226 0.02501 0.17507 0.02501 -0.05359 -0.06789 0.09647 -0.05359 -0.10362 0.2108 0.35372 -0.23939
        TString queryEmbeddingV1Func = 
            "dssm_decode("
                "base64_decode(\"XC2UPou2Az2xi1C-7VIdPuoNAD6sARa-EyhquxMoajsTKGq7VHnxvW71K75ssA6-bLAOPu1SHT5Mqpk9fnimvssHiz2LtgO96z1pv5KFW72N-yC9r0YzPj9svL7gFMA-XC2UvpTKeL3y3Fc-T--2PdLW4r0_bLy-EePMPFR58T0R48y8EyhqO69GM74rXwe-6g0APpKFW728hJC-EePMPK9GMz4R48w8koVbvcsHi73QkcU9koVbvVE01L3y3Fc-_xq1PvQhdb4,\"),"
                "\"Float32\""
            ")";
        // 0.03499 -0.0796 -0.03038 0.00961 0.03807 0.04883 0.05191 0.02346 0.06114 -0.07575 0.05345 -0.0473 0.00577 0.0273 0.04499 0.02038 -0.09113 0.07729 0.09498 -0.03576 -0.03115 0.00884 0.0596 0.02423 0.08959 0.01423 0.03115 0.01346 0.02884 -0.09344 0.03115 -0.03115 0.03653 -0.01346 0.02192 -0.05729 0.00731 -0.09805 -0.00115 -0.0496 -0.07191 0.02269 -0.04807 -0.03038 0.04807 -0.09728 0.00808 0.03422 -0.07883 0.05191
        TString queryEmbeddingV2Func = 
            "dssm_decode("
                "base64_decode(\"gFMPPYoDo70e2vi8VIAdPCDtGz3RBkg9caBUPc0mwDxSbXo9hiObvUHtWj0BukG9ZQC9O96m3zzJRjg9jPOmPJejur3uSZ49m4PCPeh5Er3uJv-8tOYQPIIgdD2dc8Y8L323PRYaaTzuJv88dYBcPH5A7DwzXb-97ib_PO4m_7xQoBU9dYBcvCyNszxKrWq95mbvO2vQyL2EM5e6OS1LvYJDk7392bk8aeBEvR7a-Lxp4EQ9Nz3HvRNNBDwXLQw9VnChvXGgVD0,\"),"
                "\"Float32\""
            ")";

        TString query = BuildQuery(
            "", 
            kps, 
            // calc
            {
                {
                    "user1",
                    "if(doc_embedding_exists(\"DocEmbeddings\",\"Version1\"),"
                        "dot_product(load_cached_embedding(\"DocEmbeddings\",\"Version1\"),load_float_vector(\"QueryEmbeddingV1\")),"
                        "if(doc_embedding_exists(\"DocEmbeddings\",\"Version2\"),"
                            "dot_product(load_cached_embedding(\"DocEmbeddings\",\"Version2\"),load_float_vector(\"QueryEmbeddingV2\")),"
                            "0.0"
                        ")"
                    ")",
                },
            },
            // store
            {
                {
                    "DocEmbeddings",
                    "preload_doc_embeddings(doc_dssm_array_decompress_with_versions(\"ModelName\",\"Float32\",\"Version1\",\"Version2\"))",
                },
                {
                    "QueryEmbeddingV1",
                    Sprintf("save_float_vector(%s)", queryEmbeddingV1Func.data()),
                },
                {
                    "QueryEmbeddingV2",
                    Sprintf("save_float_vector(%s)", queryEmbeddingV2Func.data()),
                },
            }
        );

        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;
        QuerySearch(query, results, &resultProps);

        if (results.size() != 5) {
            ythrow yexception() << "Expected 5 results, got " << results.size();
        }

        CheckQueryResult(results[0], *resultProps[0], {{"user1", 0.0868226}}); // doc#2: ~0.3*0.2891
        CheckQueryResult(results[1], *resultProps[1], {{"user1", 0.0289409}}); // doc#1: ~0.1*0.2891
        CheckQueryResult(results[2], *resultProps[2], {{"user1", 0.0139967}}); // doc#3: ~0.4*0.0345
        CheckQueryResult(results[3], *resultProps[3], {{"user1", 0.0f}}); // doc#4: default branch -> 0.0
        CheckQueryResult(results[4], *resultProps[4], {{"user1", 0.0f}}); // doc#5: default branch -> 0.0
        return true;
    }
};
