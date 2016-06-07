while true;
do
  echo "Starting server..."
  java -server -jar dist/SnakeServer.jar -a 14 -s fix.ece.cmu.edu -r 360 -i 10 &
  sleep 0.2d
  echo "Killing server..."
  killall java
  sleep 5s
done
