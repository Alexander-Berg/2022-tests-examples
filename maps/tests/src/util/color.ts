import Color from '../../../src/vector_render_engine/util/color';
import areFuzzyEqual from '../../../src/vector_render_engine/util/fuzzy_equal';

export function areColorsFuzzyEqual(c1: Color, c2: Color, tolerance?: number): boolean {
    return areFuzzyEqual(c1.r, c2.r, tolerance) &&
        areFuzzyEqual(c1.g, c2.g, tolerance) &&
        areFuzzyEqual(c1.b, c2.b, tolerance) &&
        areFuzzyEqual(c1.a, c2.a, tolerance);
}
