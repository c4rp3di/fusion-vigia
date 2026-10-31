package com.vigia.app.adb

import android.content.Context
import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.InvocationTargetException

/**
 * Modo avanzado: ejecuta órdenes del shell de ADB en este mismo teléfono (depuración inalámbrica, Android 11 o superior).
 * Solo se usan las funciones que documenta la librería; la conexión se comprueba ejecutando una orden trivial.
 */
object GestorAdb {
    @Volatile
    private var conexion: ConexionAdb? = null

    @Volatile
    var conectado: Boolean = false
        private set

    fun disponible(): Boolean = Build.VERSION.SDK_INT >= 30

    private fun obtener(contexto: Context): ConexionAdb {
        val actual = conexion
        if (actual != null) return actual
        synchronized(this) {
            val c = conexion ?: ConexionAdb.obtener(contexto.applicationContext)
            conexion = c
            return c
        }
    }

    /** Empareja con el código de seis dígitos y el puerto que muestra Ajustes › Depuración inalámbrica. */
    suspend fun emparejar(contexto: Context, puerto: Int, codigo: String): Boolean = withContext(Dispatchers.IO) {
        obtener(contexto).pair("127.0.0.1", puerto, codigo)
    }

    /** Descubre el puerto de conexión y se conecta. Devuelve true si la conexión queda funcionando. */
    suspend fun conectar(contexto: Context): Boolean = withContext(Dispatchers.IO) {
        val m = obtener(contexto)
        val ok = autoConectar(m, contexto.applicationContext)
        if (!ok) {
            conectado = false
            return@withContext false
        }
        conectado = comprobar()
        conectado
    }

    /** Ejecuta una orden de shell y devuelve lo que imprime. Lanza una excepción si la conexión no funciona. */
    suspend fun ejecutar(orden: String): String = withContext(Dispatchers.IO) {
        val m = conexion ?: throw IllegalStateException("Modo avanzado sin conectar")
        try {
            ejecutarEn(m, orden)
        } catch (e: Exception) {
            conectado = false
            throw e
        }
    }

    private fun ejecutarEn(m: AbsAdbConnectionManager, orden: String): String {
        val flujo = m.openStream("shell:" + orden)
        try {
            val lector = flujo.openInputStream().bufferedReader(Charsets.UTF_8)
            return lector.readText().trim()
        } finally {
            try {
                flujo.close()
            } catch (e: Exception) {
            }
        }
    }

    private fun comprobar(): Boolean {
        val m = conexion ?: return false
        return try {
            ejecutarEn(m, "echo vigia").contains("vigia")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun verificar(): Boolean = withContext(Dispatchers.IO) {
        conectado = if (conexion == null) false else comprobar()
        conectado
    }

    /** autoConnect(Context, timeout): el tipo del tiempo de espera cambia entre versiones, así que se prueba por reflexión. */
    private fun autoConectar(m: AbsAdbConnectionManager, contexto: Context): Boolean {
        val tipos = arrayOf<Class<*>>(java.lang.Long.TYPE, Integer.TYPE)
        for (tipo in tipos) {
            try {
                val metodo = m.javaClass.getMethod("autoConnect", Context::class.java, tipo)
                val espera: Any = if (tipo == java.lang.Long.TYPE) 10_000L else 10_000
                return metodo.invoke(m, contexto, espera) as Boolean
            } catch (e: NoSuchMethodException) {
                // probar el otro tipo
            } catch (e: InvocationTargetException) {
                throw (e.targetException ?: e)
            }
        }
        throw IllegalStateException("La librería no ofrece autoConnect")
    }

    fun olvidar(contexto: Context) {
        conexion = null
        conectado = false
        CajaClaves.borrarTodo(contexto.applicationContext)
    }
}
