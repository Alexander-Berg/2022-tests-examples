#pragma once

#include <solomon/libs/cpp/conf_db/db.h>

namespace NSolomon::NTesting {
    NDb::IDbConnectionPtr CreateMockConnection();
} // namespace NSolomon::NTesting
