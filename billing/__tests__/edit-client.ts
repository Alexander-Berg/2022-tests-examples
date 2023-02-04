import { Record } from 'immutable';

import { EditClientRecord } from '../edit-client';
import { validateDetail } from '../utils/edit-client';
import { DetailId } from 'common/types/client';
import { EditClientState } from '../../types/edit-client';

describe('admin', () => {
    describe('editclient', () => {
        describe('валидация', () => {
            it('Валюта расчетов и Полное название', () => {
                let state = EditClientRecord().setIn(
                    ['client', DetailId.isNonResident],
                    true
                ) as Record<EditClientState>;
                state = validateDetail(state, DetailId.isoCurrencyPayment);
                state = validateDetail(state, DetailId.fullname);
                expect(state.getIn(['errors', DetailId.isoCurrencyPayment])).toBe(
                    'Поле обязательно к заполнению для нерезидентов'
                );
                expect(state.getIn(['errors', DetailId.fullname])).toBe(
                    'Поле обязательно к заполнению для нерезидентов'
                );
                state = state
                    .removeIn(['errors', DetailId.isoCurrencyPayment])
                    .removeIn(['errors', DetailId.fullname])
                    .setIn(['client', DetailId.isoCurrencyPayment], 'USD');
                state = validateDetail(state, DetailId.isoCurrencyPayment);
                state = validateDetail(state, DetailId.fullname);
                expect(state.getIn(['errors', DetailId.isoCurrencyPayment])).toBe('');
                expect(state.getIn(['errors', DetailId.fullname])).toBe(
                    'Поле обязательно к заполнению для нерезидентов'
                );
                state = state = state
                    .removeIn(['errors', DetailId.isoCurrencyPayment])
                    .removeIn(['errors', DetailId.fullname])
                    .setIn(['client', DetailId.fullname], 'лелик');
                state = validateDetail(state, DetailId.isoCurrencyPayment);
                state = validateDetail(state, DetailId.fullname);
                expect(state.getIn(['errors', DetailId.isoCurrencyPayment])).toBe('');
                expect(state.getIn(['errors', DetailId.fullname])).toBe('');
            });
            it('Пометить как фрод Другой', () => {
                const state = EditClientRecord().setIn(
                    ['client', DetailId.fraudStatus, 'flag'],
                    true
                );

                const nextState = validateDetail(state, DetailId.name);

                expect(nextState.getIn(['errors', DetailId.name])).toBe(
                    'ID_PERSONEDIT_field_must_not_be_empty'
                );
            });
        });
    });
});
