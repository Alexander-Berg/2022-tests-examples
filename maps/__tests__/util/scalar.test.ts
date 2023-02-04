import {linspace} from '../../util/scalar';

describe('scalar', () => {
    // TODO: test other scalar utils

    it('linspace', () => {
        expect([...linspace(0, 1, 2)]).toEqual([0, 1]);
        expect([...linspace(0, 1, 3)]).toEqual([0, 0.5, 1]);
        expect([...linspace(0, 1, 3, false)]).toBeDeepCloseTo([0, 1 / 3, 2 / 3]);
        expect([...linspace(40, 50, 5)]).toBeDeepCloseTo([40, 42.5, 45, 47.5, 50]);
        expect([...linspace(40, 50, 5, false)]).toBeDeepCloseTo([40, 42, 44, 46, 48]);

        expect([...linspace(1, 1, 5)]).toBeDeepCloseTo([1, 1, 1, 1, 1]);
        expect([...linspace(1, 2, 1)]).toBeDeepCloseTo([2]);
        expect([...linspace(1, 2, 1, false)]).toBeDeepCloseTo([1]);

        expect([...linspace(1, 1, 0)]).toBeDeepCloseTo([]);
        expect([...linspace(1, 1, 0, false)]).toBeDeepCloseTo([]);

        expect([...linspace(-1, -1, 3)]).toBeDeepCloseTo([-1, -1, -1]);
        expect([...linspace(-1, -2, 3)]).toBeDeepCloseTo([-1, -1.5, -2]);
        expect([...linspace(-1, -2, 4, false)]).toBeDeepCloseTo([-1, -1.25, -1.5, -1.75]);
    });
});
