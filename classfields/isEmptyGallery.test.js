const isEmptyGallery = require('./isEmptyGallery');

it('должен вернуть false, если объявление с фотками', () => {
    expect(isEmptyGallery({
        state: { image_urls: Array(1) },
    })).toBe(false);
});

it('должен вернуть true, если объявление без фоток', () => {
    expect(isEmptyGallery({
        state: { image_urls: [] },
    })).toBe(true);
});

it('должен вернуть true, если объявление без фоток (нет image_urls)', () => {
    expect(isEmptyGallery({
        state: { image_urls: [] },
    })).toBe(true);
});
