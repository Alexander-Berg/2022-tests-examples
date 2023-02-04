package compiled

import (
	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestBoolEq(t *testing.T) {
	testCases := []struct {
		pattern string
		subject bool
	}{
		{"1", true},
		{"t", true},
		{"true", true},
		{"0", false},
		{"f", false},
		{"false", false},
	}

	for _, testCase := range testCases {
		b := Bool{Value: testCase.subject}
		cond := NewCondition(&core.Condition{
			Operator: core.Condition_EQ,
			Value:    testCase.pattern,
		})
		require.True(t, b.Exec(cond), "EQ %+v", testCase)
		cond = NewCondition(&core.Condition{
			Operator: core.Condition_LTE,
			Value:    testCase.pattern,
		})
		require.True(t, b.Exec(cond), "LTE %+v", testCase)
		cond = NewCondition(&core.Condition{
			Operator: core.Condition_GTE,
			Value:    testCase.pattern,
		})
		require.True(t, b.Exec(cond), "GTE %+v", testCase)
	}
}

func TestBoolCmp(t *testing.T) {
	testCases := []struct {
		pattern string
		subject bool
		lt      bool
	}{
		{"0", true, true},
		{"f", true, true},
		{"false", true, true},
		{"1", false, false},
		{"t", false, false},
		{"true", false, false},
	}

	for _, testCase := range testCases {
		b := Bool{Value: testCase.subject}
		cond := NewCondition(&core.Condition{
			Operator: core.Condition_LT,
			Value:    testCase.pattern,
		})
		require.Equal(t, testCase.lt, b.Exec(cond), "LT %+v", testCase)
		cond = NewCondition(&core.Condition{
			Operator: core.Condition_GT,
			Value:    testCase.pattern,
		})
		require.Equal(t, !testCase.lt, b.Exec(cond), "GT %+v", testCase)
	}
}

func TestBoolGlob(t *testing.T) {
	testCases := []struct {
		pattern string
		subject bool
	}{
		{"tr*", true},
		{"*", true},
		{"true", true},
		{"fa*", false},
		{"*", false},
		{"false", false},
	}

	for _, testCase := range testCases {
		b := Bool{Value: testCase.subject}
		cond := NewCondition(&core.Condition{
			Operator: core.Condition_GLOB,
			Value:    testCase.pattern,
		})
		require.True(t, b.Exec(cond), "%+v", testCase)
	}
}
