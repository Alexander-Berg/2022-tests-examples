const React = require('react');
const renderer = require('react-test-renderer');

const CardGroupComplectationTechSummary = require('./CardGroupComplectationTechSummary');

// eslint-disable-next-line
const complectation = {"tech_info":{"mark_info":{"code":"MITSUBISHI","name":"Mitsubishi","ru_name":"Митсубиси","logo":{"name":"mark-logo","sizes":{"logo":"//avatars.mds.yandex.net/get-verba/787013/2a00000164d5651d2ae1245140f43816fc51/logo","big-logo":"//avatars.mds.yandex.net/get-verba/787013/2a00000164d5651d2ae1245140f43816fc51/dealer_logo"}}},"model_info":{"code":"PAJERO_SPORT","name":"Pajero Sport","ru_name":"Паджеро Спорт"},"super_gen":{"id":"20663923","name":"III","ru_name":"3","year_from":2015,"price_segment":"MEDIUM"},"configuration":{"id":"20663959","body_type":"ALLROAD_5_DOORS","doors_count":5,"auto_class":"J","trunk_volume_min":430,"trunk_volume_max":2500},"tech_param":{"id":"20972225","displacement":2442,"engine_type":"DIESEL","gear_type":"ALL_WHEEL_DRIVE","transmission":"MECHANICAL","power":181,"power_kvt":133,"human_name":"2.4d MT (181 л.с.) 4WD","acceleration":11.4,"clearance_min":218,"fuel_rate":7.4}},"complectation_id":"20972404","complectation_name":"Invite","option_count":"41","price_from":{"price":1669000,"currency":"RUR","rur_price":1669000,"usd_price":25252,"eur_price":22052,"dprice":1669000,"rur_dprice":1669000,"usd_dprice":25252,"eur_dprice":22052},"price_to":{"price":2345000,"currency":"RUR","rur_price":2345000,"usd_price":35480,"eur_price":30985,"dprice":2345000,"rur_dprice":2345000,"usd_dprice":35480,"eur_dprice":30985},"offer_count":"22","colors_hex":["040001","CACECB","FAFBFB","97948F","0000CC","200204"],"grouping_id":"tech_param_id=20972225,complectation_id=20972404"};
// eslint-disable-next-line
const complectationWithoutColors = {"tech_info":{"mark_info":{"code":"DATSUN","name":"Datsun","ru_name":"Датсун","logo":{"name":"mark-logo","sizes":{"logo":"//avatars.mds.yandex.net/get-verba/937147/2a0000016096e19cff828b85e4003987defa/logo"}}},"model_info":{"code":"MI_DO","name":"mi-DO","ru_name":"ми-ДО"},"super_gen":{"id":"20227455","year_from":2015,"price_segment":"ECONOMY","purpose_group":"FAMILY","no_complect":false},"configuration":{"id":"20227482","body_type":"HATCHBACK_5_DOORS","doors_count":5,"auto_class":"B","human_name":"Хэтчбек 5 дв.","trunk_volume_min":240},"tech_param":{"id":"20227486","displacement":1596,"engine_type":"GASOLINE","gear_type":"FORWARD_CONTROL","transmission":"AUTOMATIC","power":87,"power_kvt":64,"human_name":"1.6 AT (87 л.с.)","acceleration":14.3,"clearance_min":174,"fuel_rate":7.7,"gear_type_autoru":"FRONT","transmission_autoru":"AUTOMATIC"}},"complectation_id":"21091122","complectation_name":"Trust III","option_count":"22","price_from":{"price":620000,"currency":"RUR","rur_price":620000,"usd_price":9380,"eur_price":8192,"dprice":620000,"rur_dprice":620000,"usd_dprice":9380,"eur_dprice":8192},"price_to":{"price":626000,"currency":"RUR","rur_price":626000,"usd_price":9470,"eur_price":8271,"dprice":626000,"rur_dprice":626000,"usd_dprice":9470,"eur_dprice":8271},"grouping_id":"tech_param_id=20227486,complectation_id=21091122"};

it('должен нормально отрендериться', () => {
    const tree = renderer.create(
        <CardGroupComplectationTechSummary
            complectation={ complectation }
        />,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

// кейс для пустой группы
it('должен нормально отрендериться, если нет цветов', () => {
    const tree = renderer.create(
        <CardGroupComplectationTechSummary
            complectation={ complectationWithoutColors }
        />,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});
