import * as assert from 'assert';
import {readFileSync} from 'fs';
import * as path from 'path';
import {Validator} from '../../server/lib/validator';
import {MetrokitBundle} from '../../server/editor/format-utils/metrokit-bundle';
import {IdGenerator, IdFormat} from '../../server/editor/format-utils/id-generator';
import {loadSecret} from '../../server/lib/secrets';

const SCHEME_XML = readFileSync(path.resolve('resources/test-fixtures/extended-legacy-scheme.xml'));
const LEGACY_SCHEME_ID = '13';
const metaValidator = new Validator('meta.yaml#/Meta');

const idGenerator = new IdGenerator(
    loadSecret('idGeneratorSalt'),
    8,
    IdFormat.decimal
);
const schemeId = `sc${idGenerator.generate(`scheme-${LEGACY_SCHEME_ID}`)}`;

describe('SchemeBundle class', () => {
    it('should create a bundle from an XML string', () => {
        const bundle = MetrokitBundle.fromXml(SCHEME_XML);

        assert.equal(bundle.getId(), schemeId);
    });

    it('should generate an invalid meta if not packed', () => {
        const bundle = MetrokitBundle.fromXml(SCHEME_XML);

        const error = metaValidator.validate(bundle.getMeta());
        assert.notEqual(error, null);
    });

    it('should generate a valid meta after being packed', (done) => {
        const bundle = MetrokitBundle.fromXml(SCHEME_XML);

        bundle.pack()
            .then(() => {
                const error = metaValidator.validate(bundle.getMeta());

                if (error === null) {
                    done();
                } else {
                    done(new Error(`Expected meta to be valid. Instead got: ${error}`));
                }
            })
            .catch(done);
    });

    it('should be able to update the version of the bundle', () => {
        const bundle = MetrokitBundle.fromXml(SCHEME_XML);

        const NEW_VERSION = 'v19283u1092';

        bundle.setVersion(NEW_VERSION);

        assert.equal(bundle.getMeta().version, NEW_VERSION);
    });
});
