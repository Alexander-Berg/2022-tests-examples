import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';
import * as r from 'view/constants/requestStatuses';

import FilesUploadingContainer from '../container';

const defaultStoreMock = {
    filesUploading: {
        network: {
            fetchUploadedFilesStatus: r.REQUEST_STATUS_LOADED,
            uploadFilesStatus: r.REQUEST_STATUS_PENDING
        },
        data: {
            files: [ {
                name: '02.png',
                url: 'http://s3.mdst.yandex.net/realty/suburban/02.png',
                lastModified: '2019-12-19T22:18:22.856Z'
            }, {
                name: '2019-08-11-22-56-13.jpg',
                url: 'http://s3.mdst.yandex.net/realty/suburban/2019-08-11-22-56-13.jpg',
                lastModified: '2019-12-19T20:17:34.518Z'
            }, {
                name: '2019-07-31 16.24.13.jpg',
                url: 'http://s3.mdst.yandex.net/realty/suburban/2019-07-31%2016.24.13.jpg',
                lastModified: '2019-12-19T20:17:34.436Z'
            } ]
        }
    }
};

const Component = ({ store }) => (
    <AppProviders store={store}>
        <FilesUploadingContainer />
    </AppProviders>
);

describe('FilesUploading', () => {
    it('correct draw with uploaded files', async() => {
        await render(<Component store={defaultStoreMock} />, { viewport: { width: 800, height: 350 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw when failed request to uploaded files', async() => {
        const mock = {
            ...defaultStoreMock,
            filesUploading: {
                ...defaultStoreMock.filesUploading,
                network: {
                    ...defaultStoreMock.filesUploading.network,
                    fetchUploadedFilesStatus: r.REQUEST_STATUS_ERRORED
                }
            }
        };

        await render(<Component store={mock} />, { viewport: { width: 800, height: 200 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
