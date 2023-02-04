jest.mock('auto-core/react/actions/scroll');

import React from 'react';
import userEvent from '@testing-library/user-event';
import _ from 'lodash';

import '@testing-library/jest-dom';

import scrollTo from 'auto-core/react/actions/scroll';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import modelMock from 'auto-core/models/catalogSuggest/mocks/model.mock';

import ModelsListMock from './mocks/ModelListMock';
import type { RenderModelListArgs } from './types';
import type { Props } from './ModelField';
import ModelField from './ModelField';

const scrollToMock = scrollTo as jest.MockedFunction<typeof scrollTo>;

const initialValues = {
    [FieldNames.MARK]: {
        data: 'KIA',
        text: 'Kia',
    },
};

let defaultProps: Props;

const renderList = ({ handleExpandListClick, handleModelClick, isCutLinkItem, isListExpanded, modelsList }: RenderModelListArgs) => {
    return (
        <ModelsListMock
            handleExpandListClick={ handleExpandListClick }
            handleModelClick={ handleModelClick }
            isCutLinkItem={ isCutLinkItem }
            isListExpanded={ isListExpanded }
            modelsList={ modelsList }
        />
    );
};

beforeEach(() => {
    defaultProps = {
        onSelect: jest.fn(),
        onFocusChange: jest.fn(),
        fetchCatalogSuggest: jest.fn(),
        isCatalogSuggestFetching: false,
        renderList,
    };
});

it('меняет значение в инпуте', async() => {
    const { findByLabelText } = await renderComponent(<ModelField { ...defaultProps }/>, { initialValues });
    const input = await findByLabelText('Модель') as HTMLInputElement;

    expect(input?.value).toBe('');

    userEvent.type(input, 'Rio');
    userEvent.tab();

    expect(input?.value).toBe('Rio');
});

it('выбирает модель из списка', async() => {
    const props = {
        ...defaultProps,
        models: [
            modelMock.withId('RIO').withName('Rio').value(),
            modelMock.withId('OPTIMA').withName('Optima').value(),
            modelMock.withId('SOUL').withName('Soul').value(),
        ],
    };

    const { findByLabelText, getByRole } = await renderComponent(<ModelField { ...props }/>, { initialValues });

    const item = await getByRole('button', { name: 'Rio' });
    userEvent.click(item);

    const input = await findByLabelText('Модель') as HTMLInputElement;

    expect(input?.value).toBe('Rio');
    expect(props.onSelect).toHaveBeenCalledTimes(1);
    expect(props.onSelect).toHaveBeenCalledWith({ data: 'RIO', text: 'Rio' });
});

describe('если значение нет', () => {
    let props: Props;

    beforeEach(() => {
        props = {
            ...defaultProps,
            models: _.range(0, 30).map((index) =>
                modelMock.withId(String(index)).withName(`model #${ _.padStart(String(index), 2, '0') }`).withOfferCount(50).value(),
            ),
            renderList,
        };
    });

    it('показываем список популярных моделей и ссылку "еще"', async() => {
        const { getAllByRole } = await renderComponent(<ModelField { ...props }/>, { initialValues });

        const nodes = await getAllByRole('button');
        const items = Array.from(nodes).map((node) => node.innerHTML);

        expect(items).toHaveLength(24);
        expect(_.first(items)).toBe('model #06');
        expect(_.last(items)).toBe('Все модели');
    });

    it('при клике на "еще" покажет полный список', async() => {
        const { getAllByRole, getByRole } = await renderComponent(<ModelField { ...props }/>, { initialValues });

        const cutLink = await getByRole('button', { name: 'Все модели' });
        userEvent.click(cutLink);

        const nodes = await getAllByRole('button');
        const items = Array.from(nodes).map((node) => node.innerHTML);

        expect(items).toHaveLength(31);
        expect(_.first(items)).toBe('model #00');
        expect(_.last(items)).toBe('Свернуть');
    });

    it('при клике на "свернуть" свернет список и подскроллит вверх', async() => {
        const { getAllByRole, getByRole } = await renderComponent(<ModelField { ...props }/>, { initialValues });

        let cutLink = await getByRole('button', { name: 'Все модели' });
        userEvent.click(cutLink);

        cutLink = await getByRole('button', { name: 'Свернуть' });
        userEvent.click(cutLink);

        const items = await getAllByRole('button');

        expect(items).toHaveLength(24);
        expect(scrollToMock).toHaveBeenCalledTimes(1);
        expect(scrollToMock).toHaveBeenCalledWith(FieldNames.MODEL, { offset: -100 });
    });
});

it('при вводе фильтрует саджест под инпутом', async() => {
    const props = {
        ...defaultProps,
        models: [
            modelMock.withId('RIO').withName('Rio').value(),
            modelMock.withId('OPTIMA').withName('Optima').value(),
            modelMock.withId('MAGENTIS').withName('Magentis').value(),
        ],
    };

    const { findByLabelText, getAllByRole } = await renderComponent(<ModelField { ...props }/>, { initialValues });

    const input = await findByLabelText('Модель') as HTMLInputElement;

    userEvent.type(input, 'o');
    userEvent.tab();

    const nodes = await getAllByRole('button');
    const items = Array.from(nodes).map((node) => node.innerHTML);

    expect(items).toEqual([ 'Rio', 'Optima' ]);
});

it('если отфильтровалась только одна модель выберет ее', async() => {
    const props = {
        ...defaultProps,
        models: [
            modelMock.withId('RIO').withName('Rio').value(),
            modelMock.withId('OPTIMA').withName('Optima').value(),
            modelMock.withId('MAGENTIS').withName('Magentis').value(),
        ],
    };

    const { findByLabelText, queryAllByRole } = await renderComponent(<ModelField { ...props }/>, { initialValues });

    const input = await findByLabelText('Модель') as HTMLInputElement;

    userEvent.type(input, 'ri');
    userEvent.tab();

    const items = await queryAllByRole('button');

    expect(items).toHaveLength(0);
    expect(input?.value).toBe('Rio');
    expect(props.onSelect).toHaveBeenCalledTimes(1);
    expect(props.onSelect).toHaveBeenCalledWith({ data: 'RIO', text: 'Rio' });
});

describe('если в саджесте только одна модель', () => {
    it('если значения нет, выберет её', async() => {
        const props = {
            ...defaultProps,
            models: [
                modelMock.withId('RIO').withName('Rio').value(),
            ],
        };
        const { findByLabelText } = await renderComponent(<ModelField { ...props }/>, { initialValues });

        const input = await findByLabelText('Модель') as HTMLInputElement;

        expect(input?.value).toBe('Rio');
        expect(props.onSelect).toHaveBeenCalledTimes(1);
        expect(props.onSelect).toHaveBeenCalledWith({ data: 'RIO', text: 'Rio' }, false);
    });

    it('если значения есть, ничего не будет делать', async() => {
        const props = {
            ...defaultProps,
            models: [
                modelMock.withId('RIO').withName('Rio').value(),
            ],
        };
        const customInitialValues = {
            ...initialValues,
            [FieldNames.MARK]: {
                data: 'KIA',
                text: 'Kia',
            },
            [FieldNames.MODEL]: {
                data: 'RIO',
                text: 'Rio',
            },
        };

        const { findByLabelText } = await renderComponent(<ModelField { ...props }/>, { initialValues: customInitialValues });

        const input = await findByLabelText('Модель') as HTMLInputElement;

        expect(input?.value).toBe('Rio');
        expect(props.onSelect).not.toHaveBeenCalled();
    });
});
