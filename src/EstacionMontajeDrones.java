import java.util.concurrent.Semaphore;

/**
 * Clase principal que simula la Estación de Montaje de Drones.
 * <p>
 * Lanza múltiples hilos (operarios) que compiten por recursos compartidos
 * (soldadores y destornilladores), aplicando técnicas de sincronización
 * para evitar interbloqueos (deadlock).
 * </p>
 */
public class EstacionMontajeDrones {

    /** Número total de operarios en la estación */
    private static final int NUM_OPERARIOS = 5;

    /**
     * Método principal del programa.
     * <p>
     * Inicializa la mesa de montaje, crea los hilos de los operarios
     * y ejecuta la simulación durante 30 segundos.
     * </p>
     *
     */
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

            // Interrumpir a todos los operarios
            for (Thread operario : operarios) {
                operario.interrupt();
            }

            // Esperar a que todos finalicen
            for (Thread operario : operarios) {
                operario.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Representa a un operario de la estación de montaje.
 * <p>
 * Cada operario es un hilo que alterna entre preparar piezas,
 * solicitar herramientas, ensamblar drones y liberar recursos.
 * </p>
 */
class Operario implements Runnable {

    /** Identificador único del operario */
    private final int id;

    /** Referencia compartida a la mesa de montaje */
    private final MesaMontaje mesa;

    /** Contador de drones ensamblados por el operario */
    private int dronesEnsamblados = 0;

    /**
     * Constructor del operario.
     *
     * @param id   identificador del operario
     * @param mesa mesa de montaje compartida
     */
    public Operario(int id, MesaMontaje mesa) {
        this.id = id;
        this.mesa = mesa;
    }

    /**
     * Ciclo principal de ejecución del hilo.
     * <p>
     * El operario trabaja continuamente hasta que el hilo es interrumpido.
     * </p>
     */
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
            System.out.println("[Operario " + id + "] - Finalizó su turno. Total de drones ensamblados: "
                    + dronesEnsamblados);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Estado PREPARANDO.
     * <p>
     * Simula el tiempo que el operario tarda en organizar las piezas
     * antes de comenzar el ensamblaje.
     * </p>
     *
     * @throws InterruptedException si el hilo es interrumpido
     */
    private void preparar() throws InterruptedException {
        System.out.println("[Operario " + id + "] - Preparando piezas...");
        Thread.sleep((long) (Math.random() * 1500 + 500));
    }

    /**
     * Estado SOLICITANDO HERRAMIENTAS.
     * <p>
     * El operario solicita a la mesa de montaje las herramientas necesarias
     * para trabajar (soldador y destornillador).
     * </p>
     *
     * @throws InterruptedException si el hilo es interrumpido
     */
    private void solicitarHerramientas() throws InterruptedException {
        System.out.println("[Operario " + id + "] - Intentando coger herramientas...");
        mesa.tomarHerramientas(id);
        System.out.println("[Operario " + id + "] - Herramientas adquiridas (Soldador "
                + id + " y Destornillador " + ((id + 1) % 5) + ")");
    }

    /**
     * Estado ENSAMBLANDO.
     * <p>
     * El operario utiliza ambas herramientas para ensamblar un dron.
     * </p>
     *
     * @throws InterruptedException si el hilo es interrumpido
     */
    private void ensamblar() throws InterruptedException {
        dronesEnsamblados++;
        System.out.println("[Operario " + id + "] - Ensamblando dron nº " + dronesEnsamblados + "...");
        Thread.sleep((long) (Math.random() * 2000 + 1000));
        System.out.println("[Operario " + id + "] - Dron nº " + dronesEnsamblados + " completado");
    }

    /**
     * Estado LIBERANDO.
     * <p>
     * El operario devuelve las herramientas a la mesa de montaje,
     * permitiendo que otros operarios puedan utilizarlas.
     * </p>
     */
    private void liberarHerramientas() {
        System.out.println("[Operario " + id + "] - Finalizado. Soltando herramientas.");
        mesa.soltarHerramientas(id);
    }
}

/**
 * Clase que gestiona la mesa de montaje circular.
 * <p>
 * Controla el acceso concurrente a las herramientas mediante semáforos
 * e implementa una estrategia para evitar el interbloqueo.
 * </p>
 */
class MesaMontaje {

    /** Número de puestos/operarios */
    private final int numPuestos;

    /** Semáforos que representan los soldadores */
    private final Semaphore[] soldadores;

    /** Semáforos que representan los destornilladores */
    private final Semaphore[] destornilladores;

    /**
     * Semáforo supervisor.
     * <p>
     * Limita el número de operarios que pueden intentar trabajar
     * simultáneamente para evitar el deadlock.
     * </p>
     */
    private final Semaphore supervisor;

    /**
     * Constructor de la mesa de montaje.
     *
     * @param numPuestos número total de puestos de trabajo
     */
    public MesaMontaje(int numPuestos) {
        this.numPuestos = numPuestos;
        this.soldadores = new Semaphore[numPuestos];
        this.destornilladores = new Semaphore[numPuestos];

        for (int i = 0; i < numPuestos; i++) {
            soldadores[i] = new Semaphore(1);
            destornilladores[i] = new Semaphore(1);
        }

        // Permite que solo N-1 operarios intenten trabajar a la vez
        this.supervisor = new Semaphore(numPuestos - 1);
    }

    /**
     * Permite a un operario adquirir las dos herramientas necesarias.
     * <p>
     * Aplica una estrategia anti-deadlock:
     * el último operario adquiere los recursos en orden inverso.
     * </p>
     *
     * @param idOperario identificador del operario
     * @throws InterruptedException si el hilo es interrumpido
     */
    public void tomarHerramientas(int idOperario) throws InterruptedException {
        supervisor.acquire();

        int soldadorIzq = idOperario;
        int destornilladorDer = (idOperario + 1) % numPuestos;

        if (idOperario == numPuestos - 1) {
            destornilladores[destornilladorDer].acquire();
            soldadores[soldadorIzq].acquire();
        } else {
            soldadores[soldadorIzq].acquire();
            destornilladores[destornilladorDer].acquire();
        }
    }

    /**
     * Libera las herramientas utilizadas por el operario.
     *
     * @param idOperario identificador del operario
     */
    public void soltarHerramientas(int idOperario) {
        int soldadorIzq = idOperario;
        int destornilladorDer = (idOperario + 1) % numPuestos;

        if (idOperario == numPuestos - 1) {
            soldadores[soldadorIzq].release();
            destornilladores[destornilladorDer].release();
        } else {
            destornilladores[destornilladorDer].release();
            soldadores[soldadorIzq].release();
        }
        supervisor.release();
    }
}
