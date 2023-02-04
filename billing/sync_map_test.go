package common

import (
	"encoding/json"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"
	"gopkg.in/yaml.v2"

	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xyaml"
)

func (s *testSuite) TestSyncMapMarshalNested() {
	data := NewSyncStringMapWithData(map[string]any{
		"a": NewSyncStringMapWithData(map[string]any{"b": "c"}),
	})
	bytes, err := json.Marshal(data)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), "{\"a\":{\"b\":\"c\"}}", string(bytes))
}

type testSuite struct {
	btesting.BaseSuite
}

func TestSyncMap(t *testing.T) {
	suite.Run(t, &testSuite{})
}

func Test_SyncUnmarshalNestedYAML(t *testing.T) {
	data := `
outter:
  inner:
    foo: true
    bar: 1
`

	correctMap := xyaml.MapStr{
		"outter": xyaml.MapStr{
			"inner": xyaml.MapStr{
				"foo": true,
				"bar": 1,
			},
		},
	}

	testMap := NewSyncStringMap()

	err := yaml.Unmarshal([]byte(data), testMap)

	require.NoError(t, err)

	assert.Equal(t, correctMap, testMap.ToStrMap())
}

// This test checks that setting nested map key won't modify original map.
func Test_SyncMapInitCopiesNestedMapOnSet(t *testing.T) {

	input := map[string]any{
		"hello": map[string]any{
			"world": true,
		},
	}

	synced := NewSyncStringMapWithData(input)

	err := synced.SetRecursive(false, strings.Split("hello.world", ".")...)

	assert.NoError(t, err)

	assert.Equal(t, true, input["hello"].(map[string]any)["world"])
}

func Test_GetRecursive(t *testing.T) {
	data := NewSyncStringMapWithData(map[string]any{
		"a": NewSyncStringMapWithData(map[string]any{
			"b": 100,
			"c": "test_c",
			"d": NewSyncStringMapWithData(map[string]any{
				"e": 200,
			}),
		}),
		"f": "100",
		"g": [][]map[string]int{{{"state": 0}}, {{"state": 100}}, {{"state": 200}}},
	})

	tests := []struct {
		testname  string
		data      *SyncStringMap
		keys      []string
		want      any
		withError bool
	}{
		{
			testname:  "get top level value",
			data:      data,
			keys:      []string{"f"},
			want:      "100",
			withError: false,
		},
		{
			testname:  "get nested value",
			data:      data,
			keys:      []string{"a", "d", "e"},
			want:      200,
			withError: false,
		},
		{
			testname:  "get value from array",
			data:      data,
			keys:      []string{"g", "1", "0", "state"},
			want:      100,
			withError: false,
		},
		{
			testname:  "get value from array - error if wrong field",
			data:      data,
			keys:      []string{"g", "1", "0", "wrong"},
			withError: true,
		},
		{
			testname:  "get value from array - error if no key",
			data:      data,
			keys:      []string{"g", "1", "100"},
			withError: true,
		},
		{
			testname:  "get all values from array",
			data:      data,
			keys:      []string{"g", "*", "*", "state"},
			want:      []any{0, 100, 200},
			withError: false,
		},
		{
			testname:  "get all values from array - error if wrong field",
			data:      data,
			keys:      []string{"g", "*", "*", "wrong"},
			withError: true,
		},
		{
			testname:  "get error for non-existent path",
			data:      data,
			keys:      []string{"a", "d", "z", "e"},
			withError: true,
		},
		{
			testname:  "get map",
			data:      data,
			keys:      []string{"a", "d", "$"},
			want:      NewSyncStringMapWithData(map[string]any{"e": 200}),
			withError: false,
		},
		{
			testname:  "get map - error if can't cast value to map",
			data:      data,
			keys:      []string{"a", "d", "e", "$"},
			withError: true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			actual, err := test.data.GetRecursive(test.keys...)
			if test.withError {
				require.Error(t, err)
				return
			}
			require.NoError(t, err)
			if want, ok := test.want.([]any); ok {
				assert.Len(t, actual, len(want))
				assert.Subset(t, test.want, actual)
			} else {
				assert.Equal(t, test.want, actual)
			}
		})
	}
}

func Test_SetRecursive(t *testing.T) {
	mapGenerator := func() *SyncStringMap {
		return NewSyncStringMapWithData(map[string]any{
			"a": NewSyncStringMapWithData(map[string]any{
				"b": 100,
				"c": "test_c",
				"d": NewSyncStringMapWithData(map[string]any{
					"e": 200,
				}),
			}),
			"f": "100",
			"g": [][]map[string]int{{{"state": 0}}, {{"state": 100}}, {{"state": 200}}},
		})
	}

	tests := []struct {
		testname    string
		data        *SyncStringMap
		keys        []string
		insertValue any
		withError   bool
	}{
		{
			"set value to existing item, no traversal",
			mapGenerator(),
			strings.Split("a", "."),
			"new_value",
			false,
		},
		{
			"set value to existing item with traversal",
			mapGenerator(),
			strings.Split("a.b", "."),
			"new_value",
			false,
		},
		{
			"set value to non-existing item, no traversal",
			NewSyncStringMap(),
			strings.Split("a", "."),
			false,
			false,
		},
		{
			"set value to non-existing item with traversal",
			NewSyncStringMap(),
			strings.Split("a.b.c.d.e", "."),
			100,
			false,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			err := test.data.SetRecursive(test.insertValue, test.keys...)

			if test.withError {
				require.Error(t, err)
				return
			}

			require.NoError(t, err)

			setVal, err := test.data.GetRecursive(test.keys...)

			require.NoError(t, err)
			require.Equal(t, test.insertValue, setVal)
		})
	}
}
