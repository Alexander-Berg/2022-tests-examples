package message

import (
	"github.com/stretchr/testify/require"
	"testing"
)

func TestNewEscaper(t *testing.T) {
	escaper := NewMarkdown()
	result := escaper.escape("* `my` [mes_sage]")
	require.Equal(t, "\\* \\`my\\` \\[mes\\_sage]", result)
}

func TestValidateFail(t *testing.T) {
	invalidCases := map[string][]string{
		"unclosed": {
			"*",
			"_",
			"`",
			"```",
			"[",
			"[]",
			"[](",
		},
		"escape": {
			"[url]\\(http://ya.ru)",
			"*\\**",
			"_\\__",
			"`\\``",
		},
		"odd": {
			"***",
			"___",
		},
		"code": {
			"````",
			"``` `",
			"`````",
			"` ````",
			"`` ```",
			"``` ``",
			"```` `",
		},
	}

	for name, cases := range invalidCases {
		for _, testCase := range cases {
			result := tgMarkdownV1Validate(testCase)
			require.False(t, result, "%s: %s", name, testCase)
		}
	}
}

func TestValidateOk(t *testing.T) {
	validCases := map[string][]string{
		"empty": {
			"",
			"**",
			"``",
			"__",
			"``````",
			"[]()",
			"`` ``",
			"` ```",
		},
		"simple": {
			"*a*",
			"`a`",
			"_a_",
			"```a```",
			"[a](b)",
		},
		"escaped": {
			"\\*",
			"\\`",
			"\\_",
			"\\[",
		},
		"links": {
			"[url](http://ya.ru)",
			"\\[url](http://ya.ru)",
			"[url\\](http://ya.ru)",
			"[url](http://ya.ru\\)",
		},
	}

	for name, cases := range validCases {
		for _, testCase := range cases {
			result := tgMarkdownV1Validate(testCase)
			require.True(t, result, "%s: %s", name, testCase)
		}
	}
}
