package ytreferences

import (
	"reflect"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var now = time.Now()

func TestYtDynamicTable_GetClient(t *testing.T) {
	tests := []struct {
		testname   string
		clients    []*Client
		wantClient *Client
	}{
		{"nil if empty client's list", nil, nil},
		{
			"first client if no error",
			[]*Client{
				{cluster: "hahn"}, {cluster: "arnold"},
			},
			&Client{cluster: "hahn"},
		},
		{
			"first client if first is live",
			[]*Client{
				{cluster: "hahn", errorCount: deadErrorCount - 1, lastFailTimestamp: now.Unix()}, {cluster: "arnold"},
			},
			&Client{cluster: "hahn", errorCount: deadErrorCount - 1, lastFailTimestamp: now.Unix()},
		},
		{
			"second client if first with error",
			[]*Client{
				{cluster: "hahn", errorCount: deadErrorCount, lastFailTimestamp: now.Unix()}, {cluster: "arnold"},
			},
			&Client{cluster: "arnold"},
		},
		{
			"client with fewer errors if all dead",
			[]*Client{
				{cluster: "hahn", errorCount: deadErrorCount + 3, lastFailTimestamp: now.Add(-time.Second).Unix()},
				{cluster: "arnold", errorCount: deadErrorCount + 1, lastFailTimestamp: now.Add(-5 * time.Second).Unix()},
				{cluster: "freud", errorCount: deadErrorCount + 5, lastFailTimestamp: now.Add(-3 * time.Second).Unix()},
			},
			&Client{cluster: "arnold", errorCount: deadErrorCount + 1, lastFailTimestamp: now.Add(-5 * time.Second).Unix()},
		},
		{
			"live client if each has error",
			[]*Client{
				{cluster: "hahn", errorCount: deadErrorCount, lastFailTimestamp: now.Add(-time.Second).Unix()},
				{cluster: "arnold", errorCount: deadErrorCount, lastFailTimestamp: now.Add(-5 * time.Second).Unix()},
				{cluster: "freud", errorCount: deadErrorCount, lastFailTimestamp: now.Add(-(deadTimeout + 3*time.Second)).Unix()},
			},
			&Client{cluster: "freud", errorCount: 0, lastFailTimestamp: 0},
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			table := YtDynamicTable{clients: test.clients}
			client := table.GetClient()
			assert.Equal(t, test.wantClient, client)
		})
	}
}

func Test_DecodeToStruct(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		name           string
		dict           map[string]any
		expectedResult any
		expectedErr    string
	}{
		{
			"Empty dict, empty struct",
			map[string]any{},
			&struct{}{},
			"",
		},
		{
			"Dict corresponds to struct",
			map[string]any{
				"a_Attr": "asdf",
				"b_Attr": 1234,
				"nested_Attr": map[string]any{
					"x": "qwer",
					"y": 4576,
				},
			},
			&struct {
				A      string         `json:"a_Attr"`
				B      uint64         `json:"b_Attr"`
				Nested map[string]any `json:"nested_Attr"`
			}{
				A: "asdf",
				B: 1234,
				Nested: map[string]any{
					"x": "qwer",
					"y": 4576,
				},
			},
			"",
		},
		{
			"Dict have more fields than struct",
			map[string]any{
				"a_Attr": "asdf",
				"b_Attr": 1234,
				"c_Attr": false,
				"nested_Attr": map[string]any{
					"x": "qwer",
					"y": 4576,
				},
			},
			&struct {
				A      string         `json:"a_Attr"`
				B      uint64         `json:"b_Attr"`
				Nested map[string]any `json:"nested_Attr"`
			}{
				A: "asdf",
				B: 1234,
				Nested: map[string]any{
					"x": "qwer",
					"y": 4576,
				},
			},
			"",
		},
		{
			"Dict have fewer fields than struct",
			map[string]any{
				"a_Attr": "asdf",
			},
			&struct {
				A      string         `json:"a_Attr"`
				B      uint64         `json:"b_Attr"`
				Nested map[string]any `json:"nested_Attr"`
			}{
				A:      "asdf",
				B:      0,
				Nested: nil,
			},
			"",
		},
		{
			"Struct have incorrectly tagged fields",
			map[string]any{
				"a_Attr": "asdf",
				"b_Attr": 1234,
			},
			&struct {
				A string `not_json:"a_Attr"`
				B string `json:"b_Attr"`
			}{
				B: "1234",
			},
			"",
		},
		{
			"Dict have incorrect types: 1",
			map[string]any{
				"a_Attr": 4576,
				"b_Attr": "asdf",
			},
			&struct {
				A string `json:"a_Attr"`
				B uint64 `json:"b_Attr"`
			}{},
			"cannot parse 'b_Attr' as uint",
		},
		{
			"Dict have incorrect types: 1",
			map[string]any{
				"nested_Attr": 7,
			},
			&struct {
				Nested map[string]any `json:"nested_Attr"`
			}{},
			"'nested_Attr' expected a map, got 'int'",
		},
		{
			"Dict have incorrect but convertable types",
			map[string]any{
				"a_Attr": 4576,
				"b_Attr": "1234",
			},
			&struct {
				A      string         `json:"a_Attr"`
				B      uint64         `json:"b_Attr"`
				Nested map[string]any `json:"nested_Attr"`
			}{
				A:      "4576",
				B:      1234,
				Nested: nil,
			},
			"",
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(c.name, func(t *testing.T) {
			t.Parallel()

			result := reflect.New(reflect.TypeOf(c.expectedResult)).Elem().Interface()
			err := decodeToStruct(c.dict, &result)
			if c.expectedErr != "" {
				require.Error(t, err, c.expectedErr)
				return
			}
			require.NoError(t, err)
			require.Equal(t, c.expectedResult, result)
		})
	}
}

func TestMakeGenericTable(t *testing.T) {
	table := makeGenericTable(10)
	assert.Len(t, table.rows, 0)
	require.Equal(t, 10, cap(table.rows))
}

func TestGenericTable_AddRow(t *testing.T) {
	table := makeGenericTable(1)
	assert.Len(t, table.rows, 0)
	table.AddRow()
	assert.Len(t, table.rows, 1)
	require.Equal(t, map[string]any{}, table.rows[0])
}

func TestGenericTable_DecodeRows(t *testing.T) {
	t.Parallel()

	type testStruct struct {
		A string `json:"a_Attr"`
		B uint64 `json:"b_Attr"`
	}

	testCases := []struct {
		name           string
		rows           []map[string]any
		expectedResult []testStruct
		expectedErr    string
	}{
		{
			"Correctly parsed all rows",
			[]map[string]any{
				{
					"a_Attr": "asdf",
					"b_Attr": 1,
				},
				{
					"a_Attr": "qwer",
					"b_Attr": 2,
				},
			},
			[]testStruct{
				{
					A: "asdf",
					B: 1,
				},
				{
					A: "qwer",
					B: 2,
				},
			},
			"",
		},
		{
			"First row is invalid",
			[]map[string]any{
				{
					"a_Attr": "asdf",
					"b_Attr": "sadf",
				},
				{
					"a_Attr": "qwer",
					"b_Attr": 1,
				},
			},
			[]testStruct{},
			"cannot parse 'b_Attr' as uint",
		},
		{
			"Second row is invalid",
			[]map[string]any{
				{
					"a_Attr": "asdf",
					"b_Attr": 1,
				},
				{
					"a_Attr": "qwer",
					"b_Attr": "asdf",
				},
			},
			[]testStruct{},
			"cannot parse 'b_Attr' as uint",
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(c.name, func(t *testing.T) {
			t.Parallel()
			table := &genericTable{rows: c.rows}
			result := make([]testStruct, 0, len(c.rows))
			err := table.DecodeRows(func() any {
				result = append(result, testStruct{})
				return &(result[len(result)-1])
			})
			if c.expectedErr != "" {
				require.Error(t, err, c.expectedErr)
				return
			}
			require.NoError(t, err)
			require.Equal(t, c.expectedResult, result)
		})
	}
}
