package configops

import (
	"testing"

	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xyaml"
	"a.yandex-team.ru/library/go/core/xerrors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

type MapStr map[string]any

// ValueMock is mock of configopsvalue.Value
type ValueMock struct {
	Path     string
	VType    configopstype.ValueType
	Required bool
}

func (v ValueMock) GetRealValue(varMap MapStr) (any, error) {
	return varMap[v.Path], nil
}

// LookupTableMock is an implementation of LookupTable interface.
// We can't import other LookupTable interface realizations from configopsvalue in case of cyclic references
type LookupTableMock struct {
	varMap MapStr
	vars   map[string]ValueMock
}

func (t *LookupTableMock) TypeOf(name string) (configopstype.ValueType, error) {
	val, ok := t.vars[name]
	if !ok {
		return nil, xerrors.New("not found")
	}

	return val.VType, nil
}

func (t *LookupTableMock) Get(name string) (any, error) {
	val, ok := t.vars[name]
	if !ok {
		return ValueMock{}, xerrors.New("not found")
	}

	return val.GetRealValue(t.varMap)
}

func init() {
	RegisterTransform(Set{}, NewSet)
}

func TestAdd(t *testing.T) {
	t.Run("ints", func(t *testing.T) {
		add, err := NewAdd([]any{1, 2})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.NoError(t, err)

		value, vType, err := add.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 3, value)
	})

	t.Run("many ints", func(t *testing.T) {
		add, err := NewAdd([]any{1, 2, 3, 4, 5})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.NoError(t, err)

		value, vType, err := add.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 15, value)
	})

	t.Run("strings", func(t *testing.T) {
		add, err := NewAdd([]any{"1", "2"})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.NoError(t, err)

		value, vType, err := add.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeString, vType)
		assert.Equal(t, "12", value)
	})

	t.Run("empty", func(t *testing.T) {
		add, err := NewAdd([]any{})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "empty")
	})

	t.Run("composite", func(t *testing.T) {
		add, err := NewAdd([]any{xyaml.MapStr{"add": []any{1, 2}}, 3})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.NoError(t, err)

		value, vType, err := add.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 6, value)
	})

	t.Run("invalid types", func(t *testing.T) {
		add, err := NewAdd([]any{1, "2"})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.Error(t, err)
	})

	t.Run("invalid configops type", func(t *testing.T) {
		add, err := NewAdd([]any{xyaml.MapStr{"array": []any{"1", "2"}}, "2"})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "2 different types: array[string] and string")
	})
}

func TestMul(t *testing.T) {
	t.Run("ints", func(t *testing.T) {
		add, err := NewMul([]any{1, 2})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.NoError(t, err)

		value, vType, err := add.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 2, value)
	})

	t.Run("many ints", func(t *testing.T) {
		add, err := NewMul([]any{1, 2, 3, 4, 5})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.NoError(t, err)

		value, vType, err := add.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 120, value)
	})

	t.Run("empty", func(t *testing.T) {
		add, err := NewMul([]any{})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "empty")
	})

	t.Run("composite", func(t *testing.T) {
		add, err := NewMul([]any{xyaml.MapStr{"add": []any{1, 2}}, 3})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.NoError(t, err)

		value, vType, err := add.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 9, value)
	})

	t.Run("invalid types", func(t *testing.T) {
		add, err := NewMul([]any{1, "2"})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.Error(t, err)
	})

	t.Run("invalid configops type", func(t *testing.T) {
		add, err := NewMul([]any{xyaml.MapStr{"array": []any{1, 2}}, 2})
		require.NoError(t, err)

		_, err = add.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "2 different types: array[int] and int")
	})
}

func TestSlice(t *testing.T) {
	t.Run("string", func(t *testing.T) {
		slice, err := NewSlice([]any{"abcde", 1, 3})
		require.NoError(t, err)

		_, err = slice.Validate(nil)
		require.NoError(t, err)

		value, vType, err := slice.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeString, vType)
		assert.Equal(t, "bc", value)
	})

	t.Run("array of strings", func(t *testing.T) {
		slice, err := NewSlice([]any{xyaml.MapStr{"array": []any{"1", "2", "3"}}, 1, 2})
		require.NoError(t, err)

		_, err = slice.Validate(nil)
		require.NoError(t, err)

		value, vType, err := slice.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeString}, vType)
		assert.Equal(t, []any{"2"}, value)
	})

	t.Run("empty result", func(t *testing.T) {
		slice, err := NewSlice([]any{xyaml.MapStr{"array": []any{"1", "2", "3"}}, 1, 1})
		require.NoError(t, err)

		_, err = slice.Validate(nil)
		require.NoError(t, err)

		value, vType, err := slice.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeString}, vType)
		assert.Equal(t, []any{}, value)
	})

	t.Run("right bound equal to length", func(t *testing.T) {
		slice, err := NewSlice([]any{"12345", 1, 5})
		require.NoError(t, err)

		_, err = slice.Validate(nil)
		require.NoError(t, err)

		value, vType, err := slice.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeString, vType)
		assert.Equal(t, "2345", value)
	})

	t.Run("right bound greater than length", func(t *testing.T) {
		slice, err := NewSlice([]any{"12345", 1, 15})
		require.NoError(t, err)

		_, err = slice.Validate(nil)
		require.NoError(t, err)

		_, _, err = slice.Evaluate(nil)
		require.Error(t, err)
	})

	t.Run("left bound less than 0", func(t *testing.T) {
		slice, err := NewSlice([]any{"12345", -10, 2})
		require.NoError(t, err)

		_, err = slice.Validate(nil)
		require.NoError(t, err)

		_, _, err = slice.Evaluate(nil)
		require.Error(t, err)
	})

	t.Run("left bound greater than right bound", func(t *testing.T) {
		slice, err := NewSlice([]any{"12345", 3, 1})
		require.NoError(t, err)

		_, err = slice.Validate(nil)
		require.NoError(t, err)

		_, _, err = slice.Evaluate(nil)
		require.Error(t, err)
	})
}

func TestToUpper(t *testing.T) {
	t.Run("some string", func(t *testing.T) {
		toUpper, err := NewToUpper([]any{"abc_def"})
		require.NoError(t, err)

		_, err = toUpper.Validate(nil)
		require.NoError(t, err)

		value, vType, err := toUpper.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeString, vType)
		assert.Equal(t, "ABC_DEF", value)
	})

	t.Run("already upper", func(t *testing.T) {
		toUpper, err := NewToUpper([]any{"ABB"})
		require.NoError(t, err)

		_, err = toUpper.Validate(nil)
		require.NoError(t, err)

		value, vType, err := toUpper.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeString, vType)
		assert.Equal(t, "ABB", value)
	})

	t.Run("mix case input", func(t *testing.T) {
		toUpper, err := NewToUpper([]any{"aBc_dEf"})
		require.NoError(t, err)

		_, err = toUpper.Validate(nil)
		require.NoError(t, err)

		value, vType, err := toUpper.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeString, vType)
		assert.Equal(t, "ABC_DEF", value)
	})

	t.Run("utf chars", func(t *testing.T) {
		toUpper, err := NewToUpper([]any{"Даже так работает 😎"})
		require.NoError(t, err)

		_, err = toUpper.Validate(nil)
		require.NoError(t, err)

		value, vType, err := toUpper.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeString, vType)
		assert.Equal(t, "ДАЖЕ ТАК РАБОТАЕТ 😎", value)
	})
}

func TestSplit(t *testing.T) {
	t.Run("some string", func(t *testing.T) {
		split, err := NewSplit([]any{"abc_def", "_"})
		require.NoError(t, err)

		valType, err := split.Validate(nil)
		require.NoError(t, err)

		value, vType, err := split.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeString}, vType)
		assert.Equal(t, []any{"abc", "def"}, value)
	})

	t.Run("already split", func(t *testing.T) {
		split, err := NewSplit([]any{"abcdef", "_"})
		require.NoError(t, err)

		valType, err := split.Validate(nil)
		require.NoError(t, err)

		value, vType, err := split.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeString}, vType)
		assert.Equal(t, []any{"abcdef"}, value)
	})

	t.Run("several separators in a row", func(t *testing.T) {
		split, err := NewSplit([]any{"abbc", "b"})
		require.NoError(t, err)

		valType, err := split.Validate(nil)
		require.NoError(t, err)

		value, vType, err := split.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeString}, vType)
		assert.Equal(t, []any{"a", "", "c"}, value)
	})

	t.Run("utf chars", func(t *testing.T) {
		split, err := NewSplit([]any{"Даже так работает 😎", " работает "})
		require.NoError(t, err)

		valType, err := split.Validate(nil)
		require.NoError(t, err)

		value, vType, err := split.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeString}, vType)
		assert.Equal(t, []any{"Даже так", "😎"}, value)
	})
	t.Run("separator is last", func(t *testing.T) {
		split, err := NewSplit([]any{"abc", "c"})
		require.NoError(t, err)

		valType, err := split.Validate(nil)
		require.NoError(t, err)

		value, vType, err := split.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeString}, vType)
		assert.Equal(t, []any{"ab", ""}, value)
	})

	t.Run("separator both first and last", func(t *testing.T) {
		split, err := NewSplit([]any{"c", "c"})
		require.NoError(t, err)

		valType, err := split.Validate(nil)
		require.NoError(t, err)

		value, vType, err := split.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeString}, vType)
		assert.Equal(t, []any{"", ""}, value)
	})

	t.Run("invalid types", func(t *testing.T) {
		split, err := NewSplit([]any{"c", 1})
		require.NoError(t, err)

		_, err = split.Validate(nil)
		require.Error(t, err)

		split, err = NewSplit([]any{2, "c"})
		require.NoError(t, err)

		_, err = split.Validate(nil)
		require.Error(t, err)
	})

	t.Run("invalid arg num", func(t *testing.T) {
		_, err := NewSplit([]any{"c", "b", "d"})
		require.Error(t, err)
	})
}

func TestLen(t *testing.T) {
	t.Run("some string", func(t *testing.T) {
		lenOp, err := NewLen([]any{"abc_def"})
		require.NoError(t, err)

		valType, err := lenOp.Validate(nil)
		require.NoError(t, err)

		value, vType, err := lenOp.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 7, value)
	})

	t.Run("some array", func(t *testing.T) {
		lenOp, err := NewLen([]any{xyaml.MapStr{"array": []any{3, 7, 12}}})
		require.NoError(t, err)

		valType, err := lenOp.Validate(nil)
		require.NoError(t, err)

		value, vType, err := lenOp.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 3, value)
	})

	t.Run("some set", func(t *testing.T) {
		lenOp, err := NewLen([]any{xyaml.MapStr{"set": []any{3, 7, 12}}})
		require.NoError(t, err)

		valType, err := lenOp.Validate(nil)
		require.NoError(t, err)

		value, vType, err := lenOp.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 3, value)
	})

	t.Run("empty string", func(t *testing.T) {
		lenOp, err := NewLen([]any{""})
		require.NoError(t, err)

		valType, err := lenOp.Validate(nil)
		require.NoError(t, err)

		value, vType, err := lenOp.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 0, value)
	})

	t.Run("invalid types", func(t *testing.T) {
		lenOp, err := NewLen([]any{7})
		require.NoError(t, err)

		_, err = lenOp.Validate(nil)
		require.Error(t, err)
	})

	t.Run("invalid arg num", func(t *testing.T) {
		_, err := NewLen([]any{"c", "b", "d"})
		require.Error(t, err)
	})
}

func TestIndex(t *testing.T) {
	t.Run("some array", func(t *testing.T) {
		idxOp, err := NewIndex([]any{xyaml.MapStr{"array": []any{1, 2, 3, 4, 5}}, 3})
		require.NoError(t, err)

		valType, err := idxOp.Validate(nil)
		require.NoError(t, err)

		value, vType, err := idxOp.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 4, value)
	})

	t.Run("some string array", func(t *testing.T) {
		idxOp, err := NewIndex([]any{xyaml.MapStr{"array": []any{"1", "2", "3", "4", "5"}}, 3})
		require.NoError(t, err)

		valType, err := idxOp.Validate(nil)
		require.NoError(t, err)

		value, vType, err := idxOp.Evaluate(nil)
		require.NoError(t, err)
		assert.Equal(t, valType, vType)

		assert.Equal(t, configopstype.ValueTypeString, vType)
		assert.Equal(t, "4", value)
	})

	t.Run("negative index", func(t *testing.T) {
		idxOp, err := NewIndex([]any{xyaml.MapStr{"array": []any{"1", "2", "3", "4", "5"}}, -3})
		require.NoError(t, err)

		_, err = idxOp.Validate(nil)
		require.NoError(t, err)

		_, _, err = idxOp.Evaluate(nil)
		require.Error(t, err)
	})

	t.Run("index out of range", func(t *testing.T) {
		idxOp, err := NewIndex([]any{xyaml.MapStr{"array": []any{"1", "2", "3", "4", "5"}}, 10})
		require.NoError(t, err)

		_, err = idxOp.Validate(nil)
		require.NoError(t, err)

		_, _, err = idxOp.Evaluate(nil)
		require.Error(t, err)
	})

	t.Run("invalid types", func(t *testing.T) {
		idxOp, err := NewIndex([]any{7, 8})
		require.NoError(t, err)

		_, err = idxOp.Validate(nil)
		require.Error(t, err)
	})

	t.Run("invalid arg num", func(t *testing.T) {
		_, err := NewLen([]any{xyaml.MapStr{"array": []any{"1", "2", "3", "4", "5"}}, 10, 20})
		require.Error(t, err)
	})
}

func TestNvl(t *testing.T) {
	// Fails on creating Nvl transform
	t.Run("empty", func(t *testing.T) {
		_, err := NewNvl([]any{})
		require.Error(t, err)
		assert.Contains(t, err.Error(), "requires at least 1 arg")
	})

	// Fails on validation
	t.Run("different types error", func(t *testing.T) {
		nvl, err := NewNvl([]any{nil, []int{1, 2}, 1})
		require.NoError(t, err)

		_, err = nvl.Validate(nil)
		require.Error(t, err)
	})

	// Fails on validation
	t.Run("unknown type nil", func(t *testing.T) {
		nvl, err := NewNvl([]any{1, 2, nil})
		require.NoError(t, err)

		_, err = nvl.Validate(nil)
		require.Error(t, err)
	})

	// Fails on validation
	t.Run("different types", func(t *testing.T) {
		nvl, err := NewNvl([]any{1, "2", nil})
		require.NoError(t, err)

		_, err = nvl.Validate(nil)
		require.Error(t, err)
	})

	getNilVarBaseline := func(testVals []any, expected any, expectedType configopstype.SimpleValueType) func(t *testing.T) {
		table := &LookupTableMock{
			varMap: MapStr{"foo": nil},
			vars: map[string]ValueMock{
				"nilVal": {
					Path:     "foo",
					VType:    expectedType,
					Required: false,
				},
			},
		}

		return func(t *testing.T) {
			nvl, err := NewNvl(testVals)
			require.NoError(t, err)

			_, err = nvl.Validate(table)
			require.NoError(t, err)

			value, vType, err := nvl.Evaluate(table)
			require.NoError(t, err)

			assert.Equal(t, expectedType.Name(), vType.Name())
			assert.Equal(t, expected, value)
		}
	}

	// $nilVal is expected to have int value or null
	t.Run("int value", getNilVarBaseline([]any{"$nilVal", 2, 3}, 2, configopstype.ValueTypeInt))

	// $nilVal is expected to have string value or null
	t.Run("string value", getNilVarBaseline([]any{"$nilVal", "string", "another string"}, "string", configopstype.ValueTypeString))

	// $nilVal is expected to have float value or null
	t.Run("float value", getNilVarBaseline([]any{"$nilVal", 2.04, 12.03}, 2.04, configopstype.ValueTypeFloat))

	getVarsBaseline := func(mapStr MapStr, varMap map[string]map[string]any, expectedType configopstype.SimpleValueType, expected any) func(t *testing.T) {
		var testVals []any
		table := &LookupTableMock{
			varMap: mapStr,
			vars:   map[string]ValueMock{},
		}

		for varName, varParams := range varMap {
			testVals = append(testVals, "$"+varName)
			table.vars[varName] = ValueMock{
				Path:     varParams["path"].(string),
				VType:    varParams["type"].(configopstype.SimpleValueType),
				Required: varParams["required"].(bool),
			}
		}

		return func(t *testing.T) {
			nvl, err := NewNvl(testVals)
			require.NoError(t, err)

			_, err = nvl.Validate(table)
			require.NoError(t, err)

			value, vType, err := nvl.Evaluate(table)
			require.NoError(t, err)

			assert.Equal(t, expectedType.Name(), vType.Name())
			assert.Equal(t, expected, value)
		}
	}

	t.Run("first non nil string variable", getVarsBaseline(
		MapStr{"foo": nil, "bar": "val"},
		map[string]map[string]any{
			"var": {
				"path":     "foo",
				"type":     configopstype.ValueTypeString,
				"required": false,
			},
			"nilVar": {
				"path":     "bar",
				"type":     configopstype.ValueTypeString,
				"required": false,
			},
		},
		configopstype.ValueTypeString,
		"val",
	))
	t.Run("first non nil int variable", getVarsBaseline(
		MapStr{"foo": nil, "bar": 2},
		map[string]map[string]any{
			"var": {
				"path":     "foo",
				"type":     configopstype.ValueTypeInt,
				"required": false,
			},
			"nilVar": {
				"path":     "bar",
				"type":     configopstype.ValueTypeInt,
				"required": false,
			},
		},
		configopstype.ValueTypeInt,
		2,
	))
	t.Run("first non nil float variable", getVarsBaseline(
		MapStr{"foo": nil, "bar": 2.12},
		map[string]map[string]any{
			"var": {
				"path":     "foo",
				"type":     configopstype.ValueTypeFloat,
				"required": false,
			},
			"nilVar": {
				"path":     "bar",
				"type":     configopstype.ValueTypeFloat,
				"required": false,
			},
		},
		configopstype.ValueTypeFloat,
		2.12,
	))
	t.Run("only nil variables", getVarsBaseline(
		MapStr{"foo": nil, "bar": nil},
		map[string]map[string]any{
			"var": {
				"path":     "foo",
				"type":     configopstype.ValueTypeFloat,
				"required": false,
			},
			"anotherVar": {
				"path":     "bar",
				"type":     configopstype.ValueTypeFloat,
				"required": false,
			},
		},
		configopstype.ValueTypeFloat,
		nil,
	))
}
