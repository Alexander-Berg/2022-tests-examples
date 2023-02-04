#include <maps/libs/common/include/environment.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::common::tests {

TEST(test_environment, test_resolving_environment)
{
	EXPECT_EQ(impl::resolveEnvironment("unittest"), Environment::Unittest);
	EXPECT_EQ(impl::resolveEnvironment("development"), Environment::Development);
	EXPECT_EQ(impl::resolveEnvironment("unstable"), Environment::Unstable);
	EXPECT_EQ(impl::resolveEnvironment("testing"), Environment::Testing);

    //both "load" and "stress" must be resolved to Load
	EXPECT_EQ(impl::resolveEnvironment("load"), Environment::Load);
	EXPECT_EQ(impl::resolveEnvironment("stress"), Environment::Load);

    //"production", "prestable" and "stable" must be resolved to Stable
	EXPECT_EQ(impl::resolveEnvironment("stable"), Environment::Stable);
	EXPECT_EQ(impl::resolveEnvironment("prestable"), Environment::Stable);
	EXPECT_EQ(impl::resolveEnvironment("production"), Environment::Stable);

	EXPECT_EQ(impl::resolveEnvironment("datatesting"), Environment::Datatesting);

    //everything else must be resolved to Other
	EXPECT_EQ(impl::resolveEnvironment("other"), Environment::Other);
	EXPECT_EQ(impl::resolveEnvironment("brother"), Environment::Other);
	EXPECT_EQ(impl::resolveEnvironment("wrether"), Environment::Other);
}

TEST(test_environment, test_serializing_environment)
{
    //testing if synonymical serialization takes first option as the result
    EXPECT_EQ(toString(Environment::Stable), "stable");
    EXPECT_EQ(toString(Environment::Load), "load");
}

} //namespace maps::common::tests
