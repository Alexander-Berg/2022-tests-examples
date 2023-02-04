/* eslint-disable max-len */
import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import FeedPreview from '../index';

advanceTo(new Date('2020-09-01T03:00:00.111Z'));

const defaultStoreStub = {
    vosUserData: {
        login: ''
    },
    user: {
        crc: ''
    },
    config: {
        apiServerHosts: '',
        timeDelta: 0,
        serverTime: new Date('2020-09-01T03:00:00.111Z')
    }
};

const defaultFeed = {
    partnerId: '777',
    name: 'Test Feed',
    url: 'http://misc.s3.mdst.yandex.net/rabota/deal_status_plan.xml',
    statusId: 22,
    removed: false,
    createTime: 1598445805000
};

const Component = ({ store, ...props }) => (
    <AppProvider initialState={store}>
        <FeedPreview {...props} />
    </AppProvider>
);

describe('FeedPreview', () => {
    describe('Отклонён модератором', () => {
        const feed = {
            ...defaultFeed,
            status: 'moderation_error',
            extendedReason: {
                list: [
                    {
                        segment: 'sell-room',
                        title: 'Сомнительная цена',
                        reasonText: 'Возможно, цена указана с ошибкой. Проверьте, что в поле <price> нет лишнего знака или наоборот, нет пропущенных. Уточните валюту. Сверьте, что выбран нужный тип сделки — продажа или аренда, длительная или посуточная. Если нашли ошибку — исправьте её. Мы быстро проверим и активируем объявление. Если вы считаете, что ошибок нет — напишите в службу поддержки.',
                        reasonHTML: 'Возможно, цена указана с ошибкой. Проверьте, что в поле &#60;price&#62; нет лишнего знака или наоборот, нет пропущенных. Уточните валюту. Сверьте, что выбран нужный тип сделки — продажа или аренда, длительная или посуточная. Если нашли ошибку — исправьте её. Мы быстро проверим и активируем объявление. Если вы считаете, что ошибок нет — напишите в службу поддержки.',
                        example: '444444'
                    }
                ]
            }
        };

        it('может быть отправлен на перемодерацию', async() => {
            const recheckFeed = {
                ...feed,
                statusId: 22,
                canChangeStatusAt: new Date('2020-09-01T02:30:00.111Z').getTime()
            };

            await render(<Component store={defaultStoreStub} feed={recheckFeed} />,
                { viewport: { width: 1400, height: 450 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('прошло недостаточно времени до переотправки', async() => {
            const recheckFeed = {
                ...feed,
                statusId: 22,
                canChangeStatusAt: new Date('2020-09-03T02:00:00.111Z').getTime()
            };

            await render(<Component store={defaultStoreStub} feed={recheckFeed} />,
                { viewport: { width: 1400, height: 450 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('осталось 15 минут до переотправки', async() => {
            const recheckFeed = {
                ...feed,
                statusId: 22,
                canChangeStatusAt: new Date('2020-09-01T03:15:00.111Z').getTime()
            };

            await render(<Component store={defaultStoreStub} feed={recheckFeed} />,
                { viewport: { width: 1400, height: 450 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('автоматическая проверка провалена', async() => {
            const recheckFeed = {
                ...feed,
                statusId: 5
            };

            await render(<Component store={defaultStoreStub} feed={recheckFeed} />,
                { viewport: { width: 1400, height: 350 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Ошибка скачивания', () => {
        const feed = {
            ...defaultFeed,
            status: 'download_error',
            statusId: 3
        };

        it('показывается сообщение об ошибке', async() => {
            await render(<Component store={defaultStoreStub} feed={feed} />,
                { viewport: { width: 1400, height: 350 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
