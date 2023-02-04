#pragma once

#include <saas/protos/rtyserver.pb.h>
#include <saas/rtyserver_test/util/oxy/kiwi_util.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestJupi)
    //
    // We generate minimal messages here (even without keyinvs),
    // rty_jupi should work without any support from OXY-build indexes
    //
    void GenerateMessage(NRTYServer::TMessage& message, NRTYServer::TMessage::TMessageType messageType) {
        i64 messId = IMessageGenerator::CreateMessageId();
        message.SetMessageType(messageType);
        if (SendIndexReply)
            message.SetMessageId(messId);
    }

    NRTYServer::TMessage GenerateAddMessage(const TString& url, ui64 saasVersion) {
        using namespace NRTYServer;
        NRTYServer::TMessage message;
        GenerateMessage(message, NRTYServer::TMessage::ADD_DOCUMENT);

        TMessage::TDocument* doc = message.MutableDocument();
        doc->SetUrl(url);
        doc->SetVersion(saasVersion);
        doc->ClearBody();

        NKiwi::TKiwiObject kiwiObj;
        NSaas::KiwiAddTuple(&kiwiObj, 0, "URL", url);
        NSaas::KiwiAddNumericTuple<ui16>(&kiwiObj, 0, "MaxFreq", 1); // for indexfrq building

        NRealTime::TIndexedDoc* indexedDoc = doc->MutableIndexedDoc();
        indexedDoc->SetUrl(url);
        indexedDoc->SetKiwiObject(TString{kiwiObj.AsStringBuf()});
        return message;
    }
};
