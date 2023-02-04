package app

import (
	"github.com/stretchr/testify/assert"
	"strconv"
	"testing"
	"time"
)

func TestParseGrafanaTime(t *testing.T) {
	now := time.Date(2063, 4, 5, 7, 1, 0, 0, time.Local)
	yesterday := time.Date(2063, 4, 4, 0, 0, 0, 0, time.Local)
	today := time.Date(2063, 4, 5, 0, 0, 0, 0, time.Local)
	startOfYear := time.Date(2063, 1, 1, 0, 0, 0, 0, time.Local)
	startWeek := time.Date(2063, 4, 2, 0, 0, 0, 0, time.Local)
	cases := []struct {
		name         string
		expectedTime time.Time
		inputStr     string
		inputType    TimeType
	}{
		{"now", now, "now", TimeFrom},
		{"-1h", now.Add(-time.Hour), "now-1h", TimeFrom},
		{"today", today, "now/d", TimeFrom},
		{"end of today", today.Add(time.Hour * 24), "now/d", TimeTo},
		{"yesterday", yesterday, "now-1d/d", TimeFrom},
		{"start of year", startOfYear, "now/y", TimeFrom},
		{"timestamp", now, strconv.FormatInt(now.Unix(), 10), TimeFrom},
		{"startweek", startWeek, "now/w", TimeFrom},
		{"nextweek", startWeek.Add(time.Hour * 24 * 7), "now/w", TimeTo},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			res, err := ParseGrafanaTime(tc.inputType, tc.inputStr, now)
			if !assert.NoError(t, err) {
				return
			}
			assert.Equal(t, tc.expectedTime, res)
		})
	}
}
