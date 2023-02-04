import { DeepPartial } from 'utility-types';
import { ContractTermsInfo } from '@vertis/schema-registry/ts-types/realty/rent/api/moderation';

import { UserId } from 'types/user';

import { IUniversalStore } from 'view/modules/types';

const term: ContractTermsInfo = {
    version: 4,
    contractTermsUrl: 'https://yandex.ru/legal/realty_lease_tenant/',
    isPaymentSchemaChanged: true,
    publicationDate: '2022-02-03T00:00:00Z',
    effectiveDate: '2022-02-03T00:00:00Z',
    isSigningRequired: true,
    comment: 'Что-то изменили в соглашениях',
};

export const newUser: DeepPartial<IUniversalStore> = {
    managerUser: {
        user: { userId: '1' as UserId },
    },
    managerUserTerms: { data: { lastTerms: { terms: term } } },
    spa: {},
};

export const acceptedActualTerms: DeepPartial<IUniversalStore> = {
    managerUser: {
        user: { userId: '1' as UserId },
        tenantAgreementDate: '2021-03-20T21:00:00Z',
    },
    managerUserTerms: { data: { acceptedTerms: term, paymentChangesTerms: term } },
    spa: {},
};

export const differentPaymentAndAcceptedTerms: DeepPartial<IUniversalStore> = {
    managerUser: {
        user: { userId: '1' as UserId },
        tenantAgreementDate: '2021-03-20T21:00:00Z',
    },
    managerUserTerms: { data: { acceptedTerms: term, paymentChangesTerms: { ...term, version: 2 } } },
    spa: {},
};

export const acceptedNotActualTerms: DeepPartial<IUniversalStore> = {
    managerUser: {
        user: { userId: '1' as UserId },
        tenantAgreementDate: '2021-03-20T21:00:00Z',
    },
    managerUserTerms: {
        data: { lastTerms: { terms: { ...term, version: 5 } }, acceptedTerms: term, paymentChangesTerms: term },
    },
    spa: {},
};
