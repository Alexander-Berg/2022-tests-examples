def exhaust(test_case, gen, max_iterations):
    iterations = 0
    log = []
    try:
        while True:
            log.append(next(gen))
            iterations += 1
            test_case.assertLess(iterations, max_iterations)
    except StopIteration as exc:
        return exc.value, log
