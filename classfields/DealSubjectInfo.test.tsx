import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import { SellerStep, ParticipantType } from '@vertis/schema-registry/ts-types-snake/vertis/safe_deal/common';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import DealSubjectInfo from './DealSubjectInfo';

const Context = createContextProvider(contextMock);

const MARK = 'HYUNDAI';
const MODEL = 'SOLARIS';

const data = {
    safeDeal: {
        deal: {
            subject: {
                autoru: {
                    vin: 'WDDZF4KB63A738739',
                    mark: MARK,
                    model: MODEL,
                },
            },
        },
    },
};

const store = mockStore(data);

it('поле с маркой авто должно быть заполнено', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Марка и модель ТС' });

    expect(input.prop('value')).toBe(`${ MARK } ${ MODEL }`);
});

it('поле VIN должно быть неактивно', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'VIN номер' });

    expect(input.prop('disabled')).toBe(true);
});

it('должен выводить ошибку при невалидной модели двигателя', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Модель двигателя' });
    input.simulate('change', '$$');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Модель двигателя' }).prop('error');

    expect(error).toBe('Неверное значение');
});

it('НЕ должен выводить ошибку при валидной модели двигателя', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Модель двигателя' });
    input.simulate('change', '123');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Модель двигателя' }).prop('error');

    expect(error).toBe(false);
});

it('должен выводить ошибку при невалидном номере двигателя', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Номер двигателя' });
    input.simulate('change', '$$');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Номер двигателя' }).prop('error');

    expect(error).toBe('Неверное значение');
});

it('НЕ должен выводить ошибку при валидном номере двигателя', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Номер двигателя' });
    input.simulate('change', '123');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Номер двигателя' }).prop('error');

    expect(error).toBe(false);
});

it('должен выводить ошибку при невалидном номере шасси', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Номер шасси, рамы' });
    input.simulate('change', '$$');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Номер шасси, рамы' }).prop('error');

    expect(error).toBe('Неверное значение');
});

it('НЕ должен выводить ошибку при валидном номере шасси', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Номер шасси, рамы' });
    input.simulate('change', '123');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Номер шасси, рамы' }).prop('error');

    expect(error).toBe(false);
});

it('должен выводить ошибку при невалидном годе выпуска', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Год выпуска' });
    input.simulate('change', '1811');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Год выпуска' }).prop('error');

    expect(error).toBe('Неверный год выпуска');
});

it('НЕ должен выводить ошибку при валидном годе выпуска', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Год выпуска' });
    input.simulate('change', '2000');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Год выпуска' }).prop('error');

    expect(error).toBe(false);
});

it('должен выводить ошибку при невалидном номере ПТС', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Паспорт ТС, серия/номер' });
    input.simulate('change', 'test');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Паспорт ТС, серия/номер' }).prop('error');

    expect(error).toBe('Неверный номер документа');
});

it('НЕ должен выводить ошибку при валидном номере ПТС', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Паспорт ТС, серия/номер' });
    input.simulate('change', '11TO333333');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Паспорт ТС, серия/номер' }).prop('error');

    expect(error).toBe(false);
});

it('должен выводить ошибку при невалидном поле "ПТС, кем выдан"', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Паспорт ТС, кем выдан' });
    input.simulate('change', 'TEST');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Паспорт ТС, кем выдан' }).prop('error');

    expect(error).toBe('Неверный формат');
});

it('НЕ должен выводить ошибку при валидной поле "ПТС, кем выдан"', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Паспорт ТС, кем выдан' });
    input.simulate('change', 'ГИБДД');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Паспорт ТС, кем выдан' }).prop('error');

    expect(error).toBe(false);
});

it('должен выводить ошибку при невалидном номере СТС', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'СТС, серия/номер' });
    input.simulate('change', 'test');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'СТС, серия/номер' }).prop('error');

    expect(error).toBe('Неверный номер документа');
});

it('НЕ должен выводить ошибку при валидном номере СТС', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'СТС, серия/номер' });
    input.simulate('change', '11TO333333');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'СТС, серия/номер' }).prop('error');

    expect(error).toBe(false);
});

it('должен выводить ошибку при невалидном дате СТС', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'СТС, дата выдачи' });
    input.simulate('change', '31.12.1899');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'СТС, дата выдачи' }).prop('error');

    expect(error).toBe('Неверная дата выдачи');
});

it('НЕ должен выводить ошибку при валидной дате СТС', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'СТС, дата выдачи' });
    input.simulate('change', '01.01.1900');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'СТС, дата выдачи' }).prop('error');

    expect(error).toBe(false);
});

it('должен выводить ошибку при невалидном поле "СТС, кем выдан"', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'СТС, кем выдан' });
    input.simulate('change', 'TEST');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'СТС, кем выдан' }).prop('error');

    expect(error).toBe('Неверный формат');
});

it('НЕ должен выводить ошибку при валидном поле "СТС, кем выдан"', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'СТС, кем выдан' });
    input.simulate('change', 'ГИБДД');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'СТС, кем выдан' }).prop('error');

    expect(error).toBe(false);
});

it('должен выводить ошибку при невалидном гос. номере', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Гос. номер' });
    input.simulate('change', 'TEST');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Гос. номер' }).prop('error');

    expect(error).toBe('Неверный формат');
});

it('НЕ должен выводить ошибку при валидном гос. номере', () => {
    const wrapper = renderWrapper(store);
    const input = wrapper.find({ placeholder: 'Гос. номер' });
    input.simulate('change', 'А136ВС60');
    input.simulate('blur');

    const error = wrapper.find({ placeholder: 'Гос. номер' }).prop('error');

    expect(error).toBe(false);
});

function renderWrapper(store: ThunkMockStore<typeof data>) {
    return shallow(
        <Provider store={ store }>
            <Context>
                <DealSubjectInfo
                    onUpdate={ jest.fn() }
                    dealId="12345"
                    step={ SellerStep.SELLER_INTRODUCING_SUBJECT_DETAILS }
                    formStep="subject"
                    role={ ParticipantType.SELLER }
                    dealPageType={ ParticipantType.SELLER.toLowerCase() }
                />
            </Context>
        </Provider>,
    ).dive().dive().dive();
}
