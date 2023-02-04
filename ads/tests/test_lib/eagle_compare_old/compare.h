#pragma once

#include <library/cpp/yson/node/node.h>
#include <util/generic/algorithm.h>
#include <util/string/builder.h>



const TString HIDED = "HIDED";

namespace NEagleCompare {

    void FastTNodeSort(NYT::TNode& node) {
        Y_ENSURE(node.IsList());
        Sort(node.AsList(), [](const NYT::TNode& a, const NYT::TNode& b) {
            Y_ENSURE(a.IsMap(), b.IsMap());
            auto& aMap = a.AsMap();
            auto& bMap = b.AsMap();
            static const TVector<TString> keys = {
                "time", "update_time", "value", "bm_category_id",
                "offer_id_md5", "timestamp", "weight", "key"};
            for (const TString& key : keys) {
                auto aIt = aMap.find(key);
                auto bIt = bMap.find(key);
                int aExist = (aIt != aMap.end());
                int bExist = (bIt != bMap.end());
                if (aExist && bExist) {
                    auto& aNode = aIt->second;
                    auto& bNode = bIt->second;

                    if (aNode.GetType() == bNode.GetType()) {
                        using EType = NYT::TNode::EType;
                        switch (aNode.GetType()) {
                            case EType::Undefined:
                                [[fallthrough]];
                            case EType::Null:
                                break;
#define SIMPLE_COMPARE_CASE(type) \
                            case EType::type: \
                            if (aNode.As##type() != bNode.As##type()) { \
                                return aNode.As##type() < bNode.As##type(); \
                            } \
                            else { \
                                break; \
                            }
                            SIMPLE_COMPARE_CASE(String);
                            SIMPLE_COMPARE_CASE(Int64);
                            SIMPLE_COMPARE_CASE(Uint64);
                            SIMPLE_COMPARE_CASE(Double);
                            SIMPLE_COMPARE_CASE(Bool);
#undef SIMPLE_COMPARE_CASE
                            case EType::List:
                                [[fallthrough]];
                            case EType::Map:
                                Y_FAIL("Can't compare");
                                break;
                        }
                    } else {
                        return aNode.GetType() < bNode.GetType();
                    }
                }
                if (aExist != bExist) {
                    return aExist < bExist;
                }
            }

            return false;
        });
    }


    void FastKiGroup(NYT::TNode& data) {
        Y_ENSURE(data.IsList());
        NYT::TNode newData = NYT::TNode::CreateMap();
        for (auto& row : data.AsList()) {
            auto& kidItem = row["id"];
            TString key = kidItem.template ConvertTo<TString>();
            switch(i64 kid = kidItem.template ConvertTo<i64>()) {
                case 62:
                    row["time"] = HIDED;
                    break;
                case 328:
                    kidItem = key = (TStringBuilder{} << key << "_" << row["counter_id"].template ConvertTo<TString>());
                    break;
                case 458:
                    kidItem = key = (TStringBuilder{} << key << "_" << row["select_type"].template ConvertTo<TString>());
                    break;
                case 743:
                    kidItem = key = (TStringBuilder{} << key << "_" << row["counter_id"].template ConvertTo<TString>());
                    break;
                default:
                    break;
            }
            auto& list = newData[key];
            if (!list.IsList()) {
                list = NYT::TNode::CreateList();
            }
            list.AsList().push_back(std::move(row));
        }

        for (auto& [key, row] : newData.AsMap()) {
            FastTNodeSort(row);
        }

        data = std::move(newData);
    }

}
