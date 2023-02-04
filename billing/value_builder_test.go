package configops

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
)

func TestValueBuilderOK(t *testing.T) {
	testCases := []struct {
		name  string
		value any
		vType configopstype.ValueType
	}{
		{
			name:  "nil",
			vType: configopstype.ValueTypeAny,
		},
		{
			name:  "int",
			value: 10,
			vType: configopstype.ValueTypeInt,
		},
		{
			name:  "string",
			value: "val",
			vType: configopstype.ValueTypeString,
		},
		{
			name:  "float",
			value: 1.0,
			vType: configopstype.ValueTypeFloat,
		},
		{
			name:  "jsonNumber int",
			value: json.Number("123"),
			vType: configopstype.ValueTypeInt,
		},
		{
			name:  "jsonNumber float",
			value: json.Number("123"),
			vType: configopstype.ValueTypeFloat,
		},
		{
			name: "any",
			value: map[string][]int{
				"kek": {1, 2, 3},
			},
			vType: configopstype.ValueTypeAny,
		},
	}

	for _, tt := range testCases {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			val, err := NewValueForType(tt.value, tt.vType)
			require.NoError(t, err)

			result, vType, err := val.Evaluate(nil)
			require.NoError(t, err)
			assert.Equal(t, tt.vType, vType)
			switch tt.vType {
			case configopstype.ValueTypeInt:
				_, ok := result.(int)
				assert.Truef(t, ok, "Result must be of type 'int', got %T", result)
			case configopstype.ValueTypeFloat:
				_, ok := result.(float64)
				assert.Truef(t, ok, "Result must be of type 'float64', got %T", result)
			case configopstype.ValueTypeString:
				_, ok := result.(string)
				assert.Truef(t, ok, "Result must be of type 'string', got %T", result)
			}
		})
	}
}

func TestValueBuilderFail(t *testing.T) {
	testCases := []struct {
		name  string
		value any
		vType configopstype.ValueType
	}{
		{
			name: "nil type",
		},
		{
			name:  "nil int type",
			vType: configopstype.ValueTypeInt,
		},
		{
			name:  "string val int type",
			value: "123",
			vType: configopstype.ValueTypeInt,
		},
		{
			name: "jsonNumber string",
			// We cannot get string json.Number after marshaling.
			value: json.Number("123"),
			vType: configopstype.ValueTypeString,
		},
		{
			name:  "random value string type",
			value: map[string][]int{"kek": {1, 2, 3}},
			vType: configopstype.ValueTypeInt,
		},
	}

	for _, tt := range testCases {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			_, err := NewValueForType(tt.value, tt.vType)
			require.Error(t, err)
		})
	}
}
