package ytreferences

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestCachedGenericReference_Get(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		name             string
		cached           *CachedGenericReference
		keys             any
		expectedGenerics []map[string]any
		shouldBeErr      bool
		expectedErr      string
	}{
		{
			name: "bad type of keys",
			cached: WithInitCachedGenericReference(
				cacheStorage{
					1: {{"a": 1, "b": 2}},
					2: {{"a": 3, "b": 4}},
				},
				&GenericReference{},
			),
			keys:             "key",
			expectedGenerics: nil,
			shouldBeErr:      true,
			expectedErr:      "bad type of keys",
		},
		{
			name: "obtain nothing when cache is empty",
			cached: WithInitCachedGenericReference(
				cacheStorage{},
				&GenericReference{},
			),
			keys:             []int{1, 2},
			expectedGenerics: nil,
			shouldBeErr:      false,
		},
		{
			name: "obtain generics when given keys is included in cache keys",
			cached: WithInitCachedGenericReference(
				cacheStorage{
					1: {{"a": 1, "b": 2}},
					2: {{"a": 4, "b": 6}},
					3: {{"a": 10, "b": 12}},
				},
				&GenericReference{},
			),
			keys: []int{1, 2},
			expectedGenerics: []map[string]any{
				{"a": 1, "b": 2},
				{"a": 4, "b": 6},
			},
			shouldBeErr: false,
		},
		{
			name: "obtain generics ignoring extra keys",
			cached: WithInitCachedGenericReference(
				cacheStorage{
					1: {{"c": 3}, {"c": 4}},
					2: {{"c": 5}},
					4: {{"c": 100}},
				},
				&GenericReference{},
			),
			keys: []int{1, 2, 3},
			expectedGenerics: []map[string]any{
				{"c": 3},
				{"c": 4},
				{"c": 5},
			},
			shouldBeErr: false,
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(c.name, func(t *testing.T) {
			t.Parallel()

			actualGenerics, err := c.cached.Get(context.Background(), c.keys)

			if c.shouldBeErr {
				require.Error(t, err, c.expectedErr)

				return
			}

			require.NoError(t, err)
			require.ElementsMatch(t, c.expectedGenerics, actualGenerics)
		})
	}
}

func TestCachedGenericReference_Select(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		name             string
		cached           *CachedGenericReference
		keyName          string
		keyValue         any
		expectedGenerics []map[string]any
	}{
		{
			name: "obtain nothing when there is no key in cache",
			cached: WithInitCachedGenericReference(
				cacheStorage{},
				&GenericReference{},
			),
			keyName:          "a",
			keyValue:         1,
			expectedGenerics: nil,
		},
		{
			name: "obtain nothing when there is not row with such key value",
			cached: WithInitCachedGenericReference(
				cacheStorage{
					1: {{"id": 1, "a": 2}},
				},
				&GenericReference{},
			),
			keyName:          "a",
			keyValue:         1,
			expectedGenerics: nil,
		},
		{
			name: "obtain generics when there is row with such key and value",
			cached: WithInitCachedGenericReference(
				cacheStorage{
					1: {{"id": 1, "b": 13}},
					2: {{"id": 2, "b": 100}},
					3: {{"id": 3, "b": 1000}},
					4: {{"id": 4, "b": 13}},
					5: {{"id": 5, "b": 110}, {"id": 6, "b": 13}},
				},
				&GenericReference{},
			),
			keyName:  "b",
			keyValue: 13,
			expectedGenerics: []map[string]any{
				{"id": 1, "b": 13},
				{"id": 4, "b": 13},
				{"id": 6, "b": 13},
			},
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(c.name, func(t *testing.T) {
			t.Parallel()

			actualGenerics, err := c.cached.Select(context.Background(), c.keyName, c.keyValue)

			require.NoError(t, err)
			require.ElementsMatch(t, c.expectedGenerics, actualGenerics)
		})
	}
}
