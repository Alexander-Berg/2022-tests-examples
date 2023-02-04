#include "testlib.h"

#include <library/cpp/monlib/metrics/metric_registry.h>

#include <util/system/env.h>

namespace NSolomon::NKv {

size_t TTestRpc::NumRequests() const {
    with_lock(Lock_) {
        return NumRequests_;
    }
}

void TTestRpc::SetHostDown(size_t numSuccessfulRequests, size_t numFailedRequests) {
    with_lock(Lock_) {
        NumRequestsToSucceed_ = numSuccessfulRequests;
        NumRequestsToFail_ = numFailedRequests;
    }
}

void TTestRpc::SetHostUp() {
    SetHostDown(0, 0);
}

void TTestRpc::Reset() {
    with_lock(Lock_) {
        NumRequests_ = 0;
        NumRequestsToSucceed_ = 0;
        NumRequestsToFail_ = 0;
    }
}

NKikimr::TKikimrAsyncResponse TTestRpc::SchemeDescribe(const NKikimrClient::TSchemeDescribe& req) {
    if (CountRequest()) {
        return MakeFailedFuture();
    } else {
        return RealRpc_->SchemeDescribe(req);
    }
}

NKikimr::TKikimrAsyncResponse TTestRpc::SchemeOperation(const NKikimrClient::TSchemeOperation& req) {
    if (CountRequest()) {
        return MakeFailedFuture();
    } else {
        return RealRpc_->SchemeOperation(req);
    }
}

NKikimr::TKikimrAsyncResponse TTestRpc::SchemeOperationStatus(const NKikimrClient::TSchemeOperationStatus& req) {
    if (CountRequest()) {
        return MakeFailedFuture();
    } else {
        return RealRpc_->SchemeOperationStatus(req);
    }
}

NKikimr::TKikimrAsyncResponse TTestRpc::HiveCreateTablet(const NKikimrClient::THiveCreateTablet& req) {
    if (CountRequest()) {
        return MakeFailedFuture();
    } else {
        return RealRpc_->HiveCreateTablet(req);
    }
}

NKikimr::TKikimrAsyncResponse TTestRpc::TabletStateRequest(const NKikimrClient::TTabletStateRequest& req) {
    if (CountRequest()) {
        return MakeFailedFuture();
    } else {
        return RealRpc_->TabletStateRequest(req);
    }
}

NKikimr::TKikimrAsyncResponse TTestRpc::LocalEnumerateTablets(const NKikimrClient::TLocalEnumerateTablets& req) {
    if (CountRequest()) {
        return MakeFailedFuture();
    } else {
        return RealRpc_->LocalEnumerateTablets(req);
    }
}

NKikimr::TKikimrAsyncResponse TTestRpc::KeyValue(const NKikimrClient::TKeyValueRequest& req) {
    if (CountRequest()) {
        return MakeFailedFuture();
    } else {
        return RealRpc_->KeyValue(req);
    }
}

NKikimr::TKikimrAsyncResponse TTestRpc::MakeFailedFuture() const {
    using TRes = typename NKikimr::TKikimrAsyncResponse::value_type;
    auto promise = NThreading::NewPromise<TRes>();
    promise.SetValue(TRes::FromError(grpc::UNAVAILABLE, "imitated network failure"));
    return promise.GetFuture();
}

bool TTestRpc::CountRequest() {
    with_lock(Lock_) {
        NumRequests_ += 1;

        if (NumRequestsToSucceed_ > 0) {
            NumRequestsToSucceed_--;
            return false;
        }

        if (NumRequestsToFail_ > 0) {
            if (NumRequestsToFail_ != Max<size_t>()) {
                NumRequestsToFail_--;
            }
            return true;
        }

        return false;
    }
}

void TTestRpc::Stop(bool) {
    // nop
}

void TTestFixture::SetUp() {
    Test::SetUp();

    Endpoint = GetEnv("YDB_ENDPOINT");
    Database = GetEnv("YDB_DATABASE");

    if (!Endpoint) {
        Y_FAIL(
            "YDB_ENDPOINT environment variable is not set. This may happen if you run test binary directly"
            " instead of invoking it with 'ya make -tt' or if you haven't included local YDB recipe into your"
            " test 'ya.make'.");
    }
}

std::shared_ptr<TTestRpc> TTestFixture::CreateRpc() {
    yandex::solomon::config::rpc::TGrpcClientConfig conf;
    conf.AddAddresses(Endpoint);
    return std::make_shared<TTestRpc>(NKikimr::CreateNodeGrpc(conf, *NMonitoring::TMetricRegistry::Instance()));
}

std::pair<std::shared_ptr<TTestRpc>, TKikimrKvClient> TTestFixture::CreateKvClient() {
    auto transport = CreateRpc();
    return {transport, TKikimrKvClient{transport.get()}};
}

} // namespace NSolomon::NKv
