import {expect} from 'chai';
import zan from './models/zan.bin';
import spas from './models/spas.bin';
import spasBig from './models/spas_big.bin';
import bazil from './models/bazil.bin';
import zanReferenceOutput from './reference_output/zan.txt';
import spasReferenceOutput from './reference_output/spas.txt';
import spasBigReferenceOutput from './reference_output/spas_big.txt';
import bazilReferenceOutput from './reference_output/bazil.txt';
import {parseTrimesh} from '../../../../../../src/vector_render_engine/util/trimesh/parse_trimesh';
import Vector3 from '../../../../../../src/vector_render_engine/math/vector3';
import {Output} from '../../../../../../src/vector_render_engine/util/trimesh/output';

export default interface Block {
    vertices: Vector3[];
    triangles: number[];
}

// from mapscore implementation
const MAX_BLOCK_VERTICES = 0xFFFF;
const MAX_BLOCK_TRIANGLES = MAX_BLOCK_VERTICES;

/**
 * This class simulate the mapscore trimesh lib output behavior to make it possible to get identical output and compare.
 */
class TestBufferWriter implements Output<void> {
    readonly vertices: Vector3[];
    readonly triangles: number[];

    constructor() {
        this.vertices = [];
        this.triangles = [];
    }

    writeVertex(vertex: Vector3): number {
        this.vertices.push(vertex);
        return this.vertices.length - 1;
    }

    writeIndices(indices: number[]): void {
        this.triangles.push(...indices);
    }

    flush(): void {}

    flushBlock(nVertices: number, nTriangles: number): Block {
        const block = {
            vertices: this.vertices.splice(0, nVertices),
            triangles: this.triangles.splice(0, nTriangles)
        };

        this.triangles.forEach((vertexIndex, index) => {
            this.triangles[index] = vertexIndex - nVertices;
        });

        return block;
    }
}

/**
 * Gets output of the parser as set of lines of vertices/triangles.
 */
function getOutput(data: Uint8Array): string[] {
    const blocks = [];
    const writer = new TestBufferWriter();

    let cutVertices = 0;
    let cutTriangles = 0;

    for (const _ of parseTrimesh((data).buffer, writer)) {
        if (cutTriangles !== 0 && MAX_BLOCK_TRIANGLES < writer.triangles.length) {
            blocks.push(writer.flushBlock(cutVertices, cutTriangles));
        }

        cutVertices = writer.vertices.length;
        cutTriangles = writer.triangles.length;
    }

    if (writer.triangles.length > 0) {
        blocks.push(writer.flushBlock(writer.vertices.length, writer.triangles.length));
    }

    const result = [];

    for (const block of blocks) {
        for (const vertex of block.vertices) {
            result.push([vertex.x, vertex.y, vertex.z].map((component) => component.toFixed(4)).join(' '));
        }

        let i = 0;

        while (i < block.triangles.length) {
            result.push([
                block.triangles[i++],
                block.triangles[i++],
                block.triangles[i++]
            ].join(' '));
        }
    }

    // there is an empty line at the end of reference files
    result.push('');

    return result;
}

function compareOutputs(output: string[], reference: string[]): void {
    expect(output.length).to.be.equal(reference.length);

    for (let i = 0; i < output.length; i++) {
        expect(output[i], `line ${i}`).to.be.equal(reference[i]);
    }
}

/**
 * The trimesh parser implementation was made using the mapscore trimesh lib as a reference.
 * The lib is already tested and is in production quite a while, hat is why it was considered safe to rely on its
 * output to check the correctness of this implementation.
 */
describe('trimesh parser', () => {
    it('should be identical to the mapscore implementation output for test "zan"', function (): void {
        compareOutputs(getOutput(zan), zanReferenceOutput.split('\n'));
    });

    // TODO: run these tests if trimesh-parser related logic is changed
    describe.skip('slow trimesh-parser tests', () => {
        it('should be identical to the mapscore implementation output for test "spas"', function (): void {
            compareOutputs(getOutput(spas), spasReferenceOutput.split('\n'));
        });
        it('should be identical to the mapscore implementation output for test "spasBig"', function (): void {
            this.timeout(25000); // the test might take more then 2.5s due to huge input data
            compareOutputs(getOutput(spasBig), spasBigReferenceOutput.split('\n'));
        });
        it('should be identical to the mapscore implementation output for test "bazil"', function (): void {
            compareOutputs(getOutput(bazil), bazilReferenceOutput.split('\n'));
        });
    });
});
