import {extent2} from '../../../../webgl_toolkit/util/extent';
import {getVisibleTiles} from '../../../panorama/util/tile_visibility';

describe('panorama utils', () => {
    const SIZE = extent2.create(14, 7);
    const tileProvider = (x: number, y: number) => ({x, y});

    it('getVisibleTiles', () => {
        // these tests were obtained by observing panorama debugger, i.e. by looking at the visualized panorama,
        // comparing the picture with an ordinary human expectations and then manually calculating tile set

        expect(getVisibleTiles(
            0.5235987755982988,
            -0.5235987755982988,
            -0.518729772704829,
            0.518729772704829,
            2.5157175838246264,
            0,
            SIZE,
            tileProvider
        )).toEqual([
            {x: 5, y: 3},
            {x: 6, y: 3},
            {x: 4, y: 3},
            {x: 5, y: 2},
            {x: 5, y: 4},
            {x: 6, y: 2},
            {x: 6, y: 4},
            {x: 4, y: 2},
            {x: 4, y: 4}
        ]);

        expect(getVisibleTiles(
            0.9075712110370513,
            -0.13962634015954634,
            -0.518729772704829,
            0.518729772704829,
            -0.2942625118862443,
            0,
            SIZE,
            tileProvider
        )).toEqual([
            {x: 13, y: 2},
            {x: 12, y: 2},
            {x: 13, y: 3},
            {x: 0, y: 2},
            {x: 13, y: 1},
            {x: 12, y: 3},
            {x: 11, y: 2},
            {x: 0, y: 3},
            {x: 12, y: 1},
            {x: 0, y: 1},
            {x: 11, y: 1},
            {x: 1, y: 1}
        ]);

        expect(getVisibleTiles(
            1.7453292519943293,
            0.6981317007977318,
            -0.518729772704829,
            0.518729772704829,
            2.094395102393195,
            0,
            SIZE,
            tileProvider
        )).toEqual([
            {x: 4, y: 0},
            {x: 4, y: 1},
            {x: 5, y: 0},
            {x: 3, y: 0},
            {x: 5, y: 1},
            {x: 3, y: 1},
            {x: 6, y: 0},
            {x: 2, y: 0},
            {x: 6, y: 1},
            {x: 2, y: 1},
            {x: 7, y: 0},
            {x: 1, y: 0},
            {x: 7, y: 1},
            {x: 1, y: 1},
            {x: 5, y: 2},
            {x: 8, y: 0},
            {x: 3, y: 2},
            {x: 0, y: 0},
            {x: 8, y: 1},
            {x: 0, y: 1},
            {x: 6, y: 2},
            {x: 9, y: 0},
            {x: 13, y: 0},
            {x: 9, y: 1},
            {x: 10, y: 0},
            {x: 12, y: 0},
            {x: 11, y: 0}
        ]);
    });
});
