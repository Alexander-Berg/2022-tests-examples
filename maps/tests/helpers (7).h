#pragma once

#include <maps/wikimap/feedback/pushes/addresses/prepare_dwellplaces/lib/ad.h>
#include <maps/libs/common/include/hex.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <util/stream/output.h>


template <>
void Out<std::unordered_map<uint64_t, std::string>>(
    IOutputStream& os,
    const std::unordered_map<uint64_t, std::string>&);

template <>
void Out<std::unordered_set<uint64_t>>(
    IOutputStream& os,
    const std::unordered_set<uint64_t>&);

template <>
void Out<maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::Ad>(
    IOutputStream& os,
    const maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::Ad&);

template <>
void Out<maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::AdIdToAdMap>(
    IOutputStream& os,
    const maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::AdIdToAdMap&);

namespace maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::tests {

void createTable(NYT::IClientPtr client, const std::string& tableName);

void writeToTable(
    NYT::IClientPtr client,
    const std::string& tableName,
    const TVector<NYT::TNode>& rows);

} // namespace maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::tests
