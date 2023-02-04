#include "jupi.h"
#include "jupi_erf.h"

#include <robot/library/plutonium/protos/doc_wad_lumps.pb.h>
#include <robot/library/plutonium/protos/global_wad_lump.pb.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <kernel/doom/wad/wad.h>

#include <util/system/fs.h>
#include <util/system/event.h>

using namespace NRTYServer;
using namespace std::string_view_literals;


START_TEST_DEFINE_PARENT(TestRtyJupiErf, TestJupi)
private:
    NRTYServer::TMessage AddErfLumps(NRTYServer::TMessage msg) {
        Y_ENSURE(msg.HasDocument() && msg.GetDocument().HasIndexedDoc());
        NRealTime::TIndexedDoc* indexedDoc = msg.MutableDocument()->MutableIndexedDoc();
        NRealTime::TIndexedDoc::TMapType* lump = indexedDoc->AddWadLumps();
        lump->SetName("erf_doc_lumps");
        NPlutonium::TDocWadLumps docWadProto = GetWadLumpsFromCstr(
                "-", // mercury uses this as a shard name
                0, // mercury does not set docid
                // a serialized NPlutonium::TWadLumpsList value taken from //home/jupiter/shards_prepare/20180725-115421/11/erf_doc_lumps table
                "\n\xFD\1\n\nerf.struct\x12\xEE\1\x85i\x99\xCB\x8B?\xC5\xBAr\xB8\xE0\5\xC0\x19ie\xA7U\xE0\0\x08\4\3\xB8\1G\x1B\xE0\xEC_\xD9\xCE\x89@\xA5\xA9\0#`"
                    "\xF0\x7F\x85\xB3\376E\xA7\016E{\327f\xC8\xA5\x9B\xA9i\xC7\x17\307a6\x0E\xB3\x80\324A& \xB3\xEC\xB4y\x94\xD9x0\xF5\xE4jM\x96\xB9-\xB7\x9E\x15M\xD1\xA1"
                    "\xAA\3\x80*\\\xD5\xB7\xF5\xE2\xD5\1\xCD\5\x85=\x9B\x9C\xC3\32632\xA1\n\xA0\x0C\236Cl\xCC\xFB\1@\2\x0CH\x82\0\6\xE1\xAC\3\0248\x80s\x16\0\7\xAC\xCA\253"
                    "0(Q\x1D\x9D(\xE0YP\xFC\xDBV,\xA0q\xD1\0007\xFC\263A\xFC?\xE0\x11-@\3\x89\0\x80\xB7GA\0 \0X\x0F\xA8\x95\xC6\4T\xA5\0\nB)\xA2\xD2\7\xA0\x8B\5@u\xDE\4\0"
                    "\x84|0\xA2\0250\xC7\xD7\x80\x0C\xBE\0004\xC0\x0B\0@\xF5`\6\x81\x80\x82\x81S\x96\xD6\x14W\4\xA8X\x18\0\0\5\0\0"sv);
        lump->SetData(docWadProto.SerializeAsString());

        NRealTime::TIndexedDoc::TMapType* globalLump = indexedDoc->AddWadLumps();
        globalLump->SetName("erf_global_lumps");
        NPlutonium::TGlobalWadLump globalWadProto = GetGlobalLumpFromCstr(
                "-",
                // a serialized NPlutonium::TWadLump value taken from //home/jupiter/shards_prepare/20180725-115421/11/erf_global_lumps table
                TStringBuf("\n\017erf.struct_size\x12\4d\1\0\0"sv));
        globalLump->SetData(globalWadProto.SerializeAsString());
        return msg;
    }


public:
    bool InitConfig() override {
        (*ConfigDiff)["Indexer.Disk.Threads"] = "1";
        (*ConfigDiff)["AdditionalModules"] = "EXTBUILDER";
        (*ConfigDiff)["ModulesConfig.EXTBUILDER.DbgFlags"] = "dbg_mark_merged_as_incomplete, allow_missing_dependencies";
        SetNoSearch(true);
        return true;
    }
    bool Run() override {
        TVector<TMessage> messages1;
        messages1.push_back(AddErfLumps(GenerateAddMessage("http://aaa.com/a", 1)));
        messages1.push_back(AddErfLumps(GenerateAddMessage("http://aaa.com/b", 1)));
        IndexMessages(messages1, DISK, 1);
        ReopenIndexers();

        TVector<TMessage> messages2;
        messages2.push_back(AddErfLumps(GenerateAddMessage("http://aaa.com/c", 2)));
        messages2.push_back(AddErfLumps(GenerateAddMessage("http://aaa.com/b", 2)));
        IndexMessages(messages2, DISK, 1);
        ReopenIndexers();

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        auto mergedDirs = Controller->GetFinalIndexes(/*stopServer=*/true);
        CHECK_TEST_TRUE(mergedDirs.size() == 1);
        TFsPath resultDir(*mergedDirs.begin());
        CheckWadContent(resultDir / "indexerf2.wad");
        return true;
    }

private:
    bool CheckWadContent(const TFsPath& erf2) {
        CHECK_TEST_TRUE(erf2.Exists());

        using namespace NDoom;
        THolder<IWad> wad = IWad::Open(erf2);
        CHECK_TEST_TRUE(wad->HasGlobalLump(TWadLumpId(EWadIndexType::ErfIndexType, EWadLumpRole::StructModel)));
        CHECK_TEST_TRUE(wad->HasGlobalLump(TWadLumpId(EWadIndexType::ErfIndexType, EWadLumpRole::StructSize)));
        CHECK_TEST_EQ(wad->Size(), 3);
        TVector<TWadLumpId> wadIds = wad->DocLumps();
        CHECK_TEST_TRUE(wadIds.size() == 3);
        for (TWadLumpId wadId : wadIds) {
            CHECK_TEST_TRUE(wadId == TWadLumpId(EWadIndexType::ErfIndexType, EWadLumpRole::Struct));
        }
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestRtyJupiErfRebuild, TTestRtyJupiErfCaseClass)
    bool InitConfig() override {
        if (!TTestRtyJupiErfCaseClass::InitConfig())
            return false;
        (*ConfigDiff)["ModulesConfig.EXTBUILDER.ForceKeepPreparates"] = true;
        (*ConfigDiff)["ModulesConfig.EXTBUILDER.DbgFlags"] = "dbg_mark_merged_as_incomplete, force_rebuild, allow_missing_dependencies";
        return true;

        //TODO(yrum): check that ForceKeepPreparates is working (prep.lumps is present and readable in the merged index
        //TODO(yrum):TestRtyJupiErfRebuild, TestRtyJupiErf has some strange differences in wad metadata (no differences in keys, hits or docids)
    }
};
