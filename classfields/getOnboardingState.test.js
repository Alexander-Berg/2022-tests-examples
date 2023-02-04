import getOnboardingState from 'www-cabinet/react/dataDomain/Onboarding/selectors/getOnboardingState';
import dayjs from 'auto-core/dayjs';

jest.mock('auto-core/react/lib/user/canRead', () => () => true);

describe('routeName === calculator', () => {
    it('должен вернуть состояние AUCTION', () => {
        expect(getOnboardingState({
            config: {
                routeName: 'calculator',
            },
            cookies: {},
        })).toBe('auction');
    });
});

describe('routeName === sales', () => {
    it('должен вернуть состояние HOME_TOOLTIP', () => {
        expect(getOnboardingState({
            config: {
                routeName: 'sales',
                data: {
                    experimentsData: {
                        experiments: {
                            'AUTORUFRONT-21145-a': true,
                        },
                    },
                },
            },
            cookies: {
                is_showing_onboarding_offer: '2022-02-11T09:59:23+03:00',
                is_showing_onboarding_stats: '2022-02-11T09:59:23+03:00',
                is_showing_onboarding_notification: '1',
                is_showing_onboarding_home_tooltip: '0',
            },
        })).toBe('home_tooltip');
    });

    it('должен вернуть состояние OFFER', () => {
        expect(getOnboardingState({
            config: {
                routeName: 'sales',
                data: {
                    experimentsData: {
                        experiments: {
                            'AUTORUFRONT-21145-a': true,
                        },
                    },
                },
            },
            cookies: {
                is_showing_onboarding_home_tooltip: '0',
            },
        })).toBe('offer');
    });

    it('должен вернуть состояние STATS', () => {
        expect(getOnboardingState({
            config: {
                routeName: 'sales',
                data: {
                    experimentsData: {
                        experiments: {
                            'AUTORUFRONT-21145-a': true,
                        },
                    },
                },
            },
            cookies: {
                is_showing_onboarding_offer: '1',
            },
        })).toBe('stats');
    });

    it('должен вернуть состояние ARCHIVE', () => {
        expect(getOnboardingState({
            config: {
                routeName: 'sales',
                data: {
                    experimentsData: {
                        experiments: {
                            'AUTORUFRONT-21145-a': true,
                        },
                    },
                },
            },
            cookies: {
                is_showing_onboarding_offer: '1',
                is_showing_onboarding_stats: '1',
            },
        })).toBe('archive');
    });

    it('должен вернуть состояние MASS_ACTION', () => {
        expect(getOnboardingState({
            config: {
                routeName: 'sales',
            },
            cookies: {
                is_showing_onboarding_offer: dayjs().add(-20, 'minute'),
                is_showing_onboarding_stats: dayjs().add(-20, 'minute'),
                is_showing_onboarding_home_tooltip: '1',
                is_showing_onboarding_archive: dayjs().add(-1, 'day').format('YYYY-MM-DD'),
            },
        })).toBe('mass_action');
    });

    it('должен вернуть состояние FILTERS', () => {
        expect(getOnboardingState({
            config: {
                routeName: 'sales',
            },
            cookies: {
                is_showing_onboarding_offer: dayjs().add(-20, 'minute'),
                is_showing_onboarding_stats: dayjs().add(-20, 'minute'),
                is_showing_onboarding_archive: dayjs().add(-2, 'day').format('YYYY-MM-DD'),
                is_showing_onboarding_mass_action: dayjs().add(-1, 'day').format('YYYY-MM-DD'),
            },
        })).toBe('filters');
    });
});

describe('для всех остальных routeName', () => {
    it('должен вернуть состояние PRICE_REPORT_SIDEBAR', () => {
        expect(getOnboardingState({
            config: {
                routeName: 'other',
                data: {
                    experimentsData: {
                        experiments: {},
                    },
                },
            },
            state: {
                activeOfferCounters: {
                    all: {
                        count: 20,
                    },
                },
            },
            cookies: {
            },
        })).toBe('price_report_sidebar');
    });
});
