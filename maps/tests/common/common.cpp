#include "common.h"
#include <maps/factory/libs/unittest/fixture.h>

#include <maps/infra/yacare/include/test_utils.h>

namespace maps::factory::sputnica::tests {

void setAuthHeaderFor(db::Role role, http::MockRequest& rq)
{
    switch (role) {
        case db::Role::Viewer:
            rq.headers[yacare::tests::USER_ID_HEADER] = std::to_string(unittest::TEST_VIEWER_USER_ID);
            break;
        case db::Role::Customer:
            rq.headers[yacare::tests::USER_ID_HEADER] = std::to_string(unittest::TEST_CUSTOMER_USER_ID);
            break;
        case db::Role::Supplier:
            rq.headers[yacare::tests::USER_ID_HEADER] = std::to_string(unittest::TEST_SUPPLIER_USER_ID);
            break;
        default:
            throw LogicError() << "This code should not be executed";
    }
}

} //namespace maps::factory::sputnica::tests
