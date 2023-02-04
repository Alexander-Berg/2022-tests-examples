import { renderHook } from '@testing-library/react-hooks';

import { ContextPage, ContextBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import contextMock from 'autoru-frontend/mocks/contextMock';

import * as logBuyoutShowModule from 'auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutShow';
import * as logBuyoutInspectionShowModule from 'auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutInspectionShow';

import { ApplicationFormStatus } from '../types';
import { METRIKA_PAGE, MetrikaBlock, MetrikaEventType, MetrikaEventTarget } from '../helpers/sendC2BLandingMetrika';

import { useC2BLandingLogging } from './useC2BLandingLogging';

jest.useFakeTimers();

describe('useC2BLandingLogging - отправляет метрику и фронтлог', () => {
    describe('на десктопе', () => {
        it('на начальном шаге формы', () => {
            const { logShowMock } = mockFrontLog();

            renderHook(() => useC2BLandingLogging({
                metrika: contextMock.metrika,
                status: ApplicationFormStatus.IDLE,
                isDesktop: true,
            }));

            expect(logShowMock).toHaveBeenCalledTimes(1);
            expect(logShowMock).toHaveBeenLastCalledWith({
                category: 'cars',
                contextPage: ContextPage.PAGE_LANDING_BUYOUT,
                contextBlock: ContextBlock.BLOCK_CARD,
            });

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.FORM_EVALUATION,
                MetrikaEventType.SHOW,
                MetrikaEventTarget.BLOCK,
            ]);
        });

        it('на шаге формы без оценки', () => {
            renderHook(() => useC2BLandingLogging({
                metrika: contextMock.metrika,
                status: ApplicationFormStatus.NOT_ELIGIBLE,
                isDesktop: true,
            }));

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.FORM_EVALUATION,
                MetrikaEventType.SHOW,
                MetrikaEventTarget.NOT_EVALUATION,
            ]);
        });

        it('на шаге с ошибкой', () => {
            renderHook(() => useC2BLandingLogging({
                metrika: contextMock.metrika,
                status: ApplicationFormStatus.EVALUATION_FAILED,
                isDesktop: true,
            }));

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.FORM_EVALUATION,
                MetrikaEventType.SHOW,
                MetrikaEventTarget.ERROR,
            ]);
        });

        it('на шаге с заполненными данными машины', () => {
            const { logInspectionShowMock } = mockFrontLog();

            renderHook(() => useC2BLandingLogging({
                metrika: contextMock.metrika,
                status: ApplicationFormStatus.ELIGIBLE,
                isDesktop: true,
            }));

            expect(logInspectionShowMock).toHaveBeenCalledTimes(1);
            expect(logInspectionShowMock).toHaveBeenLastCalledWith({
                category: 'cars',
                contextPage: ContextPage.PAGE_LANDING_BUYOUT,
                contextBlock: ContextBlock.BLOCK_CARD,
            });

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.FORM_INSPECTION,
                MetrikaEventType.SHOW,
                MetrikaEventTarget.BLOCK,
            ]);
        });

        it('при показе экрана с успешной заявкой', () => {
            renderHook(() => useC2BLandingLogging({
                metrika: contextMock.metrika,
                status: ApplicationFormStatus.SUCCESS,
                isDesktop: true,
            }));

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.APPLICATION,
                MetrikaEventType.SHOW,
                MetrikaEventTarget.BLOCK,
            ]);
        });
    });

    describe('на мобилках и планшетах', () => {
        it('на начальном шаге формы', () => {
            const { logShowMock } = mockFrontLog();

            renderHook(() => useC2BLandingLogging({
                metrika: contextMock.metrika,
                status: ApplicationFormStatus.IDLE,
                isModalVisible: true,
            }));

            expect(logShowMock).toHaveBeenCalledTimes(1);
            expect(logShowMock).toHaveBeenLastCalledWith({
                category: 'cars',
                contextPage: ContextPage.PAGE_LANDING_BUYOUT,
                contextBlock: ContextBlock.BLOCK_CARD,
            });

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.FORM_EVALUATION,
                MetrikaEventType.SHOW,
                MetrikaEventTarget.BLOCK,
            ]);
        });

        it('на шаге формы без оценки', () => {
            renderHook(() => useC2BLandingLogging({
                metrika: contextMock.metrika,
                status: ApplicationFormStatus.NOT_ELIGIBLE,
                isModalVisible: true,
            }));

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.FORM_EVALUATION,
                MetrikaEventType.SHOW,
                MetrikaEventTarget.NOT_EVALUATION,
            ]);
        });

        it('на шаге с ошибкой', () => {
            renderHook(() => useC2BLandingLogging({
                metrika: contextMock.metrika,
                status: ApplicationFormStatus.EVALUATION_FAILED,
                isModalVisible: true,
            }));

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.FORM_EVALUATION,
                MetrikaEventType.SHOW,
                MetrikaEventTarget.ERROR,
            ]);
        });

        it('на шаге с заполненными данными машины', () => {
            const { logInspectionShowMock } = mockFrontLog();

            renderHook(() => useC2BLandingLogging({
                metrika: contextMock.metrika,
                status: ApplicationFormStatus.ELIGIBLE,
                isModalVisible: true,
            }));

            expect(logInspectionShowMock).toHaveBeenCalledTimes(1);
            expect(logInspectionShowMock).toHaveBeenLastCalledWith({
                category: 'cars',
                contextPage: ContextPage.PAGE_LANDING_BUYOUT,
                contextBlock: ContextBlock.BLOCK_CARD,
            });

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.FORM_INSPECTION,
                MetrikaEventType.SHOW,
                MetrikaEventTarget.BLOCK,
            ]);
        });

        it('при показе экрана с успешной заявкой', () => {
            renderHook(() => useC2BLandingLogging({
                metrika: contextMock.metrika,
                status: ApplicationFormStatus.SUCCESS,
                isModalVisible: true,
            }));

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.APPLICATION,
                MetrikaEventType.SHOW,
                MetrikaEventTarget.BLOCK,
            ]);
        });
    });
});

function mockFrontLog() {
    const logShowMock = jest.fn();
    const logInspectionShowMock = jest.fn();

    jest.spyOn(logBuyoutShowModule, 'logBuyoutShow').mockImplementation(logShowMock);
    jest.spyOn(logBuyoutInspectionShowModule, 'logBuyoutInspectionShow').mockImplementation(logInspectionShowMock);

    return {
        logShowMock,
        logInspectionShowMock,
    };
}
