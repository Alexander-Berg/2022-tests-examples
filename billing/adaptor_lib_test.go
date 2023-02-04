package manifest

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/common"
)

func TestCondition_Check(t *testing.T) {
	tests := []struct {
		testname  string
		left      ValueGetter
		operator  string
		right     ValueGetter
		want      bool
		wantError bool
	}{
		{
			"field to const ==",
			&PathValueGetter{
				from: common.NewSyncStringMapWithData(map[string]any{
					"a": map[string]int{"b": 100},
				}),
				path: []string{"a", "b"},
			},
			"==",
			&ConstValueGetter{val: 100},
			true,
			false,
		},
		{
			"field to const !=",
			&PathValueGetter{
				from: common.NewSyncStringMapWithData(map[string]any{
					"a": map[string]int{"b": 100},
				}),
				path: []string{"a", "b"},
			},
			"!=",
			&ConstValueGetter{val: 100},
			false,
			false,
		},
		{
			"is field empty",
			&PathValueGetter{
				from: common.NewSyncStringMapWithData(map[string]any{
					"a": []int{},
				}),
				path: []string{"a"},
			},
			"empty",
			nil,
			true,
			false,
		},
		{
			"error for non comparable elements",
			&ConstValueGetter{val: []int{1}},
			"!=",
			&ConstValueGetter{val: []int{3}},
			false,
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			condition, _ := NewCondition(test.left, test.operator, test.right)
			status, err := condition.Check()
			if test.wantError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, status)
			}
		})
	}
}

func TestLibManifestAdaptor_MakeMap(t *testing.T) {
	adaptor := LibManifestAdaptor{}
	tests := []struct {
		testname  string
		args      []any
		wantMap   map[string]any
		wantError bool
	}{
		{
			"successfully create map",
			[]any{
				"test", 1, "map", 2,
			},
			map[string]any{
				"test": 1,
				"map":  2,
			},
			false,
		},
		{
			"error if odd argument count",
			[]any{
				"test", 1, "map",
			},
			nil,
			true,
		},
		{
			"error if key is not string",
			[]any{
				[]int{1}, "value",
			},
			nil,
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			m, err := adaptor.MakeMap(context.Background(), test.args...)
			if test.wantError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.wantMap, m)
			}
		})
	}
}

func TestLibManifestAdaptor_Join(t *testing.T) {
	adaptor := LibManifestAdaptor{}
	tests := []struct {
		testname  string
		parts     []any
		want      []any
		wantError bool
	}{
		{
			"successfully join",
			[]any{
				[]any{1, 2},
				[]any{"3"},
			},
			[]any{1, 2, "3"},
			false,
		},
		{
			"error if not array of arrays",
			[]any{1, 2, 3},
			nil,
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			items, err := adaptor.Join(context.Background(), test.parts)
			if test.wantError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, items)
			}
		})
	}
}

func TestLibManifestAdaptor_JoinToString(t *testing.T) {
	adaptor := LibManifestAdaptor{}
	tests := []struct {
		testname  string
		parts     []any
		sep       string
		want      string
		wantError bool
	}{
		{
			"successfully join",
			[]any{1, 2, "3"},
			":",
			"1:2:3",
			false,
		},
		{
			"error if can't cast value to string",
			[]any{"1", []int{1, 2, 3}},
			"",
			"",
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			items, err := adaptor.JoinToString(context.Background(), test.parts, test.sep)
			if test.wantError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, items)
			}
		})
	}
}

func TestLibManifestAdaptor_Filter(t *testing.T) {
	adaptor := LibManifestAdaptor{}
	tests := []struct {
		testname     string
		items        []any
		path         string
		operator     string
		compareValue any
		want         []any
		wantError    bool
	}{
		{
			"filter == ints",
			[]any{
				map[string]any{
					"a": map[string]any{
						"b": 100,
						"c": "1",
					},
				},
				map[string]any{
					"a": map[string]any{
						"b": "200",
						"c": "2",
					},
				},
			},
			"item.a.b",
			"==",
			100,
			[]any{
				map[string]any{
					"a": map[string]any{
						"b": 100,
						"c": "1",
					},
				},
			},
			false,
		},
		{
			"filter == strings",
			[]any{
				map[string]any{
					"a": map[string]any{
						"b": "100",
						"c": "1",
					},
				},
				map[string]any{
					"a": map[string]any{
						"b": "200",
						"c": "2",
					},
				},
			},
			"item.a.b",
			"==",
			"100",
			[]any{
				map[string]any{
					"a": map[string]any{
						"b": "100",
						"c": "1",
					},
				},
			},
			false,
		},
		{
			"filter ==",
			[]any{
				map[string]any{
					"a": map[string]any{
						"b": "100",
						"c": "1",
					},
				},
				map[string]any{
					"a": map[string]any{
						"b": "200",
						"c": "2",
					},
				},
			},
			"item.a.b",
			"!=",
			"100",
			[]any{
				map[string]any{
					"a": map[string]any{
						"b": "200",
						"c": "2",
					},
				},
			},
			false,
		},
		{
			"error if wrong path",
			[]any{
				map[string]any{
					"a": map[string]any{
						"b": 100,
						"c": "1",
					},
				},
			},
			"item.a.d",
			"==",
			"",
			nil,
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			items, err := adaptor.Filter(context.Background(), test.items, test.path, test.operator, test.compareValue)
			if test.wantError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, items)
			}
		})
	}
}

func TestLibManifestAdaptor_If(t *testing.T) {
	adaptor := LibManifestAdaptor{}
	tests := []struct {
		testname        string
		left            any
		operator        string
		right           any
		trueVal         any
		falseVal        any
		conditionResult bool
		wantError       bool
	}{
		{
			"get trueVal because empty",
			[]any{},
			"empty",
			"",
			[]any{1, 2, 3},
			[]any{3, 2, 1},
			true,
			false,
		},
		{
			"get falseVal because left != right",
			"1",
			"==",
			"2",
			"12",
			"34",
			false,
			false,
		},
		{
			"get error if wrong operator",
			"1",
			"wrong",
			"2",
			"12",
			"34",
			false,
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			val, err := adaptor.If(context.Background(), test.left, test.operator, test.right, test.trueVal, test.falseVal)
			if test.wantError {
				require.Error(t, err)
				return
			}

			var want any
			if test.conditionResult {
				want = test.trueVal
			} else {
				want = test.falseVal
			}

			require.NoError(t, err)
			assert.Equal(t, want, val)
		})
	}
}

func TestLibManifestAdaptor_Zip(t *testing.T) {
	adaptor := LibManifestAdaptor{}
	tests := []struct {
		testname  string
		parts     []any
		want      [][]any
		wantError bool
	}{
		{
			"successfully zip",
			[]any{
				[]any{1, 2, 3},
				[]any{"a", "b"},
			},
			[][]any{
				{1, "a"},
				{2, "b"},
			},
			false,
		},
		{
			"empty state",
			[]any{
				[]any{1, 2, 3},
				[]any{},
			},
			[][]any{},
			false,
		},
		{
			"error if one part is not array",
			[]any{
				[]any{1, 2, 3},
				"string",
				[]any{"a", "b"},
			},
			nil,
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			m, err := adaptor.Zip(context.Background(), test.parts)
			if test.wantError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, m)
			}
		})
	}
}

func TestLibManifestAdaptor_Distinct(t *testing.T) {
	adaptor := LibManifestAdaptor{}
	tests := []struct {
		testname  string
		items     []any
		want      []any
		wantError bool
	}{
		{
			"successfully filter",
			[]any{1, 2, 2, 3, 3, 3},
			[]any{1, 2, 3},
			false,
		},
		{
			"error for non comparable elements",
			[]any{[]int{1, 2, 3}},
			nil,
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			m, err := adaptor.Distinct(context.Background(), test.items)
			if test.wantError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, m)
			}
		})
	}
}

func TestLibManifestAdaptor_Set(t *testing.T) {
	adaptor := LibManifestAdaptor{}
	value := 1
	t.Run("successfully set", func(t *testing.T) {
		result, err := adaptor.Set(context.Background(), value)
		require.NoError(t, err)
		assert.Equal(t, result, value)
	})
}

func TestLibManifestAdaptor_Max(t *testing.T) {
	adaptor := LibManifestAdaptor{}
	tests := []struct {
		testname  string
		items     []any
		want      any
		wantError bool
	}{
		{
			"successfully got max true",
			[]any{false, true, false},
			true,
			false,
		},
		{
			"successfully got max false",
			[]any{false},
			false,
			false,
		},
		{
			"successfully got max ordered",
			[]any{int64(2), "3", 1.0},
			int64(3),
			false,
		},
		{
			"successfully got max time",
			[]any{time.Date(2022, 12, 31, 23, 59, 59, 0, time.UTC), "1992-01-01T00:00:00Z"},
			time.Date(2022, 12, 31, 23, 59, 59, 0, time.UTC),
			false,
		},
		{
			"error if slice is empty",
			[]any{},
			nil,
			true,
		},
		{
			"error if unknown type",
			[]any{1 + 1i},
			nil,
			true,
		},
		{
			"error if unable to cast",
			[]any{1, "not an integer"},
			nil,
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			m, err := adaptor.Max(context.Background(), test.items)
			if test.wantError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, m)
			}
		})
	}
}
