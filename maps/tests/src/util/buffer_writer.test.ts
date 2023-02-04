import {expect} from 'chai';

import BufferWriter, {MemoryWriterLocation, WORD_BYTE_SIZE} from '../../../src/vector_render_engine/util/buffer_writer';
import {shuffle} from '../../../src/vector_render_engine/util/array';

describe('util/BufferWriter', () => {
    const INIT_VERTEX_BUFFER_SIZE = 4;
    const MAX_VERTEX_BUFFER_SIZE = 16;

    const INIT_INDEX_BUFFER_UINT16_SIZE = 8;
    const MAX_INDEX_BUFFER_UINT16_SIZE = 32;

    const TEST_VERTEX_DATA = new Uint32Array([
        0x40490fdb,
        0xdeadbeef,
        0xbad0cafe,
        0x7370616d
    ]);

    const TEST_INDEX_DATA = new Uint16Array([...new Array(16)].map((_, i) => i));
    shuffle(TEST_INDEX_DATA);

    class TestBufferWriter extends BufferWriter {
        constructor() {
            super(
                4,
                INIT_VERTEX_BUFFER_SIZE,
                MAX_VERTEX_BUFFER_SIZE,
                INIT_INDEX_BUFFER_UINT16_SIZE
            );
        }

        writeRandomMesh(vertexCount: number, indexCount: number): void {
            for (let i = 0; i < vertexCount; ++i) {
                this._writeWord(0x100000000 * Math.random() | 0);
            }

            const indices = new Array<number>(indexCount);
            for (let i = 0; i < indexCount; ++i) {
                indices[i] = vertexCount * Math.random() | 0;
            }

            this.writeIndices(indices);
        }

        writeRandomLengthMesh(): void {
            this.writeRandomMesh(
                MAX_VERTEX_BUFFER_SIZE * Math.random() >> 2,
                MAX_INDEX_BUFFER_UINT16_SIZE * Math.random() | 0
            );
        }

        writeTestVertexData(): void {
            this._writeFloat32(Math.PI);
            this._writeWord(0xdeadbeef);
            this._writeHalfWords(0xcafe, 0xbad0);
            this._writeBytes(0x6d, 0x61, 0x70, 0x73);
        }

        writeLotsOfRandomData(): void {
            for (let i = 0; i < 128; ++i) {
                const l = Math.random() * MAX_VERTEX_BUFFER_SIZE >> 2;
                for (let j = 0; j < l; ++j) {
                    this._writeWord(0xffffffff * Math.random() | 0);
                }
                this.writeIndices(
                    [...new Array(MAX_INDEX_BUFFER_UINT16_SIZE * Math.random() | 0)]
                        .map(() => l * Math.random() | 0)
                );
                this.endMesh();
            }
        }
    }

    let writer: TestBufferWriter;

    beforeEach(() => {
        writer = new TestBufferWriter();
    });

    it('should correctly write data to the vertex buffer', () => {
        writer.writeRandomLengthMesh();
        writer.endMesh();
        writer.writeTestVertexData();
        const location = writer.endMesh();
        expect(new Uint32Array(
            writer.getBuffers()[location.bufferIndex].vertexBuffer.buffer,
            location.vertexByteOffset,
            location.vertexByteLength >> 2
        )).to.be.deep.equal(TEST_VERTEX_DATA);
    });

    it('should correctly write data to the index buffer', () => {
        writer.writeRandomLengthMesh();
        writer.endMesh();
        writer.writeIndices(TEST_INDEX_DATA);
        const location = writer.endMesh();
        expect(new Uint16Array(
            writer.getBuffers()[location.bufferIndex].indexBuffer.buffer,
            location.indexByteOffset,
            location.indexByteLength >> 1
        ))
            .to.be.deep.equal(
                TEST_INDEX_DATA.map((i) => i + (location.vertexByteOffset >> 2))
            );
    });

    it('should not exceed max vertex buffer size', () => {
        writer.writeLotsOfRandomData();
        for (const buffer of writer.getBuffers()) {
            expect(buffer.vertexBuffer.byteLength).to.be.at.most(MAX_VERTEX_BUFFER_SIZE * WORD_BYTE_SIZE);
        }
    });

    it('should give correct vertex byte offset', () => {
        writer.writeTestVertexData();
        expect(writer.getCurrentVertexBufferByteOffset())
            .to.be.equal(TEST_VERTEX_DATA.byteLength);
    });

    it('should give correct index for the current vertex', () => {
        writer.writeRandomMesh(6, 10);
        expect(writer.getCurrentVertexIdx()).to.be.eq(6);
        writer.endMesh();
        expect(writer.getCurrentVertexIdx()).to.be.eq(0);
    });

    it('should correctly reapply base index to a mesh topology if its copied between buffers', () => {
        writer.writeRandomMesh(6, 10);
        const location1 = writer.endMesh();
        writer.writeRandomMesh(6, 10);
        const location2 = writer.endMesh();
        writer.writeRandomMesh(3, 6);
        writer.writeRandomMesh(12, 20);
        const location3 = writer.endMesh();
        const buffers = writer.getBuffers();

        function testIndices(location: MemoryWriterLocation): void {
            const buffer = buffers[location.bufferIndex].indexBuffer;
            const minIndex = location.vertexByteOffset >> 2;
            const maxIndex = minIndex + (location.vertexByteLength >> 2) - 1;

            for (
                let offset = location.indexByteOffset >> 1,
                    end = offset + (location.indexByteLength >> 1);
                offset < end;
                ++offset
            ) {
                expect(buffer[offset]).to.be.within(minIndex, maxIndex);
            }
        }

        testIndices(location1);
        testIndices(location2);
        testIndices(location3);
    });

    it('should correctly write index data for fans', () => {
        writer.writeIndicesForFan([...Array(8).keys()]);
        const location = writer.endMesh();
        const start = location.indexByteOffset >> 1;
        const end = start + location.indexByteLength >> 1;

        expect(Array.from(
            writer.getBuffers()[location.bufferIndex].indexBuffer.slice(start, end)
        )).to.be.deep.eq([
            0, 1, 2,
            0, 2, 3,
            0, 3, 4,
            0, 4, 5,
            0, 5, 6,
            0, 6, 7
        ]);
    });

    it('should correctly write index data for continuous fans', () => {
        writer.writeIndicesForContinuousFan(8);
        const location = writer.endMesh();
        const start = location.indexByteOffset >> 1;
        const end = start + location.indexByteLength >> 1;

        expect(Array.from(
            writer.getBuffers()[location.bufferIndex].indexBuffer.slice(start, end)
        )).to.be.deep.eq([
            0, 1, 2,
            0, 2, 3,
            0, 3, 4,
            0, 4, 5,
            0, 5, 6,
            0, 6, 7
        ]);
    });

    it('should correctly write index data for strips', () => {
        writer.writeIndicesForStrip([...Array(8).keys()]);
        const location = writer.endMesh();
        const start = location.indexByteOffset >> 1;
        const end = start + location.indexByteLength >> 1;

        expect(Array.from(
            writer.getBuffers()[location.bufferIndex].indexBuffer.slice(start, end)
        )).to.be.deep.eq([
            0, 1, 2,
            2, 1, 3,
            2, 3, 4,
            4, 3, 5,
            4, 5, 6,
            6, 5, 7
        ]);
    });

    it('should correctly write index data for continuous strips', () => {
        writer.writeIndicesForContinuousStrip(8);
        const location = writer.endMesh();
        const start = location.indexByteOffset >> 1;
        const end = start + location.indexByteLength >> 1;

        expect(Array.from(
            writer.getBuffers()[location.bufferIndex].indexBuffer.slice(start, end)
        )).to.be.deep.eq([
            0, 1, 2,
            2, 1, 3,
            2, 3, 4,
            4, 3, 5,
            4, 5, 6,
            6, 5, 7
        ]);
    });
});
