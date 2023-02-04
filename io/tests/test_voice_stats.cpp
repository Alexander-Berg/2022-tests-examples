#include <yandex_io/libs/voice_stats/voice_stats.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using ChannelData = YandexIO::ChannelData;

struct VoiceStatsFixture: public NUnitTest::TBaseFixture {
    std::shared_ptr<quasar::VoiceStats> voiceStats;
    VoiceStatsFixture()
        : voiceStats(quasar::VoiceStats::create())
    {
    }

    static ChannelData makeChannel(std::string name, ChannelData::SampleInt infill, ChannelData::Type type = ChannelData::Type::RAW) {
        ChannelData result;
        result.name = std::move(name);
        result.type = type;
        result.data.resize(1024, infill);
        return result;
    }
};

Y_UNIT_TEST_SUITE_F(VoiceStatsTest, VoiceStatsFixture) {
    Y_UNIT_TEST(BasicTest) {
        YandexIO::ChannelsData data;
        data.push_back(makeChannel("raw_1", 10));
        data.push_back(makeChannel("raw_2", 20));
        data.push_back(makeChannel("lp", 0, ChannelData::Type::FEEDBACK));
        data.push_back(makeChannel("bf", 0, ChannelData::Type::BEAMFORMING));
        data.push_back(makeChannel("vqe", 0, ChannelData::Type::VQE));
        voiceStats->pushAudioChannels(data);

        const auto rms = voiceStats->getRms();

        UNIT_ASSERT_VALUES_EQUAL(rms.size(), 2);
        UNIT_ASSERT(rms[0].name != rms[1].name);
        UNIT_ASSERT(rms[0].name == "raw_1" || rms[0].name == "raw_2");
        UNIT_ASSERT(rms[1].name == "raw_1" || rms[1].name == "raw_2");
    }

    Y_UNIT_TEST(VariousRates) {
        YandexIO::ChannelsData data;
        data.push_back(makeChannel("raw_1", 10));
        data.back().sampleRate = 48000;
        data.back().data.resize(3072, 10);
        data.push_back(makeChannel("raw_2", 20));
        voiceStats->pushAudioChannels(data);

        const auto rms = voiceStats->getRms();

        UNIT_ASSERT_VALUES_EQUAL(rms.size(), 2);

        auto& raw_1 = (rms[0].name == "raw_1" ? rms[0] : rms[1]);
        auto& raw_2 = (rms[0].name == "raw_2" ? rms[0] : rms[1]);

        UNIT_ASSERT_VALUES_EQUAL(raw_1.data[0], 10);
        UNIT_ASSERT_VALUES_EQUAL(raw_2.data[0], 20);
        UNIT_ASSERT_VALUES_EQUAL(raw_1.data[1], 0);
        UNIT_ASSERT_VALUES_EQUAL(raw_2.data[1], 0);
    }

    Y_UNIT_TEST(ChannelsDisappeared) {
        YandexIO::ChannelsData data;
        data.push_back(makeChannel("raw_1", 10));
        data.back().sampleRate = 48000;
        data.back().data.resize(3072, 10);
        data.push_back(makeChannel("raw_2", 20));
        voiceStats->pushAudioChannels(data);

        UNIT_ASSERT_VALUES_EQUAL(voiceStats->getRms().size(), 2);

        data.push_back(makeChannel("raw_3", 30));
        voiceStats->pushAudioChannels(data);
        UNIT_ASSERT_VALUES_EQUAL(voiceStats->getRms().size(), 3);

        data.resize(1);
        voiceStats->pushAudioChannels(data);
        const auto rms = voiceStats->getRms();
        UNIT_ASSERT_VALUES_EQUAL(rms.size(), 1);
        auto first = rms[0];
        UNIT_ASSERT_VALUES_EQUAL(first.name, "raw_1");
        UNIT_ASSERT_VALUES_EQUAL(first.data[0], 10);
        UNIT_ASSERT_VALUES_EQUAL(first.data[1], 10);
        UNIT_ASSERT_VALUES_EQUAL(first.data[2], 10);
        UNIT_ASSERT_VALUES_EQUAL(first.data[3], 0);
    }

    Y_UNIT_TEST(calcOnVqe) {
        YandexIO::ChannelsData data;
        data.push_back(makeChannel("raw_1", 10));
        data.push_back(makeChannel("vqe", 10, ChannelData::Type::VQE));
        voiceStats->pushAudioChannels(data);
        {
            const auto rms = voiceStats->getRms();
            UNIT_ASSERT_VALUES_EQUAL(rms.size(), 1);
        }
        voiceStats->calcOnVqe(true);
        voiceStats->pushAudioChannels(data);
        const auto rms = voiceStats->getRms();
        UNIT_ASSERT_VALUES_EQUAL(rms.size(), 2);
    }
}
