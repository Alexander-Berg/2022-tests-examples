import { describe } from '@jest/globals';

import { QuestionMarkupGeneratorService } from '../question-markup-generator.service';

describe('Autoru post schema question markup generator', () => {
    let questionMarkupGeneratorService: QuestionMarkupGeneratorService;

    beforeEach(() => {
        questionMarkupGeneratorService = new QuestionMarkupGeneratorService();
    });

    describe('getMarkup', () => {
        it.each([
            [''],
            [' '],
            ['abc'],
            ['<h2>'],
            ['<h2><p></p>'],
            ['<h2></h2>'],
            ['<h2>Test</h2>'],
            ['<h2><p>Test</p></h2>'],
            ['<h2><p>Test</p></h2><p>'],
            ['<h2><p>Header</p></h2><div>Content</div>'],
            ['<h2><p>Header</p></h2><div>Content</div><p>Content 2</p>'],
            ['<p>Content 1</p><h2>Header</h2>'],
            ['<p>Content 1</p><h2>Header</h2><p>Content 2</p>'],
            ['<h1>Header</h1><p>Content 2</p>'],
            ['<h5>Header</h5><p>Content 2</p>'],
            ['<h6>Header</h6><p>Content 2</p>'],
        ])('Возвращает null, если передан %p', a => {
            expect(questionMarkupGeneratorService.getMarkup(a)).toBeNull();
        });

        it('Возвращает null, если передан символ переноса строки', () => {
            expect(questionMarkupGeneratorService.getMarkup('\n')).toBeNull();
        });

        it('Возвращает микроразметку вопроса, не учитывая переносы строк', () => {
            const html = '\n\\n<h2>Header</h2>\\n\n<p>Content 2</p>\n';

            const markup = questionMarkupGeneratorService.getMarkup(html);

            expect(markup).toMatchSnapshot();
        });

        it.each([
            ['<h2>Header</h2><p>Content 2</p>'],
            ['<h3>Header</h3><p>Content 2</p>'],
            ['<h4>Header</h4><p>Content 2</p>'],
            ['<h2><span>Header</span></h2><p>Content 2</p>'],
            ['<h2><span>Header</span></h2><p>Content 2</p><p>Content 3</p>'],
            [
                '<h2><span>Header</span></h2><p><span class="content">Content 2</span></p><p class="content">Content 3</p>',
            ],
            [
                '<h3><strong>В чём особенности зарядки электромобиля</strong></h3><p>При заправке обычного автомобиля главное правило — выбрать проверенную АЗС.</p>',
            ],
            [
                '<h3><strong>В каких режимах заряжается электромобиль</strong></h3>' +
                    '<p>Чтобы зарядить батарею, недостаточно просто соединить её проводами с источником электри&shy;чества.</p>' +
                    '<p><strong>Mode 1</strong> — это прямое соединение с бытовой электро&shy;сетью</p>',
            ],
            [
                '\n<h3><strong>В каких режимах заряжается электромобиль</strong></h3>\\n' +
                    '<p>Чтобы зарядить батарею, недостаточно просто соединить её проводами с источником электри&shy;чества.</p>\n\\n' +
                    '<p><strong>Mode 1</strong> — это прямое соединение с бытовой электро&shy;сетью</p>\n',
            ],
        ])('Возвращает микроразметку вопроса, если передан %p', a => {
            expect(questionMarkupGeneratorService.getMarkup(a)).toMatchSnapshot();
        });
    });
});
