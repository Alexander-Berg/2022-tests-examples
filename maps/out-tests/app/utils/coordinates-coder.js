"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.encode = exports.decode = void 0;
const base64_1 = require("app/utils/base64");
const CODING_COEFFICIENT = 1000000;
const encode4bytes = (x) => {
    const chr = [];
    for (let i = 0; i < 4; i++) {
        chr[i] = x & 0x000000ff;
        x = x >> 8;
    }
    return chr;
};
const getShifts = (path) => {
    const res = [];
    let prev = [0, 0];
    let position = [0, 0];
    for (let i = 0; i < path.length; i++) {
        const encoded = [
            Math.round((path[i][0] - prev[0]) * CODING_COEFFICIENT),
            Math.round((path[i][1] - prev[1]) * CODING_COEFFICIENT)
        ];
        // Компенсируем ошибку
        let vector = [encoded[0] / CODING_COEFFICIENT, encoded[1] / CODING_COEFFICIENT];
        let point = [vector[0] + position[0], vector[1] + position[1]];
        const delta = [path[i][0] - point[0], path[i][1] - point[1]];
        const deltaEncoded = [
            Math.round(delta[0] * CODING_COEFFICIENT),
            Math.round(delta[1] * CODING_COEFFICIENT)
        ];
        if (deltaEncoded[0] || deltaEncoded[i]) {
            encoded[0] += deltaEncoded[0];
            encoded[1] += deltaEncoded[1];
            vector = [encoded[0] / CODING_COEFFICIENT, encoded[1] / CODING_COEFFICIENT];
            point = [vector[0] + position[0], vector[1] + position[1]];
        }
        res.push(encoded);
        prev = path[i];
        position = point;
    }
    return res;
};
const encodePath4bytes = (path) => {
    let result = [];
    const formated = getShifts(path);
    for (let i = 0; i < formated.length; i++) {
        result = result.concat(encode4bytes(formated[i][0]), encode4bytes(formated[i][1]));
    }
    return result;
};
/**
 * Special functions for encoding coordinates to base64 string and revert it
 */
exports.decode = (encodedCoordinates) => {
    const byteVector = base64_1.decode(encodedCoordinates);
    const byteVectorLength = byteVector.length;
    let index = 0;
    let prev = [0, 0];
    const result = [];
    while (index < byteVectorLength) {
        let pointx = 0;
        let pointy = 0;
        let iterIndex = 0;
        const sub = byteVector.substr(index, 8);
        while (iterIndex < 4) {
            pointx |= (sub.charCodeAt(iterIndex) << iterIndex * 8);
            pointy |= (sub.charCodeAt(iterIndex + 4) << iterIndex * 8);
            iterIndex++;
        }
        const vector = [
            pointx / CODING_COEFFICIENT,
            pointy / CODING_COEFFICIENT
        ];
        const point = [
            vector[0] + prev[0],
            vector[1] + prev[1]
        ];
        prev = point;
        result.push(point);
        index += 8;
    }
    return result;
};
exports.encode = (coordinates) => {
    return base64_1.encode(encodePath4bytes(coordinates).map(String));
};
