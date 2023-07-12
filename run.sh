#!/bin/bash

cleanup() {
    echo "Stopping all jobs"
    kill $VITE_PID $PYTHON_PID $GRADLE_PID
}

trap cleanup EXIT ERR

cd ./client && npx vite &
VITE_PID=$!
cd ./server/embedding-microservice && python microservice.py &
PYTHON_PID=$!
cd ./server && ./gradlew bootRun &
GRADLE_PID=$!

wait -n $VITE_PID $PYTHON_PID $GRADLE_PID
cleanup
