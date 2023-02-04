import {expect} from 'chai';
import {GeoObjectBatchStorage} from '../../../../../src/vector_render_engine/content_provider/geo_content/object/geo_object_batch_storage';
import {createPairOfCommunicators, PairOfCommunicators} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/paired_backend_communicator';
import {GeoContentGCMessages, GeoContentGCMessageType} from '../../../../../src/vector_render_engine/content_provider/geo_content/garbage_collector/geo_content_gc_messages';
import {GeoContentGCBackend} from '../../../../../src/vector_render_engine/content_provider/geo_content/garbage_collector/geo_content_gc_backend';
import {GeoObjectBatch} from '../../../../../src/vector_render_engine/content_provider/geo_content/object/geo_object_batch';
import * as bbox2 from '../../../../../src/vector_render_engine/math/vector2';
import {sleep} from '../../../../util/sleep';

class TestGeoObjectBatch extends GeoObjectBatch {
    get bbox(): bbox2.BBox2 {
        return this._fixedBbox;
    }

    constructor(id: string, bbox: bbox2.BBox2) {
        super(id);
        this._fixedBbox = bbox;
    }

    private readonly _fixedBbox: bbox2.BBox2;
}

const TIMEOUT_MS = 5;

describe('GeoObjectGCBackend', () => {
    let storage: GeoObjectBatchStorage;
    let communicator: PairOfCommunicators<GeoContentGCMessages>;
    let gc;

    beforeEach(() => {
        communicator = createPairOfCommunicators<GeoContentGCMessages>();
        storage = new GeoObjectBatchStorage();
        gc = new GeoContentGCBackend(communicator.worker, storage, TIMEOUT_MS);
    });

    it('should remove batches as the camera moves away', async () => {
        storage.add('b1', new TestGeoObjectBatch('b1', bbox2.createBBox2(0, 1, 0, 1)));
        storage.add('b2', new TestGeoObjectBatch('b2', bbox2.createBBox2(1, 2, 1, 2)));
        storage.add('b3', new TestGeoObjectBatch('b3', bbox2.createBBox2(2, 3, 2, 3)));

        expect(storage.size).to.be.equal(3);

        communicator.master.sendMessage({
            type: GeoContentGCMessageType.TO_BACKEND_CAMERA_POSITION_UPDATE,
            viewportBBox: bbox2.createBBox2(0, 0.5, 0, 0.5)
        }, true);

        expect(storage.size).to.be.equal(3);

        await sleep(TIMEOUT_MS);

        communicator.master.sendMessage({
            type: GeoContentGCMessageType.TO_BACKEND_CAMERA_POSITION_UPDATE,
            viewportBBox: bbox2.createBBox2(2.5, 3, 2.5, 3)
        }, true);

        expect(storage.size).to.be.equal(2);

        communicator.master.sendMessage({
            type: GeoContentGCMessageType.TO_BACKEND_CAMERA_POSITION_UPDATE,
            viewportBBox: bbox2.createBBox2(102.5, 103, 102.5, 103)
        }, true);

        expect(storage.size).to.be.equal(2);

        await sleep(TIMEOUT_MS);

        communicator.master.sendMessage({
            type: GeoContentGCMessageType.TO_BACKEND_CAMERA_POSITION_UPDATE,
            viewportBBox: bbox2.createBBox2(102.5, 103, 102.5, 103)
        }, true);

        expect(storage.size).to.be.equal(0);
    });
});
