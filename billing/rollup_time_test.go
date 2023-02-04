package rollup

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/cron"
)

func mustParseCron(crons []string) rollupCron {
	s, err := cron.NewScheduler(crons)
	if err != nil {
		panic(err)
	}
	return s
}

func TestGenerateUpperRollupTimes(t *testing.T) {
	type testdata struct {
		name       string
		now        time.Time
		eventTime  eventTime
		rollupCron rollupCron
		rollupTime *timeInterval
		expected   []timeInterval
	}

	now := TimeNowFunc().UTC().Truncate(time.Hour).Add(time.Minute * 15)

	hourlyCrons := mustParseCron([]string{"0 * * *"})

	tests := []testdata{
		{
			name: "EventInCurrentRollupBeforeNowHourlyCron",
			now:  now,
			eventTime: eventTime{
				Max: now.Add(-time.Minute),
				Min: now.Add(-time.Hour * 24),
			},
			rollupCron: hourlyCrons,
			rollupTime: &timeInterval{
				LowerBound: now.Truncate(time.Hour).Add(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour * 2),
			},
			expected: nil,
		},
		{
			name: "EventInCurrentRollupAfterNowHourlyCron",
			now:  now,
			eventTime: eventTime{
				Max: now.Add(time.Minute),
				Min: now.Add(-time.Hour * 24),
			},
			rollupCron: hourlyCrons,
			rollupTime: &timeInterval{
				LowerBound: now.Truncate(time.Hour).Add(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour * 2),
			},
			expected: nil,
		},
		{
			name: "EventInNextRollupHourlyCron",
			now:  now,
			eventTime: eventTime{
				Max: now.Add(time.Hour),
				Min: now.Add(-time.Hour * 24),
			},
			rollupCron: hourlyCrons,
			rollupTime: &timeInterval{
				LowerBound: now.Truncate(time.Hour).Add(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour * 2),
			},
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour * 2),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 3),
				},
			},
		},
		{
			name: "EventInDistantFutureHourlyCron",
			now:  now,
			eventTime: eventTime{
				Max: now.Add(time.Hour * 3),
				Min: now.Add(-time.Hour * 24),
			},
			rollupCron: hourlyCrons,
			rollupTime: &timeInterval{
				LowerBound: now.Truncate(time.Hour).Add(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour * 2),
			},
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour * 2),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 3),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour * 3),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 4),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour * 4),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 5),
				},
			},
		},
		{
			name: "EventInDistantPastExistingRollupHourlyCron",
			now:  now,
			eventTime: eventTime{
				Max: now.Add(-time.Hour * 3),
				Min: now.Add(-time.Hour * 24),
			},
			rollupCron: hourlyCrons,
			rollupTime: &timeInterval{
				LowerBound: now.Truncate(time.Hour).Add(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour * 2),
			},
			expected: nil,
		},
		{
			name: "EventInDistantPastHourlyCron",
			now:  now,
			eventTime: eventTime{
				Max: now.Add(-time.Hour * 3),
				Min: now.Add(-time.Hour * 24),
			},
			rollupCron: hourlyCrons,
			rollupTime: &timeInterval{
				LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 2),
				UpperBound: now.Truncate(time.Hour).Add(-time.Hour * 1),
			},
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 1),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour * 0),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 0),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 1),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour * 1),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 2),
				},
			},
		},
		{
			name: "FirstEventForAccountHourlyCron",
			now:  now,
			eventTime: eventTime{
				Max: now.Add(time.Minute * 4),
				Min: now.Add(-time.Hour * 3),
			},
			rollupCron: hourlyCrons,
			rollupTime: nil,
			expected: []timeInterval{
				{
					LowerBound: now.Add(-time.Hour * 3),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour * 2),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 2),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour * 1),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 1),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour * 0),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour * 0),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 1),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour * 1),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 2),
				},
			},
		},
		{
			name: "EventInFutureNonHourlyCron",
			now:  now.Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 7), // 15:07
			eventTime: eventTime{
				Max: now.Truncate(time.Hour * 24).Add(time.Hour * 16).Add(time.Minute * 41), // 16:40
				Min: now.Add(-time.Hour * 3),
			},
			// 15:00 15:15 15:20 15:37 15:40 16:00 16:15 16:20 16:40 17:00 17:20
			rollupCron: mustParseCron([]string{
				"*/20 * * *",
				"15 15,16 * *",
				"0 16 * *",
				"37 15 * *",
			}),
			rollupTime: &timeInterval{
				LowerBound: now.Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 15),
				UpperBound: now.Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 20),
			},
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 20),
					UpperBound: now.Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 37),
				},
				{
					LowerBound: now.Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 37),
					UpperBound: now.Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 40),
				},
				{
					LowerBound: now.Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 40),
					UpperBound: now.Truncate(time.Hour * 24).Add(time.Hour * 16).Add(time.Minute * 0),
				},
				{
					LowerBound: now.Truncate(time.Hour * 24).Add(time.Hour * 16).Add(time.Minute * 0),
					UpperBound: now.Truncate(time.Hour * 24).Add(time.Hour * 16).Add(time.Minute * 15),
				},
				{
					LowerBound: now.Truncate(time.Hour * 24).Add(time.Hour * 16).Add(time.Minute * 15),
					UpperBound: now.Truncate(time.Hour * 24).Add(time.Hour * 16).Add(time.Minute * 20),
				},
				{
					LowerBound: now.Truncate(time.Hour * 24).Add(time.Hour * 16).Add(time.Minute * 20),
					UpperBound: now.Truncate(time.Hour * 24).Add(time.Hour * 16).Add(time.Minute * 40),
				},
				{
					LowerBound: now.Truncate(time.Hour * 24).Add(time.Hour * 16).Add(time.Minute * 40),
					UpperBound: now.Truncate(time.Hour * 24).Add(time.Hour * 17).Add(time.Minute * 0),
				},
				{
					LowerBound: now.Truncate(time.Hour * 24).Add(time.Hour * 17).Add(time.Minute * 0),
					UpperBound: now.Truncate(time.Hour * 24).Add(time.Hour * 17).Add(time.Minute * 20),
				},
			},
		},
	}

	for i := range tests {
		test := tests[i]
		t.Run(test.name, func(t *testing.T) {
			res := generateUpperIntervalRollupTimes(test.now, test.eventTime, test.rollupTime, test.rollupCron)
			assert.Equal(t, test.expected, res)
		})
	}
}

func TestGenerateLowerRollupTimes(t *testing.T) {
	type testdata struct {
		name       string
		eventTime  time.Time
		rollupCron rollupCron
		rollupTime timeInterval
		expected   []timeInterval
	}

	now := TimeNowFunc().UTC().Truncate(time.Hour).Add(time.Minute * 15)

	hourlyCrons := mustParseCron([]string{"0 * * *"})

	tests := []testdata{
		{
			name:       "NoRollups",
			eventTime:  now.Truncate(time.Hour).Add(time.Minute * 10),
			rollupCron: hourlyCrons,
			rollupTime: timeInterval{
				LowerBound: now.Truncate(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour),
			},
			expected: nil,
		},
		{
			name:       "NoRollupsEventInFuture",
			eventTime:  now.Truncate(time.Hour).Add(time.Hour).Add(time.Minute * 10),
			rollupCron: hourlyCrons,
			rollupTime: timeInterval{
				LowerBound: now.Truncate(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour),
			},
			expected: nil,
		},
		{
			name:       "EventInPastWithNextRollup",
			eventTime:  now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 10),
			rollupCron: hourlyCrons,
			rollupTime: timeInterval{
				LowerBound: now.Truncate(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour),
			},
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 10),
					UpperBound: now.Truncate(time.Hour),
				},
			},
		},
		{
			name:       "EventInPastWithExistingRollups",
			eventTime:  now.Truncate(time.Hour).Add(-time.Hour * 2).Add(time.Minute * 10),
			rollupCron: hourlyCrons,
			rollupTime: timeInterval{
				LowerBound: now.Truncate(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour),
			},
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 2).Add(time.Minute * 10),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour),
					UpperBound: now.Truncate(time.Hour),
				},
			},
		},
		{
			name:       "EventInFurtherPastWithExistingRollups",
			eventTime:  now.Truncate(time.Hour).Add(-time.Hour * 4).Add(time.Minute * 10),
			rollupCron: hourlyCrons,
			rollupTime: timeInterval{
				LowerBound: now.Truncate(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour),
			},
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 4).Add(time.Minute * 10),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour * 3),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 3),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour * 2),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 2),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour),
					UpperBound: now.Truncate(time.Hour),
				},
			},
		},
		{
			name:      "ChangingCron",
			eventTime: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 10),
			rollupCron: mustParseCron([]string{
				"*/15 * * *",
			}),
			rollupTime: timeInterval{
				LowerBound: now.Truncate(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour),
			},
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 10),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 15),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 15),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 15 * 2),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 15 * 2),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 15 * 3),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 15 * 3),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 15 * 4),
				},
			},
		},
		{
			name:      "ChangingCronIrregular",
			eventTime: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 10),
			rollupCron: mustParseCron([]string{
				"*/13 * * *",
			}),
			rollupTime: timeInterval{
				LowerBound: now.Truncate(time.Hour),
				UpperBound: now.Truncate(time.Hour).Add(time.Hour),
			},
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 10),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 13),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 13),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 13 * 2),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 13 * 2),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 13 * 3),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 13 * 3),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 13 * 4),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 13 * 4),
					UpperBound: now.Truncate(time.Hour),
				},
			},
		},
	}

	for i := range tests {
		test := tests[i]
		t.Run(test.name, func(t *testing.T) {
			res := generateLowerIntervalRollupTimes(test.eventTime, test.rollupTime, test.rollupCron)
			assert.Equal(t, test.expected, res)
		})
	}
}

func TestGenerateNewRollupTimes(t *testing.T) {
	type testdata struct {
		name       string
		now        time.Time
		eventTime  eventTime
		rollupCron rollupCron
		rollupTime *rollupTime
		expected   []timeInterval
	}

	now := TimeNowFunc().UTC().Truncate(time.Hour).Add(time.Minute * 15)

	hourlyCrons := mustParseCron([]string{"0 * * *"})

	tests := []testdata{
		{
			name: "NoRollupTimes",
			now:  now,
			eventTime: eventTime{
				Min: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 10),
				Max: now.Truncate(time.Hour).Add(time.Hour).Add(time.Minute * 50),
			},
			rollupCron: hourlyCrons,
			rollupTime: nil,
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour).Add(time.Minute * 10),
					UpperBound: now.Truncate(time.Hour),
				},
				{
					LowerBound: now.Truncate(time.Hour),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 2),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour * 2),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 3),
				},
			},
		},
		{
			name: "NoRollupTimesInThePast",
			now:  now,
			eventTime: eventTime{
				Min: now.Truncate(time.Hour).Add(-time.Hour * 2).Add(time.Minute * 10),
				Max: now.Truncate(time.Hour).Add(-time.Hour * 2).Add(time.Minute * 50),
			},
			rollupCron: hourlyCrons,
			rollupTime: nil,
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 2).Add(time.Minute * 10),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour),
					UpperBound: now.Truncate(time.Hour),
				},
				{
					LowerBound: now.Truncate(time.Hour),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 2),
				},
			},
		},
		{
			name: "LowerAndUpper",
			now:  now,
			eventTime: eventTime{
				Min: now.Truncate(time.Hour).Add(-time.Hour * 2).Add(time.Minute * 10),
				Max: now.Truncate(time.Hour).Add(time.Hour * 2).Add(time.Minute * 10),
			},
			rollupCron: hourlyCrons,
			rollupTime: &rollupTime{
				Min: timeInterval{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour),
					UpperBound: now.Truncate(time.Hour),
				},
				Max: timeInterval{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 2),
				},
			},
			expected: []timeInterval{
				{
					LowerBound: now.Truncate(time.Hour).Add(-time.Hour * 2).Add(time.Minute * 10),
					UpperBound: now.Truncate(time.Hour).Add(-time.Hour),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour * 2),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 3),
				},
				{
					LowerBound: now.Truncate(time.Hour).Add(time.Hour * 3),
					UpperBound: now.Truncate(time.Hour).Add(time.Hour * 4),
				},
			},
		},
	}

	for i := range tests {
		test := tests[i]
		t.Run(test.name, func(t *testing.T) {
			res := generateNewRollupTimes(now, test.eventTime, test.rollupTime, test.rollupCron)
			assert.Equal(t, test.expected, res)
		})
	}
}
