package selector

import (
	"fmt"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestMatchGlob(t *testing.T) {
	tests := []struct {
		glob     string
		value    string
		expected bool
	}{
		{glob: "a", value: "a", expected: true},
		{glob: "ab", value: "ab", expected: true},
		{glob: "ab?", value: "abc", expected: true},
		{glob: "a?c", value: "abc", expected: true},
		{glob: "?bc", value: "abc", expected: true},
		{glob: "??c", value: "abc", expected: true},
		{glob: "a??", value: "abc", expected: true},
		{glob: "???", value: "abc", expected: true},
		{glob: "", value: "", expected: true},
		{glob: "*", value: "", expected: true},
		{glob: "**", value: "", expected: true},
		{glob: "a*", value: "abc", expected: true},
		{glob: "ab*", value: "abc", expected: true},
		{glob: "a*c", value: "aaaccc", expected: true},
		{glob: "a*c", value: "abc", expected: true},
		{glob: "*bc", value: "abc", expected: true},
		{glob: "*c", value: "abc", expected: true},
		{glob: "*", value: "abc", expected: true},
		{glob: "*ac", value: "ac", expected: true},
		{glob: "a*c*de", value: "abcdfbcde", expected: true},
		{glob: "a*d*g", value: "abcdefg", expected: true},
		{glob: "a**g", value: "abcdefg", expected: true},
		{glob: "a***", value: "a", expected: true},
		{glob: "a***", value: "abcdefg", expected: true},
		{glob: "**a", value: "a", expected: true},

		{glob: "", value: "a", expected: false},
		{glob: "a", value: "", expected: false},
		{glob: "b", value: "a", expected: false},
		{glob: "bb?", value: "abc", expected: false},
		{glob: "?bb", value: "abc", expected: false},
		{glob: "b?b", value: "abc", expected: false},
		{glob: "b??", value: "abc", expected: false},
		{glob: "ab??", value: "abc", expected: false},
		{glob: "??b", value: "abc", expected: false},
		{glob: "b*", value: "abc", expected: false},
		{glob: "bb*", value: "abc", expected: false},
		{glob: "b*c", value: "abc", expected: false},
		{glob: "b*c", value: "bcde", expected: false},
		{glob: "*bb", value: "abc", expected: false},
		{glob: "*d", value: "abc", expected: false},
		{glob: "a*z*g", value: "abcdefg", expected: false},
		{glob: "a*b", value: "abbe", expected: false},
		{glob: "a*c*g", value: "abcdefghi", expected: false},
	}
	for _, test := range tests {
		t.Run(fmt.Sprintf("match %s to glob %s", test.value, test.glob), func(t *testing.T) {
			result := MatchGlob(test.glob, test.value)
			assert.Equal(t, test.expected, result)
		})
	}
}
