const salesState = require('./salesState');

const DEFAULT_DATA = {
    sales: {},
    cookie: {},
    getCurrentUser: { user: {} },
};

it('вернет vasPromoPopup as is', () => {
    const initialData = {
        ...DEFAULT_DATA,
        getVasPromoPopup: { as: 'is' },
    };

    expect(salesState(initialData).vasPromoPopup).toEqual({ as: 'is' });
});

describe('no-photos', () => {
    it('НЕ вернет если есть getVasPromoPopup и нет куки о его закрытии', () => {
        const initialData = {
            ...DEFAULT_DATA,
            sales: { offers: [ {}, {} ] },
            getVasPromoPopup: {},
        };

        expect(salesState(initialData).promoPopup?.name).not.toEqual('no-photos');
    });

    it('НЕ вернет если нет офферов с photo_add_url', () => {
        const initialData = {
            ...DEFAULT_DATA,
            sales: { offers: [ {}, {} ] },
            getVasPromoPopup: {},
            cookie: { 'discount-popup': 'closed' },
        };

        expect(salesState(initialData).promoPopup?.name).not.toEqual('no-photos');
    });

    it('НЕ вернет если показывается баннер reseller-public-profile-promo', () => {
        const initialData = {
            ...DEFAULT_DATA,
            sales: { offers: [
                { id: '1', hash: '1', additional_info: { photo_add_url: 'url' } },
                { id: '2', hash: '2', additional_info: { photo_add_url: 'url' } },
            ] },
            getCurrentUser: {
                encrypted_user_id: 'some_encrypted_user_id',
                user: { moderation_status: { reseller: true } },
                profile: { autoru: { allow_offers_show: false } },
            },
            bunker: { 'common/reseller_public_profile_onboarding': { isFeatureEnabled: true } },
        };
        const bunker = { 'common/reseller_public_profile_onboarding': { isFeatureEnabled: true } };

        expect(salesState(initialData, bunker).promoPopup?.name).not.toEqual('no-photos');
    });

    it('вернет если нет getVasPromoPopup и есть оффер с photo_add_url', () => {
        const initialData = {
            ...DEFAULT_DATA,
            sales: { offers: [
                { id: '1', hash: '1', additional_info: { photo_add_url: 'url' } },
                { id: '2', hash: '2', additional_info: { photo_add_url: 'url' } },
            ] },
        };

        expect(salesState(initialData).promoPopup?.name).toEqual('no-photos');
    });

    it('вернет если есть getVasPromoPopup, кука о его закрытии и есть оффер с photo_add_url', () => {
        const initialData = {
            ...DEFAULT_DATA,
            sales: { offers: [
                { id: '1', hash: '1', additional_info: { photo_add_url: 'url' } },
                { id: '2', hash: '2', additional_info: { photo_add_url: 'url' } },
            ] },
            getVasPromoPopup: {},
            cookie: { 'discount-popup': 'closed' },
        };

        expect(salesState(initialData).promoPopup?.name).toEqual('no-photos');
    });
});

describe('reseller-public-profile-promo', () => {
    const defaultUser = {
        encrypted_user_id: 'some_encrypted_user_id',
        user: { moderation_status: { reseller: true } },
        profile: { autoru: { allow_offers_show: false } },
    };
    const bunker = { 'common/reseller_public_profile_onboarding': { isFeatureEnabled: true } };

    const initialData = {
        ...DEFAULT_DATA,
        sales: { offers: [
            { id: '1', hash: '1', additional_info: { photo_add_url: 'url' } },
            { id: '2', hash: '2', additional_info: { photo_add_url: 'url' } },
        ] },
        getCurrentUser: defaultUser,
    };

    it('вернет, если перекуп, есть encrypted_user_id, профиль ещё не открыт, кука ещё не проставлена', () => {
        expect(salesState(initialData, bunker).promoPopup?.name).toEqual('reseller-public-profile-promo');
    });

    it('не вернет, если фича выключена', () => {
        expect(salesState(initialData).promoPopup?.name).not.toEqual('reseller-public-profile-promo');
    });

    it('не вернет, если нет user', () => {
        const data = {
            ...initialData,
            getCurrentUser: {
                ...initialData.getCurrentUser,
                user: undefined,
            },
        };
        expect(salesState(data, bunker).promoPopup?.name).not.toEqual('reseller-public-profile-promo');
    });

    it('не вернет, если не перекуп', () => {
        const data = {
            ...initialData,
            getCurrentUser: {
                ...initialData.getCurrentUser,
                user: { moderation_status: { reseller: false } },
            },
        };
        expect(salesState(data, bunker).promoPopup?.name).not.toEqual('reseller-public-profile-promo');
    });

    it('не вернет, если нет encrypted_user_id', () => {
        const data = {
            ...initialData,
            getCurrentUser: {
                ...initialData.getCurrentUser,
                encrypted_user_id: undefined,
            },
        };
        expect(salesState(data, bunker).promoPopup?.name).not.toEqual('reseller-public-profile-promo');
    });

    it('не вернет, если профиль уже открыт', () => {
        const data = {
            ...initialData,
            getCurrentUser: {
                ...initialData.getCurrentUser,
                profile: { autoru: { allow_offers_show: true } },
            },
        };
        expect(salesState(data, bunker).promoPopup?.name).not.toEqual('reseller-public-profile-promo');
    });

    it('не вернет, если кука проставлена', () => {
        const data = {
            ...initialData,
            cookie: { 'reseller-public-profile-popup-shown': 'true' },
        };
        expect(salesState(data, bunker).promoPopup?.name).not.toEqual('reseller-public-profile-promo');
    });
});
