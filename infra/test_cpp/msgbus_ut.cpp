#include <library/cpp/testing/unittest/registar.h>
#include <infra/netlibus/msgbus.h>
#include <util/generic/ptr.h>
#include <util/generic/xrange.h>
#include <util/datetime/base.h>
#include <util/system/thread.h>

struct TReceiverData {
    TSimpleSharedPtr<TMsgBus> BusPtr;
    TManualEvent Ready;
};

void* ThreadSpawner(void *) {
    for (int i = 0; i < 100; ++i) {
        TMsgBus bus;
        bus.Start();
        Sleep(TDuration::MilliSeconds(100));
    }
    return nullptr;
}

void* ThreadReceiver(void *bus) {
    TReceiverData *data = reinterpret_cast<TReceiverData*>(bus);
    for (int i = 0; i < 10; ++i) {
        data->BusPtr->Receive(true, 5.0);
    }
    data->Ready.Signal();
    return nullptr;
}

Y_UNIT_TEST_SUITE(MsgBus) {
    void AbortOnSendResult(void *, TString, TString, TMsgBus::ESendResult) {
        UNIT_ASSERT(false);
    }

    Y_UNIT_TEST(DestroyWhileSending) {
        TAutoPtr<TMsgBus> bus = new TMsgBus();
        bus->SetSendResultCallback(nullptr, &AbortOnSendResult);
        bus->Start();
        for (auto i : xrange(500)) {
            Y_UNUSED(i);
            bus->Send(TString(1 << 20, 'x'), TString("[::1]:1"), TString());
        }
        bus.Destroy();
        Sleep(TDuration::Seconds(1));
    }

    Y_UNIT_TEST(ManyBusesInThreads) {
        TVector<TSimpleSharedPtr<TThread>> threads;
        for (int i = 0; i < 10; ++i) {
            auto thr = MakeSimpleShared<TThread>(
                TThread::TParams(&ThreadSpawner, nullptr).SetName("netliba_test")
            );
            thr->Start();
            threads.push_back(thr);
        }
        for (auto thr : threads)
            thr->Join();
    }

    Y_UNIT_TEST(Ipv4InIpv6) {
        TAutoPtr<TMsgBus> bus = new TMsgBus();
        bus->SetSendResultCallback(nullptr, &AbortOnSendResult);
        bus->Start();
        bus->Send("test", TString("[::ffff:127.0.0.1]:12345"), TString());

        TDeque<NNetliba_v12::TUdpAddress> candidates;
        candidates.push_back(CreateAddress("::ffff:127.0.0.1", 12345, NNetliba_v12::UAT_ANY));
        bus->Send("test", candidates, TString());

        bus.Destroy();
        Sleep(TDuration::Seconds(1));
    }

    // what da hell? should work
    /*Y_UNIT_TEST(OccupiedPort) {
        TAutoPtr<TMsgBus> bus = new TMsgBus();
        bus->Start();
        Sleep(TDuration::Seconds(1));
        UNIT_CHECK_GENERATED_EXCEPTION_C(TMsgBus(bus->GetListenPort()), yexception, ": double bind succeeded " << bus->GetListenPort());
        bus.Destroy();
    }*/

    Y_UNIT_TEST(BusTimeout) {
        TAutoPtr<TMsgBus> bus = new TMsgBus();
        bus->Start();
        UNIT_CHECK_GENERATED_EXCEPTION_C(bus->Receive(true, 0.5), yexception, ": receive with timeout");
        UNIT_CHECK_GENERATED_EXCEPTION_C(bus->Receive(false, 0.5), yexception, ": nonblocking receive");
        bus.Destroy();
        Sleep(TDuration::Seconds(1));
    }

    Y_UNIT_TEST(Resending) {
        TSimpleSharedPtr<TMsgBus> bus = MakeSimpleShared<TMsgBus>(0, 2.0f);
        bus->Start();

        TSimpleSharedPtr<TReceiverData> receiverData = new TReceiverData{bus, TManualEvent()};
        auto thr = MakeSimpleShared<TThread>(TThread::TParams(&ThreadReceiver, receiverData.Get()).SetName("resending_receiver"));
        thr->Start();

        for (int i = 0; i < 10; ++i) {
            TDeque<NNetliba_v12::TUdpAddress> candidates;
            candidates.push_back(CreateAddress("127.0.0.1", bus->GetListenPort() + 1000, NNetliba_v12::UAT_ANY));
            candidates.push_back(CreateAddress("127.0.0.1", bus->GetListenPort(), NNetliba_v12::UAT_ANY));
            bus->Send(TString(1 << 20, 'x'), candidates, TString());
        }
        if (!receiverData->Ready.WaitT(TDuration::Seconds(5))) {
            UNIT_ASSERT(false);
        }
        thr->Join();
    }

    Y_UNIT_TEST(ResendingWrongAddr) {
        TSimpleSharedPtr<TMsgBus> bus = MakeSimpleShared<TMsgBus>(0, 1.0f);
        bus->Start();

        TSimpleSharedPtr<TReceiverData> receiverData = new TReceiverData{bus, TManualEvent()};
        auto thr = MakeSimpleShared<TThread>(TThread::TParams(&ThreadReceiver, receiverData.Get()).SetName("resending_receiver"));
        thr->Start();

        for (int i = 0; i < 10; ++i) {
            TDeque<NNetliba_v12::TUdpAddress> candidates;
            candidates.push_back(CreateAddress("2a02:6b8::a03e", bus->GetListenPort(), NNetliba_v12::UAT_ANY));
            candidates.push_back(CreateAddress("127.0.0.1", bus->GetListenPort(), NNetliba_v12::UAT_ANY));
            bus->Send(TString(1 << 20, 'x'), candidates, TString());
        }
        if (!receiverData->Ready.WaitT(TDuration::Seconds(5))) {
            UNIT_ASSERT(false);
        }
        thr->Join();
    }

    Y_UNIT_TEST(WrongHostName) {
        TSimpleSharedPtr<TMsgBus> bus = MakeSimpleShared<TMsgBus>(0, 1.0f);
        UNIT_CHECK_GENERATED_EXCEPTION_C(bus->Send("test", "32625:::12345", TString()), TBusResolveError, ": send wrong hostname");
        UNIT_CHECK_GENERATED_EXCEPTION_C(bus->Send("test", "somenonexistentaddr.kokoko", TString()), TBusResolveError, ": send wrong hostname");
    }

}
