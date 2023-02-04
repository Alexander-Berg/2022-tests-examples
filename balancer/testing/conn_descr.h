#pragma once

#include <balancer/kernel/module/conn_descr.h>
#include <balancer/kernel/custom_io/null.h>
#include <balancer/kernel/http/parser/http.h>

namespace NSrvKernel::NTesting {

    class TTestConnDescr : TNonCopyable {
    public:
        explicit TTestConnDescr(IWorkerCtl& process)
            : TcpProps_(process, RemoteAddress_, LocalAddress_, nullptr)
        {
            Init();
        }

        explicit TTestConnDescr(IIoInput* in, IWorkerCtl& process)
            : TcpProps_(process, RemoteAddress_, LocalAddress_, nullptr)
            , ConnDescr_(*in, Out_, Props_)
        {
            Init();
        }

        TConnDescr& ConnDescr() {
            return ConnDescr_;
        }

    private:
        void Init() {
            ConnDescr_.Request = &TestRequest_;
            ConnDescr_.Hash = 0x12345;
            ConnDescr_.ExtraAccessLog = &AccessLog_;
            ConnDescr_.ErrorLog = &ErrorLog_;
        }

    private:
        TAddrHolder RemoteAddress_{ &TDummyAddr::Instance() };
        TAddrHolder LocalAddress_{ &TDummyAddr::Instance() };
        TNullOutput AccessLog_;
        TLog ErrorLog_;
        TNullStream In_;
        TNullStream Out_;
        TTcpConnProps TcpProps_;
        TConnProps Props_{ TcpProps_, TInstant::Zero(), 0, nullptr };
        TRequest TestRequest_;
        TConnDescr ConnDescr_{ In_, Out_, Props_};
    };
}
