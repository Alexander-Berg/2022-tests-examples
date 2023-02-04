#include "client_with_retries.h"

#include <infra/pod_agent/libs/porto_client/mock_client.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

namespace NInfra::NPodAgent::NPortoClientWithRetriesTest {

TLoggerConfig GetLoggerConfig() {
    TLoggerConfig result;
    result.SetBackend(TLoggerConfig_ELogBackend_STDERR);
    result.SetLevel("ERROR");
    return result;
}

class TPortoClientWithErrors : public TMockPortoClient {
public:
    TPortoClientWithErrors(
        ui32 cntTimeOut
        , TExpected<void, TPortoError> firstError
        , TExpected<void, TPortoError> otherError
    )
        : CntErrors_(cntTimeOut)
        , CurCntTries_(0)
        , FirstError_(firstError)
        , OtherError_(otherError)
    {}

    TExpected<void, TPortoError> Start(const TPortoContainerName& /* name */) override {
        ++CurCntTries_;

        if (CurCntTries_ == 1 && CntErrors_ >= 1) {
            return FirstError_;
        }

        if (CurCntTries_ <= CntErrors_) {
            return OtherError_;
        }

        return TExpected<void, TPortoError>::DefaultSuccess();
    }

private:
    ui32 CntErrors_;
    ui32 CurCntTries_;
    TPortoError FirstError_;
    TPortoError OtherError_;
};

static TLogger logger(GetLoggerConfig());

Y_UNIT_TEST_SUITE(TestPortoClientWithRetries) {

Y_UNIT_TEST(CreateClient) {
    UNIT_ASSERT_NO_EXCEPTION(TPortoClientWithRetries(new TMockPortoClient()));
}

Y_UNIT_TEST(SwitchLogFrame) {
    class TMyPortoClient: public TPortoClientWithRetries {
    public:
        TMyPortoClient(TPortoClientPtr client)
            : TPortoClientWithRetries(client)
        {}
    };

    {
        TPortoClientPtr client = new TPortoClientWithRetries(new TMockPortoClient());
        UNIT_ASSERT_UNEQUAL_C(dynamic_cast<TPortoClientWithRetries*>(client->SwitchLogFrame(logger.SpawnFrame()).Get()), nullptr, "bad object type");
        UNIT_ASSERT_EQUAL_C(dynamic_cast<TMyPortoClient*>(client->SwitchLogFrame(logger.SpawnFrame()).Get()), nullptr, "bad object type");
    }
    {
        TPortoClientPtr client = new TMyPortoClient(new TMockPortoClient());
        UNIT_ASSERT_UNEQUAL_C(dynamic_cast<TPortoClientWithRetries*>(client->SwitchLogFrame(logger.SpawnFrame()).Get()), nullptr, "bad object type");
        UNIT_ASSERT_UNEQUAL_C(dynamic_cast<TMyPortoClient*>(client->SwitchLogFrame(logger.SpawnFrame()).Get()), nullptr, "bad object type");
    }
}

Y_UNIT_TEST(BasicFunctions) {
    TPortoClientPtr client = new TPortoClientWithRetries(new TMockPortoClient());
    TPortoContainerName containerName("test");

    client->Start(containerName).Success();
    client->Stop(containerName).Success();
}

Y_UNIT_TEST(TestRetriesWithSocketErrors) {
    TVector<TExpected<void, TPortoError>> socketErrors = {
        TPortoError{.Code = EPortoError::SocketError}
        , TPortoError{.Code = EPortoError::SocketUnavailable}
        , TPortoError{.Code = EPortoError::SocketTimeout}
    };

    for (TExpected<void, TPortoError> errorTest : socketErrors) {
        TPortoClientPtr clientGood = new TPortoClientWithRetries(new TPortoClientWithErrors(0, errorTest, errorTest));
        TPortoClientPtr clientOk = new TPortoClientWithRetries(new TPortoClientWithErrors(DEFAULT_RETRIES - 1, errorTest, errorTest));
        TPortoClientPtr clientBad = new TPortoClientWithRetries(new TPortoClientWithErrors(DEFAULT_RETRIES, errorTest, errorTest));

        TExpected<void, TPortoError> result;
        TExpected<void, TPortoError> goodResult = TExpected<void, TPortoError>::DefaultSuccess();
        TExpected<void, TPortoError> badResult = errorTest;

        UNIT_ASSERT(result = clientGood->Start(TPortoContainerName("test")));
        UNIT_ASSERT_EQUAL(result, goodResult);

        UNIT_ASSERT(result = clientOk->Start(TPortoContainerName("test")));
        UNIT_ASSERT_EQUAL(result, goodResult);

        UNIT_ASSERT(!(result = clientBad->Start(TPortoContainerName("test"))));
        UNIT_ASSERT_EQUAL(result, badResult);
    }
}

Y_UNIT_TEST(TestRetriesWithNonSocketError) {
    TExpected<void, TPortoError> nonSocketError = TPortoError{.Code = EPortoError::Busy};
    TPortoClientPtr clientBad = new TPortoClientWithRetries(new TPortoClientWithErrors(1, nonSocketError, nonSocketError));

    TExpected<void, TPortoError> result;

    UNIT_ASSERT(!(result = clientBad->Start(TPortoContainerName("test"))));
    UNIT_ASSERT_EQUAL(result, nonSocketError);
}

Y_UNIT_TEST(TestRetriesWithPatchedResult) {
    TExpected<void, TPortoError> firstError = TPortoError{
        .Code = EPortoError::SocketError
        , .Message = "socket error"
    };

    {
        // Test with patched result
        TExpected<void, TPortoError> otherError = TPortoError{
            .Code = EPortoError::InvalidState
            , .Message = "in state running"
        };

        TPortoClientPtr client = new TPortoClientWithRetries(new TPortoClientWithErrors(DEFAULT_RETRIES, firstError, otherError));
        TExpected<void, TPortoError> result = client->Start(TPortoContainerName("test"));
        TExpected<void, TPortoError> goodResult = TExpected<void, TPortoError>::DefaultSuccess();

        UNIT_ASSERT(result);
        UNIT_ASSERT_EQUAL(result, goodResult);
    }

    {
        // Test with normal result on first try
        TExpected<void, TPortoError> firstAndOtherError = TPortoError{
            .Code = EPortoError::InvalidState
            , .Message = "in state running"
        };

        TPortoClientPtr client = new TPortoClientWithRetries(new TPortoClientWithErrors(DEFAULT_RETRIES, firstAndOtherError, firstAndOtherError));
        TExpected<void, TPortoError> result = client->Start(TPortoContainerName("test"));
        TExpected<void, TPortoError> goodResult = TExpected<void, TPortoError>::DefaultSuccess();

        UNIT_ASSERT(!result);
        UNIT_ASSERT_EQUAL(result, firstAndOtherError);
    }

    {
        // Test with normal result
        TExpected<void, TPortoError> otherError = TPortoError{
            .Code = EPortoError::InvalidState
            , .Message = "in state dead"
        };

        TPortoClientPtr client = new TPortoClientWithRetries(new TPortoClientWithErrors(DEFAULT_RETRIES, firstError, otherError));
        TExpected<void, TPortoError> result = client->Start(TPortoContainerName("test"));

        UNIT_ASSERT(!result);
        UNIT_ASSERT_EQUAL(result, otherError);
    }
}

}

} // NInfra::NPodAgent::NPortoClientWithRetriesTest
