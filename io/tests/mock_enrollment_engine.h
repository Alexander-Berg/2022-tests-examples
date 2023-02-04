#pragma once

#include <gmock/gmock.h>

#include <voicetech/bio/ondevice/lib/engine/enrollment_engine.h>

namespace quasar {

    class MockEnrollmentEngine: public NBio::NDevice::IEnrollmentEngine {
    public:
        MOCK_METHOD(void, SetEnrollments, (const TEnrollments&), (override));
        MOCK_METHOD((THashMap<TEnrollmentID, TString>), GetEnrollmentsVersionInfo, (), (const, override));

        MOCK_METHOD(void, StartRequest, (), (override));
        MOCK_METHOD(bool, IsRequestActive, (), (const, override));
        MOCK_METHOD(void, AddChunk, (TConstArrayRef<unsigned char>, bool), (override));
        MOCK_METHOD(void, SetRequestExternalVoiceprint, (TBlob), (override));
        MOCK_METHOD(NJson::TJsonValue, FinishRequest, (), (override));

        MOCK_METHOD(void, StartEnrollment, (), (override));
        MOCK_METHOD(TBlob, CommitEnrollment, (), (override));
        MOCK_METHOD(void, CancelEnrollment, (), (override));
        MOCK_METHOD(bool, IsEnrollmentActive, (), (const, override));
        MOCK_METHOD(TMaybe<TEnrollmentID>, GetMatchedEnrollmentID, (), (const, override));
    };

} // namespace quasar
