import {expect} from 'chai';
import {getMeshId} from '../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/util/external_mesh_description';
import * as color from '../../../../../../src/vector_render_engine/util/color';
import * as vec3 from '../../../../../../src/vector_render_engine/math/vector3';

describe('getMeshId', () => {
    const testBBox: vec3.BBox3 = {minX: 0, maxX: 1, minY: 0, maxY: 1, minZ: 0, maxZ: 1};

    it('should build "id" from objectId, meshId and color', () => {
        expect(getMeshId({
            id: 0,
            meshId: 'mesh-1',
            objectId: 'object-1',
            bbox: testBBox,
            color: color.create(0, 0, 0, 1)
        })).to.equal('object-1mesh-1-16777216');
        expect(getMeshId({
            id: 0,
            meshId: 'mesh-2',
            objectId: 'object-2',
            bbox: testBBox,
            color: color.create(1, 1, 1, 1)
        })).to.equal('object-2mesh-2-1');
    });
});
