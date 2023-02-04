#include <vector>
#include <cstdint>
#include <boost/test/unit_test.hpp>
#include <boost/format.hpp>
#include "buffer_writer.test.h"
#include "buffer_writer.h"
#include "../../../src/tile_decoder/buffer_writers/buffer_writer.h"

namespace yandex::maps::jsapi::vector::tileDecoder::bufferWriters::test {

namespace {
    struct TestDataPoint {
        float x;
        float y;
    };

    struct TestVertex {
        uint16_t x;
        uint16_t y;
        uint8_t color[4];
    };

    class TestBufferWriter: public BufferWriter<TestVertex> {
    public:
        TestBufferWriter(size_t maxVertexBufferSize): BufferWriter<TestVertex>(maxVertexBufferSize) {}

        WrittenPrimitive writeData(std::vector<TestDataPoint> data) {
            for (uint16_t i = 0; i < data.size(); i++) {
                reserveVertex_() = {
                    static_cast<uint16_t>(data[i].x * UINT16_MAX),
                    static_cast<uint16_t>(data[i].y * UINT16_MAX),
                    {0x01, 0x02, 0x03, 0x04}
                };
                writeIndex_(i);
            }

            return endMesh_();
        }
    };
}

void bufferWriterTest() {
    {
        TestBufferWriter writer(100);

        const auto primitive = writer.writeData({{1.0f / UINT16_MAX, 0.0f}});

        BOOST_CHECK_EQUAL(writer.pages().size(), 1);
        BOOST_CHECK_EQUAL(writer.pages()[0].vertices.size(), 1);
        BOOST_CHECK_EQUAL(writer.pages()[0].indices.size(), 1);

        const auto expectedSize = 1 * sizeof(TestVertex);
        const uint8_t expectedVertexData[] = {0x01, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04};
        const uint8_t* vertexData = reinterpret_cast<const uint8_t*>(writer.pages()[0].vertices.data());
        BOOST_CHECK_EQUAL_COLLECTIONS(vertexData, vertexData + expectedSize, expectedVertexData, expectedVertexData + expectedSize);
        BOOST_CHECK(primitive == (WrittenPrimitive{0, (1 * sizeof(TestVertex)), 0, 2, 0}));
    }

    {
        TestBufferWriter writer(5);
        size_t expectedVertexByteSize;
        const uint8_t* vertexData;
        size_t expectedIndexByteSize;
        const uint8_t* indexData;

        const auto primitive0 = writer.writeData({{0.0f / UINT16_MAX, 0.0f}, {1.0f / UINT16_MAX, 0.0f}});
        const auto primitive1 = writer.writeData({{10.0f / UINT16_MAX, 0.0f}, {11.0f / UINT16_MAX, 0.0f}});

        expectedVertexByteSize = 4 * sizeof(TestVertex);
        const uint8_t expectedVertexData1[] = {
            0x0, 0x0, 0x0, 0x0, 0x1, 0x2, 0x3, 0x4,  0x1, 0x0, 0x0, 0x0, 0x1, 0x2, 0x3, 0x4,
            0xA, 0x0, 0x0, 0x0, 0x1, 0x2, 0x3, 0x4,  0xB, 0x0, 0x0, 0x0, 0x1, 0x2, 0x3, 0x4,
        };
        vertexData = reinterpret_cast<const uint8_t*>(writer.pages()[0].vertices.data());
        expectedIndexByteSize = 4 * sizeof(uint16_t);
        const uint8_t expectedIndexData1[] = {
            0x00, 0x00, 0x01, 0x00, 0x02, 0x00, 0x03, 0x00
        };
        indexData = reinterpret_cast<const uint8_t*>(writer.pages()[0].indices.data());
        BOOST_CHECK_EQUAL(writer.pages().size(), 1);
        BOOST_CHECK_EQUAL_COLLECTIONS(vertexData, vertexData + expectedVertexByteSize, expectedVertexData1, expectedVertexData1 + expectedVertexByteSize);
        BOOST_CHECK_EQUAL_COLLECTIONS(indexData, indexData + expectedIndexByteSize, expectedIndexData1, expectedIndexData1 + expectedIndexByteSize);
        BOOST_CHECK(primitive0 == (WrittenPrimitive{0, (2 * sizeof(TestVertex)), 0, 4, 0}));
        BOOST_CHECK(primitive1 == (WrittenPrimitive{(2 * sizeof(TestVertex)), (2 * sizeof(TestVertex)), 4, 4, 0}));

        const auto primitive2 = writer.writeData({
            {20.0f / UINT16_MAX, 0.0f},
            {21.0f / UINT16_MAX, 0.0f},
            {22.0f / UINT16_MAX, 0.0f}
        });

        expectedVertexByteSize = 3 * sizeof(TestVertex);
        const uint8_t expectedVertexData2[] = {
            0x14, 0x0, 0x0, 0x0, 0x1, 0x2, 0x3, 0x4,
            0x15, 0x0, 0x0, 0x0, 0x1, 0x2, 0x3, 0x4,
            0x16, 0x0, 0x0, 0x0, 0x1, 0x2, 0x3, 0x4,
        };
        vertexData = reinterpret_cast<const uint8_t*>(writer.pages()[1].vertices.data());
        expectedIndexByteSize = 3 * sizeof(uint16_t);
        const uint8_t expectedIndexData2[] = {0x00, 0x00, 0x01, 0x00, 0x02, 0x00};
        indexData = reinterpret_cast<const uint8_t*>(writer.pages()[1].indices.data());
        BOOST_CHECK_EQUAL(writer.pages().size(), 2);
        BOOST_CHECK_EQUAL_COLLECTIONS(vertexData, vertexData + expectedVertexByteSize, expectedVertexData2, expectedVertexData2 + expectedVertexByteSize);
        BOOST_CHECK_EQUAL_COLLECTIONS(indexData, indexData + expectedIndexByteSize, expectedIndexData2, expectedIndexData2 + expectedIndexByteSize);
        BOOST_CHECK_EQUAL(writer.pages()[0].vertices.size(), 4);
        BOOST_CHECK_EQUAL(writer.pages()[0].indices.size(), 4);
        BOOST_CHECK_EQUAL(writer.pages()[1].vertices.size(), 3);
        BOOST_CHECK_EQUAL(writer.pages()[1].vertices.size(), 3);
        BOOST_CHECK(primitive2 == (WrittenPrimitive{0, (3 * sizeof(TestVertex)), 0, 6, 1}));

        const auto primitive3 = writer.writeData({
            {30.0f / UINT16_MAX, 0.0f},
            {31.0f / UINT16_MAX, 0.0f},
            {32.0f / UINT16_MAX, 0.0f}
        });

        expectedVertexByteSize = 3 * sizeof(TestVertex);
        const uint8_t expectedVertexData3[] = {
            0x1E, 0x0, 0x0, 0x0, 0x1, 0x2, 0x3, 0x4,
            0x1F, 0x0, 0x0, 0x0, 0x1, 0x2, 0x3, 0x4,
            0x20, 0x0, 0x0, 0x0, 0x1, 0x2, 0x3, 0x4,
        };
        vertexData = reinterpret_cast<const uint8_t*>(writer.pages()[2].vertices.data());
        expectedIndexByteSize = 3 * sizeof(uint16_t);
        const uint8_t expectedIndexData3[] = {0x00, 0x00, 0x01, 0x00, 0x02, 0x00};
        indexData = reinterpret_cast<const uint8_t*>(writer.pages()[2].indices.data());
        BOOST_CHECK_EQUAL(writer.pages().size(), 3);
        BOOST_CHECK_EQUAL_COLLECTIONS(vertexData, vertexData + expectedVertexByteSize, expectedVertexData3, expectedVertexData3 + expectedVertexByteSize);
        BOOST_CHECK_EQUAL_COLLECTIONS(indexData, indexData + expectedIndexByteSize, expectedIndexData3, expectedIndexData3 + expectedIndexByteSize);
        BOOST_CHECK_EQUAL(writer.pages()[0].vertices.size(), 4);
        BOOST_CHECK_EQUAL(writer.pages()[0].indices.size(), 4);
        BOOST_CHECK_EQUAL(writer.pages()[1].vertices.size(), 3);
        BOOST_CHECK_EQUAL(writer.pages()[1].indices.size(), 3);
        BOOST_CHECK_EQUAL(writer.pages()[2].vertices.size(), 3);
        BOOST_CHECK_EQUAL(writer.pages()[2].indices.size(), 3);
        BOOST_CHECK(primitive3 == (WrittenPrimitive{0, (3 * sizeof(TestVertex)), 0, 6, 2}));
    }

}

}
