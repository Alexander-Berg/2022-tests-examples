package consul

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestDurationWrapper_String(t *testing.T) {
	assert.Equal(t, "0.25s", (&DurationWrapper{Value: time.Millisecond*250}).String())
	assert.Equal(t, "1s", (&DurationWrapper{Value: time.Second}).String())
}
