#include "../include/metadataTest.h"
#include "tests/boost-tests/include/tools/map_tools.h"

#include <boost/lexical_cast.hpp>

using namespace boost::unit_test;
using namespace maps::renderer;

namespace maps { namespace renderer5 { namespace test { namespace metadata {

    namespace {

        void test_layers_metadata()
        {
            const std::string key("key");
            const std::string value("value");
            const size_t count = 10;
            const core::LayerIdType layerId = 1;

            auto tempFileName = io::tempFileName();

            try
            {
                // Create and serialize
                core::IMapGuiPtr mapGui = test::map::createTestMapGui();

                mapGui->open(test::map::createProgressStub());

                auto layer = mapGui->createGroupLayer();

                BOOST_CHECK(!layer->metadata()->has(key));
                BOOST_CHECK(!layer->metadata()->get(key));

                layer->metadata()->set(key, value);

                BOOST_CHECK(layer->metadata()->has(key));

                BOOST_CHECK_EQUAL(*layer->metadata()->get(key), value);

                layer->metadata()->remove(key);

                BOOST_CHECK(!layer->metadata()->has(key));

                for (size_t index = 0; index < count; index++)
                {
                    auto iterationKey = key + boost::lexical_cast<std::string>(index);
                    auto iterationValue =
                        value + boost::lexical_cast<std::string>(index);

                    layer->metadata()->set(iterationKey, iterationValue);
                }

                mapGui->saveToXml(tempFileName);

                // Deserialize
                mapGui = test::map::openMap(tempFileName);

                layer = mapGui->getLayerById(layerId);

                BOOST_CHECK(layer);

                for (size_t index = 0; index < count; index++)
                {
                    auto iterationKey = key + boost::lexical_cast<std::string>(index);
                    auto iterationValue =
                        value + boost::lexical_cast<std::string>(index);

                    BOOST_CHECK(layer->metadata()->has(iterationKey));
                    BOOST_CHECK_EQUAL(
                        *layer->metadata()->get(iterationKey), iterationValue);
                }
            }
            catch(...)
            {
                if (io::exists(tempFileName))
                    io::file::remove(tempFileName);

                throw;
            }

            if (io::exists(tempFileName))
                io::file::remove(tempFileName);
        }

    } // namespace

    test_suite * init_suite()
    {
        test_suite* suite = BOOST_TEST_SUITE("Metadata tools test suite");
        suite->add(BOOST_TEST_CASE(&test_layers_metadata));
        return suite;
    }

} } } }
