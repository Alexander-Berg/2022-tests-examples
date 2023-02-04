#define BOOST_TEST_ALTERNATIVE_INIT_API
#include "common.h"
#include "suite.h"

#include "../test_types/face_validation_test_data.h"

#include "../test_types/mock_storage.h"
#include "../test_tools/events_builder.h"
#include "../test_tools/storage_diff_helpers.h"
#include "../test_tools/test_suite.h"

#include <yandex/maps/wiki/topo/cache.h>
#include <yandex/maps/wiki/topo/face_validator.h>
#include <boost/test/unit_test.hpp>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::topo;
using namespace maps::wiki::topo::test;

class FaceValidationTestRunner
{
public:

    static void run(const FaceValidationTestData& test)
    {
        if (test.type() == FaceValidationTestData::Type::Incorrect) {
            runIncorrect(test);
        } else {
            runCorrect(test);
        }
    }

private:
    struct TestContext {
        MockStorage originalStorage;
        MockStorage storage;
        std::unique_ptr<Cache> cache;
        std::unique_ptr<FaceValidator> validator;
    };

    static std::unique_ptr<TestContext>
    prepareTestContext(const FaceValidationTestData& test)
    {
        std::unique_ptr<TestContext> result = std::make_unique<TestContext>();
        result->originalStorage = test.original();
        result->storage = test.result();
        result->storage.setOriginal(&result->originalStorage);
        result->storage.setFaceRelationsAvailability(test.faceRelationsAvailability());
        result->cache = std::make_unique<Cache>(result->storage, geolib3::EPS);
        result->validator = result->cache->faceValidator();

        return result;
    }

    static void runCorrect(const FaceValidationTestData& test)
    {
        auto context = prepareTestContext(test);
        BOOST_CHECK_NO_THROW((*context->validator)(test.faceId()));
    }

    static void runIncorrect(const FaceValidationTestData& test)
    {
        auto context = prepareTestContext(test);
        BOOST_CHECK_THROW((*context->validator)(test.faceId()), InvalidFaceError);
    }
};

// tests init and run

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    BoostTestSuiteBuilder<FaceValidationTestData, FaceValidationTestRunner> builder(
        boost::unit_test::framework::master_test_suite());
    mainTestSuite()->visit(builder);
    return nullptr;
}

#ifdef YANDEX_MAPS_BUILD
bool init_unit_test_suite()
{
    init_unit_test_suite(0, NULL);
    return true;
}
int main(int argc, char** argv)
{
    return boost::unit_test::unit_test_main(&init_unit_test_suite, argc, argv);
}
#endif //YANDEX_MAPS_BUILD
