Proyecto de Subastas
--------------------
Protocolo SAPS: Sistema de Subastas con Sockets y Protocolo es un protocolo de aplicación diseñado  
sobre TCP que permite a múltiples clientes participar en subastas coordinadas por un servidor central.

Arquitectura:
-------------
El sistema sigue una arquitectura cliente-servidor en la que múltiples clientes (Cliente A, Cliente B, Cliente C
, etc.)se conectan a un servidor central a través de TCP en el puerto 9090. El servidor, implementado en la clase
`ServidorSubasta`, actúa como punto de entrada de todas las conexiones y delega la gestión del estado en
el componente `GestorSubastas`.Este gestor mantiene un conjunto de subastas activas, cada una representada por una
instancia de la clase `Subasta`. Cada subasta dispone de su propio temporizador independiente, lo que permite
gestionar de forma concurrente múltiples subastas con tiempos de vida distintos.De este modo, 
el servidor centraliza la lógica del sistema, mientras que los clientes interactúan con él enviando peticiones 
y recibiendo respuestas y notificaciones en tiempo real.

Se añadió una extensión al temporizador de forma que si una puja válida entra cuando quedan15 segundos o menos,
el servidor extiende el tiempo  en 10 segundos adicionales y notifica a todos los clientes. El cierre es 
definitivo cuando  el temporizador llega a 0 sin nuevas pujas en la ventana final.

- Transporte: TCP (conexión persistente por cliente)
- Formato de mensaje: `TIPO|campo1|campo2|...\n`
- Separador de campos: `|`
- Delimitador de mensaje: `\n` (salto de línea)
- Concurrencia: Un hilo `ManejadorCliente` por cliente; estado global en `ConcurrentHashMap


Estructura del proyecto:
------------------------
El proyecto está organizado en diferentes paquetes que separan las responsabilidades del sistema:

- **comun/**
  Contiene la clase `Mensaje.java`, que define las constantes del protocolo y las utilidades
  necesarias para construir y procesar los mensajes intercambiados entre cliente y servidor.

- **servidor/**  
  Incluye todos los componentes del lado servidor:
  - `ServidorSubasta.java`: punto de entrada del servidor y encargado de aceptar conexiones TCP.
  - `GestorSubastas.java`: gestiona el estado global del sistema, incluyendo las subastas activas
    y los usuarios conectados.
  - `Subasta.java`: representa cada subasta individual, incluyendo su lógica interna y el temporizador.
  - `ManejadorCliente.java`: hilo encargado de gestionar la comunicación con cada cliente conectado.

- **cliente/**  
  Contiene la implementación del cliente:
  - `ClienteSubasta.java`: punto de entrada del cliente, con interfaz por consola basada en menú.
  - `ReceptorNotificaciones.java`: hilo que recibe y muestra las notificaciones asíncronas
    enviadas por el servidor.



Ejecución del código:
----------------------
**Lo primero se necesita:**
- Java 11 o superior
- Dos máquinas (físicas o virtuales) con conectividad IP entre ellas
- Puerto 9090 TCP abierto en el servidor.

**Instrucciones para compilar:** 
Situandonos en la carpeta raíz ejecutamos lo siguiente:
mkdir -p out //creamos una carpeta donde meter todos los .class
javac -d out *.java //compilamos todos los archivos .java

**Lanzamiento servidor**
java -cp out servidor.ServidorSubasta //el puerto esta automatico en 9090
//debe aparecer lo siguiente por terminal:
=== Servidor de Subastas SASP ===
Puerto: 9090
Esperando conexiones...

**Lanzamiento cliente**
java -cp out cliente.ClienteSubasta *192.168.1.10* //la ip donde este el servidor 


Ejemplos de uso:
------------------
**Registro y creación de una subasta**

**Otro cliente puja y todos reciben notificación**

**Cierre automático**




  
