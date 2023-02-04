const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const CardGroupTabs = require('./CardGroupTabs');

const LINKS_EXPECTED = [
    'link/card-group/?category=cars&section=new&catalog_filter=mark%3DMITSUBISHI%2Cmodel%3DASX%2Cgeneration%3D21905869%2Cconfiguration%3D21905949',
    'link/card-group-about/?category=cars&section=new&catalog_filter=mark%3DMITSUBISHI%2Cmodel%3DASX%2Cgeneration%3D21905869%2Cconfiguration%3D21905949',
    'link/card-group-tech/?category=cars&section=new&catalog_filter=mark%3DMITSUBISHI%2Cmodel%3DASX%2Cgeneration%3D21905869%2Cconfiguration%3D21905949',
    'link/card-group-options/?category=cars&section=new&catalog_filter=mark%3DMITSUBISHI%2Cmodel%3DASX%2Cgeneration%3D21905869%2Cconfiguration%3D21905949',
];

it('убирает tech_params из ссылок', () => {
    const searchParams = {
        category: 'cars',
        section: 'new',
        catalog_filter: [ {
            configuration: '21905949',
            generation: '21905869',
            mark: 'MITSUBISHI',
            model: 'ASX',
            tech_param: '21906068',
        } ],
    };

    const tree = shallow(
        <CardGroupTabs
            pageType="card-group-tech"
            complectations={ [] }
            searchParameters={ searchParams }
        />,
        { context: contextMock },
    );

    const linksResult = [];

    tree.find('TabsItem').forEach((node) => {
        linksResult.push(node.prop('href'));
    });

    expect(linksResult).toEqual(LINKS_EXPECTED);
});
