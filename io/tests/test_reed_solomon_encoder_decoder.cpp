#include <yandex_io/libs/setup_parser/encoder_decoder.h>
#include <yandex_io/libs/setup_parser/sound_utils.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <iostream>
#include <thread>
#include <vector>

using namespace quasar;
using namespace quasar::SoundUtils;
using byte_t = unsigned char;

Y_UNIT_TEST_SUITE_F(TestSoundInitEncoderDecoder, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testEncoderDecoder)
    {
        std::vector<byte_t> data{13, 0, 11, 2, 13, 0, 11, 0, 13, 0, 11, 15, 13, 1, 8, 0, 13, 0, 11, 12, 13, 1, 8, 3, 13, 1, 8, 11, 13, 1, 8, 6, 13, 0, 11, 10, 13, 0, 11, 11, 13, 0, 11, 15, 13, 1, 8, 0, 13, 0, 11, 12, 13, 1, 8, 3, 13, 0, 11, 10, 13, 0, 11, 11, 13, 0, 11, 15, 13, 1, 8, 0, 13, 0, 11, 12, 13, 1, 8, 3, 13, 0, 11, 11, 13, 0, 11, 10, 13, 1, 8, 0};
        std::cout << data.size() << std::endl;
        EncoderDecoder coder;
        UNIT_ASSERT(data == coder.decodeData(coder.encodeData(data, 6), 6));
        UNIT_ASSERT(data == coder.decodeData(coder.encodeData(data, 4), 4));
        std::cout << "Done" << std::endl;
    }

    Y_UNIT_TEST(testEncoderDecoder2)
    {
        // tests if no SEGV
        std::vector<byte_t> data{0, 3, 12, 0, 10, 6, 1, 8, 13, 0, 11, 1, 5, 10, 1, 11, 13, 0, 11, 0,
                                 13, 0, 11, 4, 13, 0, 7, 1, 3, 10, 11, 0, 13, 0, 11, 4, 13, 0, 11, 0,
                                 13, 9, 0, 15, 13, 0, 11, 4, 13, 0, 11, 0, 13, 0, 11, 4, 15, 8, 5, 15,
                                 13, 0, 11, 0, 13, 0, 11, 4, 13, 0, 11, 2, 12, 0, 9, 0, 1, 0};
        std::cout << data.size() << std::endl;
        std::vector<byte_t> decoded = EncoderDecoder().decodeData(data, SoundUtils::FEC_BYTES);
        std::cout << "Done" << std::endl;
    }

    Y_UNIT_TEST(testEncoderDecoderErrorsFixing)
    {
        srand(time(nullptr));
        std::vector<byte_t> payload;
        for (int i = 0; i < 11; ++i) {
            payload.push_back(rand() % 16);
        }
        EncoderDecoder decoder;
        for (int i = 0; i < 1000; ++i) {
            std::vector<byte_t> encoded = decoder.encodeData(payload, 4);
            encoded[rand() % payload.size()] = rand() % 16;
            encoded[rand() % payload.size()] = rand() % 16;
            std::vector<byte_t> decoded = decoder.decodeData(encoded, 4);
            UNIT_ASSERT(decoded == payload);
        }
        for (int i = 0; i < 11; ++i) {
            payload.push_back(rand() % 16);
        }
        for (int i = 0; i < 1000; ++i) {
            std::vector<byte_t> encoded = decoder.encodeData(payload, 4);
            encoded[rand() % payload.size()] = rand() % 16;
            encoded[rand() % payload.size()] = rand() % 16;
            std::vector<byte_t> decoded = decoder.decodeData(encoded, 4);
            UNIT_ASSERT(decoded == payload);
        }
    }
}
