/**
 * Секретные данные преобразует в звездочки.
 *
 * Используется для вывода в console.log
 */
export function secret(text) {
   const chars = Array.from(text);
   const len = chars.length;

   function hide(c) {
      return c.map(() => '*').join('');
   }

   return len < 3 ? hide(chars) : `${chars[0]}${hide(chars.slice(1, len - 1))}${chars.slice(len - 1)}`;
}
