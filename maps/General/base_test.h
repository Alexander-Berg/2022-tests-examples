#pragma once

#include <maps/libs/edge_persistent_index/include/persistent_index.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/libs/road_graph/include/graph.h>

#include <map>
#include <memory>

struct TestData {
    const maps::road_graph::Graph* roadGraphEth_ = nullptr;
    const maps::road_graph::PersistentIndex* persistentIndexEth_ = nullptr;
    const maps::road_graph::Graph* roadGraphExp_ = nullptr;
    const maps::road_graph::PersistentIndex* persistentIndexExp_ = nullptr;
};

class BaseTest {
public:
    BaseTest() = default;

    virtual ~BaseTest() = default;

    virtual void operator()() const = 0;

    void init(const TestData& data) {
        data_ = &data;
    }

    const maps::road_graph::Graph& etalonRoadGraph() const {
        return *(data_->roadGraphEth_);
    }
    const maps::road_graph::PersistentIndex& etalonPersistentIndex() const {
        return *(data_->persistentIndexEth_);
    }
    const maps::road_graph::Graph& experimentRoadGraph() const {
        return *(data_->roadGraphExp_);
    }
    const maps::road_graph::PersistentIndex& experimentPersistentIndex() const {
        return *(data_->persistentIndexExp_);
    }

private:
    const TestData* data_ = nullptr;
};

class BaseTestFactory {
public:
    virtual ~BaseTestFactory() = default;
    virtual std::unique_ptr<BaseTest> operator()(
        const TestData& data) const = 0;
};

template <typename Test>
class TestFactory : public BaseTestFactory {
    virtual std::unique_ptr<BaseTest> operator()(
            const TestData& data) const override {
        auto test = std::make_unique<Test>();
        test->init(data);
        return test;
    }
};

template <typename Test>
std::unique_ptr<BaseTestFactory> makeTestFactory() {
    return std::make_unique<TestFactory<Test>>();
}

class TestCollection {
public:
    template <typename Test>
    static void add(std::string name){
        factories().emplace(std::move(name), makeTestFactory<Test>());
    }

    static void run(const TestData& data, const std::string& testName) {
        for (const auto& [name, factory]: factories()) {
            if (!testName.empty() && testName != name) {
                continue;
            }
            INFO() << "Running test " << name;
            INFO() << "";
            auto test = (*factory)(data);
            (*test)();
            INFO() << "";
        };
    }

private:
    using Factories = std::multimap<std::string, std::unique_ptr<BaseTestFactory>>;

    static Factories& factories() {
        static Factories obj;
        return obj;
    }
};

#define DECLARE_TEST(Test) \
namespace { int add_##Test = []{TestCollection::add<Test>(""#Test); return 0;}(); }
