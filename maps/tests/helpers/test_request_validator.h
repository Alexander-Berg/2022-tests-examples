#pragma once

#include <maps/wikimap/ugc/backoffice/src/lib/request_validator.h>

namespace maps::wiki::ugc::backoffice::tests {

class TestRequestValidator : public RequestValidator {
public:
    TestRequestValidator() {}

    void checkId(maps::auth::TvmId, const std::string&) const override {}
    void checkMetadataId(maps::auth::TvmId, const MetadataId) const override {}
};

} // maps::wiki::ugc::backoffice
