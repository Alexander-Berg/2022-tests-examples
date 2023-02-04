import {BufferData, GpuMemoryPageData, IndexBufferData, VertexBufferData} from '../../memory/gpu_memory_page_data';

describe('GpuMemoryPageData', () => {
    it('should write indices relative to current mesh properly', () => {
        const data = new GpuMemoryPageData(1, 100, 8, 200);

        data.writeVertexUint32(1);
        data.writeVertexUint32(1);
        data.writeVertexUint32(2);
        data.writeVertexUint32(2);
        data.writeVertexUint32(3);
        data.writeVertexUint32(3);
        data.writeIndices([0, 1, 2]);
        data.endMesh();

        expect([...data.indexBuffer.asUint16Array()]).toBeDeepCloseTo([0, 1, 2]);

        data.writeVertexUint32(11);
        data.writeVertexUint32(11);
        data.writeVertexUint32(22);
        data.writeVertexUint32(22);
        data.writeVertexUint32(33);
        data.writeVertexUint32(33);
        data.writeIndices([0, 1, 2]);
        data.endMesh();

        expect([...data.indexBuffer.asUint16Array()]).toBeDeepCloseTo([0, 1, 2, 3, 4, 5]);
    });

    describe('BufferData', () => {
        class TestBufferData extends BufferData {
            constructor(size: number) {
                super(size, Int16Array.BYTES_PER_ELEMENT);
                this._int16 = new Int16Array(this._uint8View.buffer);
            }

            writeWord(value: number): void {
                this._int16[this._nextWordOffset++] = value;
            }

            private readonly _int16: Int16Array;
        }

        it('should write multiple meshes', () => {
            const bufferData = new TestBufferData(100);

            bufferData.writeWord(1);
            bufferData.writeWord(2);
            bufferData.writeWord(3);
            expect(bufferData.endMesh()).toBeDeepCloseTo({byteOffset: 0, byteSize: 6});

            bufferData.writeWord(4);
            bufferData.writeWord(5);
            expect(bufferData.endMesh()).toBeDeepCloseTo({byteOffset: 6, byteSize: 4});

            expect([...new Int16Array(bufferData.data, 0, 5)]).toBeDeepCloseTo([1, 2, 3, 4, 5]);
        });

        it('should reset its cursor', () => {
            const bufferData = new TestBufferData(100);

            bufferData.writeWord(1);
            bufferData.writeWord(2);
            bufferData.writeWord(3);
            bufferData.endMesh();

            bufferData.reset();

            bufferData.writeWord(4);
            bufferData.writeWord(5);
            bufferData.writeWord(6);
            bufferData.endMesh();

            expect([...new Int16Array(bufferData.data, 0, 3)]).toBeDeepCloseTo([4, 5, 6]);
        });

        it('should transfer data of current mesh', () => {
            const sourceBufferData = new TestBufferData(100);
            const targetBufferData = new TestBufferData(100);

            sourceBufferData.writeWord(1);
            sourceBufferData.writeWord(2);
            sourceBufferData.writeWord(3);
            sourceBufferData.endMesh();

            sourceBufferData.writeWord(4);
            sourceBufferData.writeWord(5);

            sourceBufferData.transferUnfinishedMesh(targetBufferData);

            sourceBufferData.writeWord(66);
            sourceBufferData.writeWord(77);
            expect(sourceBufferData.endMesh()).toBeDeepCloseTo({byteOffset: 6, byteSize: 4});
            expect([...new Int16Array(sourceBufferData.data, 0, 5)]).toBeDeepCloseTo([1, 2, 3, 66, 77]);

            targetBufferData.writeWord(6);
            targetBufferData.writeWord(7);
            targetBufferData.writeWord(8);
            expect(targetBufferData.endMesh()).toBeDeepCloseTo({byteOffset: 0, byteSize: 10});
            expect([...new Int16Array(targetBufferData.data, 0, 5)]).toBeDeepCloseTo([4, 5, 6, 7, 8]);
        });
    });

    describe('VertexBufferData', () => {
        it ('should update currentMeshBaseIndex', () => {
            const bufferData = new VertexBufferData(100, 8);
            expect(bufferData.currentMeshBaseIndex).toEqual(0);

            bufferData.pushUint32(1);
            bufferData.pushUint32(1);
            bufferData.pushUint32(2);
            bufferData.pushUint32(2);
            bufferData.endMesh();
            expect(bufferData.currentMeshBaseIndex).toEqual(2);

            bufferData.reset();
            expect(bufferData.currentMeshBaseIndex).toEqual(0);
        });
    });

    describe('IndexBufferData', () => {
        it ('should update indices during transferring data to another buffer data', () => {
            const sourceBufferData = new IndexBufferData(100);
            const targetBufferData = new IndexBufferData(100);

            sourceBufferData.push(0);
            sourceBufferData.push(1);
            sourceBufferData.push(2);

            sourceBufferData.endMesh();
            sourceBufferData.push(3);
            sourceBufferData.push(4);
            sourceBufferData.push(5);

            sourceBufferData.transferUnfinishedMesh(targetBufferData, 3);
            expect([...new Uint16Array(targetBufferData.data, 0, 3)]).toBeDeepCloseTo([0, 1, 2]);
        });
    });

});
