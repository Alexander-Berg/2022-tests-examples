#!/usr/bin/env python

DefColor = "\033[0m"

Dark = "\033[0;"
Light = "\033[1;"

White = Light + "37m"
Gray = Dark + "37m"
Black = Light + "30m"
LightBlue = Light + "34m"
Blue = Dark + "34m"
Pink = Dark + "35m"
Cyan = Dark + "36m"
Red = '31m'
Green = '32m'


Colors = {
    'red': Dark + Red,
    'green': Dark + Green,
    'gray': Gray,
    'blue': Blue,
    'white': White,
    'black': Black,
    'pink': Pink,
}


def colorize(text, color1, color2 = DefColor):
    return Colors.get(color1, DefColor) + text + Colors.get(color2, DefColor)
