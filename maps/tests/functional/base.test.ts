import * as request from 'supertest';

import app from '../../app/app';

describe('The basic interface of the HTTP API', () => {
    it('should respond with 200 on /ping requests', async () => {
        await request(app)
            .get('/ping')
            .expect(200);
    });

    it('should respond with 404 on unsupported paths', async () => {
        await request(app)
            .get('/cat_pictures')
            .expect(404);
    });

    it('should be free of `X-Powered-By` header', async () => {
        const {header} = await request(app)
            .get('/ping')
            .expect(200);

        expect(header['x-powered-by']).toBeUndefined();
    });
});
