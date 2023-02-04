package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"

	"a.yandex-team.ru/billing/hot/accrualer/internal/entities"
)

func main() {
	scanner := bufio.NewScanner(os.Stdin)
	for scanner.Scan() {
		var (
			actEntity entities.Act
		)
		data := scanner.Bytes()
		if err := json.Unmarshal(data, &actEntity); err != nil {
			fmt.Println(err)
			continue
		}

		actEntityBytes, err := json.Marshal(actEntity)
		if err != nil {
			fmt.Println(err)
			continue
		}

		fmt.Println(string(actEntityBytes))
	}
	if err := scanner.Err(); err != nil {
		fmt.Fprintln(os.Stderr, "reading standard input:", err)
	}
}
