import { Service } from '../../../types/common';

export const POLL_DATA_1 = {
    id: 1,
    service: Service.autoru,
    question: 'Вы брали ипотеку?',
    answers: ['Да', 'Нет'],
};

export const POLL_DATA_2 = {
    id: 2,
    service: Service.autoru,
    question: 'У вас есть КАСКО?',
    answers: ['Да', 'Нет'],
};

export const POLL_DATA_3 = {
    id: 3,
    service: Service.autoru,
    question: 'Как вам идея QR-кодов вместо обычных прав?',
    answers: ['Нравится!', 'Я лучше буду возить с собой ВУ и СТС'],
};

export const POLL_DATA_4 = {
    id: 4,
    service: Service.realty,
    question: 'Сколько стоит ваша квартира',
    answers: ['До 1 миллиона', 'От 1 до 3х миллионов', 'Более 3х миллионов'],
};

export const POLL_DATA_5 = {
    id: 5,
    service: Service.autoru,
    question: 'Сколько раз вы попадали в ДТП?',
    answers: ['Ни разу', 'Один', 'Два', 'Три и более'],
};

export const CREATE_POLL_DATA_1 = {
    question: 'Вы брали ипотеку?',
    answers: ['Да', 'Нет'],
};

export const UPDATE_POLL_DATA_1 = {
    question: 'У вас есть КАСКО?',
    answers: ['Да', 'Нет'],
};
