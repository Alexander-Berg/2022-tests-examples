import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MosRuSection } from '../index';
import { MOS_RU_COMPONENT_STATUSES } from '../constants';

const renderOptions = { viewport: { width: 800, height: 320 } };

const Component = props => {
    return <MosRuSection onClick={() => {}} {...props} />;
};

describe('MosRuSection', () => {
    describe('продажа', () => {
        it('привязка', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.BIND} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('привязка с лодером', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.BIND} isLoading />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('привязан', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.BOUND} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('привязан в настройках', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.BOUND} isSettings />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('привязан, указанна квартира', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.BOUND} hasFlat />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('ожидается', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.PENDING} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('ожидается, указана квартира', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.PENDING} hasFlat />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('статус bindError (ошибка привязка)', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.BIND_ERROR} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('статус techError (внутрянная ошибка)', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.TECH_ERROR} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('статус checkFlatError (ошибка проверки квартиры)', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.CHECK_FLAT_ERROR} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('статус checkOwnerError (ошибка проверки собственника)', async() => {
            await render(
                <Component type="SELL" status={MOS_RU_COMPONENT_STATUSES.CHECK_OWNER_ERROR} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('аренда', () => {
        it('привязка', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.BIND} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('привязка с лодером', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.BIND} isLoading />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('привязан', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.BOUND} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('привязан в настройках', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.BOUND} isSettings />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('привязан, указанна квартира', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.BOUND} hasFlat />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('ожидается', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.PENDING} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('ожидается, указана квартира', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.PENDING} hasFlat />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('статус bindError (ошибка привязка)', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.BIND_ERROR} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('статус techError (внутрянная ошибка)', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.TECH_ERROR} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('статус checkFlatError (ошибка проверки квартиры)', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.CHECK_FLAT_ERROR} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('статус checkOwnerError (ошибка проверки собственника)', async() => {
            await render(
                <Component type="RENT" status={MOS_RU_COMPONENT_STATUSES.CHECK_OWNER_ERROR} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
