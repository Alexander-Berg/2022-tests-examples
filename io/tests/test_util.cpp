#include <yandex_io/modules/bluetooth/util/util.h>

#include <yandex_io/libs/bluetooth/bluetooth.h>

#include <library/cpp/testing/unittest/registar.h>

#include <array>
#include <string>
#include <unordered_map>

using namespace YandexIO;
using namespace quasar;

namespace {
    struct PrepareNameArgs {
        std::string name;
        std::string deviceID;
        size_t maxSize;
    };
} // namespace

Y_UNIT_TEST_SUITE(BluetoothUtilTest) {

    Y_UNIT_TEST(testPrepareBluetoothName) {
        constexpr size_t TEST_DATA_SIZE = 5;
        const std::array<PrepareNameArgs, TEST_DATA_SIZE> input = {
            PrepareNameArgs{"Just Name", "", 4},
            PrepareNameArgs{"Name ", "123456", 4},
            PrepareNameArgs{"Name ", "123456", 5},
            PrepareNameArgs{"Name ", "123456", 6},
            PrepareNameArgs{"Name ", "123456", 0}};
        const std::array<std::string, TEST_DATA_SIZE> output = {
            "Just Name",
            "Name 3456",
            "Name 23456",
            "Name 123456",
            "Name ",
        };
        for (size_t i = 0; i != TEST_DATA_SIZE; ++i) {
            UNIT_ASSERT_VALUES_EQUAL(prepareBluetoothName(input[i].name, input[i].deviceID, input[i].maxSize), output[i]);
        }
    }

    Y_UNIT_TEST(testConvertRole) {
        constexpr size_t TEST_DATA_SIZE = 3;
        const std::array<proto::BluetoothNetwork::BluetoothRole, TEST_DATA_SIZE> input = {
            proto::BluetoothNetwork::BluetoothRole::BluetoothNetwork_BluetoothRole_NONE,
            proto::BluetoothNetwork::BluetoothRole::BluetoothNetwork_BluetoothRole_SOURCE,
            proto::BluetoothNetwork::BluetoothRole::BluetoothNetwork_BluetoothRole_SINK,
        };
        const std::array<Bluetooth::BtRole, TEST_DATA_SIZE> output = {
            Bluetooth::BtRole::UNKNOWN,
            Bluetooth::BtRole::SOURCE,
            Bluetooth::BtRole::SINK,
        };
        for (size_t i = 0; i != TEST_DATA_SIZE; ++i) {
            UNIT_ASSERT_EQUAL(convertRole(input[i]), output[i]);
        }
    }

    Y_UNIT_TEST(testConvertAVRCP) {
        constexpr size_t TEST_DATA_SIZE = 5;
        const std::array<proto::AVRCP, TEST_DATA_SIZE> input = {
            proto::AVRCP::PLAY_START,
            proto::AVRCP::PLAY_PAUSE,
            proto::AVRCP::PLAY_STOP,
            proto::AVRCP::PLAY_NEXT,
            proto::AVRCP::PLAY_PREV,
        };
        const std::array<Bluetooth::AVRCP, TEST_DATA_SIZE> output = {
            Bluetooth::AVRCP::PLAY_START,
            Bluetooth::AVRCP::PLAY_PAUSE,
            Bluetooth::AVRCP::PLAY_STOP,
            Bluetooth::AVRCP::PLAY_NEXT,
            Bluetooth::AVRCP::PLAY_PREV,
        };
        for (size_t i = 0; i != TEST_DATA_SIZE; ++i) {
            UNIT_ASSERT_EQUAL(convertAVRCP(input[i]), output[i]);
        }
    }

    Y_UNIT_TEST(testPrepareTrackInfo) {
        proto::BluetoothTrackMetaInfo track;
        track.set_title("title");
        track.set_artist("artist");
        track.set_album("album");
        track.set_genre("genre");
        track.set_song_len_ms(100500);
        track.set_curr_pos_ms(0);
        auto convertedTrackInfo = prepareTrackInfo(track);
        UNIT_ASSERT_EQUAL(convertedTrackInfo.title, "title");
        UNIT_ASSERT_EQUAL(convertedTrackInfo.artist, "artist");
        UNIT_ASSERT_EQUAL(convertedTrackInfo.album, "album");
        UNIT_ASSERT_EQUAL(convertedTrackInfo.genre, "genre");
        UNIT_ASSERT_EQUAL(convertedTrackInfo.songLenMs, 100500);
        UNIT_ASSERT_EQUAL(convertedTrackInfo.currPosMs, 0);
    }

    Y_UNIT_TEST(testEventFromVisibility) {
        constexpr size_t TEST_DATA_SIZE = 4;
        std::array<std::pair<bool, bool>, TEST_DATA_SIZE> input = {
            std::make_pair(true, true),
            std::make_pair(true, false),
            std::make_pair(false, true),
            std::make_pair(false, false),
        };
        std::array<Bluetooth::SinkEvent, TEST_DATA_SIZE> output = {
            Bluetooth::SinkEvent::DISCOVERABLE_CONNECTABLE,
            Bluetooth::SinkEvent::DISCOVERABLE,
            Bluetooth::SinkEvent::CONNECTABLE,
            Bluetooth::SinkEvent::NON_VISIBLE,
        };
        for (size_t i = 0; i != TEST_DATA_SIZE; ++i) {
            UNIT_ASSERT_EQUAL(eventFromVisibility(input[i].first, input[i].second), output[i]);
        }
    }

};
