#!/bin/bash

export PUBLIC_IP=`curl http://169.254.169.254/latest/meta-data/public-ipv4`

sudo java -jar dist/SnakeServer.jar -s ${PUBLIC_IP}
