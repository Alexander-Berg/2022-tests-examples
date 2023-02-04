import { getTermAccordingSlider } from './getTermAccordingSlider';

describe('getTermAccordingSlider', () => {
    const termSliderValues = [
        { value: 1 },
        { value: 2 },
        { value: 3 },
    ];

    it('возвращает последнее значение из слайдера, если не указан срок кредита', () => {
        const result = getTermAccordingSlider({
            creditLoanTerm: 0,
            termSliderValues,
        });

        expect(result).toBe(3);
    });

    it('возвращает блишайшее целое из слайдера', () => {
        const result = getTermAccordingSlider({
            creditLoanTerm: 0.5,
            termSliderValues,
        });

        expect(result).toBe(1);

        const result1 = getTermAccordingSlider({
            creditLoanTerm: 1.4,
            termSliderValues,
        });

        expect(result1).toBe(1);

        const result2 = getTermAccordingSlider({
            creditLoanTerm: 1.6,
            termSliderValues,
        });

        expect(result2).toBe(2);
    });
});
