import React from 'react';
import noop from 'lodash/noop';
import omit from 'lodash/omit';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageAliases, generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { AuthorImage } from 'realty-core/view/react/deskpad/components/author/AuthorImage';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { AuthorInfoPopup } from '../index';

advanceTo(new Date('2020-06-01T03:00:00.111Z'));

const renderOptions = { viewport: { width: 350, height: 450 } };

const author = {
    id: '4030469986',
    agentName: 'Шумов Борис Валериевич',
    category: 'AGENCY',
    organization: 'ООО "Лимонад"',
    creationDate: '2019-04-01T03:00:00.111Z'
};

const offer = {
    offerType: 'SELL',
    offerCategory: 'APARTMENT',
    offerStatus: 'active',
    offerId: '3584237142849818368',
    price: {
        currency: 'RUR',
        period: 'PER_MONTH',
        value: 6777888,
        valuePerPart: 101163,
        unitPerPart: 'SQUARE_METER',
        photo: '1',
        organization: '1'
    },
    author,
    location: {
        rgid: 176337
    },
    uid: '4030469986'
};

const onlineShowData = {
    remoteReview: {
        onlineShow: true
    }
};

const profileImages = generateImageAliases({ width: 200, height: 200 });

const defaultProps = {
    visible: true,
    link: noop,
    rgid: 0,
    avatarHost: ''
};

const Component = ({ offer: offerProp, isOwner }) => (
    <AppProvider
        initialState={{
            user: {
                avatarHost: '123'
            },
            config: {
                yaArendaUrl: 'https://arenda.realty.test.vertis.yandex.ru/'
            }
        }}
        context={{ link: () => 'link' }}
    >
        <AuthorInfoPopup {...defaultProps} offer={offerProp} isOwner={isOwner} authorImageComponent={AuthorImage} />
    </AppProvider>
);

describe('AuthorInfoPopup', () => {
    describe('собственник', () => {
        it('карточка оффера, без пессимизации, провереный юзер', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        trustedOfferInfo: {
                            isFullTrustedOwner: true,
                            isCadastrPersonMatched: true
                        },
                        author: { ...author, photo: generateImageUrl(), category: 'OWNER' }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка оффера, без пессимизации, не проверенный юзер', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: { ...author, photo: generateImageUrl(), category: 'OWNER' }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка оффера, без пессимизации, не проверенный юзер, c онлайн показом', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        ...onlineShowData,
                        author: { ...author, category: 'OWNER' }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка оффера, с пессимизацией, не проверенный юзер', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        socialPessimization: true,
                        author: { ...author, photo: generateImageUrl(), category: 'OWNER' }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка оффера, с пессимизацией, не проверенный юзер, но есть кадастр', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        socialPessimization: true,
                        trustedOfferInfo: {
                            isFullTrustedOwner: false,
                            isCadastrPersonMatched: true
                        },
                        author: { ...author, photo: generateImageUrl(), category: 'OWNER' }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка оффера, c пессимизацией, не проверенноый юзер, c онлайн показом', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        ...onlineShowData,
                        socialPessimization: true,
                        author: { ...author, category: 'OWNER' }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка автора, с пессимизацией, не проверенный юзер', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        socialPessimization: true,
                        author: { ...author, category: 'OWNER' }
                    }}
                    isOwner
                />,
                { viewport: { width: 450, height: 450 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка автора, без пессимизацией, не проверенный юзер', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        socialPessimization: false,
                        author: { ...author, category: 'OWNER' }
                    }}
                    isOwner
                />,
                { viewport: { width: 450, height: 450 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка автора, без пессимизацией, проверенный юзер', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        trustedOfferInfo: {
                            isFullTrustedOwner: true,
                            isCadastrPersonMatched: true
                        },
                        socialPessimization: false,
                        author: { ...author, category: 'OWNER' }
                    }}
                    isOwner
                />,
                { viewport: { width: 450, height: 450 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
    describe('агент', () => {
        it('карточка агента', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: { ...omit(author, 'organization'), category: 'AGENT' }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('coбственная карточка агента', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: { ...omit(author, 'organization'), category: 'AGENT' }
                    }}
                    isOwner
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка агента с онлайн показом', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        ...onlineShowData,
                        author: { ...omit(author, 'organization'), category: 'AGENT' }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка агента с одним именем и названием организации', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: { ...author, agentName: author.organization, category: 'AGENT' }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('картчка агента с длинным именем', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: {
                            ...author,
                            category: 'AGENT',
                            agentName: 'АИЙИЛЬЦИКЛИКИРМИЦИБАЙРАКТАЗИЙАНКАГРАМАНОГЛУ Круциатус'
                        }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка агента с длинным названием организации', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: {
                            ...author,
                            category: 'AGENCY',
                            organization: 'в четверг четвертого числа в четыре с четвертью часа'
                        }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка агента с профилем', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: {
                            ...author,
                            profile: {
                                userType: 'AGENT',
                                name: 'Сергей Мавроди',
                                logo: profileImages
                            },
                            category: 'AGENT',
                            agentName: 'АИЙИЛЬЦИКЛИКИРМИЦИБАЙРАКТАЗИЙАНКАГРАМАНОГЛУ Круциатус'
                        }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('агенство', () => {
        it('карточка агентсва', async() => {
            await render(<Component offer={offer} />, renderOptions);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка агенства с профилем', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: {
                            ...author,
                            profile: {
                                userType: 'AGENCY',
                                name: 'Пупкино и Ко',
                                logo: profileImages
                            },
                            category: 'AGENCY',
                            agentName: 'АИЙИЛЬЦИКЛИКИРМИЦИБАЙРАКТАЗИЙАНКАГРАМАНОГЛУ Круциатус'
                        }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('частный агент', () => {
        it('карточка агентсва', async() => {
            await render(<Component offer={offer} />, renderOptions);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка частного агента', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        author: {
                            ...author,
                            category: 'PRIVATE_AGENT',
                            agentName: 'Ибрашка'
                        }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('сервис яндекса', () => {
        it('яндекс аренда', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        yandexRent: true,
                        author: {
                            ...author,
                            category: 'AGENCY',
                            photo: generateImageUrl(),
                            agentName: 'Яндекс.Аренда'
                        }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
