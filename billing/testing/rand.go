package testing

import (
	"math/rand"
	"time"
)

const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

var seededRand = rand.New(rand.NewSource(time.Now().UnixNano()))

func RandSWithCharset(length int, charset string) string {
	b := make([]byte, length)
	for i := range b {
		b[i] = charset[seededRand.Intn(len(charset))]
	}
	return string(b)
}

func RandS(length int) string {
	return RandSWithCharset(length, charset)
}

func RandSP(length int) *string {
	res := RandSWithCharset(length, charset)
	return &res
}

func RandUN64() uint64 {
	return seededRand.Uint64()
}

func RandN64() int64 {
	return seededRand.Int63()
}

func RandN64n(min int64, max int64) int64 {
	return seededRand.Int63n(max-min+1) + min
}
