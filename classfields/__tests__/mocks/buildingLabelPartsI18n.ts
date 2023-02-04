import i18nLib from 'realty-core/view/react/libs/i18n';

const langs = {
    ru: {
        n_floors: '%{floor}\u00A0этаж',
        n_floors_from: '%{floor}\u00A0этаж\u00A0из\u00A0%{total}',
        newBuilding: 'новостройка',
        builtYear: '%{year}\u00A0г.',
        builtQuarterAndYear: '%{quarter}\u00A0кв. %{year}\u00A0г.',
        villageName: 'КП «%{name}»',
    },
};

export const i18n = i18nLib.include((lang) => langs[lang]);
