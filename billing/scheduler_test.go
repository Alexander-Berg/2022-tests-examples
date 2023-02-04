package cron

import (
	"testing"
	"time"
)

func TestScheduler_Next(t *testing.T) {
	scheduler, err := NewScheduler([]string{
		"*/30 * * *",
		"0 14-16 * *",
		"33 15 * *",
	})
	if err != nil {
		t.Errorf("error parsing crons: %s", err.Error())
		return
	}

	lowerBound := time.Now().UTC().Truncate(time.Hour * 24).Add(time.Hour * 13)
	upperBound := lowerBound.Add(time.Hour * 3).Add(time.Minute)

	expected := []time.Time{
		lowerBound,
		lowerBound.Add(time.Minute * 30),
		lowerBound.Add(time.Hour),
		lowerBound.Add(time.Hour).Add(time.Minute * 30),
		lowerBound.Add(time.Hour * 2),
		lowerBound.Add(time.Hour * 2).Add(time.Minute * 30),
		lowerBound.Add(time.Hour * 2).Add(time.Minute * 33),
		lowerBound.Add(time.Hour * 3),
	}
	var actual []time.Time

	for now := lowerBound; now.Before(upperBound); actual, now = append(actual, now), scheduler.Next(now) {
	}

	if len(expected) != len(actual) {
		t.Errorf("invalid actual times len: %d", len(actual))
		return
	}
	for i := range expected {
		if !expected[i].Equal(actual[i]) {
			t.Errorf("invalid time: expected: %s, got: %s", expected[i].String(), actual[i].String())
			return
		}
	}
}
