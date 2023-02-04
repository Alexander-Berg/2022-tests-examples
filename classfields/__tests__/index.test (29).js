import React from 'react';
import PropTypes from 'prop-types';
import { Provider } from 'react-redux';
import { createStore } from 'redux';
import { mount } from 'enzyme';

import dayjs from '@realty-front/dayjs';

import OfferStatus from '..';

const store = createStore(jest.fn(), {});
const date = dateStr => dayjs(dateStr, [ 'DD.MM.YYYY HH:mm', 'DD.MM.YYYY' ]).utcOffset(0).toISOString();
const baseToday = date('15.12.2019 12:00');
const currentYear = dayjs().year();
const activeOffer = {
    offerId: '0',
    updateDate: date('11.12.2019 12:00'),
    creationDate: date('12.12.2019 12:00'),
    expireDate: date('18.12.2019 12:00'),
    active: true,
    offerStatus: 'active',
    isEditable: true
};
const defaultVas = {
    placement: {
        free: {}
    },
    vasServices: {
        placement: {}
    }
};

describe('OfferStatus', () => {
    describe('expire date', () => {
        const withToday = today => mount(
            <Provider store={store}>
                <OfferStatus
                    offer={activeOffer}
                    today={date(today)}
                    vas={defaultVas}
                    isNotEnoughFunds={false}
                    isJuridical={false}
                />
            </Provider>,
            { context: { link: () => {} }, childContextTypes: { link: PropTypes.func } }
        ).text();

        it('should display “less then minute before expiring” if expire date already passed but offer is still ' +
            'active (should be impossible)', () => {
            expect(withToday('19.12.2019 12:00')).toContain('меньше минуты до снятия');
        });

        it('should display how many minutes before expiring left', () => {
            expect(withToday('18.12.2019 11:25')).toContain('35 минут до снятия');
            expect(withToday('18.12.2019 11:39')).toContain('21 минута до снятия');
            expect(withToday('18.12.2019 11:58')).toContain('2 минуты до снятия');
            expect(withToday('18.12.2019 11:59')).toContain('меньше минуты до снятия');
        });

        it('should display how many hours before expiring left', () => {
            expect(withToday('18.12.2019 10:00')).toContain('2 часа до снятия');
            expect(withToday('18.12.2019 10:29')).toContain('2 часа до снятия');
            expect(withToday('18.12.2019 10:30')).toContain('2 часа до снятия');
            expect(withToday('18.12.2019 10:31')).toContain('2 часа до снятия');
            expect(withToday('18.12.2019 10:59')).toContain('2 часа до снятия');
            expect(withToday('18.12.2019 11:00')).toContain('1 час до снятия');
        });

        it('should display how many days before expiring left', () => {
            expect(withToday('17.12.2019')).toContain('1 день до снятия');
            expect(withToday('10.12.2019')).toContain('8 дней до снятия');
            expect(withToday('01.12.2019')).toContain('17 дней до снятия');
            expect(withToday('18.12.2018')).toContain('365 дней до снятия');
            expect(withToday('01.12.2018')).toContain('382 дня до снятия');
        });
    });

    describe('creation date', () => {
        const withCreationDate = creationDate => mount(
            <Provider store={store}>
                <OfferStatus
                    offer={{ ...activeOffer, creationDate: date(creationDate) }}
                    today={baseToday}
                    vas={defaultVas}
                    isNotEnoughFunds={false}
                    isJuridical={false}
                />
            </Provider>,
            { context: { link: () => {} }, childContextTypes: { link: PropTypes.func } }
        ).text();

        it('should display current date without year if year is the current year', () => {
            expect(withCreationDate(`17.12.${currentYear}`)).toContain('17 декабря,');
            expect(withCreationDate(`01.04.${currentYear}`)).toContain('1 апреля,');
        });

        it('should display current date with year if year is not the current year', () => {
            expect(withCreationDate('01.04.2018')).toContain('1 апреля 2018,');
            expect(withCreationDate('01.04.2017')).toContain('1 апреля 2017,');
        });
    });

    describe('update date', () => {
        const withUpdateDate = updateDate => mount(
            <Provider store={store}>
                <OfferStatus
                    offer={{ ...activeOffer, updateDate: date(updateDate) }}
                    today={baseToday}
                    vas={defaultVas}
                    isNotEnoughFunds={false}
                    isJuridical={false}
                />
            </Provider>,
            { context: { link: () => {} }, childContextTypes: { link: PropTypes.func } }
        ).text();

        it('should display “updated now” if update time is now', () => {
            expect(withUpdateDate('15.12.2019 12:00')).toContain('Обновлено сейчас,');
        });

        it('should display “updated now” if update time is in the future (should be impossible)', () => {
            expect(withUpdateDate('15.12.2019 13:00')).toContain('Обновлено сейчас,');
            expect(withUpdateDate('16.12.2019')).toContain('Обновлено сейчас,');
        });

        it('should display how many minutes have passed', () => {
            expect(withUpdateDate('15.12.2019 11:01')).toContain('Обновлено 59 минут назад,');
            expect(withUpdateDate('15.12.2019 11:30')).toContain('Обновлено 30 минут назад,');
            expect(withUpdateDate('15.12.2019 11:39')).toContain('Обновлено 21 минуту назад,');
            expect(withUpdateDate('15.12.2019 11:58')).toContain('Обновлено 2 минуты назад,');
            expect(withUpdateDate('15.12.2019 11:59:00')).toContain('Обновлено сейчас,');
        });

        it('should display how many hours have passed', () => {
            expect(withUpdateDate('15.12.2019 10:00')).toContain('Обновлено 2 часа назад,');
            expect(withUpdateDate('15.12.2019 10:29')).toContain('Обновлено 2 часа назад,');
            expect(withUpdateDate('15.12.2019 10:30')).toContain('Обновлено 2 часа назад,');
            expect(withUpdateDate('15.12.2019 10:31')).toContain('Обновлено 2 часа назад,');
            expect(withUpdateDate('15.12.2019 11:00')).toContain('Обновлено 1 час назад,');
        });

        it('should display "yesterday" if day have passed', () => {
            expect(withUpdateDate('14.12.2019')).toContain('Обновлено вчера,');
        });

        it('should display date if more then day have passed', () => {
            expect(withUpdateDate('13.12.2019')).toContain('Обновлено 13 декабря 2019,');
            expect(withUpdateDate('01.12.2019')).toContain('Обновлено 1 декабря');
        });

        it('should display year if it is not current year', () => {
            expect(withUpdateDate('01.11.2018')).toContain('Обновлено 1 ноября 2018,');
        });

        it('should not display year if it is not current year but less then 2 days have passed', () => {
            const wrapper = mount(
                <Provider store={store}>
                    <OfferStatus
                        offer={{ ...activeOffer, updateDate: date('31.12.2018') }}
                        today={date('01.01.2019')}
                        vas={defaultVas}
                        isNotEnoughFunds={false}
                        isJuridical={false}
                    />
                </Provider>,
                { context: { link: () => {} }, childContextTypes: { link: PropTypes.func } }
            );

            expect(wrapper.text()).toContain('Обновлено вчера');
        });

        it('should not display update date when creation date is the same', () => {
            const offer = {
                ...activeOffer,
                updateDate: date('11.12.2019 12:00'),
                creationDate: date('11.12.2019 12:00')
            };
            const wrapper = mount(
                <Provider store={store}>
                    <OfferStatus
                        offer={offer}
                        today={baseToday}
                        vas={defaultVas}
                        isNotEnoughFunds={false}
                        isJuridical={false}
                    />
                </Provider>,
                { context: { link: () => {} }, childContextTypes: { link: PropTypes.func } }
            );

            expect(wrapper.text()).toContain('Размещено 11 декабря 2019,');
        });
    });

    describe('not active', () => {
        const withStatus = (offerStatus, isEditable = true) => mount(
            <Provider store={store}>
                <OfferStatus
                    offer={{ active: false, offerStatus, isEditable }}
                    today={date('15.12.2019 12:00')}
                    vas={defaultVas}
                    isNotEnoughFunds={false}
                    isJuridical={false}
                />
            </Provider>,
            { context: { link: () => {} }, childContextTypes: { link: PropTypes.func } }
        ).text();

        it('should render moderation status correctly', () => {
            expect(withStatus('moderation')).toBe(
                'Проверка объявления После быстрой проверки объявление ' +
                'будет доступно на сервисе'
            );
        });

        it('should render blocked status correctly', () => {
            expect(withStatus('blocked')).toBe('Заблокировано');
            expect(withStatus('banned')).toBe('Заблокировано');
        });

        it('should render inactive status correctly', () => {
            expect(withStatus('inactive')).toBe('Снято с публикации Объявление не отображается на сервисе, ' +
                'покупатели его не видят');
        });

        it('should render banned status correctly', () => {
            expect(withStatus('banned', false)).toBe('Заблокировано');
        });

        it('should render draft status correctly', () => {
            expect(withStatus('draft')).toBe('Черновик');
        });

        it('should render unpaid status correctly', () => {
            expect(withStatus('unpaid')).toBe('Не оплачено');
        });

        it('should render remove status correctly', () => {
            expect(withStatus('remove')).toBe('Удалено');
            expect(withStatus('remove', false)).toBe('Удалено');
        });
    });

    describe('placement', () => {
        const withVas = ({ end, free, quota, paymentRequired, offerData }) => mount(
            <Provider store={store}>
                <OfferStatus
                    offer={{ ...activeOffer, ...offerData }}
                    today={date('15.12.2019 12:00')}
                    vas={{
                        vasServices: {
                            placement: {
                                end
                            }
                        },
                        placement: {
                            free,
                            quota,
                            paymentRequired
                        }
                    }}
                    isNotEnoughFunds={false}
                    isJuridical={false}
                />
            </Provider>,
            { context: { link: () => { } }, childContextTypes: { link: PropTypes.func } }
        ).text();

        const needActivationText = 'Требует активации ' +
          'Объявление не отображается на сервисе, покупатели его не видят';

        it('should render "need activate placement" status for inactive offer correctly', () => {
            expect(withVas({
                paymentRequired: {}
            })).toBe(needActivationText);
        });

        it('should render unpublished status when placement still not end', () => {
            expect(withVas({
                paymentRequired: { paid: true },
                end: date('16.12.2019 12:00'),
                offerData: { offerStatus: 'inactive', active: false }
            })).toBe('Снято с публикации ' +
            'Объявление не отображается на сервисе, покупатели его не видят');
        });

        it('should render unpublished status when placement bought by quota', () => {
            expect(withVas({
                quota: { },
                offerData: { offerStatus: 'inactive', active: false }
            })).toBe('Снято с публикации ' +
                'Объявление не отображается на сервисе, покупатели его не видят');
        });

        it('should render paid offer correctly', () => {
            expect(withVas({
                paymentRequired: { paid: true },
                end: date('25.12.2019 12:00'),
                offerData: {
                    updateDate: date('14.12.2019 12:00'),
                    creationDate: date('14.12.2019 12:00')
                }
            })).toBe('Опубликовано Размещено вчера, 10\u00a0дней до\u00a0продления');
        });
    });
});
