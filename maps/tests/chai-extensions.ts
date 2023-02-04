import * as chai from 'chai';

// Extend chai interface.
declare global {
    namespace Chai {
        interface Assertion {
            roughlyEqual(value: number, diff?: number): void;
            roughlyEqualPoint(point: number[], diff?: number): void;
            roughlyEqualBounds(bounds: number[][], diff?: number): void;
        }
    }
}

const Assertion = (chai as any).Assertion;

Assertion.addMethod('roughlyEqualBounds', function (this: any, bounds: any, diff: any) {
    this.assert(
        areBoundsRoughlyEqual(this._obj, bounds, diff),
        'expected #{act} to roughly equal bounds #{exp}',
        'expected #{act} to not roughly equal bounds #{exp}',
        bounds,
        this._obj,
        true
    );
});

Assertion.addMethod('roughlyEqualPoint', function (this: any, point: any, diff: any) {
    this.assert(
        arePointsRoughlyEqual(this._obj, point, diff),
        'expected #{act} to roughly equal point #{exp}',
        'expected #{act} to not roughly equal point #{exp}',
        point,
        this._obj,
        true
    );
});

Assertion.addMethod('roughlyEqual', function (this: any, value: any, diff: any) {
    this.assert(
        areRoughlyEqual(this._obj, value, diff),
        'expected #{act} to roughly equal #{exp}',
        'expected #{act} to not roughly equal #{exp}',
        value,
        this._obj,
        true
    );
});

function areBoundsRoughlyEqual(bounds1: number[][], bounds2: number[][], diff?: number): boolean {
    return arePointsRoughlyEqual(bounds1[0], bounds2[0], diff) &&
        arePointsRoughlyEqual(bounds1[1], bounds2[1], diff);
}

function arePointsRoughlyEqual(point1: number[], point2: number[], diff?: number): boolean {
    return areRoughlyEqual(point2[0], point1[0], diff) &&
        areRoughlyEqual(point2[1], point1[1], diff);
}

function areRoughlyEqual(value1: number, value2: number, diff = 1e-9): boolean {
    return Math.abs(value2 - value1) < diff;
}
