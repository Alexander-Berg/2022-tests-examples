jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

import { renderHook, act } from '@testing-library/react-hooks';

import contextMock from 'autoru-frontend/mocks/contextMock';

import gateApi from 'auto-core/react/lib/gateApi';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type TContext from 'auto-core/types/TContext';

import useOfferComplaintPopup from './useOfferComplaintPopup';

const getResourceMock = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const defaultProps = {
    metrikaOrigin: 'origin',
    onDone: jest.fn(),
    onRequestHide: jest.fn(),
    offer: offerMock,
    placement: 'placement',
    showAutoclosableMessage: jest.fn(),
    showAutoclosableErrorMessage: jest.fn(),
    isUserBannedInCurrentCategory: false,
};

it('правильно обрабатывает изменение инпута и чекбоксов', () => {
    const { result } = render();
    act(() => {
        result.current.onReasonsChange([ 'SOLD' ]);
        result.current.onInputChange('bruh', { name: 'SOLD' });
    });
    expect(result.current.getComplaint('SOLD')).toEqual({ type: 'SOLD', value: 'bruh' });
    act(() => {
        result.current.onReasonsChange([ 'SOLD', 'DUPLICATE' ]);
        result.current.onInputChange('bruuuuh', { name: 'DUPLICATE' });
    });
    expect(result.current.getComplaint('SOLD')).toEqual({ type: 'SOLD', value: 'bruh' });
    expect(result.current.getComplaint('DUPLICATE')).toEqual({ type: 'DUPLICATE', value: 'bruuuuh' });
    act(() => {
        result.current.onReasonsChange([ 'SOLD' ]);
        result.current.onReasonsChange([ 'SOLD', 'DUPLICATE' ]);
    });
    expect(result.current.getComplaint('SOLD')).toEqual({ type: 'SOLD', value: 'bruh' });
    expect(result.current.getComplaint('DUPLICATE')).toEqual({ type: 'DUPLICATE', value: '' });
});

describe('должен правильно обработать отправку формы', () => {
    it('если юзер не забанен', async() => {
        getResourceMock.mockResolvedValue({});
        const { result, waitForNextUpdate } = render();
        act(() => {
            result.current.onReasonsChange([ 'SOLD', 'OTHER' ]);
            result.current.onInputChange('bruh', { name: 'SOLD' });
            result.current.onInputChange('bruuuuh', { name: 'OTHER' });
        });
        act(() => {
            result.current.onFormSubmit();
        });
        await waitForNextUpdate();
        expect(getResourceMock).toHaveBeenCalledWith('postComplaint', {
            category: 'cars', offerIdHash: '1085562758-1970f439', placement: 'placement', reason: 'SOLD', text: 'bruh',
        });
        expect(getResourceMock).toHaveBeenCalledWith('postComplaint', {
            category: 'cars', offerIdHash: '1085562758-1970f439', placement: 'placement', reason: 'OTHER', text: 'bruuuuh',
        });
        expect(defaultProps.onDone).toHaveBeenCalled();
        expect(defaultProps.onRequestHide).toHaveBeenCalled();
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'offer-report', 'origin', 'SOLD' ]);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'offer-report', 'origin', 'OTHER' ]);
        expect(defaultProps.showAutoclosableMessage).toHaveBeenCalledWith({ message: 'Спасибо, мы всё проверим', view: 'success' });
    });

    it('если юзер забанен', () => {
        getResourceMock.mockResolvedValue({});
        const { result } = render({ ...defaultProps, isUserBannedInCurrentCategory: true });
        act(() => {
            result.current.onReasonsChange([ 'SOLD' ]);
            result.current.onInputChange('bruh', { name: 'SOLD' });
        });
        act(() => {
            result.current.onFormSubmit();
        });
        expect(defaultProps.onDone).toHaveBeenCalled();
        expect(defaultProps.onRequestHide).toHaveBeenCalled();
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        expect(getResourceMock).toHaveBeenCalledTimes(0);
        expect(defaultProps.showAutoclosableMessage).toHaveBeenCalledWith({ message: 'Спасибо, мы всё проверим', view: 'success' });
    });

    it('ошибка при отправке', async() => {
        getResourceMock.mockRejectedValue({});
        const { result, waitForNextUpdate } = render();
        act(() => {
            result.current.onReasonsChange([ 'SOLD' ]);
            result.current.onInputChange('bruh', { name: 'SOLD' });
        });
        act(() => {
            result.current.onFormSubmit();
        });
        await waitForNextUpdate();
        expect(defaultProps.showAutoclosableErrorMessage).toHaveBeenCalled();
    });
});

function render(props = defaultProps) {
    return renderHook(() => useOfferComplaintPopup(props, contextMock as unknown as TContext));
}
