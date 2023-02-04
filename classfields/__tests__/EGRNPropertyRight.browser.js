import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import PropertyRight from '../index';

describe('EGRNPropertyRight', () => {
    it('right column with 2 owners', async() => {
        const rightMock = {
            owners: [
                {
                    type: 'NATURAL_PERSON'
                },
                {
                    type: 'NATURAL_PERSON'
                }
            ],
            registration: {
                idRecord: '732735771877',
                regNumber: '77-77/007-77/007/003/2015-1474/3',
                type: 'OWNERSHIP',
                name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                regDate: '2019-12-13T11:05:39.282Z'
            }
        };

        await render(
            <PropertyRight right={rightMock} />,
            { viewport: { width: 400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('right column with 1 owner without name', async() => {
        const rightMock = {
            owners: [
                {
                    type: 'NATURAL_PERSON'
                }
            ],
            registration: {
                idRecord: '732735771877',
                regNumber: '77-77/007-77/007/003/2015-1474/3',
                type: 'OWNERSHIP',
                name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                regDate: '2019-12-13T11:05:39.282Z'
            }
        };

        await render(
            <PropertyRight right={rightMock} />,
            { viewport: { width: 400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('right column with 1 owner without type', async() => {
        const rightMock = {
            owners: [
                {
                    name: 'Антон'
                }
            ],
            registration: {
                idRecord: '732735771877',
                regNumber: '77-77/007-77/007/003/2015-1474/3',
                type: 'OWNERSHIP',
                name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                regDate: '2019-12-13T11:05:39.282Z'
            }
        };

        await render(
            <PropertyRight right={rightMock} />,
            { viewport: { width: 400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('"filler" right column (without regNumber and regDate but with shareText)', async() => {
        const rightMock = {
            owners: [
                {
                    name: 'Антон',
                    type: 'JURIDICAL_PERSON'
                }
            ],
            registration: {
                shareText: '1/2'
            }
        };

        await render(
            <PropertyRight right={rightMock} />,
            { viewport: { width: 400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('right row with end date', async() => {
        const rightMock = {
            owners: [
                {
                    name: 'Антон'
                }
            ],
            registration: {
                idRecord: '732735771877',
                regNumber: '77-77/007-77/007/003/2015-1474/3',
                type: 'OWNERSHIP',
                name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                regDate: '2019-10-13T11:05:39.282Z',
                endDate: '2019-11-01T11:05:39.282Z'
            }
        };

        await render(
            <PropertyRight right={rightMock} />,
            { viewport: { width: 400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
