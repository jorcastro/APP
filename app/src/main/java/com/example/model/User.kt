package com.example.model

data class User(
    val name: String,
    val rut: String,
    val email: String,
    val role: String,
    val avatarInitial: String = name.firstOrNull()?.uppercase() ?: "U",
    val loginTime: String,
    val securityLevel: String = "Nivel 3 (Módulo 11 + Clave Segura)"
)

data class DemoAccount(
    val rut: String,
    val password: String,
    val name: String,
    val role: String,
    val email: String
)

object DemoAccountsRepository {
    val accounts = listOf(
        DemoAccount(
            rut = "12.345.678-5",
            password = "password123",
            name = "Juan Carlos Pérez Soto",
            role = "Ciudadano Verificado",
            email = "juan.perez@portal.cl"
        ),
        DemoAccount(
            rut = "19.876.543-0",
            password = "password123",
            name = "Valentina Paz Rojas Morales",
            role = "Administradora de Servicios",
            email = "v.rojas@portal.cl"
        ),
        DemoAccount(
            rut = "15.000.005-K",
            password = "password123",
            name = "Camila Andrea Muñoz Bravo",
            role = "Contribuyente (DV: K)",
            email = "c.munoz@portal.cl"
        ),
        DemoAccount(
            rut = "11.111.111-1",
            password = "password123",
            name = "Esteban Andrés Silva Castro",
            role = "Usuario Regular",
            email = "e.silva@portal.cl"
        )
    )
}
