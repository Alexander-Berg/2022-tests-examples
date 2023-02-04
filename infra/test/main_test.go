package test

import (
	"bytes"
	"log"
	"os"
	"os/exec"
	"os/user"
	"testing"

	"a.yandex-team.ru/infra/goxcart/internal/app"
)

func init() {
	log.SetFlags(log.Ldate | log.Ltime | log.Llongfile)
}

func TestMain(m *testing.M) {
	_, err := user.Lookup(app.BalancerUser)
	if err != nil {
		cmd := exec.Command("adduser",
			"--no-create-home",
			"--disabled-password",
			app.BalancerUser,
			// app.BalancerGroup,
		)
		var buff bytes.Buffer
		cmd.Stdout = &buff
		cmd.Stderr = &buff
		if err := cmd.Run(); err != nil {
			log.Fatalf("adduser: %s\n%s", buff.String(), err)
		}
		log.Printf("adduser: %s", buff.String())
	}
	os.Exit(m.Run())
}
