import {rect2d} from '../../util/rectangle';

describe('rectangle 2D  utils', () => {
    it('geo to normalized cartesian coordinates conversion', () => {
        const rect1 = {
            p1: {x: -2, y: -1},
            p2: {x: -2, y: +1},
            p3: {x: +2, y: +1},
            p4: {x: +2, y: -1}
        };

        expect(rect2d.isPointInside(rect1, {x: 0, y: 0})).toBeTruthy();
        expect(rect2d.isPointInside(rect1, {x: -2, y: -1})).toBeTruthy();
        expect(rect2d.isPointInside(rect1, {x: -2, y: 0})).toBeTruthy();
        expect(rect2d.isPointInside(rect1, {x: 2, y: 2})).toBeFalsy();

        const rect2 = {
            p1: {x: 2, y: 0},
            p2: {x: 0, y: 2},
            p3: {x: 3, y: 5},
            p4: {x: 5, y: 3}
        };

        expect(rect2d.isPointInside(rect2, {x: 0, y: 0})).toBeFalsy();
        expect(rect2d.isPointInside(rect2, {x: 1, y: 1})).toBeTruthy();
        expect(rect2d.isPointInside(rect2, {x: 3, y: 5})).toBeTruthy();
        expect(rect2d.isPointInside(rect2, {x: -10, y: 0})).toBeFalsy();
        expect(rect2d.isPointInside(rect2, {x: 5, y: 5})).toBeFalsy();
    });

});
