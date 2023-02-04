#include "unit_test_wrapper.h"
#include <sstream>
#include <fstream>

#include <iostream>

namespace boost { namespace unit_test {

class test_suite;
typedef test_suite* (*init_unit_test_func)( int, char* [] );
int unit_test_main(boost::unit_test::init_unit_test_func, int,char**);

} } // boost::unit_test

                    
boost::unit_test::test_suite* init_unit_test(int, char **)
{
    return 0;
}


int do_unit_tests()
{
    char arg0[] {"test"};
    char arg1[] {"--build_info"};
    char arg2[] {"--log_level=test_suite"};
    char arg3[] {"--run_test=*"};
    char* argv[] = {arg0, arg1, arg2, arg3};
    return boost::unit_test::unit_test_main(&init_unit_test, sizeof(argv) / sizeof(argv[0]), argv);
}
