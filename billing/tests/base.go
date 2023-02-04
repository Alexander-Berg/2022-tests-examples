package tests

import (
	"math/rand"
	"time"

	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type BaseTestSuite struct {
	btesting.BaseSuite
	SampleTimes []time.Time
}

// GenerateSampleTimes method is used to create a series of time objects so one
// don't have to repeat unix timestamp over and over in different tests.
// Times are going in strictly increasing order for convinience when comparison is needed.
// Start unix timestamp is random number between 1000000000 and time.Now().Unix()
func (s *BaseTestSuite) GenerateSampleTimes(n int) {
	result := make([]time.Time, 0)
	min := 1000000000
	max := int(time.Now().Unix())
	start := rand.Intn(max-min) + min
	step := 1000

	for i := 1; i < n; i++ {
		result = append(result, time.Unix(int64(start+step*i), 0))
	}

	s.SampleTimes = result
}
