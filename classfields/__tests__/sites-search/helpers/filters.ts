// DECORAIONS
export const DECORATION_FILTER_VALUES = [
    'ROUGH',
    'CLEAN',
    'TURNKEY',
    'NO_DECORATION',
    'PRE_CLEAN',
    'WHITE_BOX',
] as const;

export const decorationSeoTextsByValue = {
    ROUGH: 'с черновой отделкой',
    CLEAN: 'с чистовой отделкой',
    TURNKEY: 'с отделкой под ключ',
    NO_DECORATION: 'без отделки',
    PRE_CLEAN: 'с предчистовой отделкой',
    WHITE_BOX: 'с отделкой white-box',
};
