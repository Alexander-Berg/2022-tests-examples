module.exports = {
    mortgageCalculator: {
        calculator: {
            creditAmount: 123,
            monthlyPayment: 123,
            monthlyPaymentParams: {
                propertyCost: 123,
                downPaymentSum: 123,
                periodYears: 123,
                rate: 123
            },
            calculatorLimits: {
                minPropertyCost: 123,
                maxPropertyCost: 123,
                minDownPayment: 123,
                maxDownPayment: 123,
                minDownPaymentSum: 123,
                maxDownPaymentSum: 123,
                minPeriodYears: 123,
                maxPeriodYears: 123,
                minCreditAmount: 123,
                maxCreditAmount: 123,
                minRate: 123,
                maxRate: 123
            },
            queryId: '123'
        },
        programs: {
            items: {
                id: '2420173',
                bank: {
                    id: '358023',
                    name: 'Альфа-Банк',
                    genitiveName: 'Альфа-Банка',
                    prepositionalName: 'Альфа-Банке',
                    legalName: 'АО «Альфа-Банк»',
                    logo: '//avatars.mds.yandex.net/get-verba/216201/2a0000016cb04a09bb8d84d276bc2531af2f/optimize',
                    color: '#dc4433',
                    licenseNumber: '1326',
                    licenseDate: '2015-01-16',
                    headOfficeAddress: 'Москва, ул. Каланчевская, 27',
                    croppedLogo:
                            '//avatars.mds.yandex.net/get-verba/997355/2a000001773ff0a75cf42b493228f4d60789/optimize',
                    blackLogo:
                            '//avatars.mds.yandex.net/get-verba/787013/2a000001786dfc6f2bc36a044f0e8d43981a/optimize'
                },
                programName: 'Ипотека на строящееся жильё',
                flatType: [ 'NEW_FLAT' ],
                flatOrApartment: [ 'APARTMENT', 'FLAT' ],
                mortgageType: 'STANDARD',
                maternityCapital: true,
                requirements: {
                    incomeConfirmation: [ 'PFR', 'REFERENCE_2NDFL', 'BANK_REFERENCE' ],
                    borrowerCategory: [ 'BUSINESS_OWNER', 'INDIVIDUAL_ENTREPRENEUR', 'EMPLOYEE' ],
                    minAge: 21,
                    maxAge: 70,
                    minExperienceMonths: 4,
                    totalExperienceMonths: 12,
                    requirements: [
                        'Износ здания, в котором расположена квартира — не более 65%.',
                        'Не состоит в планах на снос, нет в списке по программе реновации.',
                        'Подключен к канализационной сети и системе водоснабжения.',
                        'Без незарегистрированных перепланировок и переоборудований.'
                    ],
                    nationality: [ 'RF', 'FOREIGNER' ]
                },
                creditParams: {
                    minRate: 8.79,
                    rateDescription: [
                        // eslint-disable-next-line max-len
                        'Базовая процентная ставка по ипотеке на строящееся жилье  — 8,79% / 8,19% на крупные суммы кредита (при сумме кредита от 6 млн ₽ в Москве и МО, в Санкт-Петербурге и ЛО и в других регионах РФ). Ставка за крупный чек от 6 млн ₽ для всех регионов - 8,19%.'
                    ],
                    increasingFactor: [
                        {
                            factor: 'Первоначальный взнос менее 20%',
                            rate: 0.5
                        },
                        {
                            factor: 'Отказ от страхования жизни и титула',
                            rate: 2
                        },
                        {
                            factor: 'Отказ от страхования жизни',
                            rate: 2
                        },
                        {
                            factor: 'Ипотека для ИП и собственников бизнеса',
                            rate: 0.5
                        }
                    ],
                    reducingFactor: [
                        {
                            // eslint-disable-next-line max-len
                            factor: 'При подписании Кредитного договора в течение 33 календарный дней с даты одобрения',
                            rate: 0.2
                        },
                        {
                            factor: 'Зарплатным клиентам Альфа-Банка и Клиентам A-Private',
                            rate: 0.4
                        },
                        {
                            factor: 'Покупка недвижимости у Exclusive-партнера Альфа-Банка',
                            rate: 0.4
                        },
                        {
                            factor: 'Покупка недвижимости у ключевого партнера Альфа-Банка',
                            rate: 0.3
                        }
                    ],
                    minDownPayment: 10,
                    minAmount: '600000',
                    maxAmount: '50000000',
                    minPeriodYears: 3,
                    maxPeriodYears: 30,
                    payType: 'ANNUITY',
                    provisionType: 'PURCHASED',
                    solutionPeriodMonths: 3,
                    discountRate: 0.4,
                    minRateWithDiscount: 8.39,
                    specialCondition: [
                        // eslint-disable-next-line max-len
                        'Оформите заявку на ипотеку на Яндекс.Недвижимости и получите более выгодные условия. Базовая ставка банка будет снижена на 0,4%.'
                    ],
                    additionalDescription: [
                        // eslint-disable-next-line max-len
                        'Для оплаты первоначального взноса можно использовать материнский капитал. Но не менее 10% всей стоимости недвижимости нужно внести своими деньгами.',
                        'Предлагаем дополнительные привилегии для зарплатных клиентов.'
                    ]
                },
                monthlyPayment: '20544',
                partnerIntegrationType: [ 'ALFABANK_FRAME_FORM' ],
                url:
                        'https://realty.test.vertis.yandex.ru/ipoteka/alfa-bank-358023/ipoteka-na-stroyashcheesya-zhilyo-2420173/',
                descriptionUrl: 'https://alfabank.ru/get-money/mortgage/complete_house/',
                trackingUrl: '12345'
            },
            pager: {
                totalItems: 123,
                page: 123,
                pageSize: 123
            },
            calculatorLimits: {
                minPropertyCost: 123,
                maxPropertyCost: 123,
                minDownPayment: 123,
                maxDownPayment: 123,
                minDownPaymentSum: 123,
                maxDownPaymentSum: 123,
                minPeriodYears: 123,
                maxPeriodYears: 30,
                minCreditAmount: 123,
                maxCreditAmount: 123,
                minRate: 123,
                maxRate: 123
            },
            queryId: '123',
            fallback: false,
            bankCount: 100,
            minRate: 13
        },
        isMortgageProgramsLoading: false,
        isMortgageProgramsError: false,
        isMoreMortgageProgramsLoading: false,
        offersRequestStatus: 'LOADED'
    },
    cards: {
        sites: {
            locativeFullName: 'в жилом комплексе «Люблинский парк»'
        }
    },
    routing: {
        currentRoute: '123',
        prevRoute: null,
        locationBeforeTransitions: {
            search: '123',
            pathname: '123',
            query: {},
            action: 'POP'
        }
    },
    cookies: {},
    config: {
        hostname: '123',
        traceId: '123',
        branch: '123',
        canary: '123',
        version: '123',
        hasDevTools: false,
        trafficFrom: '123',
        rootUrl: '123',
        serverTimeStamp: 123,
        timeDelta: 123,
        view: 'desktop',
        avatarsHost: '123',
        constants: {
            PIK_PARTNER_ID: '123'
        },
        origin: '123',
        passportApiHost: '123',
        passportHost: '123',
        retpath: '123',
        env: '123',
        yaArendaAgencyId: 123,
        yaArendaUrl: '123',
        domains: {
            desktop: '123',
            'touch-phone': '123'
        },
        realtyUrl: '123',
        creamUrl: '123',
        adAliasName: '123',
        cspNonce: '123',
        partnerUrl: '123',
        moderationServiceHost: '123'
    },
    loader: {},
    page: {
        queryId: '123',
        name: 'newbuilding-mortgage',
        route: '123',
        isFailed: false,
        isLoading: false,
        isLocked: false
    },
    user: {
        yuid: '123',
        isAuth: false,
        isJuridical: false,
        userType: 'OWNER',
        comparison: [],
        favoritesMap: {},
        promoSubscription: {},
        avatarHost: '123',
        paymentTypeSuffix: 'natural',
        isVosUser: false
    },
    geo: {
        name: '123',
        locative: '123',
        rgid: 123,
        rgidMsk: 123,
        rgidMO: 123,
        rgidSpb: 123,
        rgidLO: 123,
        id: 123,
        type: 'SUBJECT_FEDERATION',
        parents: [],
        isMsk: false,
        isSpb: false,
        isInMO: false,
        isInLO: false,
        isInSverdObl: false,
        isInNovosibirskObl: false,
        latitude: 123,
        longitude: 123,
        zoom: 123,
        country: 123,
        hasVillages: false,
        hasCommercialBuildings: false,
        hasConcierge: false,
        hasYandexRent: false,
        searchFilters: {},
        refinements: []
    },
    siteSpecialProject: null,
    siteSpecialProjectSecondPackage: null,
    header: {
        promoblocks: {}
    },
    ads: {
        code: '123',
        blocks: {},
        direct: {},
        server: {}
    }
};
