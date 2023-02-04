package tracker

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestFilterToString(t *testing.T) {
	tests := []struct {
		name     string
		expected string
		filter   Filter
	}{
		{name: "empty", expected: "", filter: Filter{}},
		{name: "queue", expected: "filter=queue:SOLOMON", filter: Filter{Queue: &SolomonQueue}},
		{name: "one component", expected: "filter=components:1", filter: Filter{Components: []int{1}}},
		{name: "few components", expected: "filter=components:1,2,3", filter: Filter{Components: []int{1, 2, 3}}},
		{name: "one status", expected: "filter=status:open", filter: Filter{Statuses: []string{StatusOpen}}},
		{
			name:     "few statuses",
			expected: "filter=status:open,commited",
			filter:   Filter{Statuses: []string{StatusOpen, StatusCommited}}},
		{
			name:     "mixed",
			expected: "filter=queue:SOLOMON&filter=components:38167&filter=status:inProgress",
			filter: Filter{
				Queue:      &SolomonQueue,
				Components: []int{SolomonReleaseID},
				Statuses:   []string{StatusInProgress},
			}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			assert.Equal(t, test.expected, test.filter.String())
		})
	}
}
