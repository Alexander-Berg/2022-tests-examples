package configvars

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/valyala/fastjson"

	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
)

func TestGetInt(t *testing.T) {
	value := fastjson.MustParse(`{"intKey": 123, "complex": {"key":[{"with":{"arrays":12345}}]}}`)

	testCases := []struct {
		name        string
		path        []string
		vType       configopstype.ValueType
		required    bool
		parsedValue any
		expectError bool
	}{
		{
			name:        "valid",
			path:        []string{"intKey"},
			vType:       configopstype.ValueTypeInt,
			required:    true,
			parsedValue: 123,
		},
		{
			name:        "invalid path required",
			path:        []string{"invalidKey"},
			vType:       configopstype.ValueTypeInt,
			required:    true,
			expectError: true,
		},
		{
			name:        "invalid path not required",
			path:        []string{"invalidKey"},
			vType:       configopstype.ValueTypeInt,
			required:    false,
			parsedValue: nil,
		},
		{
			name:        "cast is not allowed",
			path:        []string{"intKey"},
			vType:       configopstype.ValueTypeString,
			required:    false,
			expectError: true,
		},
		{
			name:        "complex key with arrays",
			path:        []string{"complex", "key", "0", "with", "arrays"},
			vType:       configopstype.ValueTypeInt,
			required:    true,
			parsedValue: 12345,
		},
	}

	for _, testCase := range testCases {
		tt := testCase
		t.Run(tt.name, func(t *testing.T) {
			v := Value{
				Path:     tt.path,
				VType:    tt.vType,
				Required: tt.required,
			}

			result, err := v.GetRealValue(value)
			if tt.expectError {
				require.Error(t, err)
				return
			}
			require.NoError(t, err)
			assert.Equal(t, tt.parsedValue, result)
		})
	}
}

func TestGetString(t *testing.T) {
	value := fastjson.MustParse(`{"strKey": "123", "complex": {"key":[{"with":{"arrays":"12345"}}]}}`)

	testCases := []struct {
		name        string
		path        []string
		vType       configopstype.ValueType
		required    bool
		parsedValue any
		expectError bool
	}{
		{
			name:        "valid",
			path:        []string{"strKey"},
			vType:       configopstype.ValueTypeString,
			required:    true,
			parsedValue: "123",
		},
		{
			name:        "invalid path required",
			path:        []string{"invalidKey"},
			vType:       configopstype.ValueTypeString,
			required:    true,
			expectError: true,
		},
		{
			name:        "invalid path not required",
			path:        []string{"invalidKey"},
			vType:       configopstype.ValueTypeString,
			required:    false,
			parsedValue: nil,
		},
		{
			name:        "cast is not allowed",
			path:        []string{"strKey"},
			vType:       configopstype.ValueTypeInt,
			required:    false,
			expectError: true,
		},
		{
			name:        "complex key with arrays",
			path:        []string{"complex", "key", "0", "with", "arrays"},
			vType:       configopstype.ValueTypeString,
			required:    true,
			parsedValue: "12345",
		},
	}

	for _, testCase := range testCases {
		tt := testCase
		t.Run(tt.name, func(t *testing.T) {
			v := Value{
				Path:     tt.path,
				VType:    tt.vType,
				Required: tt.required,
			}

			result, err := v.GetRealValue(value)
			if tt.expectError {
				require.Error(t, err)
				return
			}
			require.NoError(t, err)
			assert.Equal(t, tt.parsedValue, result)
		})
	}
}

func TestInvalidType(t *testing.T) {
	value := fastjson.MustParse(`{"key":[]}`)

	v := Value{
		Path:     []string{"key"},
		VType:    nil,
		Required: true,
	}

	_, err := v.GetRealValue(value)
	assert.EqualError(t, err, "provided type is nil")
}

func TestArrayNotImplemented(t *testing.T) {
	value := fastjson.MustParse(`{"key":[]}`)

	v := Value{
		Path:     []string{"key"},
		VType:    configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeInt},
		Required: true,
	}

	_, err := v.GetRealValue(value)
	assert.EqualError(t, err, "getting arrays is not implemented")
}
