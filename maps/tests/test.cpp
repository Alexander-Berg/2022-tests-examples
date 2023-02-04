#include <boost/functional/hash.hpp>
#include <library/cpp/geobase/lookup.hpp>
#include <maps/analyzer/libs/flat_buffers/hash_map/include/map_builder.h>
#include <maps/analyzer/libs/flat_buffers/hash_map/include/map_reader.h>
#include <maps/analyzer/libs/flat_buffers/hash_map/tests/storage.fbs64.h>
#include <maps/libs/succinct_buffers/include/writers.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <util/generic/string.h>
#include <util/stream/file.h>
#include <util/system/filemap.h>

#include <unordered_map>
#include <string>
#include <fstream>
#include <iostream>

const std::string TEST_DATA_ROOT = "maps/analyzer/libs/flat_buffers/hash_map/tests/";

namespace fbs64 = maps::analyzer::flat_buffers::hash_map::tests::fbs64;

constexpr auto LOAD_FACTOR = 0.1;

struct MyNonScalarData {
    uint64_t foo;
    std::string bar;
};

bool operator==(const MyNonScalarData& lhs, const MyNonScalarData& rhs) {
    return lhs.foo == rhs.foo && lhs.bar == rhs.bar;
}

bool operator!=(const MyNonScalarData& lhs, const MyNonScalarData& rhs) {
    return !(lhs == rhs);
}

std::ostream& operator<<(std::ostream& os, const MyNonScalarData& data)
{
    os << "MyNonScalarData{" << data.foo << ", " << data.bar << "}";
    return os;
}

template<> void Out<MyNonScalarData>(IOutputStream& out, const MyNonScalarData& data) {
    out << data;
}

std::size_t hash_value(const MyNonScalarData& data) {
    std::size_t seed = 0;
    boost::hash_combine(seed, data.foo);
    boost::hash_combine(seed, data.bar);
    return seed;
}

namespace std {

template <>
class hash<MyNonScalarData> {
public:
    size_t operator() (const MyNonScalarData& d) const {
        return hash_value(d);
    }
};

} // std

struct MyNonScalarDataReader {
public:
    using value_type = MyNonScalarData;

    explicit MyNonScalarDataReader(const fbs64::NonScalarData& data):
        data_{
            data.foo(),
            {data.bar()->c_str(), data.bar()->size()}
        }
    {}

    value_type read() const {
        return data_;
    }

    bool operator!= (const value_type& val) { return val != data_; }

    value_type value() const { return data_; }
private:
    value_type data_;
};

struct MyNonScalarDataWriter {
public:
    using value_type = MyNonScalarData;

    MyNonScalarDataWriter() = default;
    explicit MyNonScalarDataWriter(const value_type& val):
        data_{val}
    {}

    auto commit(flatbuffers64::FlatBufferBuilder* builder) const {
        const auto barOffset = builder->CreateString(data_.bar);
        return fbs64::CreateNonScalarData(
            *builder,
            data_.foo,
            barOffset
        );
    }

    bool operator!=(const value_type& other) const { return data_ != other;}

    value_type value() const { return data_; }

private:
    value_type data_;
};


using NonScalarToScalarMapReader = maps::analyzer::flat_buffers::impl::MapReader<
    MyNonScalarDataReader, uint64_t, fbs64::NonScalarToScalarMap, std::hash<MyNonScalarData>
>;
using NonScalarToNonScalarMapReader = maps::analyzer::flat_buffers::impl::MapReader<
    MyNonScalarDataReader, MyNonScalarDataReader, fbs64::NonScalarToNonScalarMap, std::hash<MyNonScalarData>
>;
using ScalarToScalarMapReader = maps::analyzer::flat_buffers::impl::MapReader<
    uint64_t, uint64_t, fbs64::ScalarToScalarMap
>;
using ScalarToNonScalarMapReader = maps::analyzer::flat_buffers::impl::MapReader<
    uint64_t, MyNonScalarDataReader, fbs64::ScalarToNonScalarMap
>;

class MyTestStorageReader {
public:
    MyTestStorageReader(const std::string& filePath, bool populate):
        fileMap_{TString{filePath}},
        ptr_{nullptr},
        len_{static_cast<std::size_t>(fileMap_->Length())}
    {
        fileMap_->Map(/* offset = */ 0, fileMap_->Length());
        ptr_ = fileMap_->Ptr();
        if (populate) {
            fileMap_->Precharge();
        }
    }

    explicit MyTestStorageReader(std::vector<uint8_t>&& data):
        data_{std::move(data)},
        ptr_{data_->data()},
        len_{data_->size()}
    {}

    MyTestStorageReader(const void* data, std::size_t len):
        ptr_{data},
        len_{len}
    {}

    MyTestStorageReader(const MyTestStorageReader&) = delete;
    MyTestStorageReader(MyTestStorageReader&&) = default;
    MyTestStorageReader& operator=(const MyTestStorageReader&) = delete;
    MyTestStorageReader& operator=(MyTestStorageReader&&) = default;

    const void* ptr() const { return ptr_; }
    std::size_t len() const { return len_; }

private:
    std::optional<TFileMap> fileMap_;
    std::optional<std::vector<uint8_t>> data_;
    const void* ptr_;
    std::size_t len_;
};

const fbs64::Test* getMyTestStoragePtr(
    const void* data,
    std::size_t len
) {
    flatbuffers64::Verifier verifier{
        reinterpret_cast<const uint8_t*>(data),
        len
    };
    REQUIRE(
        fbs64::VerifyTestBuffer(verifier),
        "Invalid Test data. Failed offset verification"
    );

    const auto storage = fbs64::GetTest(data);

    REQUIRE(storage->nonScalarToScalarMap(), "TestStorage: nonScalarToScalarMap required");
    REQUIRE(storage->nonScalarToNonScalarMap(), "TestStorage: nonScalarToNonScalarMap required");
    REQUIRE(storage->scalarToScalarMap(), "TestStorage: scalarToScalarMap required");
    REQUIRE(storage->scalarToNonScalarMap(), "TestStorage: scalarToNonScalarMap required");
    return storage;
}

class MyTestStorage {
public:
    MyTestStorage(const std::string& filePath, bool populate):
        MyTestStorage{MyTestStorageReader{filePath, populate}}
    {}
    explicit MyTestStorage(std::vector<uint8_t>&& data):
        MyTestStorage{MyTestStorageReader{std::move(data)}}
    {}
    MyTestStorage(const void* data, std::size_t len):
        MyTestStorage{MyTestStorageReader{data, len}}
    {}

    MyTestStorage(const MyTestStorage&) = delete;
    MyTestStorage(MyTestStorage&&) = default;
    MyTestStorage& operator=(const MyTestStorage&) = delete;
    MyTestStorage& operator=(MyTestStorage&&) = default;

    std::optional<uint64_t> nonScalarToScalarMapFind(const MyNonScalarData& key) {
        return nonScalarToScalarMapReader_.find(key);
    }

    std::optional<MyNonScalarData> nonScalarToNonScalarMapFind(const MyNonScalarData& key) {
        if (const auto reader = nonScalarToNonScalarMapReader_.find(key)) {
            return reader->read();
        }
        return std::nullopt;
    }

    std::optional<uint64_t> scalarToScalarMapFind(uint64_t key) {
        return scalarToScalarMapReader_.find(key);
    }

    std::optional<MyNonScalarData> scalarToNonScalarMapFind(uint64_t key) {
        if (const auto reader = scalarToNonScalarMapReader_.find(key)) {
            return reader->read();
        }
        return std::nullopt;
    }

private:
    explicit MyTestStorage(
        MyTestStorageReader&& myTestStorageReader
    ):
        rawStorage_{std::move(myTestStorageReader)},
        storage_{getMyTestStoragePtr(rawStorage_.ptr(), rawStorage_.len())},
        nonScalarToScalarMapReader_{storage_->nonScalarToScalarMap()},
        nonScalarToNonScalarMapReader_{storage_->nonScalarToNonScalarMap()},
        scalarToScalarMapReader_{storage_->scalarToScalarMap()},
        scalarToNonScalarMapReader_{storage_->scalarToNonScalarMap()}
    {}

    MyTestStorageReader rawStorage_;
    const fbs64::Test* storage_;

    const NonScalarToScalarMapReader nonScalarToScalarMapReader_;
    const NonScalarToNonScalarMapReader nonScalarToNonScalarMapReader_;
    const ScalarToScalarMapReader scalarToScalarMapReader_;
    const ScalarToNonScalarMapReader scalarToNonScalarMapReader_;
};

using NonScalarToScalarMapWriter = maps::analyzer::flat_buffers::impl::MapWriter<
    MyNonScalarDataWriter, uint64_t, std::hash<MyNonScalarData>
>;
using NonScalarToNonScalarMapWriter = maps::analyzer::flat_buffers::impl::MapWriter<
    MyNonScalarDataWriter, MyNonScalarDataWriter, std::hash<MyNonScalarData>
>;
using ScalarToScalarMapWriter = maps::analyzer::flat_buffers::impl::MapWriter<
    uint64_t, uint64_t
>;
using ScalarToNonScalarMapWriter = maps::analyzer::flat_buffers::impl::MapWriter<
    uint64_t, MyNonScalarDataWriter
>;

struct MyTestStorageWriter {
public:
    using offset_type = flatbuffers64::Offset<fbs64::Test>;

    explicit MyTestStorageWriter(
        std::unordered_map<MyNonScalarData, uint64_t> nonScalarToScalarMap,
        std::unordered_map<MyNonScalarData, MyNonScalarData> nonScalarToNonScalarMap,
        std::unordered_map<uint64_t, uint64_t> scalarToScalarMap,
        std::unordered_map<uint64_t, MyNonScalarData> scalarToNonScalarMap
    ):
        nonScalarToScalarMapWriter_{
            static_cast<std::size_t>(nonScalarToScalarMap.size() / LOAD_FACTOR)
        },
        nonScalarToNonScalarMapWriter_{
            static_cast<std::size_t>(nonScalarToNonScalarMap.size() / LOAD_FACTOR)
        },
        scalarToScalarMapWriter_{
            static_cast<std::size_t>(scalarToScalarMap.size() / LOAD_FACTOR)
        },
        scalarToNonScalarMapWriter_{
            static_cast<std::size_t>(scalarToNonScalarMap.size() / LOAD_FACTOR)
        }
    {
        for (const auto& [k, v]: nonScalarToScalarMap) {
            nonScalarToScalarMapWriter_.emplace(k, v);
        }

        for (const auto& [k, v]: nonScalarToNonScalarMap) {
            nonScalarToNonScalarMapWriter_.emplace(k, v);
        }

        for (const auto& [k, v]: scalarToScalarMap) {
            scalarToScalarMapWriter_.emplace(k, v);
        }

        for (const auto& [k, v]: scalarToNonScalarMap) {
            scalarToNonScalarMapWriter_.emplace(k, v);
        }
    }

    offset_type commit(flatbuffers64::FlatBufferBuilder* builder) const {
        const auto nonScalarToScalarMapOffset = nonScalarToScalarMapWriter_.commit<
            fbs64::NonScalarPtrBuilder,
            fbs64::ScalarPtrBuilder,
            fbs64::NonScalarToScalarMapBuilder
        >(builder);

        const auto nonScalarToNonScalarMapOffset = nonScalarToNonScalarMapWriter_.commit<
            fbs64::NonScalarPtrBuilder,
            fbs64::NonScalarPtrBuilder,
            fbs64::NonScalarToNonScalarMapBuilder
        >(builder);

        const auto scalarToScalarMapOffset = scalarToScalarMapWriter_.commit<
            fbs64::ScalarPtrBuilder,
            fbs64::ScalarPtrBuilder,
            fbs64::ScalarToScalarMapBuilder
        >(builder);

        const auto scalarToNonScalarMapOffset = scalarToNonScalarMapWriter_.commit<
            fbs64::ScalarPtrBuilder,
            fbs64::NonScalarPtrBuilder,
            fbs64::ScalarToNonScalarMapBuilder
        >(builder);

        return fbs64::CreateTest(
            *builder,
            nonScalarToScalarMapOffset,
            nonScalarToNonScalarMapOffset,
            scalarToScalarMapOffset,
            scalarToNonScalarMapOffset
        );
    }

private:
    mutable NonScalarToScalarMapWriter nonScalarToScalarMapWriter_;
    mutable NonScalarToNonScalarMapWriter nonScalarToNonScalarMapWriter_;
    mutable ScalarToScalarMapWriter scalarToScalarMapWriter_;
    mutable ScalarToNonScalarMapWriter scalarToNonScalarMapWriter_;
};

Y_UNIT_TEST_SUITE(HashMapTest) {
    Y_UNIT_TEST(WriteReadTest) {
        const auto testFile = BinaryPath(TEST_DATA_ROOT + "tmp.test.fb");

        std::unordered_map<MyNonScalarData, uint64_t> nonScalarToScalarMap {
            { { 12, "twelve" }, 144 }
        };

        std::unordered_map<MyNonScalarData, MyNonScalarData> nonScalarToNonScalarMap {
            { { 12, "twelve" }, { 11, "eleven" } }
        };

        std::unordered_map<uint64_t, uint64_t> scalarToScalarMap {
            { 12, 144 }
        };

        std::unordered_map<uint64_t, MyNonScalarData> scalarToNonScalarMap {
            { 12, { 11, "eleven" } }
        };

        flatbuffers64::FlatBufferBuilder builder;
        MyTestStorageWriter writer {
            nonScalarToScalarMap,
            nonScalarToNonScalarMap,
            scalarToScalarMap,
            scalarToNonScalarMap
        };

        const auto offset = writer.commit(&builder);
        builder.Finish(offset);
        maps::writeFlatBuffersToFile(builder, testFile);

        MyTestStorage storage {testFile, false};

        {
            const auto found = storage.nonScalarToScalarMapFind({12, "twelve"});
            EXPECT_TRUE(found);
            EXPECT_EQ(*found, 144);
            const auto notFound = storage.nonScalarToScalarMapFind({10, "ten"});
            EXPECT_FALSE(notFound);
        }
        {
            const auto found = storage.nonScalarToNonScalarMapFind({12, "twelve"});
            EXPECT_TRUE(found);
            const auto foundCheck = MyNonScalarData{11, "eleven"};
            EXPECT_EQ(*found, foundCheck);

            const auto notFound = storage.nonScalarToNonScalarMapFind({10, "ten"});
            EXPECT_FALSE(notFound);
        }
        {
            const auto found = storage.scalarToScalarMapFind(12);
            EXPECT_TRUE(found);
            EXPECT_EQ(*found, 144);

            const auto notFound = storage.scalarToScalarMapFind(10);
            EXPECT_FALSE(notFound);
        }
        {
            const auto found = storage.scalarToNonScalarMapFind(12);
            EXPECT_TRUE(found);
            const auto foundCheck = MyNonScalarData{11, "eleven"};
            EXPECT_EQ(*found, foundCheck);

            const auto notFound = storage.scalarToNonScalarMapFind(10);
            EXPECT_FALSE(notFound);
        }
    }
}
