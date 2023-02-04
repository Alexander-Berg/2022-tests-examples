#include <yandex_io/libs/glagol_sdk/discovery.h>
#include <yandex_io/libs/glagol_sdk/i_account_devices.h>
#include <yandex_io/libs/glagol_sdk/avahi_wrapper/i_avahi_browse_client.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/signals/signal_external.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace glagol;

namespace {
    const IAvahiBrowseClient::ResolvedItems defaultResolvedItems =
        {
            {{"YandexIOReceiver-NameA", "_yandexio._tcp", "local"},
             {"hostname.a", 0x1111, "192.168.0.1", {{"platform", "mini"}, {"deviceId", "DEVICEID_AAAAAAAAAA"}}}},
            {{"YandexIOReceiver-NameB", "_yandexio._tcp", "local"},
             {"hostname.b", 0x1111, "192.168.0.2", {{"platform", "mini"}, {"deviceId", "DEVICEID_BBBBBBBBBB"}}}},
            {{"YandexIOReceiver-NameC", "TypeC", "local"},
             {"hostname.c", 0xCCC, "192.168.0.3", {{"platform", "maxi"}, {"deviceId", "DEVICEID_CCCCCCCCCC"}}}},
            {{"NameD", "_yandexio._tcp", "local"},
             {"hostname.d", 0xDDD, "192.168.0.4", {{"platform", "maxi"}, {"deviceId", "DEVICEID_DDDDDDDDDD"}}}},
            {{"YandexIOReceiver-NameE", "_yandexio._tcp", "local"},
             {"hostname.e", 0xDDD, "192.168.0.5", {{"deviceId", "DEVICEID_EEEEEEEEEE"}}}},
            {{"YandexIOReceiver-NameF", "_yandexio._tcp", "local"},
             {"hostname.f", 555, "1234:56789::123", {{"platform", "maxi"}, {"deviceId", "DEVICEID_FFFFFFFFFF"}}}}};

    const BackendApi::DevicesMap defaultDeviceMap =
        {
            {{"DEVICEID_AAAAAAAAAA", "mini"}, BackendApi::Device{quasar::parseJson("{ \"name\":\"My device A\" }")}},
            {{"DEVICEID_BBBBBBBBBB", "mini"}, BackendApi::Device{quasar::parseJson("{ \"name\":\"My device B\" }")}}};

    const Discovery::Result::Item* findByName(const Discovery::Result& r, const std::string& name)
    {
        for (const auto& p : r.items) {
            if (p.second.name == name) {
                return &p.second;
            }
        }
        return nullptr;
    }

    class MockAvahiBrowseClient: public IAvahiBrowseClient {
    public:
        MockAvahiBrowseClient(IResolvedItemsChangedSignal& resolvedItemsChangedSignal)
            : resolvedItemsChangedSignal_(resolvedItemsChangedSignal)
        {
        }

        IResolvedItemsChangedSignal& resolvedItemsChangedSignal() override {
            return resolvedItemsChangedSignal_;
        }

        IResolvedItemsChangedSignal& resolvedItemsChangedSignal_;
    };

    class MockAccountDevices: public IAccountDevices {
    public:
        MockAccountDevices(IDeviceListChangedSignal& deviceListChangedSignal)
            : deviceListChangedSignal_(deviceListChangedSignal)
        {
        }

        BackendApi::DevicesMap devices() const noexcept override {
            UNIT_ASSERT(!"Unexpected");
            return BackendApi::DevicesMap{};
        }

        IDeviceListChangedSignal& deviceListChangedSignal() noexcept override {
            return deviceListChangedSignal_;
        }
        IDeviceListChangedSignal& deviceListChangedSignal_;

        bool setSettings(const BackendApi::Settings& /*settings*/) override {
            return true;
        }

        bool resumeUpdate() noexcept override {
            return true;
        };

        bool scheduleUpdate() noexcept override {
            return true;
        };
    };

} // namespace

Y_UNIT_TEST_SUITE(Discovery) {
    Y_UNIT_TEST(testDiscoveryWithoutAccountDevices1)
    {
        std::atomic<bool> awating = false;
        Lifetime lifetime;
        auto lifecycle = std::make_shared<NamedCallbackQueue>("test");
        SignalExternal<IAvahiBrowseClient::IResolvedItemsChangedSignal> signal(
            [&](bool /*onConenct*/)
            {
                return defaultResolvedItems;
            }, lifetime);

        Discovery::Result result;
        Discovery discovery(lifecycle, nullptr, std::make_shared<MockAvahiBrowseClient>(signal), Discovery::Settings());
        UNIT_ASSERT_VALUES_EQUAL(result.items.size(), 0);

        awating = true;
        // Awating while discovery will be stable
        lifecycle->add([&] { awating = false; });
        while (awating) {
            std::this_thread::yield();
        }

        awating = true;
        discovery.devicesAroundHasChangedSignal().connect(
            [&](const Discovery::Result& r)
            {
                result = r;
                awating = false;
            }, lifetime);
        while (awating) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_VALUES_EQUAL(result.items.size(), 3);
        UNIT_ASSERT(findByName(result, "YandexIOReceiver-NameA"));
        UNIT_ASSERT(findByName(result, "YandexIOReceiver-NameB"));
        auto f = findByName(result, "YandexIOReceiver-NameF");
        UNIT_ASSERT(f);
        UNIT_ASSERT_STRINGS_EQUAL(f->address, "1234:56789::123");
        UNIT_ASSERT_STRINGS_EQUAL(f->uri, "[1234:56789::123]:555");
    }

    Y_UNIT_TEST(testDiscoveryWithoutAccountDevices2)
    {
        std::atomic<bool> awating = false;
        auto resolvedItems = defaultResolvedItems;
        Lifetime lifetime;
        auto lifecycle = std::make_shared<NamedCallbackQueue>("test");
        SignalExternal<IAvahiBrowseClient::IResolvedItemsChangedSignal> signal(
            [&](bool /*onConnect*/)
            {
                return resolvedItems;
            }, lifetime);

        Discovery::Result result;
        Discovery discovery(lifecycle, nullptr, std::make_shared<MockAvahiBrowseClient>(signal), Discovery::Settings());
        UNIT_ASSERT_VALUES_EQUAL(result.items.size(), 0);
        awating = true;
        // Awating while discovery will be stable
        lifecycle->add([&] { awating = false; });
        while (awating) {
            std::this_thread::yield();
        }

        awating = true;
        discovery.devicesAroundHasChangedSignal().connect(
            [&](const Discovery::Result& r)
            {
                result = r;
                awating = false;
            }, lifetime);
        while (awating) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_VALUES_EQUAL(result.items.size(), 3);

        auto resolvedItemCount = resolvedItems.size();
        for (auto it = resolvedItems.begin(); it != resolvedItems.end(); ++it) {
            if (it->first.name == "YandexIOReceiver-NameA") {
                resolvedItems.erase(it);
                break;
            }
        }
        UNIT_ASSERT_VALUES_EQUAL(resolvedItems.size(), resolvedItemCount - 1);

        awating = true;
        signal.emit(); // Avahi send notification about changes
        while (awating) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_VALUES_EQUAL(result.items.size(), 2);
    }

    Y_UNIT_TEST(testDiscoveryWithAccountDevices)
    {
        std::atomic<bool> awating = false;
        Lifetime lifetime;
        auto lifecycle = std::make_shared<NamedCallbackQueue>("test");
        auto myDevices = defaultDeviceMap;
        SignalExternal<IAccountDevices::IDeviceListChangedSignal> adSignal(
            [&](bool /*onConnect*/)
            {
                return myDevices;
            }, lifetime);
        auto resolvedItems = defaultResolvedItems;
        SignalExternal<IAvahiBrowseClient::IResolvedItemsChangedSignal> avahiSignal(
            [&](bool /*onConnect*/)
            {
                return defaultResolvedItems;
            }, lifetime);

        Discovery::Result result;
        Discovery discovery(lifecycle, std::make_shared<MockAccountDevices>(adSignal), std::make_shared<MockAvahiBrowseClient>(avahiSignal), Discovery::Settings{.accountOnly = true});
        UNIT_ASSERT_VALUES_EQUAL(result.items.size(), 0);

        awating = true;
        // Awating while "discovery" will stabilize
        lifecycle->add([&] { awating = false; });
        while (awating) {
            std::this_thread::yield();
        }

        /*
         * Check (I) Filter local device with myDevices list
         */
        awating = true;
        discovery.devicesAroundHasChangedSignal().connect(
            [&](const Discovery::Result& r)
            {
                result = r;
                awating = false;
            }, lifetime);
        while (awating) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_VALUES_EQUAL(result.items.size(), 2);
        UNIT_ASSERT(findByName(result, "My device A")); // Name must be assigned from myDevices instead of YandexIOReceiver-NameA
        UNIT_ASSERT(findByName(result, "My device B")); // Name must be assigned from myDevices instead of YandexIOReceiver-NameB

        /*
         * Check (II) Remove on of my device
         */
        for (auto it = myDevices.begin(); it != myDevices.end(); ++it) {
            if (it->first.id == "DEVICEID_AAAAAAAAAA") {
                myDevices.erase(it);
                break;
            }
        }
        awating = true;
        adSignal.emit();
        while (awating) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_VALUES_EQUAL(result.items.size(), 1);
        UNIT_ASSERT(findByName(result, "My device B")); // Name must be assigned from myDevices instead of YandexIOReceiver-NameB

        /*
         * Check (III) Return device to my list
         */
        myDevices = defaultDeviceMap;
        awating = true;
        adSignal.emit();
        while (awating) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_VALUES_EQUAL(result.items.size(), 2);
        UNIT_ASSERT(findByName(result, "My device A")); // Name must be assigned from myDevices instead of YandexIOReceiver-NameA
        UNIT_ASSERT(findByName(result, "My device B")); // Name must be assigned from myDevices instead of YandexIOReceiver-NameB
    }
}
