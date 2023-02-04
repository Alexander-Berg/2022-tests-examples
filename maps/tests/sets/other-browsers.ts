import {makeScreenshotTest, variateSize, variateTheme} from '../utils';

describe('Верстка в различных браузерах', () => {
    describe('Организация с рейтингом и несколькими отзывами', () => {
        variateTheme((theme) =>
            variateSize((size) =>
                makeScreenshotTest(157564835865, 'many-reviews', {size, theme})
            )
        );
    });

    describe('Организация без рейтинга и отзывов', () => {
        variateSize((size) =>
            makeScreenshotTest(163056758196, 'wo-rating-wo-review', {size})
        );
    });

    describe('Несуществующая организация', () => {
        variateSize((size) =>
            makeScreenshotTest(1, 'not-found', {size, theme: 'dark'})
        );
    });
});
