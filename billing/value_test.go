package configops

import (
	"testing"

	"github.com/stretchr/testify/assert"
	require "github.com/stretchr/testify/require"

	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xyaml"
)

func TestValue(t *testing.T) {
	t.Run("int value", func(t *testing.T) {
		value := NewValue(1)

		_, err := value.Validate(nil)
		require.NoError(t, err)

		val, vType, err := value.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeInt, vType)
		assert.Equal(t, 1, val)
	})

	t.Run("string value", func(t *testing.T) {
		value := NewValue("123")

		_, err := value.Validate(nil)
		require.NoError(t, err)

		val, vType, err := value.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeString, vType)
		assert.Equal(t, "123", val)
	})

	t.Run("nil value", func(t *testing.T) {
		value := NewValue(nil)

		_, err := value.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "nil")
	})

	t.Run("array value", func(t *testing.T) {
		value := NewValue([]any{1, 2, 3})

		_, err := value.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "unknown type")
	})
}

func TestArray(t *testing.T) {
	t.Run("int array", func(t *testing.T) {
		array, err := NewArrayFromYAML([]any{1, 2, 5})
		require.NoError(t, err)

		_, err = array.Validate(nil)
		require.NoError(t, err)

		val, vType, err := array.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeInt}, vType)
		assert.Equal(t, []any{1, 2, 5}, val)
	})

	t.Run("string array", func(t *testing.T) {
		array, err := NewArrayFromYAML([]any{"1", "2", "5"})
		require.NoError(t, err)

		_, err = array.Validate(nil)
		require.NoError(t, err)

		val, vType, err := array.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeString}, vType)
		assert.Equal(t, []any{"1", "2", "5"}, val)
	})

	t.Run("empty array", func(t *testing.T) {
		array, err := NewArrayFromYAML([]any{})
		require.NoError(t, err)

		_, err = array.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "empty")
	})

	t.Run("composite", func(t *testing.T) {
		array, err := NewArrayFromYAML([]any{xyaml.MapStr{"add": []any{1, 2}}, 5})
		require.NoError(t, err)

		_, err = array.Validate(nil)
		require.NoError(t, err)

		val, vType, err := array.Evaluate(nil)
		require.NoError(t, err)

		assert.Equal(t, configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeInt}, vType)
		assert.Equal(t, []any{3, 5}, val)
	})
}
