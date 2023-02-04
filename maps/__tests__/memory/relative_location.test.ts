import {MemoryRelativeLocation, batchAllocatedObjects} from '../../memory/relative_location';

interface TestMemoryLocatedObject extends MemoryRelativeLocation {
    featureX: number;
}

function batchPrimitiveDescriptions(primitives: Iterable<TestMemoryLocatedObject>): Iterable<TestMemoryLocatedObject> {
    return batchAllocatedObjects<TestMemoryLocatedObject, TestMemoryLocatedObject>(
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
                {featureX: 1, vertexByteOffset: 0, vertexByteLength: 10, indexByteOffset: 0, indexByteLength: 100},
                {featureX: 1, vertexByteOffset: 10, vertexByteLength: 11, indexByteOffset: 100, indexByteLength: 110}
            ])]).toBeDeepCloseTo([
                {featureX: 1, vertexByteOffset: 0, vertexByteLength: 21, indexByteOffset: 0, indexByteLength: 210}
            ]);
        });

        it('should not batch non adjacent regions', () => {
            expect([...batchPrimitiveDescriptions([
                {featureX: 1, vertexByteOffset: 0, vertexByteLength: 10, indexByteOffset: 0, indexByteLength: 100},
                {featureX: 1, vertexByteOffset: 11, vertexByteLength: 11, indexByteOffset: 100, indexByteLength: 110}
            ])]).toBeDeepCloseTo([
                {featureX: 1, vertexByteOffset: 0, vertexByteLength: 10, indexByteOffset: 0, indexByteLength: 100},
                {featureX: 1, vertexByteOffset: 11, vertexByteLength: 11, indexByteOffset: 100, indexByteLength: 110}
            ]);
        });

        it('should respect external requirements on batching (canBatch() method)', () => {
            expect([...batchPrimitiveDescriptions([
                {featureX: 1, vertexByteOffset: 0, vertexByteLength: 10, indexByteOffset: 0, indexByteLength: 100},
                {featureX: 2, vertexByteOffset: 10, vertexByteLength: 11, indexByteOffset: 100, indexByteLength: 110}
            ])]).toBeDeepCloseTo([
                {featureX: 1, vertexByteOffset: 0, vertexByteLength: 10, indexByteOffset: 0, indexByteLength: 100},
                {featureX: 2, vertexByteOffset: 10, vertexByteLength: 11, indexByteOffset: 100, indexByteLength: 110}
            ]);
        });
    });

});
