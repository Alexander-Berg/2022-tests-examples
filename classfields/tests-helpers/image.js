/* globals window, document */

const BLACK_COLOR = '#505050';
const WHITE_COLOR = '#dedede';

/**
 * Возвращает шахматное изображение заданного размера в формате dataUri
 * @param {Object} params Параметры изображения
 * @param {Number} params.width ширина
 * @param {Number} params.height высота
 * @param {Number} [params.size=20] размер стороны шахматной ячейки
 * @returns {String} dataUri
 */
export const generateImageUrl = params => {
    if (typeof window === 'undefined') {
        return '';
    }

    const { width = 100, height = 100, size = 20 } = params || {};
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');

    canvas.width = width;
    canvas.height = height;

    const xCount = Math.ceil(width / size);
    const yCount = Math.ceil(height / size);

    for (let i = 0; i < yCount; i++) {
        for (let j = 0; j < xCount; j++) {
            ctx.beginPath();
            ctx.fillStyle = [ WHITE_COLOR, BLACK_COLOR ][(i + j) % 2];
            ctx.fillRect(j * size, i * size, size, size);
            ctx.closePath();
        }
    }

    return canvas.toDataURL('image/png');
};

/**
 * На основе размеров исходного изображения возвращает объект с шахматныи картинками в формате dataUri.
 * Ключ - названия алиаса из аватарницы, значение - изображение соответсвующего алиасу размера.
 * https://wiki.yandex-team.ru/realty/csssr/razmery-kartinok/
 * @param {Object} params Параметры изображения
 * @param {Number} params.width ширина
 * @param {Number} params.height высота
 * @param {Number} [params.size=20] размер стороны шахматной ячейки
 * @returns {Object} dataUri
 */
export const generateImageAliases = ({ width, height, size }) => {
    return {
        minicard: generateImageUrl({ width: 146, height: 110, size }),
        main: generateImageUrl({ width: 543, height: 332, size }),
        alike: generateImageUrl({ width: 80, height: 60, size }),
        large: generateImageUrl({ width: 1024, height: 768, size }), // Не честно, это максимальное
        cosmic: generateImageUrl({ width: 190, height: 142, size }),
        appMiddle: generateImageUrl({ width: Math.round(width * 280 / height), height: 280, size }),
        appLarge: generateImageUrl({ width: Math.round(width * 560 / height), height: 560, size }),
        appSnippetMini: generateImageUrl({ width: 132, height: 132, size }),
        appSnippetSmall: generateImageUrl({ width: 232, height: 232, size }),
        appSnippetMiddle: generateImageUrl({ width: 272, height: 272, size }),
        appSnippetLarge: generateImageUrl({ width: 450, height: 450, size }),
        optimize: generateImageUrl({ width, height, size })
    };
};
