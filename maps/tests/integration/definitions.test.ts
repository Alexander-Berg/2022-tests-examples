import * as assert from 'assert';
import {
    readFileSync,
    readdirSync
} from 'fs';
import {safeLoad} from 'js-yaml';
import {
    Validator,
    DEFINITIONS_DIRECTORY
} from '../../server/lib/validator';

const eventsExample = require('../../../docs/examples/events.json');

describe('Metrokit json schemes', () => {
    it('should be compilablable', () => {
        const schemeUris: string[] = readdirSync(DEFINITIONS_DIRECTORY)
            .reduce((result: string[], fileName: string) => {
                const fileContents = readFileSync(
                    `${DEFINITIONS_DIRECTORY}/${fileName}`,
                    {encoding: 'utf8'}
                );
                const rootObject = safeLoad(fileContents);

                return result.concat(
                    Object.keys(rootObject).map((fieldName) => `${fileName}#/${fieldName}`)
                );
            }, []);

        // The constructor tries to compile the scheme
        schemeUris.forEach((schemeUri) => new Validator(schemeUri));
    });

    it('should validate the events example', () => {
        const eventsValidator = new Validator('events-response-container.yaml#/EventsResponseContainer');

        assert.equal(eventsValidator.validate(eventsExample), null);
    });
});
