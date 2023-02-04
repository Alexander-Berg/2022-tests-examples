#pragma once
#include "messages_generator.h"
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/protos/rtyserver.pb.h>
#include <util/system/types.h>
#include <util/generic/set.h>
#include <util/generic/vector.h>
#include <util/generic/string.h>
#include <util/string/cast.h>
#include <library/cpp/logger/global/global.h>

class TStandartAttributesFiller: public IAttributesFiller {
public:
    enum TAttributeType {
        atGroup = 1,
        atLitSearch = 2,
        atIntSearch = 4,
        atProp = 8,
        atFactor = 16
    };
    typedef ui32 TAttributeTypeSet;
    struct TAttributeValue {
        TString Name;
        TString Value;
        TAttributeTypeSet Types;
        TAttributeValue(const TString& name, const TString& value, TAttributeTypeSet types) {
            Name = name;
            Value = value;
            Types = types;
        }
    };
    typedef TVector<TAttributeValue> TDocAttrs;
private:
    TDocAttrs CommonAttributes;
    TVector<TDocAttrs> DocAttributes;

    void FillDocAttr(const TAttributeValue& attr, NRTYServer::TMessage::TDocument& document) const;

    void FillDocAttrs(const TDocAttrs& Attributes, NRTYServer::TMessage::TDocument& document) const {
        for (TDocAttrs::const_iterator i = Attributes.begin(), e = Attributes.end(); i != e; ++i) {
            FillDocAttr(*i, document);
        }
    }
public:
    virtual void Fill(ui32 docIndex, i64 /*messId*/, NRTYServer::TMessage::TDocument& document) const {
        FillDocAttrs(CommonAttributes, document);
        if (DocAttributes.size()) {
            VERIFY_WITH_LOG(docIndex < DocAttributes.size(), "Incorrect fill method usage");
            FillDocAttrs(DocAttributes[docIndex], document);
        }
    }

    void AddCommonAttribute(const TString& name, const TString& value, TAttributeTypeSet types) {
        CommonAttributes.push_back(TAttributeValue(name, value, types));
    }

    void SetDocumentsCount(ui32 numDocs) {
        DocAttributes.resize(numDocs);
    }

    void AddDocAttribute(ui32 docIndex, const TString& name, const TString& value, TAttributeTypeSet types) {
        VERIFY_WITH_LOG(docIndex < DocAttributes.size(), "Incorrect AddDocAttribute method usage");
        DocAttributes[docIndex].push_back(TAttributeValue(name, value, types));
    }
};

class TStandartDocumentGenerator: public IDocumentGenerator {
public:
    enum TPrefixGeneratorPolicy {pgpGrow, pgpConstant};
    enum TTextGeneratorPolicy {tgpConstant, tgpRandom, tgpArray};
private:
    bool IsPrefixed;
    TPrefixGeneratorPolicy PrefixPolicy;
    ui64 PrefixConstant;

    TTextGeneratorPolicy TextPolicy;
    TString TextConstant;
    TVector<TString> Texts;
public:

    void SetPrefixConstant(ui64 prefixConstant) {
        PrefixConstant = prefixConstant;
        PrefixPolicy = pgpConstant;
    }

    void SetTextsArray(const TVector<TString>& texts) {
        Texts = texts;
        TextPolicy = tgpArray;
    }

    void SetTextConstant(const TString& text) {
        TextConstant = text;
        TextPolicy = tgpConstant;
    }

    void SetTextRandom() {
        TextPolicy = tgpRandom;
    }

    static const int BASE_KEY_PREFIX = 100000;

    TStandartDocumentGenerator(bool isPrefixed)
        : IsPrefixed(isPrefixed)
        , PrefixPolicy(pgpConstant)
        , PrefixConstant(1)
        , TextPolicy(tgpConstant)
        , TextConstant("body")
    {

    }

    virtual void BuildDocumentProperties(ui32 docIndex, i64 messId, NRTYServer::TMessage::TDocument& document) const;

    static TStandartDocumentGenerator* Instance(bool isPrefixed) {
        return new TStandartDocumentGenerator(isPrefixed);
    }

};

class TStandartMessagesGenerator: public IMessageGenerator {
private:
    NRTYServer::TMessage::TMessageType MessageType;
public:

    void SetMessageType(NRTYServer::TMessage::TMessageType messageType) {
        MessageType = messageType;
    }

    TStandartMessagesGenerator(IDocumentGenerator* documentGenerator, bool needInReply)
        : IMessageGenerator(documentGenerator, needInReply)
        , MessageType(NRTYServer::TMessage::ADD_DOCUMENT)
    {

    }

    virtual void BuildMessageFeatures(ui32 /*messIndex*/, i64 /*messId*/, NRTYServer::TMessage& message) const {
        message.SetMessageType(MessageType);
    }

    static TStandartMessagesGenerator Instance(bool needInReply, bool IsPrefixed) {
        return TStandartMessagesGenerator(TStandartDocumentGenerator::Instance(IsPrefixed), needInReply);
    }

};
