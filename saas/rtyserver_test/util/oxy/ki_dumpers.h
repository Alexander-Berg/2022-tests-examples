#pragma once
#include <saas/rtyserver/components/oxy/doom/ki_doom_merger_io.h>

#include <kernel/doom/offroad_attributes_wad/offroad_attributes_wad_io.h>
#include <kernel/doom/offroad_key_wad/offroad_key_wad_io.h>
#include <kernel/doom/offroad_doc_wad/offroad_doc_wad_io.h>
#include <kernel/doom/offroad_wad/offroad_ann_wad_io.h>
#include <kernel/doom/key/key_decoder.h>
// #include <kernel/doom/wad/wad.h>

#include <kernel/doom/adaptors/read_adaptors.h>
#include <kernel/doom/adaptors/key_filtering_index_reader.h>

#include <kernel/doom/yandex/yandex_io.h>

#include <saas/rtyserver_test/util/oxy/docid_restore.h>
#include <util/folder/path.h>
#include <util/generic/map.h>
#include <util/string/printf.h>
#include <util/string/builder.h>

class TRTYKeyInvDumper {
public:
    struct TIdentityHitWriter {
        template<typename THit>
        TString operator()(const THit& hit) const {
            return TStringBuilder() << hit;
        }
    };

    template<typename THitWriter = TIdentityHitWriter>
    static TVector<TString> DumpYandexKeyInv(const TString& prefix, const THitWriter& writer = THitWriter()) {
        using TKeyInvReader = NDoom::TYandexIo::TReader;
        TKeyInvReader reader(prefix);
        reader.Restart();

        using TKeyRef = TKeyInvReader::TKeyRef;
        using THit = TKeyInvReader::THit;

        TVector<TString> result;
        TStringStream dumpedHit;
        TKeyRef key;
        while (reader.ReadKey(&key)) {
            TString escapedKey = EscapeKey(key);

            bool hadHits = false;

            THit hit;
            while (reader.ReadHit(&hit)) {
                dumpedHit.Clear();
                dumpedHit << escapedKey << "\t" << writer(hit);
                result.push_back(dumpedHit.Str());
                hadHits = true;
            }

            if (!hadHits) { //should not happen
                dumpedHit.Clear();
                dumpedHit << escapedKey << "\t<no hits>";
                result.push_back(dumpedHit.Str());;
            }
        }

        Sort(result);
        return result;
    }

    // Dumps <key>\t<termId> from keyinvwad
    static TVector<TString> DumpKeysTableFromWad(const TString& file) {
        using TIo = NDoom::TOffroadFactorAnnKeyWadIo;

        TVector<TString> result;
        THolder<NDoom::IWad> wad = NDoom::IWad::Open(file);
        //TBlob keys = wad->LoadGlobalLump(NDoom::TWadLumpId(NDoom::FactorAnnIndexType, NDoom::EWadLumpRole::Keys));
        TIo::TReader reader(wad.Get());
        TStringBuf key;
        TIo::TReader::TKeyData termId;
        while (reader.ReadKey(&key, &termId)) {
            TString escapedKey = EscapeKey(key);
            TStringStream dump;
            result.push_back((dump << escapedKey << "\t" << termId, dump.Str()));
        }

        Sort(result);
        return result;
    }

    static TString DecodeKey(TStringBuf key) {
        NDoom::TKeyDecoder decoder;
        NDoom::TDecodedKey decodedKey;
        decoder.Decode(key, &decodedKey);
        TString keyStr;
        TStringOutput out(keyStr);
        out << decodedKey;
        return keyStr;
    }

    static void PrintHit(IOutputStream& out, const NDoom::TReqBundleHit& hit, bool skipTermId) {
        out << "[";
        if (skipTermId) {
            out << '_';
        } else {
            out << hit.DocId();
        }
        out << "." << hit.Break() << "." << hit.Word() << "." << hit.Relevance() << "." << hit.Form() << "]";
    }

    // Dumps <key>\t<docId>\t<hit>\t<url> from keyinvwad
    static TVector<TString> DumpKeyInvFromWad(const TString& file, const NSaas::TId2Url* urlResolver=nullptr, bool skipTermId = false) {
        using namespace NDoom;

        TVector<TString> result;
        THolder<IWad> wad = IWad::Open(file);
        size_t docCount = wad->Size();

        //
        // Dump keys into a map, to restore terms from termIds later
        //
        using TKeyIo = NDoom::NRTYServer::TTextWadKeyIo;

        TMap<ui32, TString> termById;
        {
            TKeyIo::TReader reader(wad.Get());
            TStringBuf key;
            TKeyIo::TReader::TKeyData termId;
            while (reader.ReadKey(&key, &termId)) {
                termById[termId] = EscapeKey(DecodeKey(key));
            }
        }

        //
        // Dump hits from doclumps
        //
        using TDocIo = NDoom::NRTYServer::TTextWadDocIo;

        TDocIo::TReader reader(wad.Get());
        for (ui32 docId = 0; docId < docCount; docId++) {
            ui32 changedDocId = docId; // never changes?

            if (!reader.ReadDoc(&changedDocId)) {
                // should not happen
                result.push_back(Sprintf("\t%09u\t<docid not found>", changedDocId));
                continue;
            }

            TString url, docIdStr;
            if (urlResolver && urlResolver->contains(changedDocId)) {
                url = urlResolver->find(changedDocId)->second;
                docIdStr = "-";
            } else {
                docIdStr = Sprintf("%09u", changedDocId);
            }

            TDocIo::TReader::THit hit;
            while (reader.ReadHit(&hit)) {
                ui32 termId = hit.DocId(); // sic!

                auto&& iter = termById.find(termId);
                TString termStr = iter == termById.end() ? Sprintf("<term no %u>", termId) : iter->second;
                TStringStream dump;
                dump << termStr << "\t" << docIdStr << "\t";
                PrintHit(dump, hit, skipTermId);
                dump << "\t" << url;
                result.push_back(dump.Str());
            }
        }

        Sort(result);
        return result;
    }

    static TVector<TString> DumpKeyInvAttrsFromWad(const TString& prefix) {
        using TReader = NDoom::TOffroadAttributesIo::TReader;
        using TKeyRef = TReader::TKeyRef;
        using TKeyData = TReader::TKeyData;
        using THit = TReader::THit;

        THolder<NDoom::IWad> keyWad = NDoom::IWad::Open(prefix + ".key.wad");
        THolder<NDoom::IWad> invWad = NDoom::IWad::Open(prefix + ".inv.wad");

        TReader reader(invWad.Get(), keyWad.Get());

        TKeyRef keyRef;
        TKeyData keyData;

        TVector<TString> result;
        while (reader.ReadKey(&keyRef, &keyData)) {
            TString escapedKey = EscapeKey(keyRef);

            bool hadHits = false;

            THit hit;
            while (reader.ReadHit(&hit)) {
                result.push_back(TStringBuilder() << escapedKey << '\t' << hit);
                hadHits = true;
            }

            if (!hadHits) {
                result.push_back(escapedKey + "\t<no hits>");
            }
        }

        Sort(result);
        return result;
    }

    // Diff routine: compare two sorted sequences of strings
    static TVector<TString> ZipDiffDumps(const TVector<TString>& a, const TVector<TString>& b);

    // A helper routine for tests: make ZipDiffDumps and store the outcome in a nice format in FS
    static bool ZipDiffHelper(const TVector<TString>& expected, const TVector<TString>& actual, const TFsPath expectedFile, const TFsPath actualFile);

private:
    // Char escaping for a tsv-compartible output
    static TString EscapeKey(const TStringBuf& key);
};
