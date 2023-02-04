import {expect} from 'chai';
import {cycleRestrictWorldPrimitiveCoordinates} from '../../../../../../src/vector_render_engine/content_provider/graphics/content_manager/util/world_coordinates';

describe('cycleRestrictWorldPrimitiveCoordinates', () => {
    it('should cycle restrict coordinates if any vertex exceeds world to the left', () => {
        const coordinates = cycleRestrictWorldPrimitiveCoordinates(
            [{x: -1, y: 0}, {x: -2, y: 0}, {x: -3, y: 0}]
        );

        expect(coordinates).to.deep.equal([{x: 1, y: 0}, {x: 0, y: 0}, {x: -1, y: 0}]);
    });

    it('should cycle restrict coordinates if any vertex exceeds world to the right', () => {
        const coordinates = cycleRestrictWorldPrimitiveCoordinates(
            [{x: 1, y: 0}, {x: 2, y: 0}, {x: 3, y: 0}]
        );

        expect(coordinates).to.deep.equal([{x: -1, y: 0}, {x: 0, y: 0}, {x: 1, y: 0}]);
    });

    it('should not cycle restrict coordinates that are in range of the world', () => {
        const coordinates = cycleRestrictWorldPrimitiveCoordinates([
            {x: 0, y: 0}, {x: 2, y: 2}, {x: -2, y: -2}, {x: 1, y: -1}
        ]);

        expect(coordinates).to.deep.equal([{x: 0, y: 0}, {x: 2, y: 2}, {x: -2, y: -2}, {x: 1, y: -1}]);
    });

    it('should throw an error if coordinates are out of world\'s range and is bigger than world', () => {
        expect(() => cycleRestrictWorldPrimitiveCoordinates([{x: -3, y: -3}, {x: 0, y: 0}])).to.throw;
    })
});
