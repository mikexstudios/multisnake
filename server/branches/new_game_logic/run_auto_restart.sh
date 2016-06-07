while true;
do
  echo "Starting server..."
  java -server -jar dist/SnakeServer.jar -a 8 -s roboclub1.frc.ri.cmu.edu -r 60 -i 10 &
  sleep 0.2d
  echo "Killing server..."
  killall java
  sleep 5s
done
