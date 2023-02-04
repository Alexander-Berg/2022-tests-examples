import {expect} from 'chai';
import {IndexBufferLocation, batchAdjacentInIndexBufferObjects} from '../../../../src/vector_render_engine/render/memory/buffer_location';
import {Type} from '../../../../src/vector_render_engine/render/gl/enums';

interface TestMemoryLocatedObject extends IndexBufferLocation {
    featureX: number;
}

function batchPrimitiveDescriptions(primitives: Iterable<TestMemoryLocatedObject>): Iterable<TestMemoryLocatedObject> {
    return batchAdjacentInIndexBufferObjects(
        primitives,
        (primitive: TestMemoryLocatedObject) => primitive,
        (primitive: TestMemoryLocatedObject) => ({...primitive}),
        (a: TestMemoryLocatedObject, b: TestMemoryLocatedObject) => a.featureX === b.featureX
    );
}

describe('render/memory/relative_location', () => {
    describe('batchMemoryLocatedObjects', () => {
        it('should batch adjacent memory regions', () => {
            expect([...batchPrimitiveDescriptions([
                {featureX: 1, indexByteOffset: 0, indexByteLength: 100, indexType: Type.UNSIGNED_SHORT},
                {featureX: 1, indexByteOffset: 100, indexByteLength: 110, indexType: Type.UNSIGNED_SHORT}
            ])]).to.be.deep.eq([
                {featureX: 1, indexByteOffset: 0, indexByteLength: 210, indexType: Type.UNSIGNED_SHORT}
            ]);
        });

        it('should not batch non adjacent regions', () => {
            expect([...batchPrimitiveDescriptions([
                {featureX: 1, indexByteOffset: 0, indexByteLength: 100, indexType: Type.UNSIGNED_SHORT},
                {featureX: 1, indexByteOffset: 101, indexByteLength: 110, indexType: Type.UNSIGNED_SHORT}
            ])]).to.be.deep.eq([
                {featureX: 1, indexByteOffset: 0, indexByteLength: 100, indexType: Type.UNSIGNED_SHORT},
                {featureX: 1, indexByteOffset: 101, indexByteLength: 110, indexType: Type.UNSIGNED_SHORT}
            ]);
        });

        it('should respect external requirements on batching (canBatch() method)', () => {
            expect([...batchPrimitiveDescriptions([
                {featureX: 1, indexByteOffset: 0, indexByteLength: 100, indexType: Type.UNSIGNED_SHORT},
                {featureX: 2, indexByteOffset: 100, indexByteLength: 110, indexType: Type.UNSIGNED_SHORT}
            ])]).to.be.deep.eq([
                {featureX: 1, indexByteOffset: 0, indexByteLength: 100, indexType: Type.UNSIGNED_SHORT},
                {featureX: 2, indexByteOffset: 100, indexByteLength: 110, indexType: Type.UNSIGNED_SHORT}
            ]);
        });
    });

});
