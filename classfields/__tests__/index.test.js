const zlib = require('zlib');

const { bodyWrite, bodyRead } = require('@realty-front/jest-utils/proxy/libs/mock-body');

const stubs = [

    // 'application/json'

    {
        title: 'application/json - пришло строкой',
        data: { a: 1 },
        buildBody: data => JSON.stringify(data),
        headers: {
            'content-type': 'application/json',
            'content-encoding': undefined
        }
    },
    {
        title: 'application/json - пришло в gzip',
        data: { a: 1 },
        buildBody: data => Buffer.from(zlib.gzipSync(JSON.stringify(data))),
        headers: {
            'content-type': 'application/json',
            'content-encoding': 'gzip'
        }
    },
    {
        title: 'application/json - пришло строкой с mergeFn',
        data: { a: 1 },
        buildBody: data => JSON.stringify(data),
        headers: {
            'content-type': 'application/json',
            'content-encoding': undefined
        },
        mergeFn: body => {
            body.a = 2;

            return body;
        }
    },
    {
        title: 'application/json - пришло в gzip с mergeFn',
        data: { a: 1 },
        buildBody: data => Buffer.from(zlib.gzipSync(JSON.stringify(data))),
        headers: {
            'content-type': 'application/json',
            'content-encoding': 'gzip'
        },
        mergeFn: body => {
            body.a = 2;

            return body;
        }
    },

    // 'text/plain'

    {
        title: 'text/plain - пришло строкой',
        data: 'something',
        buildBody: data => data,
        headers: {
            'content-type': 'text/plain',
            'content-encoding': undefined
        }
    },
    {
        title: 'text/plain - пришло в gzip',
        data: 'something',
        buildBody: data => Buffer.from(zlib.gzipSync(data)),
        headers: {
            'content-type': 'text/plain',
            'content-encoding': 'gzip'
        }
    },
    {
        title: 'text/plain - пришло строкой с mergeFn',
        data: 'something',
        buildBody: data => data,
        headers: {
            'content-type': 'text/plain',
            'content-encoding': undefined
        },
        mergeFn: body => {
            return body + ' happened';
        }
    },
    {
        title: 'text/plain - пришло в gzip с mergeFn',
        data: 'something',
        buildBody: data => Buffer.from(zlib.gzipSync(data)),
        headers: {
            'content-type': 'text/plain',
            'content-encoding': 'gzip'
        },
        mergeFn: body => {
            return body + ' happened';
        }
    },

    // 'application/x-protobuf'

    {
        title: 'application/x-protobuf - пришло без шифрования',
        data: Buffer.from('something binary'),
        buildBody: data => data,
        headers: {
            'content-type': 'application/x-protobuf',
            'content-encoding': undefined
        }
    },
    {
        title: 'application/x-protobuf - пришло в gzip',
        data: Buffer.from('something binary'),
        buildBody: data => Buffer.from(zlib.gzipSync(data)),
        headers: {
            'content-type': 'application/x-protobuf',
            'content-encoding': 'gzip'
        }
    }
];

describe('mock-body', () => {
    describe('Синхронизированно пишет и читает моки', () => {
        stubs.forEach(({ title, data, buildBody, headers, mergeFn }) => {
            it(title, () => {
                const originalBody = buildBody(data);
                const finalBody = buildBody(mergeFn?.(data) || data);

                const mockString = JSON.stringify({
                    body: bodyWrite(originalBody, headers),
                    headers
                });

                const mockObject = JSON.parse(mockString);
                const bodyResponce = bodyRead({ body: mockObject.body, headers: mockObject.headers, mergeFn });

                expect(finalBody).toStrictEqual(bodyResponce);
            });
        });
    });
});
