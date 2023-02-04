import {
    BorrowerCategoryTypes,
    FlatOrApartmentTypes,
    FlatTypes,
    IMortgageProgram,
    IncomeConfirmationTypes,
    MortgageTypes,
    NationalityTypes,
    PayTypes,
    ProvisionTypes,
} from 'realty-core/types/mortgage/mortgageProgram';

export const firstProgram: IMortgageProgram = {
    id: 2390797,
    bank: {
        id: '323579',
        name: 'Банк Открытие',
    },
    programName: 'Семейная ипотека',
    flatType: [FlatTypes.NEW_FLAT],
    flatOrApartment: [FlatOrApartmentTypes.FLAT],
    mortgageType: MortgageTypes.STATE_SUPPORT,
    requirements: {
        incomeConfirmation: [IncomeConfirmationTypes.WITHOUT_PROOF],
    },
    creditParams: {
        minRate: 4.7,
        minDownPayment: 15.0,
        minAmount: '500000',
        maxAmount: '12000000',
        minPeriodYears: 3,
        maxPeriodYears: 30,
        payType: PayTypes.ANNUITY,
    },
    monthlyPayment: '27271',
    partnerIntegrationType: [],
};

export const secondProgram: IMortgageProgram = {
    id: 2390797,
    bank: {
        id: '323579',
        name: 'АТБ',
    },
    programName: 'Семейная ипотека',
    flatType: [FlatTypes.NEW_FLAT, FlatTypes.SECONDARY],
    flatOrApartment: [FlatOrApartmentTypes.FLAT, FlatOrApartmentTypes.APARTMENT],
    mortgageType: MortgageTypes.STATE_SUPPORT,
    maternityCapital: true,
    requirements: {
        incomeConfirmation: [
            IncomeConfirmationTypes.PFR,
            IncomeConfirmationTypes.REFERENCE_2NDFL,
            IncomeConfirmationTypes.BANK_REFERENCE,
        ],
        borrowerCategory: [
            BorrowerCategoryTypes.BUSINESS_OWNER,
            BorrowerCategoryTypes.INDIVIDUAL_ENTREPRENEUR,
            BorrowerCategoryTypes.EMPLOYEE,
        ],
        minAge: 21,
        maxAge: 70,
        minExperienceMonths: 4,
        totalExperienceMonths: 12,
        requirements: [
            'Износ здания, в котором расположена квартира — не более 65%',
            'Не состоит в планах на снос, нет в списке по программе реновации.',
            'Подключен к канализационной сети и системе водоснабжения.',
            'Без незарегистрированных перепланировок и переоборудований.',
        ],
        documents: [
            'Отчет об оценке предмета ипотеки;',
            'Правоустанавливающие и правоподтверждающие документы на объект недвижимости (договор купли-продажи,' +
                ' мены, дарения и т. п., свидетельство о государственной регистрации права собственности на ' +
                'недвижимость (при наличии) или выписка из ЕГРН);',
            'Кадастровый или технический паспорт/план (техническая документация).',
        ],
        nationality: [NationalityTypes.RF, NationalityTypes.FOREIGNER],
    },
    creditParams: {
        minRate: 4.7,
        minRateWithDiscount: 4.3,
        discountRate: 0.4,
        rateDescription: ['Ставка 4.3% годовых действует при выполнении условий.'],
        increasingFactor: [
            {
                factor: 'Простая ипотека',
                rate: 0.3,
            },
            {
                factor: 'Отказ от личного/титульного страхования',
                rate: 1.0,
            },
            {
                factor: 'Заемщик ИП или владелец бизнеса',
                rate: 0.5,
            },
            {
                factor: 'первоначальный взнос менее 20%',
                rate: 0.75,
            },
        ],
        reducingFactor: [
            {
                factor: 'Ключевой партнёр',
                rate: 0.7,
            },
            {
                factor: 'Заемщик приобретает квартиру у партнеров Банка',
                rate: 0.4,
            },
            {
                factor: 'Площадь недвижимости свыше 65 м²',
                rate: 0.2,
            },
            {
                factor: 'Акция «Бери больше - плати меньше»',
                rate: 0.2,
            },
            {
                factor: 'Быстрый выход на сделку',
                rate: 0.2,
            },
            {
                factor: 'Пользователь «Программы Статус»',
                rate: 0.4,
            },
            {
                factor: 'Зарплатный счет в АТБ (ПАО)',
                rate: 0.4,
            },
            {
                factor: 'При сумме кредита выше 4 000 000',
                rate: 0.2,
            },
            {
                factor: 'Сотрудник бюджетной сферы',
                rate: 0.4,
            },
        ],
        minDownPayment: 15.0,
        minAmount: '500000',
        maxAmount: '12000000',
        minPeriodYears: 3,
        maxPeriodYears: 30,
        payType: PayTypes.DIFFERENTIATED,
        provisionType: ProvisionTypes.PURCHASED,
        solutionPeriodMonths: 1.5,
        specialCondition: [
            'Базовая ставка банка для некоторых видов кредитов будет снижена на 0,1%.',
            'Оформите заявку на ипотеку на нашем сервисе и получите более выгодные условия.',
        ],
        additionalDescription: [
            'Для оплаты первоначального взноса можно использовать материнский капитал. Но не менее 10% всей ' +
                'стоимости недвижимости нужно внести своими деньгами.',
            'Предлагаем дополнительные привилегии для зарплатных клиентов.',
            'Также уменьшаем кредитную ставку при быстром выходе на сделку и условии заключения договора ' +
                'комплексного страхования.',
        ],
    },
    monthlyPayment: '27271',
    partnerIntegrationType: [],
};
