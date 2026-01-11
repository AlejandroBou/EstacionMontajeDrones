## Descripción

Práctica de **Gestión de Procesos e Hilos** que simula una estación de montaje de drones donde varios operarios (hilos) compiten por herramientas compartidas. El objetivo es aplicar conceptos de **exclusión mutua**, **sincronización** y **prevención de deadlock**.

### Contexto del Problema

Una empresa de tecnología dispone de una **mesa circular con 5 puestos de trabajo**. En cada puesto hay un operario que necesita **dos herramientas** para ensamblar un dron:

- **Soldador** (situado a la izquierda)
- **Destornillador** (situado a la derecha)

** El problema**: Las herramientas son compartidas. El soldador de un operario es el destornillador del operario contiguo, por lo que dos operarios adyacentes no pueden trabajar simultáneamente.

## Arquitectura

```
┌─────────────────────────────────────┐
│  EstacionMontajeDrones (Main)       │
│  - Crea la mesa y los operarios     │
│  - Inicia la simulación              │
└──────────────┬──────────────────────┘
               │
               ├──────────────────────────┐
               │                          │
       ┌───────▼────────┐        ┌───────▼────────┐
       │   Operario     │        │   MesaMontaje  │
       │   (Thread)     │◄───────┤  (Sincroniza)  │
       │                │        │                │
       │ - preparar()   │        │ - Semáforos    │
       │ - solicitar()  │        │ - Supervisor   │
       │ - ensamblar()  │        │                │
       │ - liberar()    │        └────────────────┘
       └────────────────┘
```

## Estados del Hilo

Cada operario ejecuta un ciclo infinito con 4 estados:

```mermaid
graph LR
    A[1. PREPARANDO] --> B[2. SOLICITANDO]
    B --> C[3. ENSAMBLANDO]
    C --> D[4. LIBERANDO]
    D --> A
```

1. **PREPARANDO** (500-2000ms): Organiza sus piezas
2. **SOLICITANDO**: Intenta adquirir soldador y destornillador
3. **ENSAMBLANDO** (1000-3000ms): Trabaja con ambas herramientas
4. **LIBERANDO**: Suelta las herramientas en orden inverso

## Ejecución

### Requisitos Previos

- **Java 17** o superior
- JDK instalado y configurado

### Compilar

```bash
javac EstacionMontajeDrones.java
```

### Ejecutar

```bash
java EstacionMontajeDrones
```

### Salida Esperada

```
[Operario 0] - Preparando piezas...
[Operario 1] - Preparando piezas...
[Operario 2] - Intentando coger herramientas...
[Operario 2] - Herramientas adquiridas (Soldador 2 y Destornillador 3)
[Operario 2] - Ensamblando dron nº 1...
[Operario 0] - Intentando coger herramientas...
[Operario 2] - Dron nº 1 completado
[Operario 2] - Finalizado. Soltando herramientas.
[Operario 0] - Herramientas adquiridas (Soldador 0 y Destornillador 1)
...
```

## Solución al Deadlock

### Problema de Interbloqueo

Si todos los operarios toman su herramienta izquierda al mismo tiempo, quedarán esperando indefinidamente por la derecha → **DEADLOCK**

### Estrategia Implementada

**Ruptura de la espera circular** mediante orden de adquisición:

```java
if (idOperario == numPuestos - 1) {
    // Operario 4: primero destornillador, luego soldador
    destornilladores[destornilladorDer].acquire();
    soldadores[soldadorIzq].acquire();
} else {
    // Operarios 0-3: primero soldador, luego destornillador
    soldadores[soldadorIzq].acquire();
    destornilladores[destornilladorDer].acquire();
}
```

### Mecanismos Adicionales

- **Semáforo Supervisor**: Solo permite 4 operarios trabajando simultáneamente
- **Liberación en orden inverso**: Garantiza consistencia

## Conceptos Aplicados

| Concepto | Implementación |
|----------|----------------|
| **Hilos** | `Thread` y `Runnable` |
| **Sincronización** | `Semaphore` |
| **Exclusión Mutua** | Semáforos binarios (1 permiso) |
| **Prevención de Deadlock** | Orden de adquisición asimétrico |
| **Prevención de Starvation** | Semáforo supervisor (fairness) |

## Criterios de Evaluación

- **CE a)**: Identificación de estados de hilos
- **CE b)**: Uso de clases para creación y control de hilos
- **CE c)**: Mecanismos de sincronización aplicados
- **CE d)**: Solución que previene bloqueo mutuo

## Estructura del Proyecto

```
.
├── EstacionMontajeDrones.java  # Clase principal
├── Operario.java               # Hilo que representa un operario
├── MesaMontaje.java            # Gestión de herramientas compartidas
└── README.md                   # Este archivo
```

### Notas Adicionales

- La simulación se ejecuta durante **30 segundos**
- Cada operario lleva cuenta de los drones ensamblados
- Los tiempos son aleatorios para simular variabilidad real
- El programa finaliza de forma ordenada, mostrando estadísticas
