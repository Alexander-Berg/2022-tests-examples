#include <yandex_io/libs/base/channel_splitter.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <vector>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(TestChannelSplitter, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testChannelSplitter)
    {
        int micChannels = 7;
        int spkChannels = 3;
        int numberOfChannels = micChannels + spkChannels;
        int periodSize = 256;
        auto splitter = IChannelSplitter::create(micChannels, spkChannels, 2);

        std::vector<int16_t> rawData;
        rawData.resize(numberOfChannels* periodSize);

        for (size_t i = 0; i < rawData.size(); ++i) {
            rawData[i] = i;
        }

        std::vector<float> inputMic;
        std::vector<float> inputSpk;

        std::vector<uint8_t> raw;
        const uint8_t* start = reinterpret_cast<uint8_t*>(rawData.data());
        const uint8_t* end = reinterpret_cast<uint8_t*>(rawData.data() + rawData.size());
        raw.assign(start, end);
        splitter->split(raw, inputMic, inputSpk);
        UNIT_ASSERT_VALUES_EQUAL((int)inputMic.size(), periodSize* micChannels);
        UNIT_ASSERT_VALUES_EQUAL((int)inputSpk.size(), periodSize* spkChannels);

        int rawDataPos = 0;
        for (size_t i = 0; i < inputMic.size(); ++i)
        {
            UNIT_ASSERT_DOUBLES_EQUAL(inputMic[i], rawData[rawDataPos], 0.001);
            if (rawDataPos % numberOfChannels < micChannels - 1) {
                rawDataPos++;
            } else {
                rawDataPos += spkChannels + 1;
            }
        }

        rawDataPos = micChannels;
        for (size_t i = 0; i < inputSpk.size(); ++i)
        {
            UNIT_ASSERT_DOUBLES_EQUAL(inputSpk[i], rawData[rawDataPos], 0.001);
            if (rawDataPos % numberOfChannels < numberOfChannels - 1) {
                rawDataPos++;
            } else {
                rawDataPos += micChannels + 1;
            }
        }

        std::vector<int16_t> separateChannel;

        auto checkSeparateChannel = [&](int channelNumber) {
            splitter->getSeparateChannel(raw, channelNumber, separateChannel);
            int rawDataPos = channelNumber;
            UNIT_ASSERT_VALUES_EQUAL((int)separateChannel.size(), periodSize);
            for (size_t i = 0; i < separateChannel.size(); ++i)
            {
                UNIT_ASSERT_VALUES_EQUAL(separateChannel[i], rawData[rawDataPos]);
                rawDataPos += numberOfChannels;
            }
        };

        checkSeparateChannel(0);
        checkSeparateChannel(3);
        checkSeparateChannel(6);
        checkSeparateChannel(9);
        UNIT_ASSERT_EXCEPTION(checkSeparateChannel(10), std::runtime_error);
        UNIT_ASSERT_EXCEPTION(checkSeparateChannel(-1), std::runtime_error);
    };

    Y_UNIT_TEST(testChannelSplitterInt32SkipMic)
    {
        const int micChannels = 6;
        const int spkChannels = 2;
        const int skipMics = 2;
        const int numberOfChannels = micChannels + spkChannels;
        const int periodSize = 64;
        auto splitter = IChannelSplitter::create(micChannels, spkChannels, 4);

        std::vector<int32_t> rawData;
        rawData.resize(numberOfChannels* periodSize);

        for (size_t i = 0; i < rawData.size(); ++i) {
            rawData[i] = i + (i << 16);
        }

        std::vector<float> inputMic;
        std::vector<float> inputSpk;

        std::vector<uint8_t> raw;
        const uint8_t* start = reinterpret_cast<uint8_t*>(rawData.data());
        const uint8_t* end = reinterpret_cast<uint8_t*>(rawData.data() + rawData.size());
        raw.assign(start, end);
        splitter->splitAndSkip(raw, inputMic, inputSpk, skipMics);
        UNIT_ASSERT_VALUES_EQUAL((int)inputMic.size(), periodSize*(micChannels - skipMics));
        UNIT_ASSERT_VALUES_EQUAL((int)inputSpk.size(), periodSize* spkChannels);

        int rawDataPos = 0;
        for (size_t i = 0; i < inputMic.size(); ++i)
        {
            UNIT_ASSERT_DOUBLES_EQUAL(inputMic[i], rawData[rawDataPos], 0.001);
            if (rawDataPos % numberOfChannels < micChannels - skipMics - 1) {
                rawDataPos++;
            } else {
                rawDataPos += spkChannels + skipMics + 1;
            }
        }

        rawDataPos = micChannels;
        for (size_t i = 0; i < inputSpk.size(); ++i)
        {
            UNIT_ASSERT_DOUBLES_EQUAL(inputSpk[i], rawData[rawDataPos], 0.001);
            if (rawDataPos % numberOfChannels < numberOfChannels - 1) {
                rawDataPos++;
            } else {
                rawDataPos += micChannels + 1;
            }
        }

        std::vector<int16_t> separateChannel;

        auto checkSeparateChannel = [&](int channelNumber) {
            splitter->getSeparateChannel(raw, channelNumber, separateChannel);
            int rawDataPos = channelNumber;
            UNIT_ASSERT_VALUES_EQUAL((int)separateChannel.size(), periodSize * 2);
            for (size_t i = 0; i < separateChannel.size(); ++i)
            {
                // int32 value of k + (k << 16) was converted to 2 int16: k and k.
                UNIT_ASSERT_VALUES_EQUAL(separateChannel[i], (i / 2) * numberOfChannels + channelNumber);
                rawDataPos += numberOfChannels;
            }
        };

        for (int i = 0; i < numberOfChannels; ++i) {
            checkSeparateChannel(i);
        }
        UNIT_ASSERT_EXCEPTION(checkSeparateChannel(numberOfChannels), std::runtime_error);
    }
}
