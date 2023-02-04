import {GpuMemoryBufferWriter, VertexWriter} from '../../memory/gpu_memory_buffer_writer';
import {GpuMemoryPageData} from '../../memory/gpu_memory_page_data';
import {GpuMemoryPageLocation} from '../../memory/gpu_memory_page_location';
import {Vector2} from '../../util/vector';

class TestGpuMemoryBufferWriter extends GpuMemoryBufferWriter<[Vector2, number]> {

    writePrimitive(primitives: {position: Vector2, param: number}[], indices: number = 0): GpuMemoryPageLocation {
        for (const primitive of primitives) {
            this.writeVertex(primitive.position, primitive.param);
        }

        this.writeIndices(Array.from(new Array(indices), (value_, index) => index));

        return this.endMesh();
    }

    protected _writeVertex(writer: VertexWriter, position: Vector2, param: number): void {
        writer.writeVertexFloat32(position.x);
        writer.writeVertexFloat32(position.y);
        writer.writeVertexUint32(param);
    }
}

describe('GpuMemoryBufferWriter', () => {
    it('should request new page data if there is no room for the next mesh', () => {
        const memory_pages = [];
        const writer = new TestGpuMemoryBufferWriter(() => {
            const data = new GpuMemoryPageData(memory_pages.length, 4, 12, 10);
            memory_pages.push(data);
            return data;
        });

        writer.writePrimitive([
            {position: {x: 1, y: 1}, param: 100},
            {position: {x: 2, y: 2}, param: 100},
            {position: {x: 3, y: 3}, param: 100}
        ], 5);
        writer.endMesh();
        expect(memory_pages.length).toEqual(1);

        writer.writePrimitive([
            {position: {x: 1, y: 1}, param: 200},
            {position: {x: 2, y: 2}, param: 200},
            {position: {x: 3, y: 3}, param: 200}
        ], 5);
        writer.endMesh();
        expect(memory_pages.length).toEqual(2);

        writer.endMesh();
        writer.writePrimitive([], 6);
        expect(memory_pages.length).toEqual(3);
    });

});
