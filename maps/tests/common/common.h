#pragma once

#include <maps/factory/libs/db/acl.h>
#include <maps/factory/libs/db/order.h>

#include <maps/libs/http/include/test_utils.h>

namespace maps::factory::sputnica::tests {

void setAuthHeaderFor(db::Role role, http::MockRequest& rq);

} //namespace maps::factory::sputnica::tests
