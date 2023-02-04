# Невзирая на гордое название графита, в реальности этот стенд просто ловит
# в сокете на 42000 порту все, что можно и  выводит на экран
import socket
import sys

HOST = '127.0.0.1'
PORT = 42000

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    s.listen()
    while True:
        conn, addr = s.accept()
        with conn:
            print("Accepted conn from addr", addr)
            while True:
                data = conn.recv(2**24)
                if not data:
                    break
                print(repr(data))
            sys.stdout.flush()
