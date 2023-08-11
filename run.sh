#!/bin/bash

# Check for the "open" parameter
if [ "$1" == "open" ]; then
  VITE_HOST="--host"
  SPRING_ARGS="-PopenServer=true"
else
  VITE_HOST=""
  SPRING_ARGS=""
fi

cleanup() {
    echo "Stopping all jobs"
    kill "$VITE_PID" "$PYTHON_PID" "$GRADLE_PID"
}

trap cleanup EXIT ERR

cd ./client && npx vite "$VITE_HOST" &
VITE_PID=$!
cd ./server/embedding-microservice && python microservice.py &
PYTHON_PID=$!
cd ./server && ./gradlew bootRun "$SPRING_ARGS" &
GRADLE_PID=$!

wait -n $VITE_PID $PYTHON_PID $GRADLE_PID
cleanup
