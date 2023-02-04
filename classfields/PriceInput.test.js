const React = require('react');
const { shallow } = require('enzyme');

// Components
const Checkbox = require('auto-core/react/components/islands/Checkbox');
const PriceInput = require('./PriceInput');
const PriceInputDiscounts = require('./PriceInputDiscounts');

// Mocks
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const dealerWithAccessMock = require('auto-core/react/dataDomain/user/mocks/dealerWithAccess.mock');
const privateUserMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');

let initialState;

beforeEach(() => {
    initialState = {
        formFields: {
            data: {
                currency: {
                    value: 'RUR',
                },
            },
        },
        parsedOptions: {},
        state: {
            parent_category: 'trucks',
        },
        user: { data: {} },
    };
});

describe('чекбокс "с НДС"', () => {
    it('показывается для дилера', () => {
        const props = {
            section: 'new',
        };
        initialState.state.parent_category = 'trucks';
        initialState.user = dealerWithAccessMock;

        const priceInput = shallowRenderComponent({ initialState, props });

        expect(priceInput.find(Checkbox).prop('children')).toBe('Цена с НДС');
    });

    it('показывается для частника, если в описании есть "НДС"', () => {
        const props = {
            section: 'new',
        };
        initialState.state.parent_category = 'trucks';
        initialState.user = privateUserMock;
        initialState.parsedOptions = { showWithNds: true };

        const priceInput = shallowRenderComponent({ initialState, props });

        expect(priceInput.find(Checkbox).prop('children')).toBe('Учитывать НДС');
    });

    it('показывается для частника, если в флаг был включен ранее', () => {
        const props = {
            section: 'used',
        };
        initialState.state.parent_category = 'trucks';
        initialState.user = privateUserMock;
        initialState.formFields.data.withNdsStatus = { value: true };

        const priceInput = shallowRenderComponent({ initialState, props });

        expect(priceInput.find(Checkbox).at(2).prop('children')).toBe('Учитывать НДС');
    });

    it('не показывается частнику, если в описании нет "НДС"', () => {
        const props = {
            section: 'new',
        };
        initialState.state.parent_category = 'trucks';
        initialState.user = privateUserMock;

        const priceInput = shallowRenderComponent({ initialState, props });

        expect(priceInput.find(Checkbox).isEmptyRender()).toBe(true);
    });
});

it('не показывает чекбоксы для комтранса, если пользователь — не дилер', () => {
    const props = {
        section: 'new',
    };
    initialState.state.parent_category = 'trucks';

    const priceInput = shallowRenderComponent({ initialState, props });

    expect(priceInput.exists(Checkbox)).toBe(false);
});

it('не показывает чекбоксы обмена и торга для мототехники', () => {
    const props = {
        section: 'new',
    };
    initialState.state.parent_category = 'moto';
    initialState.user = dealerWithAccessMock;

    const priceInput = shallowRenderComponent({ initialState, props });

    expect(priceInput.find(Checkbox).map(node => node.prop('children'))).toEqual([ 'Цена с НДС' ]);
});

it('показывает чекбоксы обмена и торга для б/у', () => {
    const props = {
        section: 'used',
    };
    initialState.state.parent_category = 'trucks';

    const priceInput = shallowRenderComponent({ initialState, props });

    expect(priceInput.find(Checkbox).map(node => node.prop('children'))).toStrictEqual([ 'Возможен обмен', 'Возможен торг' ]);
});

it('показывает чекбоксы НДС, обмена и торга для б/у, если пользователь — дилер', () => {
    const props = {
        section: 'used',
    };
    initialState.state.parent_category = 'trucks';
    initialState.user = dealerWithAccessMock;

    const priceInput = shallowRenderComponent({ initialState, props });

    expect(priceInput.find(Checkbox).map(node => node.prop('children'))).toStrictEqual([ 'Возможен обмен', 'Возможен торг', 'Цена с НДС' ]);
});

it('не показывает чекбоксы для новых', () => {
    const props = {
        section: 'new',
    };
    initialState.state.parent_category = 'trucks';

    const priceInput = shallowRenderComponent({ initialState, props });

    expect(priceInput.exists(Checkbox)).toBe(false);
});

it('показывает блок со скидками для нового коммтранса', () => {
    const props = {
        section: 'new',
    };
    initialState.state.parent_category = 'trucks';

    const priceInput = shallowRenderComponent({ initialState, props });

    expect(priceInput.exists(PriceInputDiscounts)).toBe(true);
});

it('не показывает блок со скидками для б/у коммтранса', () => {
    const props = {
        section: 'used',
    };
    initialState.state.parent_category = 'trucks';

    const priceInput = shallowRenderComponent({ initialState, props });

    expect(priceInput.exists(PriceInputDiscounts)).toBe(false);
});

it('не показывает блок со скидками для нового не коммтранса', () => {
    const props = {
        section: 'used',
    };
    initialState.state.parent_category = 'moto';

    const priceInput = shallowRenderComponent({ initialState, props });

    expect(priceInput.exists(PriceInputDiscounts)).toBe(false);
});

function shallowRenderComponent({ initialState, props }) {
    const store = mockStore(initialState);

    return shallow(
        <PriceInput { ...props } store={ store }/>,
    ).dive();
}
