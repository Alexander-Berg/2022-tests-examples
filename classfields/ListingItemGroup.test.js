const React = require('react');
const { shallow } = require('enzyme');

const groupCard = require('autoru-frontend/mockData/state/groupCard.mock');
const emptyGroupCard = require('autoru-frontend/mockData/state/emptyGroupCard.mock');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const ListingItemGroup = require('./ListingItemGroup');

describe('диапазон цен', () => {
    let tree;

    beforeEach(() => {
        tree = shallow(
            <ListingItemGroup
                offer={ groupCard }
                params={{ category: 'cars', section: 'new' }}
                hasStateSupport={ false }
            />,
            { context: contextMock },
        );
    });

    it('должен корректно отрендерить диапазон цен в рублях', () => {
        tree.setProps({ currency: 'RUR' });
        expect(tree.find('.ListingItemGroup__price').text()).toEqual('1 969 688 – 4 661 500 ₽');
    });

    it('должен корректно отрендерить диапазон цен в иностранной валюте', () => {
        tree.setProps({ currency: 'USD' });
        expect(tree.find('.ListingItemGroup__price').text()).toEqual('30 802 – 72 898 $');
    });
});

describe('параметры ссылки', () => {
    it('правильно строит ссылку для группового сниппета новых', () => {
        const tree = shallow(
            <ListingItemGroup
                offer={ groupCard }
                params={{ category: 'cars', section: 'new' }}
                searchParams={{ category: 'cars', section: 'new', power: 420 }}
                hasStateSupport={ false }
            />,
            { context: contextMock },
        );

        const link = tree.find('.ListingItemGroup__column.ListingItemGroup__column_right').find('Link');

        // eslint-disable-next-line max-len
        expect(link.prop('url')).toBe('link/card-group/?section=new&category=cars&power=420&from=&mark=BMW&model=3ER&configuration_id=21398651&super_gen=21398591');
    });

    it('правильно строит ссылку для группового сниппета новых с выбранными шильдами', () => {
        const tree = shallow(
            <ListingItemGroup
                offer={ groupCard }
                params={{ category: 'cars', section: 'new' }}
                searchParams={{
                    category: 'cars',
                    section: 'new',
                    power: 420,
                    catalog_filter: [ { mark: 'BMW', model: '3ER', nameplate_name: '320' } ],
                }}
                hasStateSupport={ false }
            />,
            { context: contextMock },
        );

        const link = tree.find('.ListingItemGroup__column.ListingItemGroup__column_right').find('Link');

        // eslint-disable-next-line max-len
        expect(link.prop('url')).toBe('link/card-group/?section=new&category=cars&power=420&from=&mark=BMW&model=3ER&configuration_id=21398651&super_gen=21398591&catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D21398591%2Cconfiguration%3D21398651%2Ctech_param%3D21605511&catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D21398591%2Cconfiguration%3D21398651%2Ctech_param%3D21398903&catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D21398591%2Cconfiguration%3D21398651%2Ctech_param%3D21398869&catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D21398591%2Cconfiguration%3D21398651%2Ctech_param%3D21398791&catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D21398591%2Cconfiguration%3D21398651%2Ctech_param%3D21605643&catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D21398591%2Cconfiguration%3D21398651%2Ctech_param%3D21592423&catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D21398591%2Cconfiguration%3D21398651%2Ctech_param%3D21592343');
    });

    it('правильно строит ссылку для группового сниппета бу', () => {
        const tree = shallow(
            <ListingItemGroup
                offer={ cloneOfferWithHelpers(groupCard).withSection('USED').value() }
                params={{ category: 'cars', section: 'used' }}
                searchParams={{ category: 'cars', section: 'used', power: 420 }}
                hasStateSupport={ false }
            />,
            { context: contextMock },
        );

        const link = tree.find('.ListingItemGroup__column.ListingItemGroup__column_right').find('Link');

        // eslint-disable-next-line max-len
        expect(link.prop('url')).toBe('link/card-group/?section=used&category=cars&power=420&from=&mark=BMW&model=3ER&configuration_id=21398651&super_gen=21398591');
    });

    it('правильно строит ссылку для группового сниппета с новым geo_radius', () => {
        const tree = shallow(
            <ListingItemGroup
                offer={ groupCard }
                params={{ category: 'cars', section: 'new' }}
                searchParams={{ category: 'cars', section: 'new', power: 420, geo_radius: 500 }}
                hasStateSupport={ false }
            />,
            { context: contextMock },
        );

        const link = tree.find('.ListingItemGroup__column.ListingItemGroup__column_right').find('Link');

        // eslint-disable-next-line max-len
        expect(link.prop('url')).toBe('link/card-group/?section=new&category=cars&power=420&geo_radius=500&from=&mark=BMW&model=3ER&configuration_id=21398651&super_gen=21398591');
    });

    it('правильно строит ссылку на группу для пустой группы', () => {
        const tree = shallow(
            <ListingItemGroup
                offer={ emptyGroupCard }
                params={{ category: 'cars', section: 'new' }}
                searchParams={{ category: 'cars', section: 'used' }}
                hasStateSupport={ false }
            />,
            { context: contextMock },
        );

        const link = tree.find('.ListingItemGroup__column.ListingItemGroup__column_right').find('Link');
        expect(link).toHaveProp('url', 'link/card-group/?section=new&category=cars&from=&mark=BMW&model=X1&configuration_id=20583371&super_gen=20583308');
    });
});

describe('комплектации', () => {
    const pathToNameNode = '.ListingItemGroup__techSummary div:last-child .ListingItemGroup__techSummaryName';
    const pathToValueNode = '.ListingItemGroup__techSummary div:last-child .ListingItemGroup__techSummaryValue';

    it('должен показать список моторов в графе комплектаций', () => {
        const tree = shallow(
            <ListingItemGroup
                offer={ groupCard }
                params={{ category: 'cars', section: 'new' }}
                searchParams={{ category: 'cars', section: 'new', power: 420 }}
                hasStateSupport={ false }
            />,
            { context: contextMock },
        );

        const lastTechSummaryItemName = tree.find(pathToNameNode);
        const lastTechSummaryItemValue = tree.find(pathToValueNode);

        expect(lastTechSummaryItemName.text()).toEqual('Комплектации');
        expect(lastTechSummaryItemValue.find('Link').prop('children')).toEqual('318d, 320d M Sport, 320i и ещё 4');
    });

    it('не должен показать графу с комплектациями, если из нет', () => {
        const offer = cloneOfferWithHelpers(groupCard).value();
        offer.groupping_info.complectations = null;

        const tree = shallow(
            <ListingItemGroup
                offer={ offer }
                params={{ category: 'cars', section: 'new' }}
                searchParams={{ category: 'cars', section: 'new', power: 420 }}
                hasStateSupport={ false }
            />,
            { context: contextMock },
        );

        const lastTechSummaryItemName = tree.find(pathToNameNode);
        const lastTechSummaryItemValue = tree.find(pathToValueNode);

        expect(lastTechSummaryItemName.text()).not.toEqual('Комплектации');
        expect(lastTechSummaryItemValue.text()).not.toEqual('Индивидуальная');
    });
});
