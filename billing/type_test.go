package configopstype

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestCompositeValues(t *testing.T) {
	cArrayValue := ValueTypeArray{
		ElementType: ValueTypeArray{
			ElementType: ValueTypeString,
		},
	}
	assert.Equal(t, "array[array[string]]", cArrayValue.Name())
	assert.Equal(t, "array[string]", cArrayValue.ElementTypeName())

	cSetValue := ValueTypeSet{
		ElementType: ValueTypeArray{
			ElementType: ValueTypeInt,
		},
	}
	assert.Equal(t, "set[array[int]]", cSetValue.Name())
	assert.Equal(t, "array[int]", cSetValue.ElementTypeName())
}
