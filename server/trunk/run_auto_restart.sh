while true;
do
  echo "Starting server..."
  java -Djava.net.preferIPv4Stack=true -server -jar dist/SnakeServer.jar -a 8 -s supplelabs.xen.prgmr.com -r 300 -i 10 &
  sleep 0.2d
  echo "Killing server..."
  killall java
  sleep 5s
done
