package common

import (
	"encoding/json"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

type testEmbedded struct {
	Duration JSONDuration `json:"duration"`
}

func TestJSONDurationUnmarshal(t *testing.T) {
	testCases := []struct {
		name        string
		input       string
		expectedErr string
		expected    time.Duration
	}{
		{
			name:     "empty",
			input:    "",
			expected: 0,
		},
		{
			name:     "null",
			input:    "null",
			expected: 0,
		},
		{
			name:        "invalid",
			input:       "wtf",
			expectedErr: `time: invalid duration "wtf"`,
		},
		{
			name:     "valid",
			input:    "1h10m",
			expected: time.Hour + 10*time.Minute,
		},
		{
			name:     "valid zero",
			input:    "0",
			expected: 0,
		},
	}

	for _, testCase := range testCases {
		tc := testCase
		t.Run(tc.name, func(t *testing.T) {
			rawStr := fmt.Sprintf(`{"duration":%q}`, tc.input)
			var embedded testEmbedded
			require.NoError(t, json.Unmarshal([]byte(rawStr), &embedded))

			value, err := embedded.Duration.Value()
			if tc.expectedErr != "" {
				require.EqualError(t, err, tc.expectedErr)
				return
			}
			require.NoError(t, err)

			assert.Equal(t, tc.expected, value)
		})
	}
}
