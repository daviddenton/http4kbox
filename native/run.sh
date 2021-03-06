#!/usr/bin/env bash

set -e

export $(cat http4kbox.env | xargs)

echo "http4kbox will run at: http://localhost:${HTTP_PORT}"
docker-compose build
docker-compose up
