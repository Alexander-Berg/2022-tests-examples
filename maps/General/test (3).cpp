#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/rice/include/rice.h>

using namespace yandex::maps;

Y_UNIT_TEST_SUITE(Rice) {
    Y_UNIT_TEST(SingleWriteAndReadNumbers) {
        const unsigned order = 6;
        const size_t n = 1000000;

        succinct::bit_vector_builder builder;

        RiceBuilder riceBuilder(order, &builder);
        for (size_t i = 0; i != n; ++i) {
            riceBuilder.add(i);
        }

        const succinct::bit_vector bits(&builder);

        RiceReader riceReader(order, bits);
        for (size_t i = 0; i != n; ++i) {
            UNIT_ASSERT(riceReader.hasNext());
            UNIT_ASSERT_EQUAL(i, riceReader.next());
        }
        UNIT_ASSERT(!riceReader.hasNext());
    }

    Y_UNIT_TEST(MultipleWriteAndReadNumbers) {
        const unsigned order = 7;
        const size_t n = 1000000;
        const size_t k = 3;

        succinct::bit_vector_builder builder;

        std::vector<uint64_t> values;

        std::vector<uint64_t> positions;

        for (size_t i = 0; i < k; ++i) {
            positions.push_back(builder.size());

            RiceBuilder riceBuilder(order, &builder);
            for (size_t j = 0; j < n; ++j) {
                values.push_back(rand() % n);
                riceBuilder.add(values.back());
            }
        }
        positions.push_back(builder.size());

        const succinct::bit_vector bits(&builder);

        auto valuesItr = values.begin();
        for (size_t i = 0; i < k; ++i) {
            RiceReader riceReader(order, bits, positions[i], positions[i + 1]);
            for (size_t j = 0; j < n; ++j) {
                UNIT_ASSERT(riceReader.hasNext());
                UNIT_ASSERT_EQUAL(*valuesItr, riceReader.next());
                ++valuesItr;
            }
            UNIT_ASSERT(!riceReader.hasNext());
        }
    }
};
