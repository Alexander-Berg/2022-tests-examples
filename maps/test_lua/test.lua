tiles_locale = require "yandex.maps.liblua_tiles_locale"

failed = 0
function check_res(lang, expected)
    result = tiles_locale.best_tiles_locale(lang)
    if(result ~= expected) then
        print("Test failed: " .. lang)
        print("\texpected: " .. expected)
        print("\tresult:   " .. result)
        failed = failed + 1
    end
end


-- Tile locales
check_res("ru_RU", "ru_RU")
check_res("ru_UA", "ru_UA")
check_res("uk_UA", "uk_UA")
check_res("tr_TR", "tr_TR")
check_res("en_RU", "en_RU")
check_res("en_TR", "mul-Latn_TR")
check_res("en_UA", "mul-Latn_UA")

-- Other supported locales
check_res("en_US", "mul-Latn_001")
check_res("he_IL", "he_IL")
check_res("en_IL", "en_IL")
check_res("ru_IL", "en_IL")

check_res("ru_TR", "mul-Latn_TR")

check_res("fi_FI", "mul-Latn_UA")
check_res("en_FI", "mul-Latn_UA")
check_res("ru_FI", "ru_UA")
check_res("fr_FR", "mul-Latn_UA")
check_res("en_FR", "mul-Latn_UA")
check_res("ru_FR", "ru_UA")
check_res("de_DE", "mul-Latn_001")
check_res("en_DE", "mul-Latn_001")
check_res("ru_DE", "mul_001")
check_res("ka_GE", "mul_UA")
check_res("en_GE", "mul-Latn_UA")
check_res("ru_GE", "ru_UA")
check_res("ro_MD", "mul-Latn_UA")
check_res("en_MD", "mul-Latn_UA")
check_res("ru_MD", "ru_UA")
check_res("iw_IL", "he_IL")
check_res("iw_RU", "en_RU")
check_res("ru_IL", "en_IL")

-- Unsupported locales
check_res("ru", "ru_RU")
check_res("ab_AB", "ru_RU")
check_res("de_RU", "en_RU")
check_res("ru_US", "mul_001")
check_res("ru_IT", "mul_001")
check_res("it_IT", "mul-Latn_001")
check_res("be-Latn_BY", "mul-Latn_UA")
check_res("en-SCOTLAND_GB_GLASGOW", "mul_001")  -- should be mul-Latn_001
check_res("nn-Latn-NO-hognorsk_NO_LOCAL", "mul-Latn_001")
check_res("unk", "mul_001")

-- Invalid locales
check_res("unknown", "mul_001")
check_res("kz_KZ", "mul_001")

assert(failed == 0, 'Tests: ' .. failed .. ' failed')
print('Tests: OK')
