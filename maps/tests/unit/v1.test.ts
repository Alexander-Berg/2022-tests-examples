import * as assert from 'assert';
import * as path from 'path';
import {readFileSync} from 'fs';
import {DomParserNode} from '../../server/editor/dom-parser/node';
import {V1SchemeModel} from '../../server/editor/scheme-model/index';
import {SchemeThread} from '../../server/editor/scheme-model';
import {Format} from '../../server/editor/format';

const SCHEME_XML = readFileSync(
    path.resolve('resources/test-fixtures/extended-legacy-scheme.xml'),
    'utf8'
);

const schemeNames = require('../../../resources/test-fixtures/v1-test-data/scheme-names');
const attributes = require('../../../resources/test-fixtures/v1-test-data/scheme-attributes');
const stationsData = require('../../../resources/test-fixtures/v1-test-data/scheme-stations');
const servicesData = require('../../../resources/test-fixtures/v1-test-data/scheme-services');
const schemeThreads = require('../../../resources/test-fixtures/v1-test-data/scheme-threads');
const schemeLinks = require('../../../resources/test-fixtures/v1-test-data/scheme-links');

const domParserNode = new DomParserNode();
const v1 = new V1SchemeModel(SCHEME_XML, domParserNode);

describe('V1SchemeModel class', () => {
    it('get scheme id', () => {
        assert.equal(v1.getId(), 'sc31709615');
    });

    it('get scheme names', () => {
        const actualNames = sortArraysInObject(v1.getName());

        assert.deepEqual(actualNames, sortArraysInObject(schemeNames));
    });

    it('get scheme attributes', () => {
        const actualAttributes = sortArraysInObject(v1.getAttributes());

        assert.deepEqual(actualAttributes, sortArraysInObject(attributes));
    });

    it('sets the scheme attributes', () => {
        const mutableModel = new V1SchemeModel(SCHEME_XML, domParserNode);
        const attributes = mutableModel.getAttributes();

        const timezone = 'Europe/Sofia';
        attributes.timezone = timezone;

        const initialPosition = {x: '123', y: '123'};
        attributes.initialPosition.x = parseFloat(initialPosition.x);
        attributes.initialPosition.y = parseFloat(initialPosition.x);

        const tags = [...attributes.tags, 'private'];
        attributes.tags = tags;

        mutableModel.setAttributes(attributes);

        const newAttributes = mutableModel.getAttributes();

        assert.equal(newAttributes.timezone, timezone);
        assert.deepEqual(newAttributes.initialPosition, initialPosition);
        assert.deepStrictEqual(newAttributes.tags, tags);

        // TODO: test the other fields
    });

    it('get scheme stations', () => {
        const actualStations = sortObjectsById(v1.getStations());

        assert.deepEqual(actualStations, sortObjectsById(stationsData));
    });

    it('set scheme station', () => {
        const sid = 'st87229162';
        const serviceIds = ['sr42693568'];
        const name = {
            be: 'asdvgfasdf',
            ru: 'asdf',
            en: 'hjkgfhkf'
        };
        const geoPoint = {
            lat: 23.3,
            lon: 23.4
        };
        const schemePosition = {
            x: 344,
            y: 408
        };
        const workingTime = {
            from: '12:23',
            to: '21:23'
        };
        const detailBlocks = [{}];

        const mutableModel = new V1SchemeModel(SCHEME_XML, domParserNode);
        const station = mutableModel.getStations().find((station) => station.id === sid)!;

        const legacyId = station.attributes.legacyId;

        station.id = sid;
        station.serviceIds = serviceIds;
        station.attributes = {name, geoPoint, schemePosition, workingTime, detailBlocks, legacyId};

        mutableModel.setStation(station);
        const updatedStation = mutableModel.getStations().find((station) => station.id === sid)!;

        const updateServiceIds = updatedStation.serviceIds;
        const updateName = updatedStation.attributes.name;
        const updateGeoCoordinates = updatedStation.attributes.geoPoint;
        const updateSchemePosition = updatedStation.attributes.schemePosition;
        const updateWorkingTime = updatedStation.attributes.workingTime;
        const updateDetailBlocks = updatedStation.attributes.detailBlocks;

        assert.deepEqual(updateServiceIds, serviceIds);
        assert.deepEqual(sortArraysInObject(updateName), sortArraysInObject(name));
        assert.deepEqual(updateGeoCoordinates, geoPoint);
        assert.deepEqual(updateSchemePosition, schemePosition);
        assert.deepEqual(updateWorkingTime, workingTime);
        assert.deepEqual(updateDetailBlocks, detailBlocks);
    });

    it('create scheme station', () => {
        const schemeModel = new V1SchemeModel(SCHEME_XML, domParserNode);

        const stationId = schemeModel.createStation();

        const createdStation = schemeModel.getStations().find((station) => station.id === stationId)!;
        const createdStationId = createdStation.id;

        assert.strictEqual(createdStationId, stationId);
    });

    it('should create and delete empty station', () => {
        const mutableModel = new V1SchemeModel(SCHEME_XML, domParserNode);

        const stations = mutableModel.getStations();

        const createdStationId = mutableModel.createStation();
        const createdStation = mutableModel.getStations().find((station) => station.id === createdStationId)!;

        mutableModel.deleteStation(createdStation.id);

        const updatedStation = mutableModel.getStations();

        assert.deepStrictEqual(updatedStation, stations);
    });

    it('shouldn\'t delete station with links', () => {
        const mutableModel = new V1SchemeModel(SCHEME_XML, domParserNode);
        assert.throws(
            () => mutableModel.deleteStation('st69497333'),
            Error,
            'This station has links'
        );
    });

    it('get scheme services', () => {
        const actualServices = sortObjectsById(v1.getServices());

        assert.deepEqual(actualServices, sortObjectsById(servicesData));
    });

    it('should get service by id', () => {
        const services = v1.getServices();
        const currentServices = services.map((service) => v1.getServiceById(service.id));

        assert.deepStrictEqual(sortArraysInObject(currentServices), sortArraysInObject(servicesData));
    });

    it('shouldn\'t get service by incorrect id', () => {
        const incorrectId = '234234242342l';
        assert.throws(
            () => v1.getServiceById(incorrectId),
            Error,
            'Service not found'
        );
    });

    it('set scheme service', () => {
        const color = 'DA2230';
        const name = {
            en: 'sfsdf',
            ru: 'dsss'
        };
        const shortName = '1';
        const interval = 23;
        const sid = 'sr42693568';

        const mutableModel = new V1SchemeModel(SCHEME_XML, domParserNode);
        const service = mutableModel.getServices().find((service) => service.id === sid)!;

        service.attributes.color = color;

        service.attributes.name = {...service.attributes.name, ...name};
        const names = service.attributes.name;

        service.attributes.shortName = shortName;
        service.attributes.interval = interval;

        mutableModel.setService(service);
        const updatedService = mutableModel.getServices().find((service) => service.id === sid)!;

        const updatedColor = updatedService.attributes.color;
        const updatedName = updatedService.attributes.name;
        const updatedShortName = updatedService.attributes.shortName;
        const updatedInterval = updatedService.attributes.interval;

        assert.equal(updatedInterval, interval);
        assert.equal(updatedShortName, shortName);
        assert.equal(updatedColor, color);
        assert.deepEqual(updatedName, names);
    });

    it('should create scheme service', () => {
        const schemeModel = new V1SchemeModel(SCHEME_XML, domParserNode);

        const serviceId = schemeModel.createService();

        const createdService = schemeModel.getServices().find((service) => service.id === serviceId)!;
        const createdServiceId = createdService.id;

        assert.strictEqual(createdServiceId, serviceId);
    });

    it('should create and delete empty service', () => {
        const mutableModel = new V1SchemeModel(SCHEME_XML, domParserNode);

        const services = mutableModel.getServices();

        const createdServiceId = mutableModel.createService();
        const createdService = mutableModel.getServices().find((service) => service.id === createdServiceId)!;

        mutableModel.deleteService(createdService.id);

        const updatedServices = mutableModel.getServices();

        assert.deepStrictEqual(services, updatedServices);
    });

    it('shouldn\'t delete service with stationsRef', () => {
        const mutableModel = new V1SchemeModel(SCHEME_XML, domParserNode);

        assert.throws(
            () => {
                mutableModel.deleteService('sr42693568');
            },
            Error,
            'This service has stations'
        );
    });

    it('should get thread by serviceId', () => {
        const services = v1.getServices();
        const threads = sortArraysInObject(services.reduce((result, service) => {
            return result.concat(v1.getThreadsByServiceId(service.id));
        }, [] as SchemeThread[]));

        assert.deepStrictEqual(threads, sortArraysInObject(schemeThreads));
    });

    it('should get thread by id', () => {
        const threads = schemeThreads.map((thread: SchemeThread) => v1.getThreadById(thread.id));

        assert.deepStrictEqual(sortArraysInObject(threads), sortArraysInObject(schemeThreads));
    });

    it('shouldn\'t get thread by incorrect id', () => {
        const incorrectThreadId = 'sadfdghfdsfgsdg';

        assert.throws(
            () => v1.getThreadById(incorrectThreadId),
            Error,
            'Can\t find thread by this threadId'
        );
    });

    it('should get links by threadId', () => {
        const services = v1.getServices();
        const threads = services.reduce((result, service) => {
            return result.concat(v1.getThreadsByServiceId(service.id));
        }, [] as SchemeThread[]);
        const links = threads.map((thread) => v1.getLinksByThreadId(thread.id));

        assert.deepStrictEqual(sortArraysInObject(links), sortArraysInObject(schemeLinks));
    });

    it('get scheme format', () => {
        assert.equal(v1.getFormat(), Format.EXTENDED_LEGACY);
    });

    it('get xml string', () => {
        assert.equal(v1.getXmlString(), SCHEME_XML);
    });

    it('check consistency', () => {
        assert.throws(
            v1.checkConsistency,
            Error,
            'Not implemented'
        );
    });
});

function sortObjectsById(obj: ObjectWithId[]) {
    return obj.sort((a: ObjectWithId, b: ObjectWithId) => {
        return (a.id > b.id) ? 1 : -1;
    });
}

function sortArraysInObject(obj: IndexedObject) {
    const sortedObj: IndexedObject = {};
    Object.keys(obj).sort().forEach((key) => {
        if (Array.isArray(obj[key])) {
            sortedObj[key] = obj[key].sort();
        } else if (typeof obj[key] === 'object') {
            sortedObj[key] = sortArraysInObject(obj[key]);
        } else {
            sortedObj[key] = obj[key];
        }
    });

    return sortedObj;
}

interface IndexedObject {
    [key: string]: any;
}

interface ObjectWithId {
    id: string;
}
