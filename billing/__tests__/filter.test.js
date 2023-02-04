import { fromJS, Map } from 'immutable';

import { DateTypes } from '../../constants';
import { FilterAction } from '../../actions';
import filter, { FilterState } from '../filter';
import { InitialDataAction } from 'common/actions';

const services = fromJS([
    { value: '11', content: 'Сервис Один' },
    { value: '22', content: 'Сервис Два' },
    { value: '33', content: 'Сервис Три' }
]);

describe('admin - invoices - reducers - filter', () => {
    const state = Map({
        current: FilterState(),
        isCancelled: true,
        shouldApplyFilterToHistory: false,
        next: FilterState({
            dateType: DateTypes.REQUEST
        })
    });

    it('apply with history', () => {
        expect.assertions(3);

        const nextState = filter(state, {
            type: FilterAction.APPLY,
            shouldApplyFilterToHistory: true
        });

        expect(nextState.get('current').equals(nextState.get('next'))).toBeTruthy();
        expect(nextState.get('isCancelled')).toBeFalsy();
        expect(nextState.get('shouldApplyFilterToHistory')).toBeTruthy();
    });

    it('apply without history', () => {
        expect.assertions(3);

        const nextState = filter(state, {
            type: FilterAction.APPLY
        });

        expect(nextState.get('current').equals(nextState.get('next'))).toBeTruthy();
        expect(nextState.get('isCancelled')).toBeTruthy();
        expect(nextState.get('shouldApplyFilterToHistory')).toBeFalsy();
    });

    it('apply with service order id filled - should set service', () => {
        expect.assertions(1);

        const state = Map({
            services,
            next: FilterState({
                serviceOrderId: '22-12345'
            })
        });

        const nextState = filter(state, {
            type: FilterAction.APPLY
        });

        expect(nextState.getIn(['next', 'service'])).toBe('22');
    });

    it('apply with unknown service order id filled - should not set service', () => {
        expect.assertions(1);

        const state = Map({
            services,
            next: FilterState({
                serviceOrderId: '44-12345'
            })
        });

        const nextState = filter(state, {
            type: FilterAction.APPLY
        });

        expect(nextState.getIn(['next', 'service'])).toBe('');
    });

    it('receive initial data', () => {
        expect.assertions(5);

        const state = Map({
            isFetching: true,
            firms: null,
            services: null,
            groupedPaysyses: null,
            serviceCodes: null
        });

        const firms = [];
        const services = [];
        const groupedPaysyses = [];
        const serviceCodes = [];

        const nextState = filter(state, {
            type: InitialDataAction.RECEIVE,
            firms,
            services,
            groupedPaysyses,
            serviceCodes
        });

        expect(nextState.get('firms').toJS()).toEqual(firms);
        expect(nextState.get('services').toJS()).toEqual(services);
        expect(nextState.get('groupedPaysyses')).toEqual(groupedPaysyses);
        expect(nextState.get('serviceCodes').toJS()).toEqual(serviceCodes);
        expect(nextState.get('isFetching')).toBeFalsy();
    });
});
