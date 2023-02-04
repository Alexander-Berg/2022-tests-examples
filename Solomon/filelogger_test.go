package filelogger

import (
	"strconv"
	"testing"
	"time"
)

func TestLogger(t *testing.T) {
	localTest := false

	// Only for local testing
	if localTest {
		g := NewFileLogger("qqq", 1000, 5)

		data := []byte("123456789\n")
		for i := 0; i < 300; i++ {
			d := []byte(strconv.Itoa(i) + " ")
			d = append(d, data...)
			_, _ = g.Write(d)
			time.Sleep(time.Millisecond)
		}

		time.Sleep(1 * time.Second)
	}
}
