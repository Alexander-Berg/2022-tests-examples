#ifndef YANDEX_MAPS_WIKI_TEST_TOOLS_SUITE_H
#error "Direct inclusion of test_suite_inl.h is forbidden"
#endif

namespace maps {
namespace wiki {
namespace test_tools {

template <class TestDataT>
void TestSuite<TestDataT>::add(const std::string& name, TestDataT test)
{
    REQUIRE(!index_.count(name),
        "Test named " << name << " already exists in suite " << this->name());
    test.setName(name);
    tests_.push_back(std::move(test));
    index_.insert({name, std::prev(tests_.end())});
}


template <class TestTypeT>
void BaseTestSuitesHolder<TestTypeT>::add(const TestSuite<TestTypeT>* suite)
{
    REQUIRE(suiteNames_.insert(suite->name()).second,
        "Test suite with name " << suite->name() << " already exists");
    suites_.insert({suite->name(), suite});
}
template <class TestTypeT>
void BaseTestSuitesHolder<TestTypeT>::visit(
    PlainVisitor<TestTypeT>& visitor) const
{
    for (const auto& suite : suites_) {
        visitor.visit(*suite.second);
    }
}
template <class TestTypeT>
void BaseTestSuitesHolder<TestTypeT>::visit(
    PlainVisitor<TestTypeT>& visitor, const std::string& suiteName) const
{
    auto it = suites_.find(suiteName);
    if (it == suites_.end()) {
        return;
    }
    visitor.visit(*it->second);
}
template <class TestTypeT>
void BaseTestSuitesHolder<TestTypeT>::visit(
    PlainVisitor<TestTypeT>& visitor,
    const std::string& suiteName,
    const std::string& testName) const
{
    auto it = suites_.find(suiteName);
    if (it == suites_.end()) {
        return;
    }
    for (const auto& test : *it->second) {
        if (test.name() == testName) {
            visitor.visit(suiteName, test);
            return;
        }
    }
    REQUIRE(false, "Test " << testName << " not found in suite " << suiteName);
}

} // namespace test_tools
} // namespace wiki
} // namespace maps
