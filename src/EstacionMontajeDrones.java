import java.util.concurrent.Semaphore;

/**
 * Solución al problema de la Estación de Montaje de Drones
 * Simula 5 operarios que compiten por herramientas compartidas (soldadores y destornilladores)
 * Previene interbloqueo mediante orden de adquisición de recursos
 */
public class EstacionMontajeDrones {
    private static final int NUM_OPERARIOS = 5;

    public static void main(String[] args) {
        MesaMontaje mesa = new MesaMontaje(NUM_OPERARIOS);

        // Crear y arrancar los hilos de los operarios
        Thread[] operarios = new Thread[NUM_OPERARIOS];
        for (int i = 0; i < NUM_OPERARIOS; i++) {
            operarios[i] = new Thread(new Operario(i, mesa));
            operarios[i].start();
        }

        // Ejecutar durante 30 segundos y luego detener
        try {
            Thread.sleep(30000);
            System.out.println("\n=== Finalizando jornada de trabajo ===\n");
            for (Thread operario : operarios) {
                operario.interrupt();
            }

            // Esperar a que todos terminen
            for (Thread operario : operarios) {
                operario.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Clase que representa a un operario como un hilo
 * Cada operario necesita un soldador (izquierda) y un destornillador (derecha)
 */
class Operario implements Runnable {
    private final int id;
    private final MesaMontaje mesa;
    private int dronesEnsamblados = 0;

    public Operario(int id, MesaMontaje mesa) {
        this.id = id;
        this.mesa = mesa;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                preparar();
                solicitarHerramientas();
                ensamblar();
                liberarHerramientas();
            }
        } catch (InterruptedException e) {
            System.out.println("[Operario " + id + "] - Finalizó su turno. Total de drones ensamblados: " + dronesEnsamblados);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Estado 1: PREPARANDO
     * El operario organiza sus piezas
     */
    private void preparar() throws InterruptedException {
        System.out.println("[Operario " + id + "] - Preparando piezas...");
        Thread.sleep((long) (Math.random() * 1500 + 500)); // 500ms a 2000ms
    }

    /**
     * Estado 2: SOLICITANDO HERRAMIENTAS
     * El operario intenta adquirir ambas herramientas
     */
    private void solicitarHerramientas() throws InterruptedException {
        System.out.println("[Operario " + id + "] - Intentando coger herramientas...");
        mesa.tomarHerramientas(id);
        System.out.println("[Operario " + id + "] - Herramientas adquiridas (Soldador " +
                id + " y Destornillador " + ((id + 1) % 5) + ")");
    }

    /**
     * Estado 3: ENSAMBLANDO
     * El operario trabaja con ambas herramientas
     */
    private void ensamblar() throws InterruptedException {
        dronesEnsamblados++;
        System.out.println("[Operario " + id + "] - Ensamblando dron nº " + dronesEnsamblados + "...");
        Thread.sleep((long) (Math.random() * 2000 + 1000)); // 1000ms a 3000ms
        System.out.println("[Operario " + id + "] - Dron nº " + dronesEnsamblados + " completado");
    }

    /**
     * Estado 4: LIBERANDO
     * El operario suelta las herramientas en orden inverso
     */
    private void liberarHerramientas() {
        System.out.println("[Operario " + id + "] - Finalizado. Soltando herramientas.");
        mesa.soltarHerramientas(id);
    }
}

/**
 * Clase que gestiona la mesa circular con las herramientas compartidas
 * Implementa la sincronización mediante semáforos
 */
class MesaMontaje {
    private final int numPuestos;
    private final Semaphore[] soldadores;
    private final Semaphore[] destornilladores;
    private final Semaphore supervisor; // Previene interbloqueo

    public MesaMontaje(int numPuestos) {
        this.numPuestos = numPuestos;

        // Crear un semáforo por cada herramienta
        this.soldadores = new Semaphore[numPuestos];
        this.destornilladores = new Semaphore[numPuestos];

        for (int i = 0; i < numPuestos; i++) {
            soldadores[i] = new Semaphore(1); // 1 permiso = solo un operario puede usar la herramienta
            destornilladores[i] = new Semaphore(1);
        }

        // Semáforo supervisor: solo permite que 4 operarios trabajen simultáneamente
        // Esto rompe la espera circular y previene el deadlock
        this.supervisor = new Semaphore(numPuestos - 1);
    }

    /**
     * Método para adquirir ambas herramientas
     * Implementa estrategia de prevención de deadlock:
     * - El último operario toma las herramientas en orden inverso
     */
    public void tomarHerramientas(int idOperario) throws InterruptedException {
        // El supervisor controla cuántos operarios pueden intentar trabajar
        supervisor.acquire();

        int soldadorIzq = idOperario;
        int destornilladorDer = (idOperario + 1) % numPuestos;

        // Estrategia anti-deadlock: el último operario invierte el orden
        if (idOperario == numPuestos - 1) {
            // Operario 4 toma primero el destornillador, luego el soldador
            destornilladores[destornilladorDer].acquire();
            soldadores[soldadorIzq].acquire();
        } else {
            // Resto de operarios: primero soldador, luego destornillador
            soldadores[soldadorIzq].acquire();
            destornilladores[destornilladorDer].acquire();
        }
    }

    /**
     * Método para liberar ambas herramientas en orden inverso
     */
    public void soltarHerramientas(int idOperario) {
        int soldadorIzq = idOperario;
        int destornilladorDer = (idOperario + 1) % numPuestos;

        // Liberar en orden inverso al que se adquirieron
        if (idOperario == numPuestos - 1) {
            soldadores[soldadorIzq].release();
            destornilladores[destornilladorDer].release();
        } else {
            destornilladores[destornilladorDer].release();
            soldadores[soldadorIzq].release();
        }

        // Liberar el supervisor
        supervisor.release();
    }
}