#include <yandex_io/services/ble_initd/gatt_value_format.h>
#include <yandex_io/services/ble_initd/gatt_value_reader.h>
#include <yandex_io/services/ble_initd/gatt_value_writer.h>
#include <yandex_io/services/ble_initd/quasar_gatt_characteristic.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <future>

using namespace quasar;
using namespace quasar::ble_configuration;
using namespace quasar::TestUtils;

namespace {

    std::vector<uint8_t> stringToVector(const std::string& value) {
        std::vector<uint8_t> res;
        res.insert(res.end(), (uint8_t*)value.data(), (uint8_t*)(value.data() + value.size()));
        return res;
    }

    std::vector<uint8_t> generateValue(size_t length) {
        std::vector<uint8_t> value(length);

        uint8_t i = 0;

        std::generate(value.begin(), value.end(), [&i]() {
            return i++;
        });

        return value;
    }

    std::vector<uint8_t> readGattValue(GattValueReader& reader, uint16_t mtu) {
        std::optional<QuasarGattMessageHeader> header;
        std::vector<uint8_t> value;

        while (!header || value.size() < header->size) {
            uint16_t offset = 0;

            while (true) {
                auto res = reader.read(mtu, offset);
                UNIT_ASSERT_EQUAL(res.code, GattValueReader::ResultCode::OK);

                size_t from = 0;

                if (!header) {
                    header = deserialize(res.data, from);
                }

                value.insert(value.end(), res.data.begin() + from, res.data.end());

                if (res.data.size() < mtu - 1) {
                    break;
                }

                offset += mtu - 1;
            }
        }

        UNIT_ASSERT(reader.isFinished());

        return value;
    }

} // namespace

Y_UNIT_TEST_SUITE(GattValueTest) {

    Y_UNIT_TEST(testGattValue) {
        std::vector<std::vector<uint8_t>> examples{
            stringToVector("{"
                           "\"field1\": \"value1\""
                           "\"field2\": \"value2\""
                           "\"field3\": \"value3\""
                           "}"),
            stringToVector("{}") // Short value, less than 23 bytes
        };
        for (const auto& data : examples) {
            QuasarGattMessageHeader header;
            header.version = QuasarGattProtocol::CHUNKED;
            header.size = data.size();
            std::promise<void> done;

            auto characteristic = std::make_shared<QuasarGattCharacteristic>(
                "name",
                "uuid",
                std::set<gatt_permission::GattPermission>());

            characteristic->setWriteCallback([&data, &done](std::vector<uint8_t> receivedData) {
                UNIT_ASSERT_VALUES_EQUAL(receivedData, data);
                done.set_value();
            });

            GattValueReader reader(data);
            GattValueWriter writer(characteristic);

            while (true) {
                auto res = reader.read(512, 0);
                if (res.code == GattValueReader::ResultCode::ALL_READ) {
                    break;
                }
                UNIT_ASSERT(res.code == GattValueReader::ResultCode::OK);
                writer.write(res.data);
            }

            done.get_future().get();
        }
    }

    Y_UNIT_TEST(testGattValueTooBigSize) {
        QuasarGattMessageHeader header;
        header.version = QuasarGattProtocol::CHUNKED;
        header.size = MAX_DATA_SIZE_BYTES + 1; // Size too big,

        std::vector<uint8_t> serializedHeader = serialize(header);
        std::vector<uint8_t> data(512, 0);

        auto characteristic = std::make_shared<QuasarGattCharacteristic>(
            "name",
            "uuid",
            std::set<gatt_permission::GattPermission>());

        characteristic->setWriteCallback([](std::vector<uint8_t> /*data*/) {
            UNIT_FAIL("write callback called");
        });

        GattValueWriter writer(characteristic);
        auto resultCode = writer.write(serializedHeader);
        UNIT_ASSERT(resultCode == GattValueWriter::ResultCode::TOO_LARGE_DATA);

        resultCode = writer.write(data);
        UNIT_ASSERT(resultCode == GattValueWriter::ResultCode::ALREADY_MISTAKEN);
    }

    Y_UNIT_TEST(testGattValueRead) {
        {
            // Short value
            const auto value = generateValue(10);
            GattValueReader reader(value);
            UNIT_ASSERT_EQUAL(readGattValue(reader, 23), value);
        }

        {
            // Small MTU
            const auto value = generateValue(100);
            GattValueReader reader(value);
            UNIT_ASSERT_EQUAL(readGattValue(reader, 23), value);
        }

        {
            // Large MTU
            const auto value = generateValue(100);
            GattValueReader reader(value);
            UNIT_ASSERT_EQUAL(readGattValue(reader, 513), value);
        }

        {
            // Value size (with header) is a multiple of (MTU - 1)
            const auto value = generateValue((23 - 1) * 10 - 5);
            GattValueReader reader(value);
            UNIT_ASSERT_EQUAL(readGattValue(reader, 23), value);
        }

        {
            // Long value, small MTU
            const auto value = generateValue(1000);
            GattValueReader reader(value);
            UNIT_ASSERT_EQUAL(readGattValue(reader, 23), value);
        }

        {
            // Long value, large MTU
            const auto value = generateValue(1000);
            GattValueReader reader(value);
            UNIT_ASSERT_EQUAL(readGattValue(reader, 513), value);
        }

        {
            // Invalid offset
            const auto value = generateValue(100);
            GattValueReader reader(value);
            UNIT_ASSERT_EQUAL(reader.read(23, 1000).code, GattValueReader::ResultCode::INVALID_OFFSET);
        }
    }

}
