"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.kmlParser = void 0;
const parseLinearRing = (linearRing) => {
    const result = Object.assign({}, linearRing);
    result.coordinates = linearRing.coordinates.replace(/\n+\t+/gmi, '');
    return result;
};
const parseBoundaryItem = (boundary) => {
    const result = Object.assign({}, boundary);
    if (boundary.LinearRing) {
        result.LinearRing = parseLinearRing(boundary.LinearRing);
    }
    return result;
};
const transformBoundaryToArray = (boundaryRow) => {
    const key = Object.keys(boundaryRow)[0];
    return boundaryRow[key].map((data) => ({ [key]: data }));
};
const parseGeometry = (geometry, geometryKey) => {
    if (geometryKey === 'Polygon') {
        const result = Object.assign({}, geometry);
        if (result.innerBoundaryIs) {
            if (result.innerBoundaryIs.LinearRing && Array.isArray(result.innerBoundaryIs.LinearRing)) {
                result.innerBoundaryIs = transformBoundaryToArray(result.innerBoundaryIs)
                    .map((x) => parseBoundaryItem(x));
            }
            else if (result.innerBoundaryIs.LinearRing) {
                result.innerBoundaryIs = [parseBoundaryItem(result.innerBoundaryIs)];
            }
        }
        if (result.outerBoundaryIs) {
            result.outerBoundaryIs = parseBoundaryItem(result.outerBoundaryIs);
        }
        return result;
    }
    return geometry;
};
const makeArray = (data) => {
    if (!data) {
        return;
    }
    if (Array.isArray(data)) {
        return data;
    }
    return [data];
};
const parsePlacemark = (placemark) => {
    const result = Object.assign({}, placemark);
    if (placemark.Style) {
        result.Style = makeArray(placemark.Style);
    }
    if (placemark.MultiGeometry) {
        result.MultiGeometry = Object.keys(placemark.MultiGeometry).reduce((res, geometryKey) => {
            let geometriesRow = placemark.MultiGeometry[geometryKey];
            geometriesRow = Array.isArray(geometriesRow) ? geometriesRow : [geometriesRow];
            const geometries = geometriesRow.map((g) => ({
                [geometryKey]: parseGeometry(g, geometryKey)
            }));
            res.push(...geometries);
            return res;
        }, []);
    }
    if (placemark.Polygon) {
        result.Polygon = parseGeometry(placemark.Polygon, 'Polygon');
    }
    return result;
};
const parseCollection = (kml) => {
    const result = {
        name: kml.name,
        description: kml.description,
        ExtendedData: kml.ExtendedData
    };
    const folders = makeArray(kml.Folder);
    if (folders) {
        result.Folder = folders.map((folder) => parseCollection(folder));
    }
    const documents = makeArray(kml.Document);
    if (documents) {
        result.Document = documents.map((document) => parseCollection(document));
    }
    const placemarks = makeArray(kml.Placemark);
    if (placemarks) {
        result.Placemark = placemarks.map((placemark) => parsePlacemark(placemark));
    }
    const styles = makeArray(kml.Style);
    if (styles) {
        result.Style = styles;
    }
    const stylesMaps = makeArray(kml.StyleMap);
    if (stylesMaps) {
        result.StyleMap = stylesMaps;
    }
    return result;
};
exports.kmlParser = ({ kml: kmlRow }) => {
    delete kmlRow.xmlns;
    const { Document, Folder } = kmlRow;
    const documents = makeArray(Document);
    const folders = makeArray(Folder);
    let kmlData = {};
    if (documents) {
        kmlData.Document = documents.map((kml) => parseCollection(kml));
    }
    else if (folders) {
        kmlData.Folder = folders.map((kml) => parseCollection(kml));
    }
    else {
        kmlData = parseCollection(kmlRow);
    }
    return { kml: kmlData };
};
