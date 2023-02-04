#include <library/cpp/testing/unittest/registar.h>

#include <maps/automotive/parking/fastcgi/parking_api/lib/device_registry.h>

#include <vector>

using namespace std::chrono_literals;

namespace maps::automotive::parking::tests {

Y_UNIT_TEST_SUITE(test_parking_api_device_registry) {

    Y_UNIT_TEST(check_that_device_removed_after_timeout)
    {
        struct {
            DeviceId deviceId;
            UserId userId;
            std::chrono::seconds pause;
            std::vector<DeviceId> registeredDevices;
        } entries[] = {
            { "a", 1,  3s, { "a" } },
            { "b", 2,  3s, { "a", "b" } },
            { "c", 3,  0s, { "a", "b", "c" } },
            { "d", 4,  5s, { "b", "c", "d" } },
            { "e", 5,  0s, { "b", "c", "d", "e" } },
            { "f", 6,  5s, { "e", "f" } }
        };
        auto currentTime = std::chrono::steady_clock::now();
        DeviceRegistry deviceRegistry(10s,
            [&currentTime] { return currentTime; });

        for (auto entry : entries) {
            deviceRegistry.registerDevice(entry.deviceId, entry.userId);
            currentTime += entry.pause;
            deviceRegistry.checkExpired();
            UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), entry.registeredDevices.size());
            for (auto deviceId : entry.registeredDevices)
                UNIT_ASSERT(deviceRegistry.lookupHeadUnit(deviceId));
        }
        currentTime += 5s;
        deviceRegistry.checkExpired();
        UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), 0);
    }

    Y_UNIT_TEST(check_that_ttl_is_extended_after_repeat_registration)
    {
        auto currentTime = std::chrono::steady_clock::now();
        DeviceRegistry deviceRegistry(5s,
            [&currentTime] { return currentTime; });

        deviceRegistry.registerDevice("a", 1);
        currentTime += 3s;
        deviceRegistry.checkExpired();
        UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), 1);

        deviceRegistry.registerDevice("a", 1);
        currentTime += 3s;
        deviceRegistry.checkExpired();
        UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), 1);

        currentTime += 3s;
        deviceRegistry.checkExpired();
        UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), 0);
    }

    Y_UNIT_TEST(check_that_ttl_extension_works_fine_with_few_devices)
    {
        auto currentTime = std::chrono::steady_clock::now();
        DeviceRegistry deviceRegistry(5s,
            [&currentTime] { return currentTime; });

        deviceRegistry.registerDevice("a", 1);

        currentTime += 1s;
        deviceRegistry.checkExpired();
        UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), 1);
        deviceRegistry.registerDevice("b", 2);

        currentTime += 2s;
        deviceRegistry.checkExpired();
        UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), 2);
        deviceRegistry.registerDevice("a", 1);

        currentTime += 4s;
        deviceRegistry.checkExpired();
        UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), 1);
        UNIT_ASSERT(deviceRegistry.lookupHeadUnit("a"));

        currentTime += 2s;
        deviceRegistry.checkExpired();
        UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), 0);
    }

    Y_UNIT_TEST(check_that_few_devices_may_be_registered_simultaneously)
    {
        DeviceRegistry deviceRegistry(5s, []() { return std::chrono::time_point<std::chrono::steady_clock>(0s); });

        auto ttlSeconds = 10s;
        DeviceRegistryEntries entries {
            { 1, "a", ttlSeconds },
            { 2, "b", ttlSeconds }
        };
        deviceRegistry.registerDevices(entries);

        UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), entries.size());
        for (auto& entry : entries) {
            auto uid = deviceRegistry.lookupHeadUnit(entry.deviceId_);
            UNIT_ASSERT(uid);
            UNIT_ASSERT_EQUAL(*uid, entry.uid_);

            auto ttlSeconds = deviceRegistry.ttl(entry.deviceId_);
            UNIT_ASSERT(ttlSeconds);
            UNIT_ASSERT_EQUAL(*ttlSeconds, entry.ttlSeconds_);
        }
    }

    Y_UNIT_TEST(check_that_older_forwarded_entries_are_ignored)
    {
        DeviceRegistry deviceRegistry(5s, []() { return std::chrono::time_point<std::chrono::steady_clock>(0s); });

        auto ttlSeconds = 100s;
        DeviceRegistryEntries entriesOriginal {
            { 1, "a", ttlSeconds },
            { 2, "b", ttlSeconds }
        };
        DeviceRegistryEntries entriesForUpdate {
            { 3, "a", ttlSeconds - 5s },
            { 4, "b", ttlSeconds + 5s }
        };
        DeviceRegistryEntries entriesUpdatedReference {
            entriesOriginal[0],
            entriesForUpdate[1]
        };
        deviceRegistry.registerDevices(entriesOriginal);
        deviceRegistry.registerDevices(entriesForUpdate);

        UNIT_ASSERT_EQUAL(deviceRegistry.numRegistered(), entriesUpdatedReference.size());
        for (auto& entry : entriesUpdatedReference) {
            auto uid = deviceRegistry.lookupHeadUnit(entry.deviceId_);
            UNIT_ASSERT(uid);
            UNIT_ASSERT_EQUAL(*uid, entry.uid_);

            auto ttlSeconds = deviceRegistry.ttl(entry.deviceId_);
            UNIT_ASSERT(ttlSeconds);
            UNIT_ASSERT_EQUAL(*ttlSeconds, entry.ttlSeconds_);
        }
    }

} //Y_UNIT_TEST_SUITE

} //maps::automotive::parking::tests
