const getParentCategoryBySubcategory = require('auto-core/lib/util/getParentCategoryBySubcategory');

it('возвращает корректный алиас категории если в переданном объекте есть id подкатегории', () => {
    const result = getParentCategoryBySubcategory({ category_id: 4 });
    expect(result).toBe('moto');
});

it('возвращает корректный алиас категории если в переданном объекте нет id но есть алиас подкатегории', () => {
    const result = getParentCategoryBySubcategory({ category: 'agricultural' });
    expect(result).toBe('commercial');
});

it('возвращает корректный алиас категории если в переданная категория является родительской', () => {
    const result = getParentCategoryBySubcategory({ category: 'cars' });
    expect(result).toBe('cars');
});
