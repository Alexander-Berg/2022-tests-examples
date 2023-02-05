import * as path from 'path';
import {readFileSync, writeFileSync} from 'fs';
import {DomParserNode} from '../server/editor/dom-parser/node';
import {SchemeThread} from '../server/editor/scheme-model';
import {V1SchemeModel} from '../server/editor/scheme-model/index';

const savePath = path.resolve('resources/test-fixtures/v1-test-data/');
const SCHEME_XML = readFileSync(path.resolve(
    'resources/test-fixtures/extended-legacy-scheme.xml'),
    'utf8'
);
const domParserNode = new DomParserNode();
const v1 = new V1SchemeModel(SCHEME_XML, domParserNode);

writeFileSync(path.join(savePath, 'scheme-names.json'), stringifyAndFormat(v1.getName()));

writeFileSync(path.join(savePath, 'scheme-attributes.json'), stringifyAndFormat(v1.getAttributes()));

writeFileSync(path.join(savePath, 'scheme-stations.json'), stringifyAndFormat(v1.getStations()));

const services = v1.getServices();
writeFileSync(path.join(savePath, '/scheme-services.json'), stringifyAndFormat(services));

const threads = services.reduce((result, {id}) => result.concat(v1.getThreadsByServiceId(id)), [] as SchemeThread[]);
writeFileSync(path.join(savePath, 'scheme-threads.json'), stringifyAndFormat(threads));

const links = threads.map((thread) => v1.getLinksByThreadId(thread.id));
writeFileSync(path.join(savePath, 'scheme-links.json'), stringifyAndFormat(links));

function stringifyAndFormat(obj: any) {
    return JSON.stringify(obj, null, 4) + '\n';
}
