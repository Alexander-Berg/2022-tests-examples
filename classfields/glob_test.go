package compiled_test

import (
	"github.com/YandexClassifieds/vtail/cmd/streamer/filter/compiled"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestGlob(t *testing.T) {
	testCases := []struct {
		pattern string
		subject string
		match   bool
	}{
		{"", "", true},
		{"*", "any", true},
		{"*", "", true},
		{"m*5", "m5", true},
		{"m*5", "m125", true},
		{"m*5", "lm5", false},
		{"m*5", "m53", false},
		{"m*", "m125", true},
		{"m*", "m", true},
		{"m*", "3m", false},
		{"*m", "3m", true},
		{"*m", "m", true},
		{"*m", "3m5", false},
		{"*m*", "3m5", true},
		{"*m*", "m", true},
		{"*m*", "mm", true},
		{"*m*", "mmm", true},
		{"mdb*", "md", false},
		{"*mdb", "m", false},
		{"*ack*9*", "I acked 99", true},
		{`\*`, "*", true},
		{`*\*`, "*", true},
		{`*\*`, "1*", true},
		{`\**`, "*", true},
		{`\**`, "*2", true},
		{`*\**`, "*", true},
		{`*\**`, "1*2", true},
		{`\**\*`, "**", true},
		{`\**\*`, "***", true},
	}

	for _, testCase := range testCases {
		glob := compiled.NewGlob(testCase.pattern)
		result := glob.Match(testCase.subject)
		require.Equal(t, testCase.match, result, "%+v", testCase)
	}
}
