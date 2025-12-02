#!/bin/bash
# Run SpecFlux backend in development mode with Firebase Emulator support

export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099

echo "Starting SpecFlux backend (dev mode)"
echo "Firebase Emulator: $FIREBASE_AUTH_EMULATOR_HOST"
echo ""

mvn spring-boot:run -Dspring-boot.run.profiles=dev "$@"
