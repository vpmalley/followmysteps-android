package fr.vpm.followmysteps.model

data class MapboxLocation(val geometry: Geometry,
                          val properties: Properties,
                          val type: String = "Feature")