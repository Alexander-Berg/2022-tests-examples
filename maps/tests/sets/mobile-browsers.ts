import {makeScreenshotTest, variateSize, variateTheme} from '../utils';

describe('Верстка в мобильном браузере', () => {
    describe('Организация с рейтингом и несколькими отзывами', () => {
        variateSize((size) =>
            variateTheme((theme) =>
                makeScreenshotTest(157564835865, 'many-reviews', {size, theme})
            )
        );
    });

    describe('Организация с одним отзывом', () => {
        makeScreenshotTest(211456447621, 'one-review');
    });

    describe('Организация без рейтинга и отзывов', () => {
        makeScreenshotTest(163056758196, 'wo-rating-wo-review');
    });

    describe('Организация с рейтингом без отзыва', () => {
        makeScreenshotTest(204349032263, 'w-rating-wo-review');
    });

    describe('Организация без рейтинга с отзывами', () => {
        makeScreenshotTest(73070573291, 'wo-rating-w-review');
    });

    describe('Несуществующая организация', () => {
        makeScreenshotTest(1, 'not-found', {theme: 'dark'});
    });
});
