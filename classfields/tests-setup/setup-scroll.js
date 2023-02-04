/* global window */
/*
 * Переопределяем поведение скролл-функций в window на дефолтное
 * Сделано, для того чтобы убрать анимацию при скролле с параметром behavior: smooth
 */
if (typeof window !== 'undefined') {
    const _scrollTo = window.scrollTo;

    window.scrollTo = function(opts) {
        if (typeof opts === 'object') {
            return _scrollTo.call(this, {
                ...opts,
                behavior: 'auto'
            });
        }

        return _scrollTo.apply(this, arguments);
    };
}
