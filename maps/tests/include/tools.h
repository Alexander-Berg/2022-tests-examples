#pragma once

/*
   BOOST analogs are not thread safe, so we use our own implementation
*/

#define SAFE_CHECK_EQUAL(a, b) if (!((a) == (b))) {\
    std::cerr << #a << "=" << a << " != " << #b << "=" << b << std::endl;\
    exit(1); \
}

#define SAFE_CHECK(a) if (!(a)) {\
    std::cerr << #a << " failed" << std::endl;\
    exit(1); \
}
