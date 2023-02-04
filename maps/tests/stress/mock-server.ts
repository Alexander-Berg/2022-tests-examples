import express from 'express';
import {delay} from 'prex';
import {asyncMiddleware} from '@yandex-int/express-async';
import {logger} from 'app/lib/logger';
import {ProjectList} from 'app/lib/keys/service-project-provider';
import {MockDataGenerator} from './lib/mock-data-generator';

export function startApikeysMockServer() {
    const app = express();
    const generator = new MockDataGenerator();
    const projects = generator.getProjects(10000);

    const getProjectListPage = (pageSize: number, pageNumber: number): ProjectList => {
        const data = projects.slice((pageNumber - 1) * pageSize, pageNumber * pageSize);

        return {
            data,
            page: {
                items: data.length,
                number: pageNumber,
                pages: Math.ceil(projects.length / pageSize),
                size: pageSize
            },
            max_update_dt: new Date().toISOString()
        };
    };

    app.get('/api/v2/project_service_link_export', asyncMiddleware(async (req, res) => {
        const pageSize = parseInt(req.query._page_size, 10) || 10;
        const pageNumber = parseInt(req.query._page_number, 10) || 1;

        await delay(1000);

        res.json(getProjectListPage(pageSize, pageNumber));
    }));

    app.get('/api/update_counters', asyncMiddleware(async (req, res) => {
        await delay(100);

        res.json({result: 'OK'});
    }));

    const port = 8088;
    app.listen(port, () => {
        logger.info(`Apikeys mock server started on port ${port}`);
    });

    return `http://localhost:${port}/`;
}

export function startKeyservMockServer() {
    const app = express();

    app.get('/2.x', asyncMiddleware(async (req, res) => {
        await delay(10);

        if (req.query.action !== 'checkKey') {
            res.status(404).send('Not found');
            return;
        }

        res.setHeader('content-type', 'text/xml');
        res.send('<?xml version="1.0"?><keystate><valid>true</valid><broken>false</broken></keystate>');
    }));

    const port = 8089;
    app.listen(port, () => {
        logger.info(`Keyserv mock server started on port ${port}`);
    });

    return `http://localhost:${port}/`;
}
