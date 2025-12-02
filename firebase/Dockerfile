FROM andreysenov/firebase-tools:latest

# Pre-download the emulator UI
RUN mkdir -p /home/node/.cache/firebase/emulators && \
    curl -L -o /home/node/.cache/firebase/emulators/ui-v1.15.0.zip \
    https://storage.googleapis.com/firebase-preview-drop/emulator/ui-v1.15.0.zip

WORKDIR /home/node/firebase
