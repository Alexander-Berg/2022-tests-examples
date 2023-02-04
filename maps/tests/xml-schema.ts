import * as path from 'path';
import {spawnSync} from 'child_process';

const XSD_PATH = path.resolve('tests/xsd');

export function validateYmapsmlSchema(xmlString: string): void {
    validateXmlSchema(xmlString, `${XSD_PATH}/ymapsml/representation.xsd`);
}

export function validateKmlSchema(xmlString: string): void {
    validateXmlSchema(xmlString, `${XSD_PATH}/kml/ogckml22.xsd`);
}

/**
 * Validates the given XML string against the XSD.
 *
 * For validation [xmllint](http://linuxcommand.org/man_pages/xmllint1.html) tool is used, because
 * [libxmljs](https://github.com/libxmljs/libxmljs) can't validate multiple XSD's:
 * https://github.com/libxmljs/libxmljs/issues/202.
 *
 * @throws {Error} If xml validation failed.
 */
function validateXmlSchema(xmlString: string, xsdPath: string): void {
    const result = spawnSync('xmllint', ['--nonet', '--schema', xsdPath, '-'], {
        encoding: 'utf8',
        input: xmlString
    });

    if (result.status) {
        throw new Error(`${result.stderr}\n${result.stdout}`);
    }
}
