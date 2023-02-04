import React from 'react';
import noop from 'lodash/noop';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IFlatUser } from 'types/flat';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/manager/reducer';

import { ManagerAssignedUsers } from '../index';

import * as assignedUsers from './stubs';

const renderOptions = [
    {
        viewport: {
            width: 820,
            height: 900,
        },
    },
    {
        viewport: {
            width: 375,
            height: 900,
        },
    },
];

const Component: React.FunctionComponent<{ assignedUsers: IFlatUser[] }> = ({ assignedUsers }) => (
    <AppProvider rootReducer={rootReducer}>
        <ManagerAssignedUsers assignedUsers={assignedUsers} unassignUser={noop} />
    </AppProvider>
);

describe('ManagerAssignedUsers', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            it(`width ${renderOption.viewport.width}`, async () => {
                await render(<Component assignedUsers={assignedUsers.assignedUsersQuestionnaireValid} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Вывод статуса модерации анкеты', () => {
        it(`не отправлена на проверку`, async () => {
            await render(<Component assignedUsers={assignedUsers.assignedUsersChecksUndefined} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`статус - IN_PROGRESS`, async () => {
            await render(
                <Component assignedUsers={assignedUsers.assignedUsersQuestionnaireInProgress} />,
                renderOptions[0]
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`статус - VALID`, async () => {
            await render(<Component assignedUsers={assignedUsers.assignedUsersQuestionnaireValid} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`статус - INVALID`, async () => {
            await render(
                <Component assignedUsers={assignedUsers.assignedUsersQuestionnaireInvalid} />,
                renderOptions[0]
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Вывод информации по проверкам', () => {
        it(`проверок вообще нет`, async () => {
            await render(<Component assignedUsers={assignedUsers.assignedUsersChecksUndefined} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`статус IN_PROGRESS без предыдущей резолюции`, async () => {
            await render(<Component assignedUsers={assignedUsers.assignedUsersChecksInProgress} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`статус IN_PROGRESS с предыдущей резолюцией`, async () => {
            await render(
                <Component assignedUsers={assignedUsers.assignedUsersChecksInProgressWithResolution} />,
                renderOptions[0]
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`статус ERROR`, async () => {
            await render(<Component assignedUsers={assignedUsers.assignedUsersChecksError} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`статус READY, резолюция INVALID`, async () => {
            await render(
                <Component assignedUsers={assignedUsers.assignedUsersChecksReadyResolutionInvalid} />,
                renderOptions[0]
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`статус READY, резолюция VALID, долгов нет`, async () => {
            await render(
                <Component assignedUsers={assignedUsers.assignedUsersChecksReadyResolutionValid} />,
                renderOptions[0]
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`статус READY, резолюция VALID, но есть долги в фссп`, async () => {
            await render(
                <Component assignedUsers={assignedUsers.assignedUsersChecksReadyWithDebts} />,
                renderOptions[0]
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`статус READY, резолюция INVALID и есть долги в фссп`, async () => {
            await render(
                <Component assignedUsers={assignedUsers.assignedUsersChecksInvalidWithDebts} />,
                renderOptions[0]
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
