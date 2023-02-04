#include "maps/automotive/store_internal/tests/postgres.h"

#include <maps/automotive/store_internal/lib/dao/app.h>
#include <maps/automotive/store_internal/lib/dao/firmware.h>
#include <maps/automotive/store_internal/lib/dao/firmware_rollout.h>
#include <maps/automotive/store_internal/lib/dao/headunit.h>
#include <maps/automotive/store_internal/lib/dao/idm.h>
#include <maps/automotive/store_internal/lib/dao/package.h>
#include <maps/automotive/store_internal/lib/dao/package_rollout.h>
#include <maps/automotive/store_internal/lib/helpers.h>
#include <maps/automotive/store_internal/lib/common/namespace.h>
#include <maps/automotive/store_internal/tests/helpers.h>

namespace maps::automotive::store_internal {

namespace {

void populateTestData(pqxx::transaction_base& txn)
{
    HeadUnitDao(txn).add(defaultHeadUnits());

    FirmwareDao fwDao(txn);
    fwDao.create(firmwareNoRollout());
    fwDao.create(firmwareWithRollout());
    fwDao.create(firmwareWithFeature());

    FirmwareRolloutDao roDao(txn);
    roDao.upsert(defaultFirmwareRollout());

    AppDao appDao(txn);
    auto apps = defaultApps();
    for (const auto& app: apps.apps()) {
        appDao.createOrUpdate(app);
    }

    PackageDao pkgDao(txn);
    pkgDao.create(packageNoRollout());
    pkgDao.create(packageWithRollout());
    pkgDao.create(packageWithDependency());

    PackageRolloutDao pkgRolloutDao(txn);
    pkgRolloutDao.upsert(defaultPackageRollout());

    IdmDao(txn).addIdmUserRole("admin-arnold", role::ADMIN);
    IdmDao(txn).addIdmUserRole("manager-prod", role::RELEASE_MANAGER_PRODUCTION);
    IdmDao(txn).addIdmUserRole("manager", role::RELEASE_MANAGER_INTERNAL);
    IdmDao(txn).addIdmUserRole("key-manager-prod", role::DEVICE_KEY_MANAGER_PRODUCTION);
    IdmDao(txn).addIdmUserRole("key-manager", role::DEVICE_KEY_MANAGER_VIRTUAL);
    IdmDao(txn).addIdmUserRole("viewer-victor", role::VIEWER);
}

maps::local_postgres::Database& initPostgresTemplate()
{
    struct Initializer
    {
        Initializer()
        {
            postgres.createExtension("hstore");
            postgres.applyMigrations(appSourcePath("migrations"));

            pqxx::connection connection(postgres.connectionString());
            pqxx::transaction<> txn(connection);
            populateTestData(txn);
            txn.commit();
        }
        maps::local_postgres::Database postgres;
    };
    static Initializer db;
    return db.postgres;
}

const pgpool3::PoolConstants POOL_CONSTANTS = pgpool3::PoolConstants(
    /* masterSize */    1,
    /* masterMaxSize */ concurrent::DEFAULT_THREADS_NUMBER.value(),
    /* slaveSize */     1,
    /* slaveMaxSize */  concurrent::DEFAULT_THREADS_NUMBER.value());

} //namespace

AppContextPostgresMock::AppContextPostgresMock()
    : AppContextMock()
    , postgres_(initPostgresTemplate().clone())
    , pgPool_(postgres_.connectionString(), POOL_CONSTANTS)
{
    initIdm();
}

AppContextPostgresFixture::AppContextPostgresFixture()
    : AppContextFixture(BaseCtorTag{})
{
    s3::initAwsApi();
    g_ctx.reset(new AppContextPostgresMock());
}

} // namespace maps::automotive::store_internal
