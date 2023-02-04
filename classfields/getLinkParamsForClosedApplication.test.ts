import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import type { C2bApplication } from 'auto-core/server/blocks/c2bAuction/types';

import { getLinkParamsForClosedApplication } from './getLinkParamsForClosedApplication';

describe('Формирование ссылки для редиректа после выхода из Выкупа', () => {
    it('Возвращает пустоту, если заявка создана не из оффера/черновика', () => {
        expect(getLinkParamsForClosedApplication({} as C2bApplication)).toBeUndefined();
    });

    it('Возвращает параметры для редиректа в форму создания черновика, если заявка создана из черновика', () => {
        const application = {
            offer: {
                category: 'cars',
                section: 'used',
                status: OfferStatus.DRAFT,
            },
        } as unknown as C2bApplication;

        const linkParams = {
            category: 'cars',
            section: 'used',
            form_type: 'add',
        };

        expect(getLinkParamsForClosedApplication(application)).toEqual(linkParams);
    });

    it('Возвращает параметры для редиректа в форму редактирования оффера, если заявка создана из оффера', () => {
        const application = {
            offer: {
                category: 'cars',
                section: 'used',
                status: OfferStatus.ACTIVE,
                id: '123456-abcd',
            },
        } as unknown as C2bApplication;

        const linkParams = {
            category: 'cars',
            section: 'used',
            form_type: 'edit',
            sale_id: '123456',
            sale_hash: 'abcd',
        };

        expect(getLinkParamsForClosedApplication(application)).toEqual(linkParams);
    });

    it('Фоллбэк на категорию и секцию', () => {
        const application = {
            offer: {
                status: OfferStatus.DRAFT,
            },
        } as unknown as C2bApplication;

        const linkParams = {
            category: 'cars',
            section: 'used',
            form_type: 'add',
        };

        expect(getLinkParamsForClosedApplication(application)).toEqual(linkParams);
    });

    it('Считает оффером все, что не черновик', () => {
        const statuses = [
            OfferStatus.ACTIVE,
            OfferStatus.BANNED,
            OfferStatus.EXPIRED,
            OfferStatus.INACTIVE,
            OfferStatus.NEED_ACTIVATION,
            OfferStatus.REMOVED,
            OfferStatus.STATUS_UNKNOWN,
            OfferStatus.UNRECOGNIZED,
        ];
        const applications = statuses.map((status) => ({
            offer: {
                category: 'cars',
                section: 'used',
                status,
                id: '123456-abcd',
            },
        } as unknown as C2bApplication));

        const linkParams = {
            category: 'cars',
            section: 'used',
            form_type: 'edit',
            sale_id: '123456',
            sale_hash: 'abcd',
        };

        applications.forEach((application) => {
            expect(getLinkParamsForClosedApplication(application)).toEqual(linkParams);
        });
    });
});
