const {
    getCostWithDiscount,
    getDiscountPromocodes
} = require('../discounts');

describe('applyDiscount', () => {
    it('Корректно работает при нулевой скидке', () => {
        expect(getCostWithDiscount(123, 0)).toBe(123);
    });

    it('Округляет цену в меньшую сторону при нечетной цене', () => {
        expect(getCostWithDiscount(101, 50)).toBe(50);
    });

    it('Првильно округляет при четной цене', () => {
        expect(getCostWithDiscount(100, 50)).toBe(50);
    });

    it('Возвращает исходную цену если не передана скидка', () => {
        expect(getCostWithDiscount(100, undefined)).toBe(100);
        expect(getCostWithDiscount(51, null)).toBe(51);
        expect(getCostWithDiscount(42, '')).toBe(42);
    });
});

describe('getDiscountPromocodes', () => {
    it('Выбирает только промокоды с типом "discount"', () => {
        const promocodes = [
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'raising',
                    discount: {
                        discountType: 'percent',
                        value: 25
                    }
                }
            },
            {
                tag: 'premium'
            },
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 35
                    }
                }
            }
        ];

        expect(Object.keys(getDiscountPromocodes(promocodes))).toHaveLength(2);
    });

    it('targetTag = "all" добавлет скидку на все типы', () => {
        const promocodes = [
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'all',
                    discount: {
                        discountType: 'percent',
                        value: 25
                    }
                }
            },
            {
                tag: 'promotion'
            },
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 35
                    }
                }
            }
        ];

        const discounts = getDiscountPromocodes(promocodes);

        expect(discounts).toMatchObject({
            premium: {
                discountType: 'percent',
                value: 25
            },
            raising: {
                discountType: 'percent',
                value: 25
            },
            turboSale: {
                discountType: 'percent',
                value: 25
            }
        });
    });

    it('Если несколько одинаковых скидок - берем максимальную', () => {
        const promocodes = [
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 25
                    }
                }
            },
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 35
                    }
                }
            },
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 15
                    }
                }
            }
        ];

        expect(getDiscountPromocodes(promocodes).promotion.value).toBe(35);
    });

    it('Игнорируем промокод-скидку если есть обычный промокод этого же типа (бесплатно для пользователя)', () => {
        const promocodes = [
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'premium',
                    discount: {
                        discountType: 'percent',
                        value: 25
                    }
                }
            },
            {
                tag: 'premium'
            },
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 35
                    }
                }
            }
        ];

        expect(Object.keys(getDiscountPromocodes(promocodes))).toHaveLength(1);
        expect(getDiscountPromocodes(promocodes).promotion).toBeTruthy();
    });

    it('максимальный размер скидки 100%', () => {
        const promocodes = [
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 25
                    }
                }
            },
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 135
                    }
                }
            },
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 15
                    }
                }
            }
        ];

        expect(getDiscountPromocodes(promocodes).promotion.value).toBe(100);
    });

    it('Не корректно составленные промокоды должны игнорироваться', () => {
        const promocodes = [
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 25
                    }
                }
            },
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 'скидка 13%'
                    }
                }
            },
            {
                tag: 'discount',
                jsonPayload: {
                    targetTag: 'promotion',
                    discount: {
                        discountType: 'percent',
                        value: 15
                    }
                }
            }
        ];

        expect(getDiscountPromocodes(promocodes).promotion.value).toBe(25);
    });
});
