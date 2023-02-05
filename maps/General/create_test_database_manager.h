#pragma once

#include <yandex/datasync/database_manager.h>

#include <yandex/maps/export.h>

namespace yandex::datasync::internal {

YANDEX_EXPORT std::shared_ptr<DatabaseManager> createTestDatabaseManager();

} // namespace yandex::datasync::internal
