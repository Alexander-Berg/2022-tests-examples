import creditApplicationMockchain from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import personProfileMock from 'auto-core/react/dataDomain/credit/mocks/personProfile.mock';

import { NotEmployedReason, PhoneType } from 'auto-core/types/TPersonProfile';
import type { PersonProfile } from 'auto-core/types/TPersonProfile';
import type { CreditApplication } from 'auto-core/types/TCreditBroker';

import {
    sharkAdditionalNotRequiredBlocks,
    sharkBlocksAllowedToView,
} from 'www-callmen/react/preparers/sharkAdditionalNotRequiredBlocks';
import secureDataCleaners from 'www-callmen/react/preparers/secureDataCleaners';

import prepareCreditApplicationForSecureUsing from './prepareCreditApplicationForSecureUsing';

it('оставляет только необходимые поля', () => {
    const mock = creditApplicationMockchain()
        .withInfo({
            okb_statement_agreement: {
                is_agree: true,
                timestamp: '123',
            },
            advert_statement_agreement: {
                is_agree: true,
                timestamp: '123',
            },
        })
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock);

    const fieldList: Array<keyof CreditApplication> = [
        'borrower_person_profile',
        'id',
        'payload',
        'requirements',
        'state',
        'claims',
        'created',
        'updated',
        'user_id',
        'info',
    ];

    expect(Object.keys(result)).toEqual(fieldList);

    fieldList.forEach((fieldName) => {
        expect(fieldName in result).toEqual(true);
    });
});

it('удаляет  контрольное слово', () => {
    const mock = creditApplicationMockchain()
        .withInfo({
            okb_statement_agreement: {
                is_agree: true,
                timestamp: '123',
            },
            advert_statement_agreement: {
                is_agree: true,
                timestamp: '123',
            },
            control_word: {
                word: 'control secret!!!!',
            },
        })
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock);

    expect(result && result.info && Object.keys(result.info)).toEqual([
        'advert_statement_agreement',
        'okb_statement_agreement',
    ]);
});

it('удаляет незаконное из профиля', () => {
    const mock = creditApplicationMockchain()
        .withProfile(personProfileMock)
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock);
    const allowedFields = Object.values(sharkAdditionalNotRequiredBlocks).concat('block_types');

    expect(Object.keys(result.borrower_person_profile || {})).toEqual(allowedFields);

    const personProfile: PersonProfile | void = mock.borrower_person_profile;

    (allowedFields as unknown as Array<keyof PersonProfile>).forEach(fieldName => {
        expect(result.borrower_person_profile && result.borrower_person_profile[fieldName]).toEqual(personProfile && personProfile[fieldName]);
    });
});

it('удаляет незаконное из профиля, оставляя дополнительное дозволенное, обработанное клеанерами', () => {
    const mock = creditApplicationMockchain()
        .withProfile(personProfileMock)
        .value();

    const resultMock = creditApplicationMockchain()
        .withProfile({
            ...personProfileMock,
            employment: {
                employed: {
                    // nак надо, ибо мок результата чистилки
                    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                    // @ts-ignore
                    employee: {},
                },
            },
        })
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock, true);
    const allowedFields = Object.values(sharkAdditionalNotRequiredBlocks)
        .concat(Object.values(sharkBlocksAllowedToView))
        .concat(Object.keys(secureDataCleaners))
        .concat('block_types');

    expect(Object.keys(result.borrower_person_profile || {})).toEqual(allowedFields);

    const personProfile: PersonProfile | void = resultMock.borrower_person_profile;

    (allowedFields as unknown as Array<keyof PersonProfile>).forEach(fieldName => {
        expect(result.borrower_person_profile && result.borrower_person_profile[fieldName]).toEqual(personProfile && personProfile[fieldName]);
    });
});

it('добавляет блок INCOME в профиль, для НЕ работающего', () => {
    const mock = creditApplicationMockchain()
        .withProfile({
            employment: {
                not_employed: {
                    reason: NotEmployedReason.SPOUSE,
                    other_reason: '',
                },
            },
        })
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock);

    expect(result?.borrower_person_profile?.block_types).toContain('INCOME');
});

it('добавляет блок RELATED_PERSONS в профиль, если есть ADDITIONAL телефон', () => {
    const mock = creditApplicationMockchain()
        .withProfile({
            phones: {
                phone_entities: [
                    {
                        phone: '8276tr123912',
                        phone_type: PhoneType.ADDITIONAL,
                    },
                ],
            },
        })
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock);

    expect(result?.borrower_person_profile?.block_types).toContain('RELATED_PERSONS');
});

it('НЕ добавляет блок RELATED_PERSONS в профиль, если НЕТ ADDITIONAL телефона', () => {
    const mock = creditApplicationMockchain()
        .withProfile({
            phones: {
                phone_entities: [
                    {
                        phone: '8276tr123912',
                        phone_type: PhoneType.PERSONAL,
                    },
                ],
            },
        })
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock);

    expect(result?.borrower_person_profile?.block_types).not.toContain('RELATED_PERSONS');
});

it('добавляет виртуальный блок CONTROL_WORD в профиль, если указано контрольное слово', () => {
    const mock = creditApplicationMockchain()
        .withProfile({
            ...personProfileMock,
            block_types: [],
        })
        .withInfo({
            okb_statement_agreement: {
                is_agree: true,
                timestamp: '123',
            },
            advert_statement_agreement: {
                is_agree: true,
                timestamp: '123',
            },
            control_word: {
                word: 'control secret!!!!',
            },
        })
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock);

    expect(result?.borrower_person_profile?.block_types).toContain('CONTROL_WORD');
});

it('НЕ добавляет виртуальный блок CONTROL_WORD в профиль, если НЕ указано контрольное слово', () => {
    const mock = creditApplicationMockchain()
        .withProfile({
            ...personProfileMock,
            block_types: [],
        })
        .withInfo({
            okb_statement_agreement: {
                is_agree: true,
                timestamp: '123',
            },
            advert_statement_agreement: {
                is_agree: true,
                timestamp: '123',
            },
        })
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock);

    expect(result?.borrower_person_profile?.block_types).not.toContain('CONTROL_WORD');
});

it('добавляет блок EXPENSES в профиль, если указано "есть квартира"', () => {
    const mock = creditApplicationMockchain()
        .withProfile({
            ...personProfileMock,
            block_types: [],
            property_ownership: {
                has_property: true,
            },
        })
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock);

    expect(result?.borrower_person_profile?.block_types).toContain('EXPENSES');
});

it('НЕ добавляет блок EXPENSES в профиль, если НЕ указано "есть квартира"', () => {
    const mock = creditApplicationMockchain()
        .withProfile({
            ...personProfileMock,
            block_types: [],
            property_ownership: {
                has_property: false,
            },
        })
        .value();

    const result = prepareCreditApplicationForSecureUsing(mock);

    expect(result?.borrower_person_profile?.block_types).not.toContain('EXPENSES');
});
