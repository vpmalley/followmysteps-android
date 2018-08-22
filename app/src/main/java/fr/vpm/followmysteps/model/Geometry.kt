package fr.vpm.followmysteps.model

data class Geometry(val coordinates: List<Double>,
                    val type: String = "Point")