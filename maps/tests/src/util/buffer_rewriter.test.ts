import {expect} from 'chai';
import BufferWriter, {MemoryWriterLocation} from '../../../src/vector_render_engine/util/buffer_writer';
import {allocateAttributes, Attribute} from '../../../src/vector_render_engine/render/attrib_mapping';
import {Type} from '../../../src/vector_render_engine/render/gl/enums';
import Vector2, {create} from '../../../src/vector_render_engine/math/vector2';
import BufferRewriter from '../../../src/vector_render_engine/util/buffer_rewriter';

const ATTRIBUTE_MAPPING = allocateAttributes([
    [
        Attribute.POSITION,
        {
            size: 2,
            type: Type.UNSIGNED_SHORT,
            normalized: true
        }
    ],
    [
        Attribute.PRIORITY,
        {
            size: 1,
            type: Type.FLOAT,
            normalized: false
        }
    ],
    [
        Attribute.DISPLACEMENT,
        {
            size: 2,
            type: Type.SHORT,
            normalized: false
        }
    ]
]);

class TestBufferWriter extends BufferWriter {
    constructor() {
        super(ATTRIBUTE_MAPPING.vertexByteSize);
    }

    writeData(data: {position: Vector2, priority: number, displacement: Vector2}[]): MemoryWriterLocation {
        for (const vertex of data) {
            this._writeHalfWords(vertex.position.x, vertex.position.y);
            this._writeFloat32(vertex.priority);
            this._writeHalfWords(vertex.displacement.x, vertex.displacement.y);
        }

        return this.endMesh();
    }
}

class TestBufferRewriter extends BufferRewriter {
    constructor(buffers: {vertexBuffer: Uint32Array, indexBuffer: Uint16Array}[]) {
        super(ATTRIBUTE_MAPPING, buffers);
    }

    rewritePositions(location: MemoryWriterLocation, position: Vector2): void {
        const offset = this._getAttribOffset(Attribute.POSITION);
        this._forEachVertex(location, (writer) => {
            writer.writeHalfWords(offset, position.x, position.y);
        });
    }

    rewritePriorities(location: MemoryWriterLocation, priority: number): void {
        const offset = this._getAttribOffset(Attribute.PRIORITY);
        this._forEachVertex(location, (writer) => {
            writer.writeFloat32(offset, priority);
        });
    }

}

describe('util/BufferRewriter', () => {
    it('should correctly write data to the vertex buffer', () => {
        const writer = new TestBufferWriter();
        const location = writer.writeData([
            {position: create(0x00F1, 0x0088), priority: 19, displacement: create(0x0022, 0x0011)}
        ]);

        const rewriter = new TestBufferRewriter(writer.getBuffers());
        const vertexBuffer = writer.getBuffers()[location.bufferIndex].vertexBuffer.buffer;
        const uints = new Uint32Array(vertexBuffer);
        const floats = new Float32Array(vertexBuffer);

        rewriter.rewritePositions(location, create(0x00F2, 0x0088));

        expect(uints[0]).to.be.equal(0x008800F2);
        expect(floats[1]).to.be.equal(19);
        expect(uints[2]).to.be.equal(0x00110022);

        rewriter.rewritePriorities(location, 29);

        expect(uints[0]).to.be.equal(0x008800F2);
        expect(floats[1]).to.be.equal(29);
        expect(uints[2]).to.be.equal(0x00110022);
    });

});
