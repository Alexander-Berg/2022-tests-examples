"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ymapsmlParser = void 0;
const generate_lod_1 = require("app/utils/generate-lod");
const parsePoint = (dataRow) => {
    return dataRow.pos.split(' ');
};
const getValue = (data, key) => data[key]._ || data[key];
const parseMetaDataProperty = (dataRow) => {
    return Object.keys(dataRow).reduce((res, key) => {
        if (key.startsWith('xmlns')) {
            return res;
        }
        const data = dataRow[key];
        res[key] = [Object.keys(data).reduce((propsRes, fieldKey) => {
                propsRes[fieldKey] = [{ value: data[fieldKey] }];
                return propsRes;
            }, {})];
        return res;
    }, {});
};
const makeCoordinates = (coordsRow) => {
    if (typeof coordsRow === 'string') {
        const coordsList = coordsRow.replace(/\s\s+/gim, ' ').trim().split(' ');
        const coords = [];
        for (let i = 0; i < coordsList.length; i += 2) {
            coords.push(coordsList.slice(i, i + 2).map(Number));
        }
        return coords;
    }
    return coordsRow.map((x) => x.split(' ').map(Number));
};
const parseGeoObject = (geoObjectRow) => {
    const result = {};
    if (geoObjectRow.name) {
        result.name = getValue(geoObjectRow, 'name');
    }
    if (geoObjectRow.description) {
        result.description = getValue(geoObjectRow, 'description');
    }
    if (geoObjectRow.style) {
        result.style = geoObjectRow.style;
    }
    if (geoObjectRow.id) {
        result.id = geoObjectRow.id;
    }
    if (geoObjectRow.boundedBy) {
        result.boundedBy = geoObjectRow.boundedBy;
    }
    if (geoObjectRow.metaDataProperty) {
        result.metaDataProperty = parseMetaDataProperty(geoObjectRow.metaDataProperty);
    }
    if (geoObjectRow.Point) {
        result.Point = parsePoint(geoObjectRow.Point);
    }
    if (geoObjectRow.LinearRing) {
        const { pos, posList } = geoObjectRow.LinearRing;
        result.polylod = generate_lod_1.generateLOD(makeCoordinates(pos || posList));
    }
    if (geoObjectRow.LineString) {
        const { pos, posList } = geoObjectRow.LineString;
        result.polylod = generate_lod_1.generateLOD(makeCoordinates(pos || posList));
    }
    if (geoObjectRow.Polygon) {
        const { exterior, interior } = geoObjectRow.Polygon;
        result.Polygon = {
            exterior: {
                polylod: generate_lod_1.generateLOD(makeCoordinates(exterior.LinearRing.posList || exterior.LinearRing.pos))
            },
            interior: interior ? Array.isArray(interior) ? interior.map((x) => ({
                polylod: generate_lod_1.generateLOD(makeCoordinates(x.LinearRing.posList))
            })) : [{
                    polylod: generate_lod_1.generateLOD(makeCoordinates(interior.LinearRing.posList))
                }] : undefined
        };
    }
    return result;
};
const parseGeoObjectCollection = (geoObjectCollectionRow) => {
    const result = {};
    const featureMembers = geoObjectCollectionRow.featureMembers || geoObjectCollectionRow.featureMember;
    if (!featureMembers) {
        return result;
    }
    const geoObjectRow = featureMembers.GeoObject;
    if (geoObjectRow) {
        result.featureMembers = Array.isArray(geoObjectRow) ? geoObjectRow.map((data) => ({
            GeoObject: parseGeoObject(data)
        })) : [{
                GeoObject: parseGeoObject(geoObjectRow)
            }];
    }
    const geoObjectSumCollectionRow = featureMembers.GeoObjectCollection;
    if (geoObjectSumCollectionRow) {
        result.featureMembers = Array.isArray(geoObjectSumCollectionRow) ? geoObjectSumCollectionRow.map((data) => ({
            GeoObjectCollection: parseGeoObjectCollection(data)
        })) : [{
                GeoObjectCollection: parseGeoObjectCollection(geoObjectSumCollectionRow)
            }];
    }
    if (Array.isArray(featureMembers)) {
        result.featureMembers = featureMembers.map((data) => {
            if (data.GeoObject) {
                return {
                    GeoObject: parseGeoObject(data.GeoObject)
                };
            }
            if (data.GeoObjectCollection) {
                return {
                    GeoObjectCollection: parseGeoObjectCollection(data.GeoObjectCollection)
                };
            }
            return data;
        });
    }
    const meta = geoObjectCollectionRow.metaDataProperty;
    if (meta) {
        result.metaDataProperty = parseMetaDataProperty(meta);
    }
    if (geoObjectCollectionRow.name) {
        result.name = getValue(geoObjectCollectionRow, 'name');
    }
    if (geoObjectCollectionRow.style) {
        result.style = getValue(geoObjectCollectionRow, 'style');
    }
    return result;
};
const parseRepresentation = (representation) => {
    const { Style, Template, View } = representation;
    const result = {};
    const parser = (data) => {
        if (Array.isArray(data)) {
            return data.reduce((res, curr) => {
                const id = curr.id;
                delete curr.id;
                res[id] = curr;
                return res;
            }, {});
        }
        const id = data.id;
        delete data.id;
        return { [id]: data };
    };
    if (Style) {
        result.Style = parser(Style);
    }
    if (Template) {
        result.Template = parser(Template);
    }
    if (View) {
        result.View = View;
    }
    return result;
};
exports.ymapsmlParser = (data) => {
    const { ymaps } = data;
    delete ymaps.ymaps;
    delete ymaps.repr;
    delete ymaps.gml;
    delete ymaps.xsi;
    const result = {
        ymaps: {
            schemaLocation: ymaps.schemaLocation
        }
    };
    if (ymaps.GeoObjectCollection) {
        result.ymaps.GeoObjectCollection = parseGeoObjectCollection(ymaps.GeoObjectCollection);
    }
    if (ymaps.Representation) {
        result.ymaps.Representation = parseRepresentation(ymaps.Representation);
    }
    return result;
};
