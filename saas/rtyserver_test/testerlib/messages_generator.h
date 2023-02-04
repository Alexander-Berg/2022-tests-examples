#pragma once
#include <util/datetime/base.h>
#include <util/generic/map.h>
#include <util/system/types.h>
#include <library/cpp/deprecated/atomic/atomic.h>
#include <saas/protos/rtyserver.pb.h>

class IAttributesFiller {
public:
    virtual ~IAttributesFiller() {}
    virtual void Fill(ui32 docIndex, i64 messId, NRTYServer::TMessage::TDocument& document) const = 0;
};

class IDocumentGenerator {
private:
    TMap<TString, TAtomicSharedPtr<IAttributesFiller> > AttributesFillers;
public:
    virtual ~IDocumentGenerator() {}

    void RegisterFiller(TString name, IAttributesFiller* filler) {
        AttributesFillers[name] = filler;
    }

    void BuildDocument(ui32 docIndex, i64 messId, NRTYServer::TMessage::TDocument& document) const {
        BuildDocumentProperties(docIndex, messId, document);
        for (TMap<TString, TAtomicSharedPtr<IAttributesFiller> >::const_iterator i = AttributesFillers.begin(), e = AttributesFillers.end(); i != e; ++i) {
            i->second->Fill(docIndex, messId, document);
        }
    }

    virtual void BuildDocumentProperties(ui32 docIndex, i64 messId, NRTYServer::TMessage::TDocument& document) const = 0;
};

class IMessageGenerator {
private:
    TAtomicSharedPtr<IDocumentGenerator> DocumentGenerator;
    static TAtomic GlobalCountMessages;
    bool NeedInReply;
public:

    virtual ~IMessageGenerator() {}

    static i64 CreateMessageId() { return AtomicIncrement(GlobalCountMessages); }

    static void ResetMessageId() { return AtomicSet(GlobalCountMessages, 0); }

    IMessageGenerator(IDocumentGenerator* documentGenerator, bool needInReply)
        : DocumentGenerator(documentGenerator)
        , NeedInReply(needInReply)
    {

    }

    void BuildMessage(ui32 messIndex, NRTYServer::TMessage& message) const {
        i64 messId = IMessageGenerator::CreateMessageId();
        BuildMessageFeatures(messIndex, messId, message);
        if (NeedInReply)
            message.SetMessageId(messId);
        message.MutableDocument()->SetModificationTimestamp(Seconds());
        DocumentGenerator->BuildDocument(messIndex, messId, *message.MutableDocument());
    }

    virtual void BuildMessageFeatures(ui32 messIndex, i64 messId, NRTYServer::TMessage& message) const = 0;

};

// doesn't set KeyPrefix if |kps| < 0
NRTYServer::TMessage CreateSimpleKVMessage(const TString& key, const TString& value, const TString& attr, int kps);
