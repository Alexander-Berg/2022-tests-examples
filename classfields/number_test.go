package compiled

import (
	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestNumberEq(t *testing.T) {
	testCases := []struct {
		pattern string
		subject float64
	}{
		{"200", 200},
		{"-200", -200},
		{"2.20", 2.2},
		{"2.20", 2.20},
	}

	for _, testCase := range testCases {
		b := Number{Value: testCase.subject}
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

func TestNumberLt(t *testing.T) {
	testCases := []struct {
		pattern  string
		subject  float64
		expected bool
	}{
		{"199", 200, true},
		{"200", 200, false},
		{"201", 200, false},
		{"-201", -200, true},
		{"-200", -200, false},
		{"-199", -200, false},
		{"2.2", 2.21, true},
		{"2.20", 2.2, false},
		{"2.21", 2.2, false},
	}

	for _, testCase := range testCases {
		b := Number{Value: testCase.subject}
		cond := NewCondition(&core.Condition{
			Operator: core.Condition_LT,
			Value:    testCase.pattern,
		})
		require.Equal(t, testCase.expected, b.Exec(cond), "%+v", testCase)
	}
}

func TestNumberGt(t *testing.T) {
	testCases := []struct {
		pattern  string
		subject  float64
		expected bool
	}{
		{"199", 200, false},
		{"200", 200, false},
		{"201", 200, true},
		{"-201", -200, false},
		{"-200", -200, false},
		{"-199", -200, true},
		{"2.2", 2.21, false},
		{"2.20", 2.2, false},
		{"2.21", 2.2, true},
	}

	for _, testCase := range testCases {
		b := Number{Value: testCase.subject}
		cond := NewCondition(&core.Condition{
			Operator: core.Condition_GT,
			Value:    testCase.pattern,
		})
		require.Equal(t, testCase.expected, b.Exec(cond), "%+v", testCase)
	}
}

func TestNumberGlob(t *testing.T) {
	testCases := []struct {
		pattern string
		subject float64
	}{
		{"*0*", 200},
		{"*0", 200},
		{"2*", 200},
		{"2*0", 200},
		{"-2*", -200},
		{"2.20*", 2.202},
	}

	for _, testCase := range testCases {
		b := Number{Value: testCase.subject}
		cond := NewCondition(&core.Condition{
			Operator: core.Condition_GLOB,
			Value:    testCase.pattern,
		})
		require.True(t, b.Exec(cond), "%+v", testCase)
	}
}
