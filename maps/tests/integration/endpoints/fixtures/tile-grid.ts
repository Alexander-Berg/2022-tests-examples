import * as morton from 'morton';
import * as Long from 'long';
import {MAX_ZOOM} from 'src/v2/geo-constants';

/**
 * Zoom level of the test tile grid.
 */
export const TILE_GRID_ZOOM = 3;

/**
 * Computes minimum quad key on `MAX_ZOOM` level for tile coordinates on `TILE_GRID_ZOOM` level.
 */
export function minQuadKey(tileX: number, tileY: number): number {
    return Long
        .fromNumber(morton(tileX, tileY))
        .shiftLeft((MAX_ZOOM - TILE_GRID_ZOOM) * 2)
        .toNumber();
}

/**
 * Computes maximum quad key on `MAX_ZOOM` level for tile coordinates on `TILE_GRID_ZOOM` level.
 */
export function maxQuadKey(tileX: number, tileY: number): number {
    const zDiffBits = (MAX_ZOOM - TILE_GRID_ZOOM) * 2;
    const min = Long.fromNumber(morton(tileX, tileY)).shiftLeft(zDiffBits);
    const maxChild = Long.MAX_UNSIGNED_VALUE.shiftLeft(zDiffBits).not();
    return min.or(maxChild).toNumber();
}
