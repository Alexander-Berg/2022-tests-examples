import format from 'date-fns/format';
import ru from 'date-fns/locale/ru';
import parseISO from 'date-fns/parseISO';
import { isString } from 'lodash';

export default function (inputDate: string | Date, formatStr: string): string {
  const date = isString(inputDate) ? parseISO(inputDate) : inputDate;
  return format(date, formatStr, { locale: ru });
}
