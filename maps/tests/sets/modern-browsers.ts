import {makeScreenshotTest, variateSize, variateTheme} from '../utils';

describe('Верстка в современном браузере', () => {
    describe('Организация с рейтингом и несколькими отзывами', () => {
        variateSize((size) =>
            variateTheme((theme) =>
                makeScreenshotTest(157564835865, 'many-reviews', {size, theme})
            )
        );
    });

    describe('Организация с одним отзывом', () => {
        variateSize(
            (size) => makeScreenshotTest(211456447621, 'one-review', {size}),
            true
        );
    });

    describe('Организация без рейтинга и отзывов', () => {
        variateSize((size) =>
            makeScreenshotTest(163056758196, 'wo-rating-wo-review', {size})
        );
    });

    describe('Организация с рейтингом без отзыва', () => {
        variateSize((size) =>
            makeScreenshotTest(204349032263, 'w-rating-wo-review', {size})
        );
    });

    describe('Организация без рейтинга с отзывами', () => {
        variateSize(
            (size) => variateTheme(
                (theme) => makeScreenshotTest(73070573291, 'wo-rating-w-review', {size, theme})
            ),
            true
        );
    });

    describe('Организация с рейтингом 1.0', () => {
        makeScreenshotTest(88280747996, 'rating-1-star');
    });

    describe('Онлайн организация', () => {
        makeScreenshotTest(192652887153, 'online-org');
    });

    describe('Несуществующая организация', () => {
        variateSize((size) =>
            makeScreenshotTest(1, 'not-found', {size, theme: 'dark'})
        );
    });

    describe('Организация с ошибкой в отзывах', () => {
        variateTheme((theme) =>
            makeScreenshotTest(1210449111, 'review-error', {theme})
        );
    });
});
