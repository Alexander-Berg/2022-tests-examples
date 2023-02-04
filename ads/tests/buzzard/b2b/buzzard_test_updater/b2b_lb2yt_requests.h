#pragma once

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/logger/log.h>

#include <util/stream/file.h>
#include <util/stream/fwd.h>
#include <util/string/cast.h>

#include <ads/bsyeti/libs/log_parser/parser.h>
#include <ads/bsyeti/libs/yt_queues_parser/rows_parser.h>
#include <logfeller/lib/parsing/config_storage/config_storage.h>
#include <library/cpp/yson/node/node.h>


TVector<TVector<TString>> GetUidLogtypePairs(TString blob) {
    static TIntrusivePtr<NBSYeti::NBuzzard::IYtRowsParser> rowParser = NBSYeti::NBuzzard::CreateYtRowsParser("buzzard_production", "bigb_events");
    TDeque<NYT::TNode> Nodes;
    rowParser->Parse(TStringBuf(blob), Nodes);
    TVector<TVector<TString>> result;
    for (const auto& node : Nodes) {
        result.push_back({node["ProfileID"].AsString(), node["__ChunkType"].AsString()});
    }
    return result;
}


void GrepRequests(TVector<TString> chunks, TString outputDir) {
    static TIntrusivePtr<NBSYeti::NBuzzard::IYtRowsParser> rowParser = NBSYeti::NBuzzard::CreateYtRowsParser("buzzard_production", "bigb_events");
    TString TAG = "yt_data";
    DoInitGlobalLog(CreateLogBackend("cerr", (ELogPriority)6, true));
    ui64 maxTimestamp = 0;

    INFO_LOG << "Will read chunks, total count" << chunks.size() << Endl;

    // Initialize parser
    NLogFeller::NParsing::UseEmbeddedConfigs(); // To look for tskv parser config in embeded resources
    auto parser = NBSYeti::CreateUniversalLogParser(TAG);

    TFile phraseFile(outputDir + "/Phrase", OpenAlways | WrOnly | ForAppend);
    TFileOutput phraseFileStream(phraseFile);
    TString PHRASE_ID = "PhraseID";

    TFile appFile(outputDir + "/StoreApp", OpenAlways | WrOnly | ForAppend);
    TFileOutput appFileStream(appFile);
    TString APP_ID = "AppID";

    TFile bannerFile(outputDir + "/CaesarBanners", OpenAlways | WrOnly | ForAppend);
    TFileOutput bannerFileStream(bannerFile);
    TString BANNER_ID = "BannerID";
    TString Banners = "Banners";
    TString PARENT_BANNER_ID = "ParentBannerID";

    TFile offerFile(outputDir + "/CaesarOffer", OpenAlways | WrOnly | ForAppend);
    TFileOutput offerFileStream(offerFile);
    TString URL = "URL";
    TString url = "url";
    TString Url = "url";
    TString NORM_URL = "NormUrl";

    TString TIMESTAMP = "TimeStamp";
    TString EventTime = "EventTime";


    TString chunkLine;
    NYT::TNode node;
    size_t total = 0;
    size_t good  = 0;


    for (const auto& chunk : chunks) {
        ++total;
        TDeque<NYT::TNode> Nodes;
        rowParser->Parse(TStringBuf(chunk), Nodes);
        if (Nodes) {
            ++good;
            for (const auto& node : Nodes) {
                if (node.HasKey(Banners)) {
                    for (const auto& banner : node[Banners].AsList()) {
                        if (banner.HasKey(PHRASE_ID)) {
                            phraseFileStream.Write(banner[PHRASE_ID].ConvertTo<TString>() + "\n");
                        }
                        if (banner.HasKey(BANNER_ID)) {
                            bannerFileStream.Write(banner[BANNER_ID].ConvertTo<TString>() + "\n");
                        }
                    }
                }
                if (node.HasKey(PHRASE_ID)) {
                    phraseFileStream.Write(node[PHRASE_ID].ConvertTo<TString>() + "\n");
                }
                if (node.HasKey(BANNER_ID)) {
                    bannerFileStream.Write(node[BANNER_ID].ConvertTo<TString>() + "\n");
                }
                if (node.HasKey(PARENT_BANNER_ID)) {
                    bannerFileStream.Write(node[PARENT_BANNER_ID].ConvertTo<TString>() + "\n");
                }
                if (node.HasKey(APP_ID)) {
                    appFileStream.Write(node[APP_ID].ConvertTo<TString>() + "\n");
                };
                if (node.HasKey(URL)) {
                    offerFileStream.Write(node[URL].AsString() + "\n");
                }
                if (node.HasKey(url)) {
                    offerFileStream.Write(node[url].AsString() + "\n");
                }
                if (node.HasKey(Url)) {
                    offerFileStream.Write(node[Url].AsString() + "\n");
                }
                if (node.HasKey(NORM_URL)) {
                    offerFileStream.Write(node[NORM_URL].AsString() + "\n");
                }

                if (node.HasKey(TIMESTAMP)) {
                    ui64 timestamp = node[TIMESTAMP].ConvertTo<ui64>();
                    if (timestamp > maxTimestamp) {
                        maxTimestamp = timestamp;
                    }
                } else if (node.HasKey(EventTime)) {
                    ui64 timestamp = node[EventTime].ConvertTo<ui64>();
                    if (timestamp > maxTimestamp) {
                        maxTimestamp = timestamp;
                    }
                } else {
                    WARNING_LOG << "NO TIMESTAMP\n" << chunkLine << "\n";
                }
            }

        } else {
            ERROR_LOG << "Failed to parse line:\n" << chunkLine << Endl;
        }
    }
    INFO_LOG << "Good " << good << " of " << total << Endl;
    INFO_LOG << "Max timestamp " << maxTimestamp << Endl;

    for (auto file : {&phraseFileStream, &bannerFileStream, &appFileStream, &offerFileStream}) {
        file->Flush();
    }
    for (auto file : {&phraseFile, &bannerFile, &appFile, &offerFile}) {
        file->Close();
    }
}
