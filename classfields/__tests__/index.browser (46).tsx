import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { RequestStatus } from 'realty-core/types/network';

import { rootReducer } from 'view/entries/user/reducer';
import { AppProvider } from 'view/libs/test-helpers';
import { NavigationButtonBaseSizes } from 'view/components/NavigationButtonBase';

import { IUniversalStore } from 'view/modules/types';

import { NavigationFlatIcon, INavigationFlatIconProps } from '../index';

const renderOptions = { viewport: { width: 120, height: 120 } };
const imageUrl = generateImageUrl({ width: 400, height: 300, size: 20 });
const address = 'Большая Пироговская улица, 5, кв 3';
const addressOneLower = 'большая Пироговская улица, 5, кв 3';
const addressAllLower = 'большая пироговская улица, 5, кв 3';
const addressAllUpper = 'Большая Пироговская Улица, 5, кв 3';
const addressWithHyphen = 'Улица Бонч-Бруевича, 5, кв 3';

const initialState: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.INITIAL,
    },
};

const Component = (props: INavigationFlatIconProps) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={initialState}>
            <NavigationFlatIcon {...props} />
        </AppProvider>
    );
};

describe('NavigationFlatIcon', () => {
    describe('Внешний вид', () => {
        it('S, без фото', async () => {
            await render(
                <Component address={address} isActive={false} size={NavigationButtonBaseSizes.SIZE_S} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('S, с фото', async () => {
            await render(
                <Component
                    address={address}
                    isActive={false}
                    size={NavigationButtonBaseSizes.SIZE_S}
                    image={imageUrl}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('S, активный, без фото', async () => {
            await render(
                <Component address={address} isActive={true} size={NavigationButtonBaseSizes.SIZE_S} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('S, активный, с фото', async () => {
            await render(
                <Component
                    address={address}
                    isActive={true}
                    size={NavigationButtonBaseSizes.SIZE_S}
                    image={imageUrl}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('M, без фото', async () => {
            await render(
                <Component address={address} isActive={false} size={NavigationButtonBaseSizes.SIZE_M} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('M, с фото', async () => {
            await render(
                <Component
                    address={address}
                    isActive={false}
                    size={NavigationButtonBaseSizes.SIZE_M}
                    image={imageUrl}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('M, активный, без фото', async () => {
            await render(
                <Component address={address} isActive={true} size={NavigationButtonBaseSizes.SIZE_M} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('M, активный, с фото', async () => {
            await render(
                <Component
                    address={address}
                    isActive={true}
                    size={NavigationButtonBaseSizes.SIZE_M}
                    image={imageUrl}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('L, без фото', async () => {
            await render(
                <Component address={address} isActive={false} size={NavigationButtonBaseSizes.SIZE_L} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('L, с фото', async () => {
            await render(
                <Component
                    address={address}
                    isActive={false}
                    size={NavigationButtonBaseSizes.SIZE_L}
                    image={imageUrl}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('L, активный, без фото', async () => {
            await render(
                <Component address={address} isActive={true} size={NavigationButtonBaseSizes.SIZE_L} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('L, активный, с фото', async () => {
            await render(
                <Component
                    address={address}
                    isActive={true}
                    size={NavigationButtonBaseSizes.SIZE_L}
                    image={imageUrl}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
    describe('Формирование инициалов', () => {
        it('Адрес с одной заглавной буквой', async () => {
            await render(
                <Component isActive={false} size={NavigationButtonBaseSizes.SIZE_S} address={addressOneLower} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Адрес без заглавных букв', async () => {
            await render(
                <Component address={addressAllLower} isActive={false} size={NavigationButtonBaseSizes.SIZE_S} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Адрес с тремя заглавными буквами', async () => {
            await render(
                <Component address={addressAllUpper} isActive={false} size={NavigationButtonBaseSizes.SIZE_S} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Пустой адрес', async () => {
            await render(
                <Component address={''} isActive={false} size={NavigationButtonBaseSizes.SIZE_S} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Адрес с дефисом', async () => {
            await render(
                <Component address={addressWithHyphen} isActive={false} size={NavigationButtonBaseSizes.SIZE_S} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
