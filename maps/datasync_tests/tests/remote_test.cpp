#include "../datasync/remote.h"
#include "../datasync/exception.h"

#include <yandex/datasync/database.h>

#include <yandex/maps/internal/unit_test_settings.h>
#include <yandex/maps/runtime/auth/test/mock_account.h>
#include <yandex/maps/proto/mobile_config/datasync/data.pb.h>
#include <yandex/maps/runtime/async/promise.h>
#include <yandex/maps/runtime/async/utils/publisher.h>
#include <yandex/maps/runtime/config/mock_config.h>
#include <yandex/maps/runtime/network/async.h>
#include <yandex/maps/runtime/network/auth.h>
#include <yandex/maps/runtime/network/exceptions.h>
#include <yandex/maps/runtime/network/request.h>
#include <yandex/maps/runtime/network/test/request_factory.h>

#include <boost/test/unit_test.hpp>

#include <algorithm>
#include <string>
#include <vector>

const std::string MIID = "ac8885655a3fc99b9fa9977041876d3f1da099623";
const std::string FAKE_MIID = "fake_miid";

const std::string DATABASE = "testDatabase";
const std::string COLLECTION = "testCollection";
const std::string RECORD = "testRecord";
const std::string CLIENT = "testClient";
const std::string OTHER_CLIENT = "otherTestClient";

using namespace yandex::datasync;
using namespace yandex::maps::runtime;

namespace {

class MockConfigManager: public config::ConfigManager
{
public:
    MockConfigManager()
    {
        yandex::maps::proto::mobile_config::Config newConfig;
        auto* datasyncConfig = newConfig.MutableExtension(
            yandex::maps::proto::mobile_config::datasync::data::config);
        datasyncConfig->set_service_url(yandex::maps::internal::testHost() + "/datasync/data/2.x/");
        publisher_.publish(newConfig);
    }

    virtual void submit(config::ConfigManager::SubmitPolicy /*policy*/) override {}

    virtual async::MultiFuture<
        yandex::maps::proto::mobile_config::Config> subscribe() override
    {
        return publisher_.subscribe();
    }

    virtual async::MultiFuture<network::UrlParams> experimentParameters(
        const std::string&, TestBuckets) override
    {
        throw LogicError("Not implemented");
    }

    virtual void setExperimentalParameter(
            const std::string&,
            const std::string&,
            const boost::optional<std::string>&) override
    {}

private:
    async::utils::MultiPublisher<
        yandex::maps::proto::mobile_config::Config> publisher_;
};


network::RequestFactory createMockRequestFactory(const std::string& miid)
{
    auto defaultRequestFactory = network::test::createRequestFactory("datasync");
    return [miid, defaultRequestFactory] () -> network::Request
    {
        return defaultRequestFactory()
            .addParam("uuid", "mock_uuid")
            .addParam("miid", miid)
            .addParam("deviceid", "mock_deviceid");
    };
}

} // namespace


struct Fixture {
    Fixture():
        configManager(new MockConfigManager()),
        remoteManager(createRemoteManager(
                    configManager.get(),
                    createMockRequestFactory(MIID))),
        account(std::make_shared<auth::test::MockAccount>()),
        remoteDatabase(remoteManager->database(DATABASE, auth::async::Account(account)))
    {
    }

    std::unique_ptr<config::ConfigManager> configManager;
    std::unique_ptr<RemoteManager> remoteManager;
    std::shared_ptr<auth::Account> account;
    std::unique_ptr<RemoteDatabase> remoteDatabase;
};

BOOST_FIXTURE_TEST_CASE(authorization_error, Fixture)
{
    auto failRemoteManager = createRemoteManager(
            configManager.get(),
            createMockRequestFactory(MIID));

    BOOST_REQUIRE_THROW(
        failRemoteManager->databasesInfo(
            auth::async::Account(std::make_shared<auth::test::MockAccount>("invalid_uid", "invalid_token"))),
        network::RemoteException);
}

BOOST_FIXTURE_TEST_CASE(create_database, Fixture)
{
    auto remoteSync = remoteDatabase->open(CLIENT);
    BOOST_REQUIRE_NO_THROW(remoteSync->push(0L, {}));

    std::vector<DatabaseInfo> databases;
    BOOST_REQUIRE_NO_THROW(databases = remoteManager->databasesInfo(auth::async::Account(account)));

    size_t databaseCount = databases.size();
    auto comparator = [](const DatabaseInfo& databaseInfo) {
                return databaseInfo.databaseId.compare(DATABASE) == 0;
            };
    BOOST_REQUIRE(
            std::find_if(databases.begin(), databases.end(), comparator) !=
            databases.end());

    // check opening existing database
    BOOST_REQUIRE_NO_THROW(remoteDatabase->open(CLIENT));

    // delete database
    BOOST_REQUIRE_NO_THROW(remoteManager->deleteDatabase(DATABASE, auth::async::Account(account)));
    databases = remoteManager->databasesInfo(auth::async::Account(account));
    BOOST_REQUIRE(databases.size() == databaseCount - 1);
    BOOST_REQUIRE(
            std::find_if(databases.begin(), databases.end(), comparator) ==
            databases.end());

    /* now disk accepts deleting, ask why
    // check deleting nonexisting database
    BOOST_REQUIRE_THROW(remoteManager->deleteDatabase(DATABASE, auth::async::Account(account)),
            network::NotFoundException);
    */
}

BOOST_FIXTURE_TEST_CASE(modify_database, Fixture)
{
    BOOST_REQUIRE_THROW(remoteDatabase->info(), network::NotFoundException);

    auto remoteSync = remoteDatabase->open(CLIENT);
    remoteSync->push(0L, {});

    DatabaseInfo info;
    BOOST_REQUIRE_NO_THROW(info = remoteDatabase->info());
    BOOST_REQUIRE(info.databaseId == DATABASE);
    BOOST_REQUIRE(info.revision == 0);

    Revision oldRev = info.revision;
    Delta delta;
    apply(&delta, COLLECTION, RECORD, RecordDelta::Insert);

    Revision newRev = UNDEFINED_REVISION;
    Delta conflictDelta;
    BOOST_REQUIRE_NO_THROW(
            std::tie(newRev, conflictDelta) =
                remoteSync->push(oldRev, delta));
    BOOST_REQUIRE(newRev == oldRev + 1);
    BOOST_REQUIRE(conflictDelta.empty());

    // check pushing same delta again
    BOOST_REQUIRE_NO_THROW(
            std::tie(newRev, conflictDelta) =
                remoteSync->push(oldRev, delta));
    BOOST_REQUIRE(newRev == oldRev + 1);
    BOOST_REQUIRE(conflictDelta.empty());

    // check pushing empty delta
    BOOST_REQUIRE_NO_THROW(
            std::tie(newRev, conflictDelta) =
                remoteSync->push(oldRev + 1, {}));
    BOOST_REQUIRE(newRev == oldRev + 1);
    BOOST_REQUIRE(conflictDelta.empty());

    // check pushing empty delta from different client
    remoteSync = remoteDatabase->open(OTHER_CLIENT);
    BOOST_REQUIRE_NO_THROW(
            std::tie(newRev, conflictDelta) =
                remoteSync->push(oldRev, {}));
    BOOST_REQUIRE(newRev == oldRev + 1);
    BOOST_REQUIRE(conflictDelta == delta);

    // push delta from other client
    Delta otherClientDelta;
    apply(&otherClientDelta, COLLECTION, RECORD, RecordDelta::Delete);
    BOOST_REQUIRE_NO_THROW(
            std::tie(newRev, conflictDelta) =
                remoteSync->push(oldRev + 1, otherClientDelta);
    );
    BOOST_REQUIRE(newRev == oldRev + 2);
    BOOST_REQUIRE(conflictDelta.empty());

    // check pushing old same delta
    remoteSync = remoteDatabase->open(CLIENT);
    BOOST_REQUIRE_NO_THROW(
            std::tie(newRev, conflictDelta) =
                remoteSync->push(oldRev, delta);
    );
    BOOST_REQUIRE(newRev == oldRev + 1);
    BOOST_REQUIRE(conflictDelta.empty());

    remoteManager->deleteDatabase(DATABASE, auth::async::Account(account));
}

BOOST_FIXTURE_TEST_CASE(modify_list, Fixture)
{
    auto remoteSync = remoteDatabase->open(CLIENT);

    {
        // create empty list
        Delta delta;
        apply(&delta, COLLECTION, RECORD, RecordDelta::Insert);
        FieldDelta fieldDelta{
            FieldDelta::Operation::Insert,
            FieldValue(std::vector<FieldValue>{})
        };
        apply(&delta, COLLECTION, RECORD, "list", fieldDelta);

        Revision newRev = UNDEFINED_REVISION;
        Delta conflictDelta;
        BOOST_REQUIRE_NO_THROW(
                std::tie(newRev, conflictDelta) =
                    remoteSync->push(0, delta));
        BOOST_REQUIRE(newRev == 1);
        BOOST_REQUIRE(conflictDelta.empty());
    }

    {
        Delta delta;
        FieldDelta fieldDelta{FieldDelta::Operation::ListChange};
        fieldDelta.put(ListDelta{ListDelta::Operation::InsertItem,
                FieldValue(std::string("first")),
                static_cast<size_t>(0)});
        fieldDelta.put(ListDelta{ListDelta::Operation::InsertItem,
                FieldValue(std::string("second")),
                static_cast<size_t>(1)});
        fieldDelta.put(ListDelta{ListDelta::Operation::MoveItem,
                boost::none,
                static_cast<size_t>(0),
                static_cast<size_t>(1)});
        fieldDelta.put(ListDelta{ListDelta::Operation::DeleteItem,
                boost::none,
                static_cast<size_t>(1)});
        fieldDelta.put(ListDelta{ListDelta::Operation::SetItem,
                FieldValue(std::string("third")),
                static_cast<size_t>(0)});
        apply(&delta, COLLECTION, RECORD, "list", fieldDelta);

        Revision newRev = UNDEFINED_REVISION;
        Delta conflictDelta;
        BOOST_REQUIRE_NO_THROW(
                std::tie(newRev, conflictDelta) =
                    remoteSync->push(1, delta));
        BOOST_REQUIRE(newRev == 2);
        BOOST_REQUIRE(conflictDelta.empty());
    }

    remoteManager->deleteDatabase(DATABASE, auth::async::Account(account));
}
