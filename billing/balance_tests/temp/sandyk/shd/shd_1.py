# -*- coding: utf-8 -*-
__author__ = 'sandyk'

######################################################################################################################
## задание 1
# Напишите функцию, которая получает на вход последовательность чисел и возвращает
# последовательность ее кумулятивных сумм: ноль, первое число, сумма первых двух, сумма
# первых трех и т.д. Например, для последовательности
# [1, 2, 3] она должна вернуть  [0, 1, 3, 6]

# def acc_summ(sequence):
# sequence.insert(0,0)
# acc_sequence = []
#     new_item = 0
#     for item in sequence:
#         new_item += item
#         acc_sequence.append(new_item)
#     print acc_sequence
#
# acc_summ([1,4,6,8,9])

######################################################################################################################
## задание 2
# Напишите функцию, которая получает на вход последовательность чисел, а также нижнюю границу a и верхнюю границу
# b и “обрезает” все числа в соответствии с этим диапазоном. То есть все числа, меньшие a, должны быть заменены на
# a , а все числа, большие b,  на b

# def replace(sequence, a, b):
#     new_sequence = []
#     for item in sequence:
#         if item < a: item = a
#         if item > b: item = b
#         new_sequence.append(item)
#     print new_sequence
#
# replace([2, 5, 7, 9, 3, 555, 34, 2344], 7, 100)

######################################################################################################################
## задание 3
# Напишите программу, которая получает из консоли число и проверяет, что для этого
# числа верна гипотеза Коллатца. Эта гипотеза состоит в следующем. Возьмем произволь-
# ное число n. Если оно четно, заменим его на n/2, если нет, то заменим его на 3n+1. К полученному
# числу применим те же действия, и так далее, пока не получим число 1.
# Программа также должна выводить цепочку преобразований.

# def collats(n):
#     sequence = [n]
#     while n>=1:
#         if n%2== 0: n=n/2
#         else: n = 3*n+1
#         sequence.append(n)
#         if n == 1: break
#     seq = ''
#     for item in sequence:
#         seq +=  str(item) + '->'
#     print seq[:-2]
#
# n= int(input(u'Введите натуральное число для проверки гипотезы Коллатца: '))
# collats (n)


######################################################################################################################
##задание 4
# Напишите программу, которая выводит на экран текст песни “Ten Green Bottles”:
# Ten green bottles hanging on the wall,
# Ten green bottles hanging on the wall,
# And if one green bottle should accidentally fall,
# There’ll be nine green bottles hanging on the wall.
# Nine green bottles hanging on the wall,
# Nine green bottles hanging on the wall,
# And if one green bottle should accidentally fall,
# There’ll be eight green bottles hanging on the wall.
# . . .
# One green bottle hanging on the wall,
# One green bottle hanging on the wall,
# If that one green bottle should accidentally fall
# There’ll be no green bottles hanging on the wall.
# Постарайтесь написать программу так, чтобы в ней было как можно меньше “сырого
# текста”.

count = ['ten', 'nine', 'eiqht', 'seven', 'six', 'five', 'four', 'three', 'two', 'one', 'no']
strings = {1: '{0} green bottles hanging on the wall,',
           2: 'And if one green bottle should accidentally fall,',
           3: 'There’ll be {0} green bottles hanging on the wall.'}


def bottles():
    for item in range(10):
        print strings[1].format(count[item].capitalize())
        print strings[1].format(count[item].capitalize())
        print strings[2]
        print strings[3].format(count[item + 1])
        print ''


bottles()

######################################################################################################################
##задание 5
# Напишите функцию, разлагающую данное число на простые множители. Результатом
# работы программы должен быть список, в котором каждому простому множителю p и его степени k соответствует пара
# ( p; k ) (вложенный список). Например, число 12 должно быть разложено так: [[2, 2], [3, 1]].



# def simple (n):