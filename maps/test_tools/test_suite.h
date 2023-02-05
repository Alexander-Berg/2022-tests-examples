#pragma once

#include <maps/libs/common/include/exception.h>

#include <vector>
#include <map>
#include <set>
#include <string>
#include <memory>
#include <type_traits>

namespace maps {
namespace wiki {
namespace topo {
namespace test {

/**
 * Test suite class. Holds tests of one type.
 */
template <class TestDataT>
class TestSuite {
public:
    typedef std::vector<TestDataT> TestVector;
    typedef typename TestVector::const_iterator Iterator;

    /// Test suite name must be unique within all registered suites.
    explicit TestSuite(const std::string& name)
        : name_(name)
    {}

    const std::string& name() const { return name_; }

    /// Test name must be unique within suite
    void add(const std::string& name, TestDataT test);

    Iterator begin() const { return tests_.begin(); }
    Iterator end() const { return tests_.end(); }

protected:
    std::map<std::string, typename TestVector::iterator> index_;
    TestVector tests_;

    std::string name_;
};


/// Test suite visitors

/**
 * Primary visitor interface providing methods for visiting
 *  one test ore one test suite.
 */
template <class TestTypeT>
class PlainVisitor {
public:
    virtual void visit(const TestSuite<TestTypeT>& suite) = 0;
    virtual void visit(
        const std::string& suiteName, const TestTypeT& test) = 0;
};


/**
 * Helper class inheriting PlainVisitor interfaces for several test types.
 */
template <class... TestTypesT> class VisitorImpl;

template <>
class VisitorImpl<> {};

template <class TestTypeT, class... TestTypesT>
class VisitorImpl<TestTypeT, TestTypesT...>
    : public VisitorImpl<TestTypesT...>
    , public PlainVisitor<TestTypeT>
{};

/**
 * Main Visitor class - provides interface for TestSuitesHolder traversal.
 */
template <class... TestTypesT>
class Visitor : public VisitorImpl<TestTypesT...> {};


/**
 * Helper class.
 * Stores collection of test suites of one type
 *  and provides interface for all its suites and tests traversal with PlainVisitor.
 */
template <class TestTypeT>
class BaseTestSuitesHolder
{
protected:
    explicit BaseTestSuitesHolder(std::set<std::string>& suiteNames)
        : suiteNames_(suiteNames)
    {}

    void add(const TestSuite<TestTypeT>* suite);

    void visit(PlainVisitor<TestTypeT>& visitor) const;

    void visit(
        PlainVisitor<TestTypeT>& visitor,
        const std::string& suiteName) const;

    void visit(
        PlainVisitor<TestTypeT>& visitor,
        const std::string& suiteName,
        const std::string& testName) const;

    /// all registered suite names to check whether suite name is registered
    std::set<std::string>& suiteNames_;

    typedef const TestSuite<TestTypeT>* SuitePtr;
    std::map<std::string, SuitePtr> suites_;
};

/**
 * Helper class to aggregate test suites storage and traversal
 *  for a set of test types.
 */
template <class... TestTypesT> class TestSuitesHolderImpl;

template <class TestTypeT>
class TestSuitesHolderImpl<TestTypeT> : protected BaseTestSuitesHolder<TestTypeT>
{
protected:
    explicit TestSuitesHolderImpl(std::set<std::string>& suiteNames)
        : BaseTestSuitesHolder<TestTypeT>(suiteNames)
    {}
};

template <class TestTypeT, class... TestTypesT>
class TestSuitesHolderImpl<TestTypeT, TestTypesT...>
    : protected BaseTestSuitesHolder<TestTypeT>
    , protected TestSuitesHolderImpl<TestTypesT...>
{
protected:
    explicit TestSuitesHolderImpl(std::set<std::string>& suiteNames)
        : BaseTestSuitesHolder<TestTypeT>(suiteNames)
        , TestSuitesHolderImpl<TestTypesT...>(suiteNames)
    {}
};

/**
 * Main test suites holder class.
 * Stores test suites of different test types
 *   and provides interface for its traversal with Visitor.
 * Visitor's supported test types must be a subset of test types supported by holder.
 */
template <class... TestTypesT>
class TestSuitesHolder : public TestSuitesHolderImpl<TestTypesT...>
{
public:
    TestSuitesHolder()
        : TestSuitesHolderImpl<TestTypesT...>(suiteNames_)
    {}

    template <class TestTypeT>
    void add(const TestSuite<TestTypeT>* suite)
    {
        BaseTestSuitesHolder<TestTypeT>::add(suite);
    }

    template <class... VTs>
    void visit(Visitor<VTs...>& visitor) const
    {
        visitImpl<VTs...>(static_cast<VisitorImpl<VTs...>&>(visitor));
    }

    template <class... VTs>
    void visit(Visitor<VTs...>& visitor, const std::string& suiteName) const
    {
        visitImpl<VTs...>(static_cast<VisitorImpl<VTs...>&>(visitor), suiteName);
    }

    template <class... VTs>
    void visit(Visitor<VTs...>& visitor,
        const std::string& suiteName, const std::string& testName) const
    {
        visitImpl<VTs...>(static_cast<VisitorImpl<VTs...>&>(visitor), suiteName, testName);
    }

protected:

    template <class VT, class... VTs>
    void visitImpl(VisitorImpl<VT, VTs...>& visitor,
        const typename std::enable_if<sizeof...(VTs) != 0, VT>::type* = nullptr) const
    {
        BaseTestSuitesHolder<VT>::visit(static_cast<PlainVisitor<VT>&>(visitor));
        visitImpl<VTs...>(visitor);
    }

    template <class TestTypeT>
    void visitImpl(VisitorImpl<TestTypeT>& visitor) const
    {
        BaseTestSuitesHolder<TestTypeT>::visit(
            static_cast<PlainVisitor<TestTypeT>&>(visitor));
    }


    template <class VT, class... VTs>
    void visitImpl(
        VisitorImpl<VT, VTs...>& visitor,
        const std::string& suiteName,
        const typename std::enable_if<sizeof...(VTs) != 0, VT>::type* = nullptr) const
    {
        BaseTestSuitesHolder<VT>::visit(static_cast<PlainVisitor<VT>&>(visitor), suiteName);
        visitImpl<VTs...>(visitor, suiteName);
    }

    template <class TestTypeT>
    void visitImpl(
        VisitorImpl<TestTypeT>& visitor,
        const std::string& suiteName) const
    {
        REQUIRE(suiteNames_.count(suiteName), "No such suite: " << suiteName);
        BaseTestSuitesHolder<TestTypeT>::visit(
            static_cast<PlainVisitor<TestTypeT>&>(visitor), suiteName);
    }


    template <class VT, class... VTs>
    void visitImpl(
        VisitorImpl<VT, VTs...>& visitor,
        const std::string& suiteName,
        const std::string& testName,
        const typename std::enable_if<sizeof...(VTs) != 0, VT>::type* = nullptr) const
    {
        BaseTestSuitesHolder<VT>::visit(
            static_cast<PlainVisitor<VT>&>(visitor), suiteName, testName);
        visitImpl<VTs...>(visitor, suiteName, testName);
    }

    template <class TestTypeT>
    void visitImpl(
        VisitorImpl<TestTypeT>& visitor,
        const std::string& suiteName,
        const std::string& testName) const
    {
        REQUIRE(suiteNames_.count(suiteName), "No such suite: " << suiteName);
        BaseTestSuitesHolder<TestTypeT>::visit(
            static_cast<PlainVisitor<TestTypeT>&>(visitor), suiteName, testName);
    }

    std::set<std::string> suiteNames_;
};

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps

#define YANDEX_MAPS_WIKI_TOPO_TEST_SUITE_H
#include "test_suite_inl.h"
#undef YANDEX_MAPS_WIKI_TOPO_TEST_SUITE_H
