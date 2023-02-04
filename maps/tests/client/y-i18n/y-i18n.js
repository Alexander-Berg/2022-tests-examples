/**
 * Мелкий модуль, который заменяет локали при выполнении юнит тестов.
 */
modules.define('y-i18n', [], function (provide) {
    provide(function (keyset, key) {
        return keyset + '_' + key;
    });
});
