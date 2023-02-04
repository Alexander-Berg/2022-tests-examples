"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.generateLOD = void 0;
const coordinates_coder_1 = require("app/utils/coordinates-coder");
/**
 * @see https://wiki.yandex-team.ru/maps/dev/core/libs/xsltext/generatelod/
 */
exports.generateLOD = (coords) => {
    return {
        polyline: coordinates_coder_1.encode(coords),
        levels: '',
        maxlevel: ''
    };
};
