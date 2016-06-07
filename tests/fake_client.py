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
NUM_DEATHS = int(sys.argv[2])

DIRECTIONS = ["n", "s", "e", "w"]
#DIRECTIONS = ["n"]

def get_random_direction():
  return DIRECTIONS[random.randint(0,len(DIRECTIONS)-1)]

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

    snake_host = sys.argv[3]
    print "Connecting to snake host:", snake_host

    try:
      s2.connect((snake_host, SNAKE_CLIENT_PORT))
    except:
      print "Failed to connect to", snake_host
      return

    s2.send("uSHADOW_CLONE\n\000")
    s2.send("r\n\000")
    s2.send(get_random_direction() + "\n\000")

    deaths = 0

    while 1:
      try:
        data = s2.recv(128)
      except:
        break

      if (len(data) == 0):
        break
      elif data[0] == 'd':
        deaths = deaths + 1
        if deaths > NUM_DEATHS:
          break
        else:
          s2.send("r\n\000")
          s2.send(get_random_direction() + "\n\000")

    #print "Client disconnecting..."

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
