package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class StudentShiftType(val label: String) {
    REGULAR("Jornada Regular"),
    HALF_DAY("Salida al Almuerzo"),
    EXTENDED("Jornada con Talleres"),
    DIFFERENTIATED("Turno Diferenciado");

    companion object {
        fun fromLabel(label: String): StudentShiftType {
            return entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
                ?: REGULAR
        }
    }
}

data class StudentRecord(
    val id: String,
    val fullName: String,
    val rut: String,
    val shift: String,
    val willHaveLunch: Boolean,
    val course: String,
    val lunchPreference: String = "Almuerzo General",
    val lunchNumber: Int? = null
)

object StudentRepository {

    const val DEFAULT_SHIFT = "Jornada Regular"

    private fun reassignLunchNumbers(list: List<StudentRecord>): List<StudentRecord> {
        var nextNumber = 1
        return list.map { student ->
            if (student.willHaveLunch) {
                student.copy(lunchNumber = nextNumber++)
            } else {
                student.copy(lunchNumber = null)
            }
        }
    }

    private val rawInitialStudents = listOf(
        StudentRecord(
            id = "std_1",
            fullName = "Juan Carlos Pérez Soto",
            rut = "12.345.678-5",
            shift = StudentShiftType.REGULAR.label,
            willHaveLunch = true,
            course = "3° Medio A",
            lunchPreference = "Almuerzo en Casino (Beca PAE)"
        ),
        StudentRecord(
            id = "std_2",
            fullName = "Camila Andrea Muñoz Bravo",
            rut = "15.000.005-K",
            shift = StudentShiftType.REGULAR.label,
            willHaveLunch = true,
            course = "4° Medio B (Técnico)",
            lunchPreference = "Almuerzo en Casino (Menú General)"
        ),
        StudentRecord(
            id = "std_3",
            fullName = "Esteban Andrés Silva Castro",
            rut = "11.111.111-1",
            shift = StudentShiftType.HALF_DAY.label,
            willHaveLunch = false,
            course = "2° Medio A",
            lunchPreference = "No almuerza (Almuerzo en casa)"
        ),
        StudentRecord(
            id = "std_4",
            fullName = "Matías Ignacio González Araya",
            rut = "17.456.890-3",
            shift = StudentShiftType.REGULAR.label,
            willHaveLunch = true,
            course = "3° Medio B",
            lunchPreference = "Almuerzo en Casino (Menú Saludable)"
        ),
        StudentRecord(
            id = "std_5",
            fullName = "Francisca Javiera Herrera Díaz",
            rut = "18.321.654-7",
            shift = StudentShiftType.EXTENDED.label,
            willHaveLunch = true,
            course = "1° Medio B",
            lunchPreference = "Almuerzo en Casino (Beca PAE)"
        ),
        StudentRecord(
            id = "std_6",
            fullName = "Rodrigo Alejandro Tapia Vega",
            rut = "16.789.012-4",
            shift = StudentShiftType.REGULAR.label,
            willHaveLunch = true,
            course = "4° Medio A",
            lunchPreference = "Almuerzo en Casino (Menú General)"
        ),
        StudentRecord(
            id = "std_7",
            fullName = "Daniela Sofía Vargas Pinto",
            rut = "20.123.456-8",
            shift = StudentShiftType.REGULAR.label,
            willHaveLunch = false,
            course = "2° Medio B",
            lunchPreference = "No almuerza en casino (Colación particular)"
        ),
        StudentRecord(
            id = "std_8",
            fullName = "Nicolás Eduardo San Martín Fuentes",
            rut = "19.123.456-7",
            shift = StudentShiftType.HALF_DAY.label,
            willHaveLunch = false,
            course = "1° Medio A",
            lunchPreference = "No almuerza (Retiro con apoderado)"
        )
    )

    private val _students = MutableStateFlow(reassignLunchNumbers(rawInitialStudents))
    val students: StateFlow<List<StudentRecord>> = _students.asStateFlow()

    fun updateShift(studentId: String, newShift: String) {
        _students.update { currentList ->
            currentList.map { std ->
                if (std.id == studentId) {
                    std.copy(shift = newShift)
                } else {
                    std
                }
            }
        }
    }

    fun updateShiftAndLunch(studentId: String, newShift: String, willHaveLunch: Boolean, lunchPreference: String? = null) {
        _students.update { currentList ->
            val updated = currentList.map { std ->
                if (std.id == studentId) {
                    std.copy(
                        shift = newShift,
                        willHaveLunch = willHaveLunch,
                        lunchPreference = lunchPreference ?: if (willHaveLunch) "Almuerzo en Casino" else "No almuerza en casino"
                    )
                } else {
                    std
                }
            }
            reassignLunchNumbers(updated)
        }
    }

    fun toggleLunch(studentId: String) {
        _students.update { currentList ->
            val updated = currentList.map { std ->
                if (std.id == studentId) {
                    val newStatus = !std.willHaveLunch
                    std.copy(
                        willHaveLunch = newStatus,
                        lunchPreference = if (newStatus) "Almuerzo en Casino" else "No almuerza en casino"
                    )
                } else {
                    std
                }
            }
            reassignLunchNumbers(updated)
        }
    }

    fun getStudentByRut(rutClean: String): StudentRecord? {
        return _students.value.firstOrNull {
            it.rut.replace(".", "").replace("-", "").equals(rutClean, ignoreCase = true)
        }
    }
}
