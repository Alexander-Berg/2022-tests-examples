import moment from 'moment';

const timezone = 0; // Московское время

const today = moment().format('YYYY-MM-DD');

const time = {
  TIME_TODAY: today,
  TIME_OFFSET: timezone,
  DATE_SNIPPET: {
    months: [
      'января',
      'февраля',
      'марта',
      'апреля',
      'мая',
      'июня',
      'июля',
      'августа',
      'сентября',
      'октября',
      'ноября',
      'декабря',
    ],
  },
} as const;

export default time;
