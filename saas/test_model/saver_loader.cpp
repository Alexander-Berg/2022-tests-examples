#include "saver_loader.h"
#include <library/cpp/json/json_writer.h>
#include <util/system/file.h>
#include <util/generic/buffer.h>
#include <saas/util/logging/exception_process.h>

class TFileWrapper {
public:
    constexpr static EOpenMode omReadWrite = RdWr | OpenAlways;

    TFileWrapper(const TFsPath& fileName, EOpenMode mode)
        : FileName(fileName)
        , Mode(mode)
    {}

    TBuffer& GetBlock() {
        return CurrentBlock;
    }

    ui64 GetBlockVersion() const {
        return CurrentBlockVersion;
    }

    bool ReadBlock() {
        if (!Open())
            return false;
        ui64 size;
        if (File->Read(&size, sizeof(size)) != sizeof(size))
            return false;
        if (File->Read(&CurrentBlockVersion, sizeof(ui64)) != sizeof(ui64))
            return false;
        CurrentBlock.Resize(size);
        if (File->Read(CurrentBlock.Data(), size) != size)
            return false;
        return true;
    }

    bool WriteBlock(ui64 version) {
        if (Mode != omReadWrite || !Open())
            return false;
        ui64 size = CurrentBlock.Size();
        File->Write(&size, sizeof(size));
        File->Write(&version, sizeof(version));
        File->Write(CurrentBlock.Data(), CurrentBlock.Size());
        CurrentBlockVersion = version;
        return true;
    }

    void Clear() {
        Open();
        if (Mode != omReadWrite || !Open())
            return;
        File->Resize(0);
    }

private:
    bool Open() {
        if (!!File)
            return true;
        TRY
            File.Reset(new TFile(FileName.GetPath(), Mode));
            return true;
        CATCH("");
        return false;
    }
    THolder<TFile> File;
    const TFsPath FileName;
    EOpenMode Mode;
    TBuffer CurrentBlock;
    ui64 CurrentBlockVersion;
};

const EOpenMode TFileWrapper::omReadWrite;

class TDbTypeToString : public TEnumToString<TDbData::TType, TString> {
public:
    TDbTypeToString() {
        Register(TDbData::LUNAPARK, "Lunapark");
        Register(TDbData::AQUA, "Aqua");
        Register(TDbData::ROBOT, "Robot");
        Register(TDbData::TEST_ENVIRONMENT, "TE");
    }
protected:
    TDbData::TType DefaultEnumValue() const override {
        return TDbData::TEST_ENVIRONMENT;
    }
};

const ui64 TModelSaver::DefaultVersion = 1;

TModelSaver::TModelSaver(const TSaveLoadConfig& config)
    : Config(config)
{}

TFsPath TModelSaver::GetDbPath(const TDataBase& db) const {
    return Config.RootDir / db.GetData().Name;
}

TFsPath TModelSaver::GetTestPath(const TDataBase& db, const TTest& test) const {
    return GetDbPath(db) / test.GetData().Name;
}

void TModelSaver::SaveTest(const TDataBase& db, const TTest& test, ui64 fromRevision, ui64 toRevision, ui64 version) {
    if (fromRevision >= toRevision)
        return;
    SaveDbData(db, version);
    SaveTestData(db, test, version);
    TReaderPtr<const TTest::TChildren> executions = test.GetChildren();
    for(TTest::TChildren::const_iterator i = executions->lower_bound(fromRevision); i != executions->end() && i->first <= toRevision; ++i)
        SaveTestExecution(db, test, *i->second, version);
}

void TModelSaver::SaveDbData(const TDataBase& db, ui64 version) {
    TFsPath file = GetDbPath(db) / "dbdata";
    TFileWrapper wr(file, TFileWrapper::omReadWrite);
    if (wr.ReadBlock() && wr.GetBlockVersion() == version)
        return;
    wr.Clear();
    NJson::TJsonValue dbVal(NJson::JSON_MAP);
    dbVal.InsertValue("name", db.GetData().Name);
    dbVal.InsertValue("type", Singleton<TDbTypeToString>()->Get(db.GetData().Type));
    TStringStream out;
    NJson::WriteJson(&out, &dbVal);
    wr.GetBlock().Resize(out.Size());
    memcpy(wr.GetBlock().Data(), out.Data(), out.Size());
    wr.WriteBlock(version);
}

void TModelSaver::SaveTestData(const TDataBase& db, const TTest& test, ui64 version) {
    TFsPath file = GetTestPath(db, test) / "dbdata";
    TFileWrapper wr(file, TFileWrapper::omReadWrite);
    if (wr.ReadBlock() && wr.GetBlockVersion() == version)
        return;
    wr.Clear();
    NJson::TJsonValue testVal(NJson::JSON_MAP);
    testVal.InsertValue("name", test.GetData().Name);
    TStringStream out;
    NJson::WriteJson(&out, &testVal);
    wr.GetBlock().Resize(out.Size());
    memcpy(wr.GetBlock().Data(), out.Data(), out.Size());
    wr.WriteBlock(version);
}

void TModelSaver::SaveTestExecution(const TDataBase& /*db*/, const TTest& /*test*/, const TTestExecution& /*execution*/, ui64 /*version*/) {

}
