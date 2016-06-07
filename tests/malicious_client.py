#!/usr/bin/env python
# encoding: utf-8
"""
fake_client.py
Created by Eugene Marinelli on 2007-12-30.
"""

import socket
import sys
import random
from threading import Thread

SM_SERVER = "www.multisnake.com"
SNAKE_CLIENT_PORT = 10123

COMMANDS = ["n", "s", "e", "w", "r"]

def get_random_command():
  return COMMANDS[random.randint(0,len(COMMANDS)-1)]

class Client(Thread):
  def run(self):
    """
    # Get snake server from server manager.
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
      s.connect((SM_SERVER, 80))
    except:
      print "Failed to connect to %s on port 80" % SM_SERVER
      return

    s.send("GET /server_manager/get_server.php\n")
    snake_host = s.recv(64)
    s.close()
    """
    # Connect to snake server.
    s2 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    snake_host = "localhost"
    #snake_host = "pauling.doesntexist.org"
    #print "Connecting to snake host:", snake_host

    try:
      s2.connect((snake_host, SNAKE_CLIENT_PORT))
    except:
      print "Failed to connect to", snake_host
      return

    while 1:
      s2.send("r\n\000")
      s2.send(get_random_command() + "\n\000")

    s2.close()

threads = []

def main():
  try:
    num_clients = int(sys.argv[1])
  except:
    print "Usage: %s <number of clients>" % sys.argv[0]
    return

  print "Num clients: %d" % num_clients
  for i in range(num_clients):
    print "Starting client %d..." % i
    t = Client()
    threads.append(t)
    t.start()

  for t in threads: t.join()

if __name__ == '__main__':
  main()
