#!/usr/bin/env bash

[[ -f "$1" ]] && source .env
cd ./client && npx vite --host & cd ./embeddings && python serve.py & cd ./server && ./gradlew bootRun -PopenServer=true && fg && fg
