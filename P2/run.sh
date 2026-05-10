#!/bin/bash
OUT="out"

JVM_FLAGS="-Djava.net.preferIPv4Stack=true"

case "$1" in

  compile)
    echo "=== Compilando proyecto ==="
    mkdir -p "$OUT"
    find . -name "*.java" > sources.txt
    javac -d "$OUT" @sources.txt
    if [ $? -eq 0 ]; then
      echo "Compilacion exitosa. Clases en '$OUT/'"
    else
      echo "ERROR en la compilacion."
      exit 1
    fi
    ;;

  servidor)
    IP="${2:-}"
    echo "=== Arrancando Servidor RMI ==="
    if [ -n "$IP" ]; then
      echo "Hostname RMI fijada a: $IP"
      java $JVM_FLAGS -Djava.rmi.server.hostname="$IP" -cp "$OUT" P2.servidor.ServidorRMI
    else
      java $JVM_FLAGS -cp "$OUT" P2.servidor.ServidorRMI
    fi
    ;;

  cliente)
    HOST="${2:-localhost}"
    echo "=== Arrancando Cliente RMI (conectando a $HOST) ==="
    java $JVM_FLAGS -cp "$OUT" P2.cliente.ClienteRMI "$HOST"
    ;;

  *)
    echo "Uso: $0 {compile|servidor [IP]|cliente [IP]}"
    exit 1
    ;;

esac
