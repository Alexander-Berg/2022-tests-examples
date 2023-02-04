import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

export const item = {
    title: 'Стул',
    description: '1 шт.',
    image: generateImageUrl({ width: 400, height: 400 }),
    imageX2: generateImageUrl({ width: 400, height: 400 }),
};

export const itemWithLongName = {
    title: 'Очень очень очень очень очень очень очень очень очень большой стол',
    description: '1 шт.',
    image: generateImageUrl({ width: 400, height: 400 }),
    imageX2: generateImageUrl({ width: 400, height: 400 }),
};
