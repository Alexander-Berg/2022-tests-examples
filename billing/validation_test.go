package yaml

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
)

func TestValidateInputs(t *testing.T) {
	testCases := []struct {
		name       string
		inputNames []GlobalInput
		inputs     map[string]any
		isError    bool
	}{
		{
			name:       "empty",
			inputNames: nil,
			inputs:     map[string]any{},
			isError:    false,
		},
		{
			name:       "one input ok",
			inputNames: []GlobalInput{{YAMLValue: tplops.OutputValue{Name: "inp1", Type: configopstype.ValueTypeInt}}},
			inputs:     map[string]any{"inp1": 1},
			isError:    false,
		},
		{
			name:       "1 input expected got 0",
			inputNames: []GlobalInput{{YAMLValue: tplops.OutputValue{Name: "inp1", Type: configopstype.ValueTypeInt}}},
			inputs:     map[string]any{},
			isError:    true,
		},
		{
			name:       "0 inputs expected got 1",
			inputNames: nil,
			inputs:     map[string]any{"inp1": 10},
			isError:    true,
		},
		{
			name: "many inputs ok",
			inputNames: []GlobalInput{
				{YAMLValue: tplops.OutputValue{Name: "inp1", Type: configopstype.ValueTypeInt}},
				{YAMLValue: tplops.OutputValue{Name: "inp2", Type: configopstype.ValueTypeAny}},
				{YAMLValue: tplops.OutputValue{Name: "inp3", Type: configopstype.ValueTypeString}},
			},
			inputs:  map[string]any{"inp1": 10, "inp2": struct{}{}, "inp3": "anything"},
			isError: false,
		},
		{
			name: "many inputs not equal",
			inputNames: []GlobalInput{
				{YAMLValue: tplops.OutputValue{Name: "inp1", Type: configopstype.ValueTypeInt}},
				{YAMLValue: tplops.OutputValue{Name: "inp2", Type: configopstype.ValueTypeAny}},
				{YAMLValue: tplops.OutputValue{Name: "inp3", Type: configopstype.ValueTypeAny}},
			},
			inputs:  map[string]any{"inp1": 10, "inp4": struct{}{}, "todo": "anything"},
			isError: true,
		},
		{
			name: "valid type",
			inputNames: []GlobalInput{
				{YAMLValue: tplops.OutputValue{Name: "inp1", Type: configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeFloat}}},
			},
			inputs: map[string]any{"inp1": []any{1.2, 2.4}},
		},
		{
			name: "invalid type",
			inputNames: []GlobalInput{
				{YAMLValue: tplops.OutputValue{Name: "inp1", Type: configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeFloat}}},
			},
			inputs:  map[string]any{"inp1": []any{1, 4}},
			isError: true,
		},
	}

	for _, tt := range testCases {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			err := ValidateGeneralInputs(tt.inputNames, tt.inputs)
			if tt.isError {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
			}
		})

		t.Run(tt.name+" env inputs", func(t *testing.T) {
			for i := range tt.inputNames {
				tt.inputNames[i].IsEnv = true
			}

			err := ValidateEnvInputs(tt.inputNames, tt.inputs)
			if tt.isError {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
			}
		})
	}
}

func TestValidateInputsGeneralAndEnv(t *testing.T) {
	testCases := []struct {
		name         string
		inputNames   []GlobalInput
		general, env map[string]any
	}{
		{
			name:       "empty",
			inputNames: nil,
		},
		{
			name:       "one general input",
			inputNames: []GlobalInput{{YAMLValue: tplops.OutputValue{Name: "inp1", Type: configopstype.ValueTypeAny}}},
			general:    map[string]any{"inp1": "abracadabra"},
		},
		{
			name:       "one env input",
			inputNames: []GlobalInput{{YAMLValue: tplops.OutputValue{Name: "inp1", Type: configopstype.ValueTypeAny}, IsEnv: true}},
			env:        map[string]any{"inp1": "abracadabra"},
		},
		{
			name: "general and env inputs",
			inputNames: []GlobalInput{
				{YAMLValue: tplops.OutputValue{Name: "inp1", Type: configopstype.ValueTypeAny}, IsEnv: true},
				{YAMLValue: tplops.OutputValue{Name: "inp2", Type: configopstype.ValueTypeAny}},
			},
			env:     map[string]any{"inp1": "abracadabra"},
			general: map[string]any{"inp2": "😀"},
		},
	}

	for _, tt := range testCases {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			assert.NoError(t, ValidateGeneralInputs(tt.inputNames, tt.general))
			assert.NoError(t, ValidateEnvInputs(tt.inputNames, tt.env))
		})
	}
}
