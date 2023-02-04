package manifest

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/common"
)

func TestParam_getValue(t *testing.T) {
	data := common.NewSyncStringMapWithData(map[string]any{
		"a": common.NewSyncStringMapWithData(map[string]any{
			"b": 100,
			"c": "test_c",
			"d": common.NewSyncStringMapWithData(map[string]any{
				"e": 200,
				"g": nil,
			}),
			"e": []int{0, 100, 200},
		}),
	})

	tests := []struct {
		testname  string
		data      *common.SyncStringMap
		param     Param
		want      any
		withError bool
	}{
		{
			testname: "get value by key",
			data:     data,
			param: Param{
				Key: "a.b",
			},
			want:      100,
			withError: false,
		},
		{
			testname: "get value from array",
			data:     data,
			param: Param{
				Key: "a.e.2",
			},
			want:      200,
			withError: false,
		},
		{
			testname: "get value by keys",
			data:     data,
			param: Param{
				Keys: []string{"a.b", "a.d.e", "a.d.g", "a.c"},
			},
			want:      []any{100, 200, nil, "test_c"},
			withError: false,
		},
		{
			testname: "get error for non-existent path by key",
			data:     data,
			param: Param{
				Key: "z.z.z",
			},
			withError: true,
		},
		{
			testname: "get error for non-existent path by keys",
			data:     data,
			param: Param{
				Keys: []string{"a.b", "z.z.z", "a.c"},
			},
			withError: true,
		},
		{
			testname: "get nil for non-existent path if nullifyMissing by key",
			data:     data,
			param: Param{
				Key:            "z.z.z",
				NullifyMissing: true,
			},
			want:      nil,
			withError: false,
		},
		{
			testname: "get nil for non-existent path if nullifyMissing by keys",
			data:     data,
			param: Param{
				Keys:           []string{"a.b", "z.z.z", "a.c"},
				NullifyMissing: true,
			},
			want:      []any{100, nil, "test_c"},
			withError: false,
		},
		{
			testname: "get error for nil if disallowNil by key",
			data:     data,
			param: Param{
				Key:         "a.d.g",
				DisallowNil: true,
			},
			withError: true,
		},
		{
			testname: "get error for nil if disallowNil by keys",
			data:     data,
			param: Param{
				Keys:        []string{"a.d.g"},
				DisallowNil: true,
			},
			withError: true,
		},
		{
			testname:  "get error if not (key and keys)",
			data:      data,
			param:     Param{},
			withError: true,
		},
		{
			testname: "get error if key and keys",
			data:     data,
			param: Param{
				Key:  "a.b",
				Keys: []string{"a.b"},
			},
			withError: true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			actual, err := test.param.getValue(test.data)
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, actual)
			}
		})
	}
}

func TestParam_Get(t *testing.T) {
	data := common.NewSyncStringMapWithData(map[string]any{
		"a": common.NewSyncStringMapWithData(map[string]any{
			"b": 100,
			"c": "test_c",
			"d": common.NewSyncStringMapWithData(map[string]any{
				"e": 200,
				"f": "300",
				"g": nil,
			}),
		}),
		"b": []map[string]any{
			{"a": 1, "b": 2},
			{"a": 2},
		},
	})

	tests := []struct {
		testname  string
		data      *common.SyncStringMap
		param     Param
		want      any
		withError bool
	}{
		{
			testname: "get",
			data:     data,
			param: Param{
				Action: "get",
				Key:    "a.b",
			},
			want:      100,
			withError: false,
		},
		{
			testname: "const",
			data:     data,
			param: Param{
				Action: "const",
				Value:  "test",
			},
			want:      "test",
			withError: false,
		},
		{
			testname: "const array",
			data:     data,
			param: Param{
				Action: "const",
				Value:  []int{1, 2, 3},
			},
			want:      []int{1, 2, 3},
			withError: false,
		},
		{
			testname: "const array of strings",
			data:     data,
			param: Param{
				Action: "const",
				Value:  []string{"first", "second"},
			},
			want:      []string{"first", "second"},
			withError: false,
		},
		{
			testname: "const array of strings from ints",
			data:     data,
			param: Param{
				Action: "const",
				Value:  []int{1, 2, 3},
				Type:   "string",
			},
			want:      []any{"1", "2", "3"},
			withError: false,
		},
		{
			testname: "set_null",
			data:     data,
			param: Param{
				Action: "set_null",
			},
			want:      nil,
			withError: false,
		},
		{
			testname: "unknown",
			data:     data,
			param: Param{
				Action: "some weird",
			},
			withError: true,
		},
		{
			testname: "get with type string",
			data:     data,
			param: Param{
				Action: "get",
				Key:    "a.b",
				Type:   "string",
			},
			want:      "100",
			withError: false,
		},
		{
			testname: "get with type int64",
			data:     data,
			param: Param{
				Action: "get",
				Key:    "a.d.f",
				Type:   "int64",
			},
			want:      int64(300),
			withError: false,
		},
		{
			testname: "extract with array type int64",
			data:     data,
			param: Param{
				Action: "extract",
				Path:   "$.b[1].*",
				Type:   "string",
			},
			want:      []any{"2"},
			withError: false,
		},
		{
			testname: "get with error if unknown type",
			data:     data,
			param: Param{
				Action: "get",
				Key:    "a.c",
				Type:   "int128",
			},
			withError: true,
		},
		{
			testname: "get with error if can't cast",
			data:     data,
			param: Param{
				Action: "get",
				Key:    "a.c",
				Type:   "int64",
			},
			withError: true,
		},
		{
			testname: "get with type int for keys",
			data:     data,
			param: Param{
				Action: "get",
				Keys:   []string{"a.d.f", "a.b"},
				Type:   "int64",
			},
			want:      []any{int64(300), int64(100)},
			withError: false,
		},
		{
			testname: "get error if can't cast for keys",
			data:     data,
			param: Param{
				Action: "get",
				Keys:   []string{"a.d.f", "a.c"},
				Type:   "int64",
			},
			withError: true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			actual, err := test.param.Get(test.data)
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, actual)
			}
		})
	}
}

func TestParam_ExtractValue(t *testing.T) {
	data := common.NewSyncStringMapWithData(map[string]any{
		"a": common.NewSyncStringMapWithData(map[string]any{
			"b": []int{0, 100, 200},
		}),
	})

	tests := []struct {
		testname  string
		data      *common.SyncStringMap
		param     Param
		want      any
		withError bool
		errorType error
	}{
		{
			testname: "extract jsonpath index oob error",
			data:     data,
			param: Param{
				Path: "$.a.b[5]",
			},
			withError: true,
			errorType: ErrorOutOfBounds,
		},
		{
			testname: "extract jsonpath index oob nullify no error",
			data:     data,
			param: Param{
				Path:           "$.a.b[5]",
				NullifyMissing: true,
			},
			withError: false,
		},
		{
			testname: "extract unknown key error",
			data:     data,
			param: Param{
				Path: "$.a.notexists",
			},
			withError: true,
			errorType: ErrorUnknownKey,
		},
		{
			testname: "extract unknown key error nullify no error",
			data:     data,
			param: Param{
				Path:           "$.a.notexists",
				NullifyMissing: true,
			},
			withError: false,
		},
	}
	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			actual, err := test.param.extractValue(test.data)
			if test.withError {
				require.Error(t, err)
				if test.errorType != nil {
					assert.ErrorIs(t, err, test.errorType)
				}
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, actual)
			}
		})
	}
}
