/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package main;

/**
 *
 * @author User
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


public class TriageDigital {


    static final int CAPACIDAD_SALA = 2;
    static final int CAPACIDAD_TOTAL_PACIENTES = 50;
    static final int MAX_PACIENTES_POR_DIA = 12;
    static final int MAX_DIAS = 2;
    static final int CANTIDAD_SALAS = 5;
    static final int PASO_TIEMPO_DEFAULT = 5;

    static String[] pregunta = new String[5];
    static String[] tipoUrgencia = new String[5];
    static String[] colorNivel = new String[5];
    static String[] tiempoTexto = new String[5];
    static int[] tiempoMax = new int[5];

    // ====== CONTADORES Y ESTADO GLOBAL ======
    static int[] contadorNivel = new int[5];

    static int diaActual;
    static int maxDias = MAX_DIAS;
    static int contadorGlobal;
    static int tiempoAbsoluto;
    static int pasoTiempo;
    static int registradosHoy;
    static int maxPacientes = MAX_PACIENTES_POR_DIA;
    static int totalPacientes;
    static boolean diaFinalizado;
    static boolean omitirAvanceTiempo;
    static boolean huboLiberacionCupo;

    // ====== PACIENTES (arrays) ======
    static String[] nombrePac = new String[CAPACIDAD_TOTAL_PACIENTES];
    static int[] nivelPac = new int[CAPACIDAD_TOTAL_PACIENTES];
    static int[] diaPac = new int[CAPACIDAD_TOTAL_PACIENTES];
    static String[][] respuestasPac = new String[CAPACIDAD_TOTAL_PACIENTES][5];
    static int[] estadoPac = new int[CAPACIDAD_TOTAL_PACIENTES]; // -1 no existe, 0 en espera, 1 siendo atendido, 2 atendido, 3 trasladado
    static int[] salaPac = new int[CAPACIDAD_TOTAL_PACIENTES];
    static int[] slotPac = new int[CAPACIDAD_TOTAL_PACIENTES];
    static int[] duracionAtencionPac = new int[CAPACIDAD_TOTAL_PACIENTES];
    static int[] tiempoRegistro = new int[CAPACIDAD_TOTAL_PACIENTES];
    static int[] tiempoInicioAtencionReal = new int[CAPACIDAD_TOTAL_PACIENTES];
    static int[] tiempoFinAtencion = new int[CAPACIDAD_TOTAL_PACIENTES];
    static boolean[] excepcionCriticaPac = new boolean[CAPACIDAD_TOTAL_PACIENTES];
    static boolean[] trasladoPorExcedentePac = new boolean[CAPACIDAD_TOTAL_PACIENTES];
    static boolean[] fueExcedente = new boolean[CAPACIDAD_TOTAL_PACIENTES];
    static int[] pacienteLiberadoParaIdx = new int[CAPACIDAD_TOTAL_PACIENTES];

    // Marca si al finalizar la atención debe trasladarse a quirófano (resucitación)
    static boolean[] trasladoQuirofano = new boolean[CAPACIDAD_TOTAL_PACIENTES];

    // ====== SALAS ======
    // salas[i][0] = tipo (0 emergencia,1 normal,2 otro), salas[i][1] = ocupacion actual
    static int[][] salas = new int[CANTIDAD_SALAS][2];
    static int[][] salaPacientes = new int[CANTIDAD_SALAS][CAPACIDAD_SALA];
    static int[] salaAtencionActual = new int[CANTIDAD_SALAS];

    // ====== EVENT LOG (novedades por sala) ======
    static List<String> eventLog = new ArrayList<>();

    // ====== ERROR LOG ======
    static List<String> errorLog = new ArrayList<>();

    // ====== UTILIDADES ======
    static Scanner sc = new Scanner(System.in);
    static Random rnd = new Random();

    // ====== MAIN ======
    public static void main(String[] args) {
        InicializarDatosTriage();
        InicializarSalas();
        InicializarSimulacion();

        boolean salir = false;

        while (!salir) {
            mostrarMenu();
            int opcion = leerEnteroSeguro("");

            switch (opcion) {
                case 1:
                    if (diaFinalizado) {
                        System.out.println();
                        System.out.println("*** LA SIMULACION DE LOS " + maxDias + " DIAS HA FINALIZADO. No se pueden registrar mas pacientes. ***");
                    } else {
                        RegistrarPaciente();
                    }
                    break;
                case 2:
                    MostrarTablaTriage();
                    break;
                case 3:
                    MostrarClasificacion();
                    break;
                case 4:
                    MostrarEstadoSalas();
                    break;
                case 5:
                    if (diaFinalizado) {
                        System.out.println();
                        System.out.println("*** LA SIMULACION DE LOS " + maxDias + " DIAS HA FINALIZADO. No se pueden liberar pacientes. ***");
                    } else {
                        LiberarPacienteManual();
                    }
                    break;
                case 6:
                    MostrarEstadisticas();
                    break;
                case 7:
                    MostrarPacientesRegistrados();
                    break;
                case 8:
                    if (diaFinalizado) {
                        System.out.println();
                        System.out.println("*** LA SIMULACION DE LOS " + maxDias + " DIAS HA FINALIZADO. No hay mas dias por finalizar. ***");
                    } else {
                        GenerarReporteDia();
                        if (diaActual < maxDias) {
                            ReiniciarParaNuevoDia();
                        } else {
                            FinalizarSimulacion();
                        }
                    }
                    break;
                case 9:
                    salir = true;
                    System.out.println("Saliendo del sistema...");
                    break;
                default:
                    System.out.println("Opcion invalida, intente nuevamente.");
            }

            if (!salir) {
                if (!diaFinalizado && !omitirAvanceTiempo) {
                    AvanzarTiempo();
                    ProcesarSalas();
                } else {
                    if (omitirAvanceTiempo) {
                        omitirAvanceTiempo = false;
                    }
                }

                if (huboLiberacionCupo) {
                    AsignarPacientesPendientes();
                    huboLiberacionCupo = false;
                }

                // --- Mostrar novedades acumuladas desde la ultima iteracion ---
                if (!eventLog.isEmpty()) {
                    System.out.println();
                    System.out.println("=== Novedades recientes en las salas ===");
                    for (String msg : eventLog) {
                        System.out.println(msg);
                    }
                    System.out.println("========================================");
                    // limpiar log para la siguiente iteracion
                    eventLog.clear();
                }

                System.out.println();
                System.out.println("Presione ENTER para volver al menu");
                sc.nextLine();
                limpiarPantalla();
            }
        }

        sc.close();
    }

   

    // ---------- Inicialización de datos ----------
    static void InicializarDatosTriage() {
        pregunta[0] = "Presenta paro cardiaco, paro respiratorio, shock, sangrado grave o trauma severo?";
        pregunta[1] = "Presenta trauma moderado, alteracion de conciencia, asfixia, quemadura leve, sangrado moderado o intoxicacion?";
        pregunta[2] = "Presenta trauma leve sin compromiso de conciencia, infeccion, dolor agudo, deshidratacion, fractura, luxacion o fiebre en menor de 2 anos?";
        pregunta[3] = "Presenta diarrea, dolor al orinar, alergia, dolor de mas de 24 horas o fiebre en mayor de 2 anos?";
        pregunta[4] = "Presenta dolor o golpe de mas de 3 dias, dolor de garganta o enfermedad de piel, pelo o unas?";

        tipoUrgencia[0] = "Resucitacion";
        tipoUrgencia[1] = "Emergencia";
        tipoUrgencia[2] = "Urgencia";
        tipoUrgencia[3] = "Urgencia menor";
        tipoUrgencia[4] = "Sin urgencia";

        colorNivel[0] = "Rojo";
        colorNivel[1] = "Naranja";
        colorNivel[2] = "Amarillo";
        colorNivel[3] = "Verde";
        colorNivel[4] = "Azul";

        tiempoTexto[0] = "Atencion inmediata";
        tiempoTexto[1] = "10-15 minutos";
        tiempoTexto[2] = "60 minutos";
        tiempoTexto[3] = "120 minutos";
        tiempoTexto[4] = "240 minutos";

        tiempoMax[0] = 0;
        tiempoMax[1] = 15;
        tiempoMax[2] = 60;
        tiempoMax[3] = 120;
        tiempoMax[4] = 240;
    }

    static void InicializarSalas() {
        // salas 0 y 1: tipo 0 (emergencia)
        for (int i = 0; i <= 1; i++) {
            salas[i][0] = 0;
            salas[i][1] = 0;
        }
        // salas 2 y 3: tipo 1 (normal)
        for (int i = 2; i <= 3; i++) {
            salas[i][0] = 1;
            salas[i][1] = 0;
        }
        // sala 4: tipo 2 (otro)
        salas[4][0] = 2;
        salas[4][1] = 0;

        for (int i = 0; i < CANTIDAD_SALAS; i++) {
            salaAtencionActual[i] = -1;
            for (int j = 0; j < CAPACIDAD_SALA; j++) {
                salaPacientes[i][j] = -1;
            }
        }
    }

    static void InicializarSimulacion() {
        int cantidadPacientesPrueba = 8;
        String[] nombrePrueba = new String[cantidadPacientesPrueba];
        int[] nivelPrueba = new int[cantidadPacientesPrueba];

        diaActual = 1;
        contadorGlobal = 0;
        tiempoAbsoluto = 0;
        totalPacientes = 0;
        registradosHoy = 0;
        pasoTiempo = PASO_TIEMPO_DEFAULT;
        diaFinalizado = false;
        omitirAvanceTiempo = false;
        huboLiberacionCupo = false;

        for (int j = 0; j < 5; j++) contadorNivel[j] = 0;

        for (int i = 0; i < CAPACIDAD_TOTAL_PACIENTES; i++) {
            nombrePac[i] = "";
            nivelPac[i] = -1;
            diaPac[i] = 0;
            estadoPac[i] = -1;
            salaPac[i] = -1;
            slotPac[i] = -1;
            duracionAtencionPac[i] = 0;
            tiempoRegistro[i] = -1;
            tiempoInicioAtencionReal[i] = -1;
            tiempoFinAtencion[i] = -1;
            excepcionCriticaPac[i] = false;
            trasladoPorExcedentePac[i] = false;
            fueExcedente[i] = false;
            pacienteLiberadoParaIdx[i] = -1;
            trasladoQuirofano[i] = false;
            for (int j = 0; j < 5; j++) respuestasPac[i][j] = "";
        }

        // Datos de prueba (precarga)
        nombrePrueba[0] = "Juan Perez";        nivelPrueba[0] = 0;
        nombrePrueba[1] = "Maria Lopez";       nivelPrueba[1] = 0;
        nombrePrueba[2] = "Carlos Sanchez";    nivelPrueba[2] = 1;
        nombrePrueba[3] = "Lucia Fernandez";   nivelPrueba[3] = 1;
        nombrePrueba[4] = "Ana Torres";        nivelPrueba[4] = 2;
        nombrePrueba[5] = "Pedro Ramirez";     nivelPrueba[5] = 2;
        nombrePrueba[6] = "Sofia Castro";      nivelPrueba[6] = 3;
        nombrePrueba[7] = "Miguel Rojas";      nivelPrueba[7] = 4;

        for (int k = 0; k < cantidadPacientesPrueba; k++) {
            int nivel = nivelPrueba[k];
            nombrePac[totalPacientes] = nombrePrueba[k];
            nivelPac[totalPacientes] = nivel;
            diaPac[totalPacientes] = diaActual;
            tiempoRegistro[totalPacientes] = tiempoAbsoluto;
            contadorNivel[nivel]++;

            if (nivel > 0) {
                for (int j = 0; j < nivel; j++) respuestasPac[totalPacientes][j] = "No";
            }
            respuestasPac[totalPacientes][nivel] = "Si";
            if (nivel < 4) {
                for (int j = nivel + 1; j <= 4; j++) respuestasPac[totalPacientes][j] = "-";
            }

            // Llamada corregida: solo idxPaciente y nivel
            AsignarPacienteASala(totalPacientes, nivel);

            totalPacientes++;
            registradosHoy++;
        }
    }

    // ---------- Menú y utilidades ----------
    static void mostrarMenu() {
        System.out.println();
        System.out.println("==================== TRIAGE DIGITAL IESS MACHALA ====================");
        System.out.println("Dia actual: " + diaActual + " / " + maxDias);
        System.out.println("Tiempo simulado del dia: " + contadorGlobal + " minutos");
        System.out.println("Pacientes registrados hoy: " + registradosHoy + " / " + maxPacientes);
        System.out.println("1. Registrar paciente");
        System.out.println("2. Ver tabla de triage de un paciente");
        System.out.println("3. Ver clasificacion de pacientes por nivel");
        System.out.println("4. Ver estado de las salas");
        System.out.println("5. Liberar un paciente manualmente");
        System.out.println("6. Ver estadisticas");
        System.out.println("7. Ver pacientes registrados");
        System.out.println("8. Finalizar dia y mostrar reporte");
        System.out.println("9. Salir");
        System.out.print("Seleccione una opcion: ");
    }

    // Lee un entero con reintentos y mensaje
    static int leerEnteroSeguro(String prompt) {
        while (true) {
            if (prompt != null && !prompt.isEmpty()) System.out.print(prompt);
            String line = sc.nextLine();
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.print("Entrada inválida. Ingrese un número entero: ");
            }
        }
    }

    // Lee respuesta Si/No y devuelve boolean
    static boolean leerSiNo(String prompt) {
        while (true) {
            if (prompt != null && !prompt.isEmpty()) System.out.print(prompt + " (Si/No): ");
            String r = sc.nextLine().trim();
            if (r.equalsIgnoreCase("Si")) return true;
            if (r.equalsIgnoreCase("No")) return false;
            System.out.println("Respuesta inválida. Responda 'Si' o 'No'.");
        }
    }

    static void limpiarPantalla() {
        for (int i = 0; i < 30; i++) System.out.println();
    }

    // ---------- SubProcesos de tiempo y salas ----------
    static void AvanzarTiempo() {
        contadorGlobal += pasoTiempo;
        tiempoAbsoluto += pasoTiempo;
    }

    static void ProcesarSalas() {
        for (int i = 0; i < CANTIDAD_SALAS; i++) {
            if (salaAtencionActual[i] != -1) {
                int idxPacienteActivo = salaAtencionActual[i];
                if (tiempoAbsoluto >= tiempoFinAtencion[idxPacienteActivo]) {
                    FinalizarAtencionNormal(i);
                    PromoverCompaneroEnEspera(i);
                }
            }
        }
    }

    static void FinalizarAtencionNormal(int idxSala) {
        int idxPaciente = salaAtencionActual[idxSala];
        if (idxPaciente == -1) return;

        // calcular tiempo de espera real y duracion
        int inicio = tiempoInicioAtencionReal[idxPaciente];
        int registro = tiempoRegistro[idxPaciente];
        int espera = 0;
        if (inicio >= 0 && registro >= 0) espera = Math.max(0, inicio - registro);
        int duracion = duracionAtencionPac[idxPaciente];

        String nombre = nombrePac[idxPaciente];
        String msgSalida = "En la sala " + (idxSala + 1) + " " + nombre + " fue atendido y salio; espero " + espera + " minutos y su tiempo de atencion fue " + duracion + " minutos.";
        eventLog.add(msgSalida);

        // Si este paciente estaba marcado para traslado a quirofano, registrarlo
        if (trasladoQuirofano[idxPaciente]) {
            String msgQuiro = "En la sala " + (idxSala + 1) + " " + nombre + " fue trasladado a quirofano.";
            eventLog.add(msgQuiro);
            trasladoQuirofano[idxPaciente] = false;
            // marcar como trasladado
            estadoPac[idxPaciente] = 3;
        } else {
            estadoPac[idxPaciente] = 2; // atendido normalmente
        }

        LiberarSlot(idxPaciente, idxSala);
    }

    static void LiberarSlot(int idxPaciente, int idxSala) {
        for (int j = 0; j < CAPACIDAD_SALA; j++) {
            if (salaPacientes[idxSala][j] == idxPaciente) {
                salaPacientes[idxSala][j] = -1;
            }
        }
        salas[idxSala][1] = Math.max(0, salas[idxSala][1] - 1);
        salaPac[idxPaciente] = -1;
        slotPac[idxPaciente] = -1;
        if (salaAtencionActual[idxSala] == idxPaciente) salaAtencionActual[idxSala] = -1;
        huboLiberacionCupo = true;
    }

    static void PromoverCompaneroEnEspera(int idxSala) {
        if (salaAtencionActual[idxSala] == -1) {
            int idxPaciente = -1;
            for (int j = 0; j < CAPACIDAD_SALA; j++) {
                if (salaPacientes[idxSala][j] != -1) {
                    int cand = salaPacientes[idxSala][j];
                    if (estadoPac[cand] == 0) idxPaciente = cand;
                }
            }
            if (idxPaciente != -1) {
                IniciarAtencio(idxSala, idxPaciente);
            }
        }
    }

    static void IniciarAtencio(int idxSala, int idxPaciente) {
        salaAtencionActual[idxSala] = idxPaciente;
        estadoPac[idxPaciente] = 1;
        tiempoInicioAtencionReal[idxPaciente] = tiempoAbsoluto;
        tiempoFinAtencion[idxPaciente] = tiempoAbsoluto + duracionAtencionPac[idxPaciente];

        // registrar mensaje de inicio de atencion
        String nombre = nombrePac[idxPaciente];
        String msg = "En la sala " + (idxSala + 1) + " " + nombre + " pasa a ser atendido.";
        eventLog.add(msg);
    }

    // ---------- Asignación de salas (con preempción para resucitación) ----------
    static int DeterminarTipoSalaRequerido(int nivel) {
        return (nivel <= 1) ? 0 : 1;
    }

    static int BuscarSalaLibre() {
        for (int i = 0; i < CANTIDAD_SALAS; i++) {
            if (salas[i][1] == 0) return i;
        }
        return -1;
    }

    static int BuscarSalaPorTipoConCupo(int tipoRequerido) {
        for (int i = 0; i < CANTIDAD_SALAS; i++) {
            if (salas[i][0] == tipoRequerido && salas[i][1] < CAPACIDAD_SALA) return i;
        }
        return -1;
    }

static void OcuparSlotEnSala(int idxPaciente, int idxSala) {
    if (idxPaciente < 0 || idxPaciente >= CAPACIDAD_TOTAL_PACIENTES) {
        errorLog.add("OcuparSlotEnSala: idxPaciente fuera de rango: " + idxPaciente);
        return;
    }
    if (salaPac[idxPaciente] != -1) {
        errorLog.add("OcuparSlotEnSala: paciente ya tiene sala asignada: " + nombrePac[idxPaciente]);
        return;
    }

    // Si el paciente entrante es de resucitación y la sala está completa,
    // expulsar al ÚLTIMO paciente en la fila que no esté siendo atendido.
    if (nivelPac[idxPaciente] == 0 && salas[idxSala][1] >= CAPACIDAD_SALA) {
        int slotParaExpulsar = -1;
        for (int j = CAPACIDAD_SALA - 1; j >= 0; j--) {
            int cand = salaPacientes[idxSala][j];
            if (cand != -1 && estadoPac[cand] != 1) { // no expulsar al que está siendo atendido
                slotParaExpulsar = j;
                break;
            }
        }
        if (slotParaExpulsar != -1) {
            int expulsado = salaPacientes[idxSala][slotParaExpulsar];
            String nombreExp = nombrePac[expulsado];
            // sacar al expulsado de la sala (queda en espera sin sala asignada)
            salaPacientes[idxSala][slotParaExpulsar] = -1;
            salas[idxSala][1] = Math.max(0, salas[idxSala][1] - 1);
            salaPac[expulsado] = -1;
            slotPac[expulsado] = -1;
            estadoPac[expulsado] = 0; // queda en espera hasta que haya cupo en sala de su tipo

            // registrar desplazamiento usando el mensaje existente
            eventLog.add("En la sala " + (idxSala + 1) + " " + nombreExp + " fue desplazado a espera por llegada de " + nombrePac[idxPaciente] + " (resucitacion).");
        }
    }

    int slotAsignado = -1;
    for (int j = 0; j < CAPACIDAD_SALA; j++) {
        if (salaPacientes[idxSala][j] == -1) {
            slotAsignado = j;
            break;
        }
    }
    if (slotAsignado != -1) {
        salaPacientes[idxSala][slotAsignado] = idxPaciente;
        salas[idxSala][1] = salas[idxSala][1] + 1;
        salaPac[idxPaciente] = idxSala;
        slotPac[idxPaciente] = slotAsignado;
        // Si no se ha definido duracion, asignar aleatoria
        if (duracionAtencionPac[idxPaciente] == 0) duracionAtencionPac[idxPaciente] = 15 + rnd.nextInt(45); // 15 + azar(45)
        if (salaAtencionActual[idxSala] == -1) {
            IniciarAtencio(idxSala, idxPaciente);
        } else {
            // Si es resucitacion, no debe esperar en la fila: darle prioridad inmediata.
            if (nivelPac[idxPaciente] == 0) {
                // No expulsamos al que ya está siendo atendido por regla 3.
                // Para asegurar prioridad inmediata, marcamos al resucitado como siendo atendido
                // y actualizamos tiempos; el anterior atendido pasa a estado 0 (espera) si no es resucitado.
                int anterior = salaAtencionActual[idxSala];
                if (anterior != -1 && nivelPac[anterior] != 0) {
                    estadoPac[anterior] = 0; // pasa a espera (no se saca físicamente del slot)
                }
                salaAtencionActual[idxSala] = idxPaciente;
                estadoPac[idxPaciente] = 1;
                tiempoInicioAtencionReal[idxPaciente] = tiempoAbsoluto;
                tiempoFinAtencion[idxPaciente] = tiempoAbsoluto + duracionAtencionPac[idxPaciente];
                String nombre = nombrePac[idxPaciente];
                eventLog.add("En la sala " + (idxSala + 1) + " " + nombre + " pasa a ser atendido.");
            } else {
                estadoPac[idxPaciente] = 0; // en espera
            }
        }
    } else {
        System.out.println("ERROR INTERNO: OcuparSlotEnSala recibio una sala sin cupo disponible.");
        errorLog.add("OcuparSlotEnSala: sala " + idxSala + " sin cupo para paciente " + nombrePac[idxPaciente]);
    }
}


    /**
     * AsignarPacienteASala con manejo especial para nivel 0 (resucitación).
     * - Si es resucitación: intenta sala libre o con cupo; si no hay cupo, preempciona
     *   al paciente que esté siendo atendido en una sala (preferir salas de tipo 0).
     */
    static void AsignarPacienteASala(int idxPaciente, int nivel) {
    if (idxPaciente < 0 || idxPaciente >= CAPACIDAD_TOTAL_PACIENTES) {
        errorLog.add("AsignarPacienteASala: idxPaciente fuera de rango: " + idxPaciente);
        return;
    }
    int tipoRequerido = DeterminarTipoSalaRequerido(nivel);

    // Manejo estricto para RESUCITACION (nivel 0)
    if (nivel == 0) {
        // Buscar entre todas las salas de emergencia (salas[i][0] == 0)
        // una sala que NO tenga actualmente ningún paciente de resucitación.
        int salaSeleccionada = -1;
        for (int i = 0; i < CANTIDAD_SALAS; i++) {
            if (salas[i][0] == 0) {
                boolean tieneResucitacion = false;
                for (int j = 0; j < CAPACIDAD_SALA; j++) {
                    int idx = salaPacientes[i][j];
                    if (idx != -1 && nivelPac[idx] == 0) {
                        tieneResucitacion = true;
                        break;
                    }
                }
                if (!tieneResucitacion) {
                    salaSeleccionada = i;
                    break; // preferimos la primera sala de emergencia sin resucitacion
                }
            }
        }

        if (salaSeleccionada != -1) {
            // Asegurar tipo de sala y forzar inicio inmediato
            salas[salaSeleccionada][0] = 0;
            duracionAtencionPac[idxPaciente] = 5;
            trasladoQuirofano[idxPaciente] = true;

            // Si la sala tiene ocupación completa, expulsar al último en fila (no al atendido)
            if (salas[salaSeleccionada][1] >= CAPACIDAD_SALA) {
                int slotParaExpulsar = -1;
                for (int j = CAPACIDAD_SALA - 1; j >= 0; j--) {
                    int cand = salaPacientes[salaSeleccionada][j];
                    if (cand != -1 && estadoPac[cand] != 1) { // no expulsar al que está siendo atendido
                        slotParaExpulsar = j;
                        break;
                    }
                }
                if (slotParaExpulsar != -1) {
                    int expulsado = salaPacientes[salaSeleccionada][slotParaExpulsar];
                    String nombreExp = nombrePac[expulsado];
                    salaPacientes[salaSeleccionada][slotParaExpulsar] = -1;
                    salas[salaSeleccionada][1] = Math.max(0, salas[salaSeleccionada][1] - 1);
                    salaPac[expulsado] = -1;
                    slotPac[expulsado] = -1;
                    estadoPac[expulsado] = 0; // queda en espera
                    eventLog.add("En la sala " + (salaSeleccionada + 1) + " " + nombreExp + " fue desplazado a espera por llegada de " + nombrePac[idxPaciente] + " (resucitacion).");
                }
            }

            // Ocupar slot y asegurar que el resucitado no espere en fila
            OcuparSlotEnSala(idxPaciente, salaSeleccionada);

            // Si la sala ya tenía alguien en atencion, OcuparSlotEnSala se encargó de marcar
            // al resucitado como atendido y de ajustar estados según la regla 3.
            return;
        } else {
            // Si no se encontró ninguna sala de emergencia sin resucitación,
            // entonces todas las salas de emergencia tienen al menos un resucitado:
            // el paciente debe ser trasladado a otro centro de salud.
            estadoPac[idxPaciente] = 3; // trasladado
            eventLog.add(nombrePac[idxPaciente] + " fue trasladado a otro centro de salud.");
            return;
        }
    }

    // Comportamiento normal para otros niveles (NO resucitación)
    int salaAsignada = BuscarSalaLibre();
    if (salaAsignada != -1) {
        salas[salaAsignada][0] = tipoRequerido;
    } else {
        salaAsignada = BuscarSalaPorTipoConCupo(tipoRequerido);
    }

    if (salaAsignada == -1) {
        // Si no hay cupo para pacientes que NO son resucitacion, trasladar al otro centro
        estadoPac[idxPaciente] = 3; // trasladado
        eventLog.add(nombrePac[idxPaciente] + " fue trasladado a otro centro de salud.");
    } else {
        OcuparSlotEnSala(idxPaciente, salaAsignada);
    }
}


    // ---------- NUEVO: Forzar preempcion si AsignarPacienteASala no dejó al resucitado siendo atendido ----------
    static void ForzarPreempcionResucitacion(int idxPaciente) {
        if (idxPaciente < 0 || idxPaciente >= CAPACIDAD_TOTAL_PACIENTES) {
            errorLog.add("ForzarPreempcionResucitacion: idx fuera de rango: " + idxPaciente);
            return;
        }
        // Asegurar duracion y marca de traslado
        if (duracionAtencionPac[idxPaciente] == 0) duracionAtencionPac[idxPaciente] = 5;
        trasladoQuirofano[idxPaciente] = true;

        // Buscar primero una sala con alguien siendo atendido (preferir tipo 0)
        int salaParaPreempcionar = -1;
        for (int i = 0; i < CANTIDAD_SALAS && salaParaPreempcionar == -1; i++) {
            if (salaAtencionActual[i] != -1 && salas[i][0] == 0) salaParaPreempcionar = i;
        }
        for (int i = 0; i < CANTIDAD_SALAS && salaParaPreempcionar == -1; i++) {
            if (salaAtencionActual[i] != -1) salaParaPreempcionar = i;
        }

        if (salaParaPreempcionar != -1) {
            int victima = salaAtencionActual[salaParaPreempcionar];
            String nombreVictima = nombrePac[victima];

            // Desplazar a la victima a espera (liberamos su slot para el nuevo)
            estadoPac[victima] = 0;
            int slotVictima = slotPac[victima];
            if (slotVictima != -1) {
                salaPacientes[salaParaPreempcionar][slotVictima] = -1;
                salas[salaParaPreempcionar][1] = Math.max(0, salas[salaParaPreempcionar][1] - 1);
                slotPac[victima] = -1;
                salaPac[victima] = -1;
            }
            salaAtencionActual[salaParaPreempcionar] = -1;

            // Registrar evento de desplazamiento
            eventLog.add("En la sala " + (salaParaPreempcionar + 1) + " " + nombreVictima + " fue desplazado a espera por llegada de " + nombrePac[idxPaciente] + " (resucitacion).");

            // Asegurar tipo de sala y ocupar slot para el resucitado
            salas[salaParaPreempcionar][0] = 0;
            OcuparSlotEnSala(idxPaciente, salaParaPreempcionar);
            return;
        }

        // Si no hay nadie siendo atendido (caso raro), intentar ocupar cualquier sala con cupo
        int salaConCupo = BuscarSalaPorTipoConCupo(0);
        if (salaConCupo == -1) salaConCupo = BuscarSalaLibre();
        if (salaConCupo != -1) {
            salas[salaConCupo][0] = 0;
            OcuparSlotEnSala(idxPaciente, salaConCupo);
            return;
        }

        // Si no se pudo asignar, dejar en espera (estado 0) y registrar
        estadoPac[idxPaciente] = 0;
        salaPac[idxPaciente] = -1;
        slotPac[idxPaciente] = -1;
        eventLog.add("No fue posible asignar inmediatamente a " + nombrePac[idxPaciente] + " (resucitacion). Queda en espera.");
    }

    // ---------- Registro y triage (con validaciones) ----------
    static void RegistrarPaciente() {
        // Verificar límite diario antes de iniciar el proceso de registro
        if (registradosHoy >= maxPacientes) {
            System.out.println();
            System.out.println("*** LIMITE DIARIO ALCANZADO: Ya se registraron " + registradosHoy + " pacientes hoy (límite: " + maxPacientes + "). No se pueden registrar más pacientes en este día. ***");
            return;
        }

        if (totalPacientes >= CAPACIDAD_TOTAL_PACIENTES) {
            System.out.println();
            System.out.println("*** CAPACIDAD TECNICA MAXIMA DEL SISTEMA ALCANZADA (" + CAPACIDAD_TOTAL_PACIENTES + " registros). No se pueden registrar mas pacientes en esta ejecucion. ***");
            return;
        }

        String nombrePaciente;
        while (true) {
            System.out.println("Ingrese el nombre del paciente:");
            nombrePaciente = sc.nextLine().trim();
            if (validarNombreNuevo(nombrePaciente)) break;
            // si no válido, vuelve a pedir
        }

        String[] respuestasTmp = new String[5];
        int nivelTmp = RealizarTriageConValidacion(respuestasTmp);

        int idxNuevo = totalPacientes;
        nombrePac[idxNuevo] = nombrePaciente;
        nivelPac[idxNuevo] = nivelTmp;
        diaPac[idxNuevo] = diaActual;
        tiempoRegistro[idxNuevo] = tiempoAbsoluto;
        for (int j = 0; j < 5; j++) respuestasPac[idxNuevo][j] = respuestasTmp[j];
        contadorNivel[nivelTmp]++;

        boolean excedeLimite = VerificarLimiteDiario();
        if (excedeLimite) {
            System.out.println("Se alcanzó el límite diario. Aplicando política de excedente...");
            ManejarPacienteExcedente(idxNuevo, nivelTmp);
        } else {
            AsignarPacienteASala(idxNuevo, nivelTmp);
            // Si es resucitacion y no quedó siendo atendido, forzar preempcion
            if (nivelTmp == 0 && estadoPac[idxNuevo] != 1) {
                ForzarPreempcionResucitacion(idxNuevo);
            }
        }

        boolean trasladadoParaBoletin = (estadoPac[idxNuevo] == 3);
        GenerarBoletin(idxNuevo, trasladadoParaBoletin);

        totalPacientes++;
        registradosHoy++;
        System.out.println("Paciente registrado correctamente: " + nombrePaciente);
    }

    // RealizarTriage con validación de Si/No
    static int RealizarTriageConValidacion(String[] respuestasOut) {
        int nivelOut = -1;
        int f = 0;
        while (nivelOut == -1 && f <= 4) {
            System.out.println();
            System.out.println("Pregunta " + (f + 1) + ": " + pregunta[f]);
            boolean respuesta = leerSiNo("Responda");
            if (respuesta) {
                nivelOut = f;
                respuestasOut[f] = "Si";
            } else {
                respuestasOut[f] = "No";
                f++;
            }
        }
        if (nivelOut == -1) nivelOut = 4;
        else if (nivelOut < 4) {
            for (int j = nivelOut + 1; j <= 4; j++) respuestasOut[j] = "-";
        }
        return nivelOut;
    }

    static boolean VerificarLimiteDiario() {
        return registradosHoy >= maxPacientes;
    }

    // ---------- Manejo de excedente ----------
    static int SeleccionarPacienteParaTrasladoPorExcedente() {
        int resultado = -1;
        // orden de busqueda: niveles 4..1
        for (int nivelBuscado = 4; nivelBuscado >= 1 && resultado == -1; nivelBuscado--) {
            // 1) en espera con sala fisica
            for (int i = 0; i < totalPacientes && resultado == -1; i++) {
                if (nivelPac[i] == nivelBuscado && estadoPac[i] == 0 && salaPac[i] != -1) resultado = i;
            }
            // 2) en espera sin sala
            for (int i = 0; i < totalPacientes && resultado == -1; i++) {
                if (nivelPac[i] == nivelBuscado && estadoPac[i] == 0 && salaPac[i] == -1) resultado = i;
            }
            // 3) siendo atendido
            for (int i = 0; i < totalPacientes && resultado == -1; i++) {
                if (nivelPac[i] == nivelBuscado && estadoPac[i] == 1) resultado = i;
            }
        }
        return resultado;
    }

    static void ManejarPacienteExcedente(int idxPaciente, int nivel) {
        fueExcedente[idxPaciente] = true;
        if (nivel != 0) {
            estadoPac[idxPaciente] = 3;
            trasladoPorExcedentePac[idxPaciente] = true;
        } else {
            int candidatoIdx = SeleccionarPacienteParaTrasladoPorExcedente();
            if (candidatoIdx != -1) {
                if (estadoPac[candidatoIdx] == 1) {
                    int salaCandidato = salaPac[candidatoIdx];
                    LiberarSlot(candidatoIdx, salaCandidato);
                    estadoPac[candidatoIdx] = 3;
                    PromoverCompaneroEnEspera(salaCandidato);
                } else {
                    if (salaPac[candidatoIdx] != -1) {
                        LiberarSlot(candidatoIdx, salaPac[candidatoIdx]);
                        estadoPac[candidatoIdx] = 3;
                    } else {
                        estadoPac[candidatoIdx] = 3;
                    }
                }
                trasladoPorExcedentePac[candidatoIdx] = true;
                pacienteLiberadoParaIdx[idxPaciente] = candidatoIdx;
                excepcionCriticaPac[idxPaciente] = false;
            } else {
                excepcionCriticaPac[idxPaciente] = true;
                pacienteLiberadoParaIdx[idxPaciente] = -1;
            }
            // Intentar asignar al paciente excedente a sala si hay cupo
            AsignarPacienteASala(idxPaciente, nivel);
        }
    }

    // ---------- Reasignación de pendientes ----------
    static void AsignarPacientesPendientes() {
        for (int nivelBuscado = 0; nivelBuscado <= 4; nivelBuscado++) {
            for (int i = 0; i < totalPacientes; i++) {
                if (nivelPac[i] == nivelBuscado && estadoPac[i] == 0 && salaPac[i] == -1) {
                    int tipoRequerido = DeterminarTipoSalaRequerido(nivelPac[i]);
                    int salaEncontrada = BuscarSalaLibre();
                    if (salaEncontrada != -1) salas[salaEncontrada][0] = tipoRequerido;
                    else salaEncontrada = BuscarSalaPorTipoConCupo(tipoRequerido);
                    if (salaEncontrada != -1) OcuparSlotEnSala(i, salaEncontrada);
                }
            }
        }
    }

    // ---------- Consultas y reportes ----------
    static void MostrarTablaTriage() {
    System.out.println();
    System.out.println("Ingrese indice de paciente (0 - " + (totalPacientes - 1) + "):");
    int idx = leerEnteroSeguro("");
    if (idx < 0 || idx >= totalPacientes) {
        System.out.println("Indice invalido.");
        return;
    }

    // Encabezado similar a la imagen: Tabla de Triage - <Nombre>
    System.out.println();
    System.out.println("Tabla de Triage - " + nombrePac[idx]);
    System.out.println("---------------------------------------------------------------------");
    System.out.printf("%-10s | %-10s | %-5s | %-15s | %-20s%n", "Pregunta", "Respuesta", "Nivel", "Tipo", "Tiempo");
    System.out.println("---------------------------------------------------------------------");

    for (int i = 0; i < 5; i++) {
        String resp = respuestasPac[idx][i];
        if (resp == null || resp.isEmpty()) resp = "-";
        int nivel = nivelPac[idx];
        String nivelTexto = String.valueOf((i == nivel) ? nivel : (respuestasPac[idx][i].equals("Si") ? i : nivel));
        // Mostrar el nivel real del paciente en la fila donde corresponde "Si"
        String tipo = (i == nivel) ? tipoUrgencia[nivel] : "-";
        String tiempo = (i == nivel) ? tiempoTexto[nivel] : "-";
        // Para claridad, en la fila donde la respuesta es "Si" mostramos el nivel/tipo/tiempo del paciente
        if (resp.equalsIgnoreCase("Si")) {
            System.out.printf("%-10s | %-10s | %-5s | %-15s | %-20s%n",
                    "Pregunta " + (i + 1), resp, nivel, tipo, tiempo);
        } else {
            System.out.printf("%-10s | %-10s | %-5s | %-15s | %-20s%n",
                    "Pregunta " + (i + 1), resp, "-", "-", "-");
        }
    }
    System.out.println("---------------------------------------------------------------------");
}


    static int BuscarPacientePorNombre(String nombreBuscado) {
        for (int i = 0; i < totalPacientes; i++) {
            if (nombrePac[i].equalsIgnoreCase(nombreBuscado)) return i;
        }
        return -1;
    }

    static void MostrarClasificacion() {
        for (int nivel = 0; nivel <= 4; nivel++) {
            System.out.println();
            System.out.println("=======" + tipoUrgencia[nivel] + " (" + colorNivel[nivel] + ")=======");
            boolean hayPacientes = false;
            for (int i = 0; i < totalPacientes; i++) {
                if (nivelPac[i] == nivel) {
                    hayPacientes = true;
                    System.out.print(nombrePac[i] + " (Dia " + diaPac[i] + ") - ");
                    switch (estadoPac[i]) {
                        case 0: System.out.println("En espera"); break;
                        case 1: System.out.println("Siendo atendido"); break;
                        case 2: System.out.println("Atendido"); break;
                        case 3: System.out.println("Trasladado"); break;
                        default: System.out.println("Sin estado"); break;
                    }
                }
            }
            if (!hayPacientes) System.out.println("(Sin pacientes en este nivel)");
        }
    }

    static void MostrarEstadoSalas() {
        System.out.println();
        System.out.println("=====================================Estado de Salas=========================================");
        for (int i = 0; i < CANTIDAD_SALAS; i++) {
            System.out.println();
            System.out.print("Sala " + (i + 1) + " - ");
            if (salas[i][1] == 0) System.out.print("Libre/Sin asignar - ");
            else {
                if (salas[i][0] == 0) System.out.print("Emergencia - ");
                else if (salas[i][0] == 1) System.out.print("Normal - ");
                else System.out.print("Sin asignar - ");
            }
            System.out.println("Ocupacion: " + salas[i][1] + "/" + CAPACIDAD_SALA);
            if (salas[i][1] > 0) {
                for (int j = 0; j < CAPACIDAD_SALA; j++) {
                    if (salaPacientes[i][j] != -1) {
                        int pacienteIdx = salaPacientes[i][j];
                        System.out.print("     - " + nombrePac[pacienteIdx]);
                        if (salaAtencionActual[i] == pacienteIdx) System.out.println(" (siendo atendido)");
                        else System.out.println(" (en espera dentro de la sala)");
                    }
                }
            } else {
                System.out.println("     (Sala libre. No tiene un tipo asignado actualmente; se clasificara nuevamente segun el tipo del proximo paciente que la ocupe.)");
            }
        }
        System.out.println("================================================================================================");
    }

    static void MostrarPacientesRegistrados() {
        System.out.println();
        System.out.println("=====================================Pacientes Registrados=========================================");
        if (totalPacientes == 0) {
            System.out.println("No hay pacientes registrados todavia.");
            return;
        }
        System.out.print("N.   ");
        System.out.print("Nombre          ");
        System.out.print("Dia   ");
        System.out.print("Nivel     ");
        System.out.print("Sala      ");
        System.out.print("Estado                       ");
        System.out.println("Detalle");
        System.out.println("-----------------------------------------------------------------------------------------");
        for (int i = 0; i < totalPacientes; i++) {
            System.out.print((i + 1) + "    ");
            System.out.print(nombrePac[i] + "     ");
            System.out.print(diaPac[i] + "     ");
            System.out.print((nivelPac[i] + 1) + "         ");
            switch (estadoPac[i]) {
                case 0:
                    if (salaPac[i] == -1) {
                        System.out.print("-         ");
                        System.out.println("En espera (pendiente de cupo fisico)");
                    } else {
                        System.out.print((salaPac[i] + 1) + "         ");
                        System.out.println("En espera dentro de la sala");
                    }
                    break;
                case 1:
                    System.out.print((salaPac[i] + 1) + "         ");
                    int restante = tiempoFinAtencion[i] - tiempoAbsoluto;
                    if (restante < 0) restante = 0;
                    System.out.println("Siendo atendido, " + restante + " min para salir");
                    break;
                case 2:
                    System.out.print("-         ");
                    System.out.println("Paciente ya atendido");
                    break;
                case 3:
                    System.out.print("-         ");
                    System.out.println("Trasladado a otro centro de salud");
                    break;
                default:
                    System.out.println("Sin estado");
            }
        }
        System.out.println("============================================================================================");
    }

  static void MostrarEstadisticas() {
    System.out.println();
    System.out.println("=====================================Estadisticas=========================================");
    System.out.println("Dia actual: " + diaActual + " / " + maxDias);
    System.out.println("Pacientes registrados hoy: " + registradosHoy + " / " + maxPacientes);
    System.out.println("Total de pacientes en toda la simulacion: " + totalPacientes);
    System.out.println();
    System.out.println("--- Pacientes clasificados por nivel de triage (cantidad y tiempo estimado del nivel) ---");
    for (int nivel = 0; nivel <= 4; nivel++) {
        System.out.println(tipoUrgencia[nivel] + " (" + colorNivel[nivel] + "): " + contadorNivel[nivel] + " paciente(s) - tiempo estimado: " + tiempoTexto[nivel]);
    }

    int totalAtendidos = 0, totalEnEspera = 0, totalSiendoAtendidos = 0, totalTrasladados = 0;
    int sumaEsperas = 0;

    for (int i = 0; i < totalPacientes; i++) {
        // Solo procesamos pacientes con tiempo de registro válido
        if (tiempoRegistro[i] < 0) continue;

        int esperaPaciente = 0;
        switch (estadoPac[i]) {
            case 0:
                totalEnEspera++;
                // espera hasta ahora
                esperaPaciente = Math.max(0, tiempoAbsoluto - tiempoRegistro[i]);
                sumaEsperas += esperaPaciente;
                break;
            case 1:
                totalSiendoAtendidos++;
                // espera real hasta inicio de atencion; si inicio no definido, usar tiempo actual
                if (tiempoInicioAtencionReal[i] >= 0) {
                    esperaPaciente = Math.max(0, tiempoInicioAtencionReal[i] - tiempoRegistro[i]);
                } else {
                    esperaPaciente = Math.max(0, tiempoAbsoluto - tiempoRegistro[i]);
                }
                sumaEsperas += esperaPaciente;
                break;
            case 2:
                totalAtendidos++;
                // espera real hasta inicio de atencion
                if (tiempoInicioAtencionReal[i] >= 0) {
                    esperaPaciente = Math.max(0, tiempoInicioAtencionReal[i] - tiempoRegistro[i]);
                } else {
                    esperaPaciente = Math.max(0, tiempoAbsoluto - tiempoRegistro[i]);
                }
                sumaEsperas += esperaPaciente;
                break;
            case 3:
                totalTrasladados++;
                // incluir el tiempo que el paciente esperó en el sistema hasta el traslado
                esperaPaciente = Math.max(0, tiempoAbsoluto - tiempoRegistro[i]);
                sumaEsperas += esperaPaciente;
                break;
            default:
                // pacientes no inicializados o con estado desconocido: no sumamos
                break;
        }
    }

    System.out.println();
    System.out.println("--- Estado actual de los pacientes ---");
    System.out.println("En espera: " + totalEnEspera);
    System.out.println("Siendo atendidos: " + totalSiendoAtendidos);
    System.out.println("Atendidos: " + totalAtendidos);
    System.out.println("Trasladados: " + totalTrasladados);

    System.out.println();
    System.out.println("--- Indicador general del sistema ---");
    int sumaEstimados = 0;
    for (int i = 0; i < totalPacientes; i++) {
        int nivel = nivelPac[i];
        if (nivel >= 0 && nivel <= 4) {
          sumaEstimados += tiempoMax[nivel];
    }
    }

    if (totalPacientes > 0) {
    double promedioEstimado = (double) sumaEstimados / (double) totalPacientes;
    System.out.printf("Tiempo promedio estimado de espera (segun nivel): %.1f minutos%n", promedioEstimado);
    } else {
        System.out.println("Tiempo promedio de espera: sin datos todavia.");
    }
    System.out.println("Tiempo simulado del dia actual (dia " + diaActual + "): " + contadorGlobal + " minutos");

    System.out.println();
    System.out.println("--- Ocupacion de salas ---");
    int ocupadosTotal = 0;
    for (int i = 0; i < CANTIDAD_SALAS; i++) ocupadosTotal += salas[i][1];
    int cuposTotales = CANTIDAD_SALAS * CAPACIDAD_SALA;
    System.out.println("Cupos ocupados: " + ocupadosTotal + " de " + cuposTotales);
    System.out.println("Cupos libres: " + (cuposTotales - ocupadosTotal) + " de " + cuposTotales);
    System.out.println("============================================================================================");
}


    static void GenerarBoletin(int idxPaciente, boolean trasladado) {
        if (idxPaciente < 0 || idxPaciente >= CAPACIDAD_TOTAL_PACIENTES) {
            System.out.println("GenerarBoletin: paciente inválido.");
            return;
        }
        int nivel = nivelPac[idxPaciente];
        System.out.println();
        System.out.println("Paciente " + nombrePac[idxPaciente] + " tenga su boletin");
        System.out.println("=================================Boletin==================================");
        System.out.print("Nivel   ");
        System.out.print("Tipo             ");
        System.out.print("Color     ");
        System.out.print("Sala      ");
        System.out.println("Tiempo estimado de atencion");
        System.out.println("---------------------------------------------------------------------------");
        System.out.print((nivel + 1) + "       ");
        System.out.print(tipoUrgencia[nivel] + "     ");
        System.out.print(colorNivel[nivel] + "     ");
        if (trasladado) System.out.print("-         ");
        else {
            if (salaPac[idxPaciente] == -1) System.out.print("-         ");
            else System.out.print((salaPac[idxPaciente] + 1) + "         ");
        }
        System.out.println();
        if (trasladado) System.out.println("Trasladado a otro centro de salud");
        else {
            // Ya no mostramos "en espera de cupo fisico disponible"
            System.out.println(tiempoTexto[nivel]);
        }
        System.out.println("===========================================================================");
    }

    static void GenerarReporteDia() {
        int atendidosDia = 0, enEsperaDia = 0, siendoAtendidosDia = 0, trasladadosDia = 0, excedentesDia = 0;
        int[] atendidosPorNivel = new int[5];
        int[] enEsperaPorNivel = new int[5];
        int[] siendoAtendidosPorNivel = new int[5];
        int[] trasladadosPorNivel = new int[5];

        System.out.println();
        System.out.println("=====================================Reporte de Fin de Dia=========================================");
        System.out.println("Dia: " + diaActual + " / " + maxDias);
        System.out.println("Tiempo simulado transcurrido en el dia: " + contadorGlobal + " minutos");
        System.out.println("Pacientes registrados hoy: " + registradosHoy + " / " + maxPacientes);
        System.out.println();
        System.out.println("--- Detalle de pacientes registrados el dia " + diaActual + " ---");

        for (int i = 0; i < totalPacientes; i++) {
            if (diaPac[i] == diaActual) {
                switch (estadoPac[i]) {
                    case 2:
                        atendidosDia++;
                        atendidosPorNivel[nivelPac[i]]++;
                        break;
                    case 3:
                        trasladadosDia++;
                        trasladadosPorNivel[nivelPac[i]]++;
                        break;
                    case 1:
                        siendoAtendidosDia++;
                        siendoAtendidosPorNivel[nivelPac[i]]++;
                        break;
                    case 0:
                        enEsperaDia++;
                        enEsperaPorNivel[nivelPac[i]]++;
                        break;
                }
                if (fueExcedente[i]) {
                    excedentesDia++;
                    if (excepcionCriticaPac[i]) {
                        System.out.println("  [EXCEDENTE] " + nombrePac[i] + " (nivel " + (nivelPac[i] + 1) + "): critico aceptado por EXCEPCION, no se traslado a nadie.");
                    } else {
                        if (trasladoPorExcedentePac[i]) {
                            System.out.println("  [EXCEDENTE] " + nombrePac[i] + " (nivel " + (nivelPac[i] + 1) + "): fue TRASLADADO a otro centro de salud por exceder el limite diario.");
                        } else {
                            int liberadoPara = pacienteLiberadoParaIdx[i];
                            if (liberadoPara >= 0 && liberadoPara < totalPacientes) {
                                System.out.println("  [EXCEDENTE] " + nombrePac[i] + " (nivel " + (nivelPac[i] + 1) + "): critico aceptado; se traslado a " + nombrePac[liberadoPara] + " (nivel " + (nivelPac[liberadoPara] + 1) + ") para darle espacio.");
                            } else {
                                System.out.println("  [EXCEDENTE] " + nombrePac[i] + " (nivel " + (nivelPac[i] + 1) + "): critico aceptado; sin asignacion de liberado.");
                            }
                        }
                    }
                }
            }
        }

        System.out.println();
        System.out.println("Pacientes atendidos (ya salieron de sala): " + atendidosDia);
        System.out.println("Pacientes siendo atendidos (continuan activos): " + siendoAtendidosDia);
        System.out.println("Pacientes en espera (continuan activos): " + enEsperaDia);
        System.out.println("Pacientes trasladados a otro centro de salud: " + trasladadosDia);
        System.out.println("Pacientes que excedieron el limite diario: " + excedentesDia);

        System.out.println();
        System.out.println("--- Detalle por nivel de urgencia ---");
        for (int nivel = 0; nivel <= 4; nivel++) {
            System.out.println(tipoUrgencia[nivel] + " (" + colorNivel[nivel] + "): " + atendidosPorNivel[nivel] + " atendido(s), " + siendoAtendidosPorNivel[nivel] + " siendo atendido(s), " + enEsperaPorNivel[nivel] + " en espera, " + trasladadosPorNivel[nivel] + " trasladado(s)");
        }
        System.out.println("=====================================================================================================");
    }

    static void ReiniciarParaNuevoDia() {
        diaActual++;
        contadorGlobal = 0;
        registradosHoy = 0;
        omitirAvanceTiempo = true;
        System.out.println();
        System.out.println("*** Comienza el Dia " + diaActual + ". El reloj del dia se reinicia a 0 minutos. ***");
        System.out.println("*** Los pacientes que seguian activos continuan su proceso normalmente. ***");
    }

    static void FinalizarSimulacion() {
        diaFinalizado = true;
        System.out.println();
        System.out.println("*** LA SIMULACION DE LOS " + maxDias + " DIAS HA FINALIZADO ***");
        System.out.println("*** Registrar pacientes, liberar manualmente y finalizar dia ya no estan disponibles. ***");
        System.out.println("*** Puede consultar tabla de triage, clasificacion, estadisticas y pacientes registrados, o salir. ***");
    }

    // ---------- Liberación manual ----------
    static void LiberarPacienteManual() {
        System.out.println("Ingrese el nombre del paciente a liberar:");
        String nombreBuscado = sc.nextLine().trim();
        int idxPaciente = BuscarPacientePorNombre(nombreBuscado);
        if (idxPaciente == -1) {
            System.out.println("No se encontro un paciente con ese nombre.");
            return;
        }
        switch (estadoPac[idxPaciente]) {
            case 1:
                int salaDelPaciente = salaPac[idxPaciente];
                LiberarSlot(idxPaciente, salaDelPaciente);
                estadoPac[idxPaciente] = 2;
                PromoverCompaneroEnEspera(salaDelPaciente);
                System.out.println("Paciente " + nombrePac[idxPaciente] + " fue atendido manualmente y salio de la sala " + (salaDelPaciente + 1) + ".");
                break;
            case 0:
                if (salaPac[idxPaciente] != -1) LiberarSlot(idxPaciente, salaPac[idxPaciente]);
                estadoPac[idxPaciente] = 2;
                System.out.println("Paciente " + nombrePac[idxPaciente] + " fue atendido manualmente (excepcion administrativa).");
                break;
            case 2:
                System.out.println("El paciente " + nombrePac[idxPaciente] + " ya fue atendido previamente.");
                break;
            case 3:
                System.out.println("El paciente " + nombrePac[idxPaciente] + " fue trasladado a otro centro de salud y no puede liberarse manualmente.");
                break;
            default:
                System.out.println("Estado no manejado para liberacion.");
        }
    }

    // ---------- VALIDACIONES ----------
    static boolean validarNombreNuevo(String nombre) {
        if (nombre == null) return false;
        nombre = nombre.trim();
        if (nombre.isEmpty()) {
            System.out.println("El nombre no puede estar vacío.");
            return false;
        }
        if (nombre.length() > 80) {
            System.out.println("El nombre es demasiado largo. Máx 80 caracteres.");
            return false;
        }
        if (esNombreDuplicado(nombre)) {
            System.out.println("Ya existe un paciente con ese nombre. Use un identificador único.");
            return false;
        }
        return true;
    }

    static boolean esNombreDuplicado(String nombre) {
        for (int i = 0; i < totalPacientes; i++) {
            if (nombrePac[i] != null && nombrePac[i].equalsIgnoreCase(nombre)) return true;
        }
        return false;
    }

  
}



