#!/usr/bin/env bash

cd ./client && npx vite --host & cd ./embeddings && python serve.py & cd ./server && ./gradlew bootRun -PopenServer=true && fg && fg
