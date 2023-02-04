const chai = require('chai');
const expect = chai.expect;
const path = require('path')
const formats = require('tv4-formats');

const origFormatUrl = formats.url;
formats.url = function (value) {
    return value === null || origFormatUrl(value);
};

formats['url-template'] = function (value) {
    const preparedValue = value.replace(/%|{|}/g, '');
    return formats.url(preparedValue);
};
formats['key'] = function (value) {
    if (value === null || /^%c&l=\w+(&tm=%v)?$/.test(value)) {
        return null;
    }
    return 'A valid key in %c&l=LLL&tm=%v format expected';
};

chai.use(require('chai-json-schema'));
chai.tv4.addFormat(formats);

const data = {
    'hosts v1.3': {
        schema: '/hosts/1.3/schema.json',
        envs: {
            testing: '/hosts/1.3/hosts-testing.json',
            stress: '/hosts/1.3/hosts-stress.json',
            production: '/hosts/1.3/hosts-production.json'
        }
    },
    'inthosts v1.1': {
        schema: '/inthosts/1.1/schema.json',
        envs: {
            testing: '/inthosts/1.1/hosts-testing.json',
            stress: '/inthosts/1.1/hosts-stress.json',
            production: '/inthosts/1.1/hosts-production.json'
        }
    },
};
const basePath = path.resolve(__dirname, '..', '..');

Object.keys(data).forEach((key) => {
    describe(key, () => {
        const schema = require(path.resolve(basePath + data[key].schema));
        schema.required = Object.keys(schema.properties);

        Object.keys(data[key].envs).forEach((name) => {
            const hosts = require(path.resolve(basePath + data[key].envs[name]));
            it('should have valid ' + name + ' hosts', () => {
                expect(hosts).to.be.jsonSchema(schema);
            });
        });
    });
});
