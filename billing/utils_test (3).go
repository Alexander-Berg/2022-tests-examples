package manifest

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func Test_sortGraph(t *testing.T) {
	tests := []struct {
		testname  string
		graph     map[string][]string
		vertexes  []string
		colors    map[string]string
		withError bool
	}{
		{
			"graph without edges",
			map[string][]string{
				"a": {}, "b": {},
			},
			[]string{"a", "b"},
			map[string]string{},
			false,
		},
		{
			"graph without cycle",
			map[string][]string{
				"a": {}, "b": {"a"},
			},
			[]string{"a", "b"},
			map[string]string{},
			false,
		},
		{
			"error if unknown vertex",
			map[string][]string{},
			[]string{"a"},
			map[string]string{},
			true,
		},
		{
			"error if graph with cycle",
			map[string][]string{
				"a": {"b"}, "b": {"c"}, "c": {"a"},
			},
			[]string{"a", "b", "c"},
			map[string]string{},
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			_, err := sortGraph(test.graph, test.vertexes, test.colors)
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Len(t, test.colors, len(test.vertexes))
				for _, v := range test.colors {
					assert.Equal(t, "black", v)
				}
			}
		})
	}
}

func Test_joinValues(t *testing.T) {
	tests := []struct {
		testname  string
		input     any
		want      []any
		withError bool
	}{
		{
			testname: "join",
			input: []any{
				[]any{1, 2, 3},
				[]any{"4", "5", "6"},
			},
			want:      []any{1, 2, 3, "4", "5", "6"},
			withError: false,
		},
		{
			testname:  "error on non-array",
			input:     100,
			withError: true,
		},
		{
			testname:  "error on non array of arrays",
			input:     []any{1, 2, 3},
			withError: true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			actual, err := joinValues(test.input)
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, actual)
			}
		})
	}
}

func Test_joinValuesToString(t *testing.T) {
	tests := []struct {
		testname  string
		input     any
		sep       string
		want      string
		withError bool
	}{
		{
			testname:  "join of different types",
			input:     []any{"1", 2, []byte{97}, "end"},
			sep:       ":",
			want:      "1:2:a:end",
			withError: false,
		},
		{
			testname:  "error on non-array",
			input:     100,
			withError: true,
		},
		{
			testname:  "error if can't cast value to string",
			input:     []any{"1", []int{1, 2, 3}},
			withError: true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			actual, err := joinValuesToString(test.input, test.sep)
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, actual)
			}
		})
	}
}

func Test_isNilFixed(t *testing.T) {
	var m map[string]string
	var ptr *int
	tests := []struct {
		testname string
		input    any
		want     bool
	}{
		{
			testname: "int is not nil",
			input:    100,
			want:     false,
		},
		{
			testname: "array is not nil",
			input:    []string{"1"},
			want:     false,
		},
		{
			testname: "nil is nil",
			input:    nil,
			want:     true,
		},
		{
			testname: "uninitialized map is nil",
			input:    m,
			want:     true,
		},
		{
			testname: "uninitialized ptr is nil",
			input:    ptr,
			want:     true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			actual := isNilFixed(test.input)
			assert.Equal(t, test.want, actual)
		})
	}
}
