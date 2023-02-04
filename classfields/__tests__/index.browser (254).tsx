import React from 'react';
import noop from 'lodash/noop';
import omit from 'lodash/omit';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageAliases, generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { AuthorCategoryTypes, IOfferCard, OfferType, OfferCategory } from 'realty-core/types/offerCard';
import { UserTypes } from 'realty-core/types/common';
import { IProfileCard } from 'realty-core/types/profileCard';

import { OfferCardAuthor } from '../';

advanceTo(new Date('2020-06-01T03:00:00.111Z'));

const renderOptions = { viewport: { width: 375, height: 320 } };

const author = {
    id: '4030469986',
    agentName: 'Шумов Борис Валериевич',
    category: 'AGENCY',
    organization: 'ООО "Лимонад"',
    creationDate: '2019-04-01T03:00:00.111Z',
};

const offer = ({
    offerType: OfferType.SELL,
    offerCategory: OfferCategory.APARTMENT,
    offerStatus: 'active',
    offerId: '3584237142849818368',
    price: {
        currency: 'RUR',
        period: 'PER_MONTH',
        value: 6777888,
        valuePerPart: 101163,
        unitPerPart: 'SQUARE_METER',
    },
    author,
    location: {
        rgid: 176337,
    },
    uid: '4030469986',
} as unknown) as IOfferCard;

const onlineShowData: Pick<IOfferCard, 'remoteReview'> = {
    remoteReview: {
        onlineShow: true,
        youtubeVideoReviewUrl: '',
        virtualTour: {
            modelUrl: '',
            previewUrl: '',
        },
    },
};

const profileImages = generateImageAliases({ width: 140, height: 80 });

const defaultProps = {
    visible: true,
    link: noop,
    rgid: 0,
    avatarHost: '',
};

const Component = ({ offer: offerProp }: { offer: Partial<IOfferCard> }) => (
    <AppProvider
        initialState={{
            user: {
                avatarHost: '123',
            },
            config: {
                yaArendaUrl: 'https://arenda.realty.test.vertis.yandex.ru/',
            },
        }}
        context={{ link: () => 'link' }}
    >
        <OfferCardAuthor {...defaultProps} offer={offerProp as IOfferCard} />
    </AppProvider>
);

describe('OfferCardAuthor', () => {
    describe('собственник', () => {
        it('без пессимизации, НЕ проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: { ...author, photo: generateImageUrl(), category: AuthorCategoryTypes.OWNER },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('без пессимизации, НЕ проверенный юзер, есть онлай показ', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        ...onlineShowData,
                        author: { ...author, photo: generateImageUrl(), category: AuthorCategoryTypes.OWNER },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('без пессимизации, проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        trustedOfferInfo: {
                            isFullTrustedOwner: true,
                            isCadastrPersonMatched: true,
                            ownerTrustedStatus: '',
                        },
                        author: { ...author, photo: generateImageUrl(), category: AuthorCategoryTypes.OWNER },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('с пессимизации, НЕ проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        socialPessimization: true,
                        author: { ...author, photo: generateImageUrl(), category: AuthorCategoryTypes.OWNER },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('с пессимизации, проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        socialPessimization: true,
                        trustedOfferInfo: {
                            isFullTrustedOwner: true,
                            isCadastrPersonMatched: true,
                            ownerTrustedStatus: '',
                        },
                        author: { ...author, photo: generateImageUrl(), category: AuthorCategoryTypes.OWNER },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('агент', () => {
        it('карточка агента', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: { ...omit(author, 'organization'), category: AuthorCategoryTypes.AGENT },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка агента с онлайн показом', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        ...onlineShowData,
                        author: { ...omit(author, 'organization'), category: AuthorCategoryTypes.AGENT },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка агента с одним именем и названием организации', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: { ...author, agentName: author.organization, category: AuthorCategoryTypes.AGENT },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('картчка агента с длинным именем', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: {
                            ...author,
                            category: AuthorCategoryTypes.AGENT,
                            agentName: 'АИЙИЛЬЦИКЛИКИРМИЦИБАЙРАКТАЗИЙАНКАГРАМАНОГЛУ Круциатус',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка агента с длинным названием организации', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: {
                            ...author,
                            category: AuthorCategoryTypes.AGENCY,
                            organization: 'в четверг четвертого числа в четыре с четвертью часа',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка агента с профилем', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: {
                            ...author,
                            profile: ({
                                userType: UserTypes.AGENT,
                                name: 'Сергей Мавроди',
                                logo: profileImages,
                            } as unknown) as IProfileCard,
                            category: AuthorCategoryTypes.AGENT,
                            agentName: 'АИЙИЛЬЦИКЛИКИРМИЦИБАЙРАКТАЗИЙАНКАГРАМАНОГЛУ Круциатус',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('агенство', () => {
        it('карточка агентсва', async () => {
            await render(<Component offer={offer} />, renderOptions);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка агенства с профилем', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: {
                            ...author,
                            profile: ({
                                userType: UserTypes.AGENCY,
                                name: 'Пупкино и Ко',
                                logo: profileImages,
                            } as unknown) as IProfileCard,
                            category: AuthorCategoryTypes.AGENCY,
                            agentName: 'АИЙИЛЬЦИКЛИКИРМИЦИБАЙРАКТАЗИЙАНКАГРАМАНОГЛУ Круциатус',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('частный агент', () => {
        it('карточка агентсва', async () => {
            await render(<Component offer={offer} />, renderOptions);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка частного агента', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: {
                            ...author,
                            category: AuthorCategoryTypes.PRIVATE_AGENT,
                            agentName: 'Ибрашка',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('сервис яндекса', () => {
        it('яндекс аренда', async () => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        yandexRent: true,
                        author: {
                            ...author,
                            category: AuthorCategoryTypes.AGENCY,
                            photo: generateImageUrl(),
                            agentName: 'Яндекс.Аренда',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
