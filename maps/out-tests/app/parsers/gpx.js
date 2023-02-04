"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.gpxParser = void 0;
const arrayTypes = ['trkseg', 'trkpt', 'rtept', 'rte', 'wpt', 'trk', 'ptseg'];
const parse = (data) => {
    if (Array.isArray(data)) {
        return data.map((x) => parse(x));
    }
    if (typeof data === 'object') {
        return Object.keys(data).reduce((res, key) => {
            if (!arrayTypes.includes(key)) {
                res[key] = parse(data[key]);
                return res;
            }
            if (!Array.isArray(data[key])) {
                res[key] = [parse(data[key])];
            }
            else {
                res[key] = parse(data[key]);
            }
            return res;
        }, {});
    }
    return data;
};
const makeArray = (data) => {
    if (Array.isArray(data)) {
        return data;
    }
    return [data];
};
exports.gpxParser = ({ gpx: { wpt, rte, trk, ptseg, metadata } }) => {
    return {
        gpx: {
            wpt: wpt ? parse(makeArray(wpt)) : [],
            rte: rte ? parse(makeArray(rte)) : [],
            trk: trk ? parse(makeArray(trk)) : [],
            ptseg: ptseg ? parse(makeArray(ptseg)) : [],
            metadata
        }
    };
};
