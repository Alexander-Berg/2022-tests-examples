#include <maps/wikimap/feedback/pushes/addresses/prepare_dwellplaces/tests/helpers.h>
#include <sstream>

using namespace maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces;

template <>
void Out<std::unordered_map<uint64_t, std::string>>(
    IOutputStream& os,
    const std::unordered_map<uint64_t, std::string>& map)
{
    os << "{";
    for (const auto& [id, name] : map) {
        os << "{" << id << ": " << name << "}\t";
    }
    os << "}";
}

template <>
void Out<std::unordered_set<uint64_t>>(
    IOutputStream& os,
    const std::unordered_set<uint64_t>& set)
{
    os << "{";
    for (const auto& item : set) {
        os << item << "\t";
    }
    os << "}";
}

template <>
void Out<maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::Ad>(
    IOutputStream& os,
    const maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::Ad& ad)
{
    std::ostringstream ostr;
    ostr << ad;
    os << ostr.str();
}

template <>
void Out<maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::AdIdToAdMap>(
    IOutputStream& os,
    const maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::AdIdToAdMap& map)
{
    os << "{";
    for (const auto& [id, ad] : map) {
        os << "{" << id << ": " << ad << "}\t";
    }
    os << "}";
}

namespace maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::tests {

void createTable(NYT::IClientPtr client, const std::string& tableName)
{
    client->Create(
        TString(tableName),
        NYT::NT_TABLE,
        NYT::TCreateOptions()
            .Recursive(true)
            .IgnoreExisting(true)
    );
}

void writeToTable(
    NYT::IClientPtr client,
    const std::string& tableName,
    const TVector<NYT::TNode>& rows)
{
    auto writer = client->CreateTableWriter<NYT::TNode>(
        NYT::TRichYPath(TString(tableName)).Append(true)
    );

    for (const auto& row : rows) {
        writer->AddRow(row);
    }

    writer->Finish();
}

} // namespace maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::tests
