#!/usr/bin/env bash

if [ "$(uname)" == "Darwin" ]; then
  curl -O -L https://github.com/askonomm/bruno/releases/latest/download/bruno-macos && \
  mv bruno-macos bruno && \
  chmod +x bruno
else
  curl -O -L https://github.com/askonomm/bruno/releases/latest/download/bruno-linux && \
  mv bruno-linux bruno && \
  chmod +x bruno
fi

while [[ "$#" -gt 0 ]]; do
  case $1 in
    -g|--global) global="true"; shift ;;
  esac
  shift
done

if [ "$global" == "true" ]; then
  sudo mv bruno /usr/local/bin/bruno
fi