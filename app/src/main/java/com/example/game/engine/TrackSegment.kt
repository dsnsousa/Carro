package com.example.game.engine

data class Point3D(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
)

data class ScreenPoint(
    var screenX: Float = 0f,
    var screenY: Float = 0f,
    var screenWidth: Float = 0f,
    var scale: Float = 0f
)

enum class RoadsidePropType {
    PALM_TREE,
    CYBER_LAMP,
    BILLBOARD_NITRO,
    BILLBOARD_DRIFT,
    NEON_OVERHEAD_ARCH,
    CANYON_ROCK,
    SKYSCRAPER_TOWER,
    GRANDSTAND,
    SNOW_PINE
}

data class RoadsideProp(
    val type: RoadsidePropType,
    val lateralOffset: Float, // -2.0 = left of road, +2.0 = right of road
    val scaleMultiplier: Float = 1.0f
)

class TrackSegment(
    val index: Int,
    val worldP1: Point3D,
    val worldP2: Point3D,
    val curve: Float, // Horizontal curve delta
    val elevation: Float, // World Y height
    val bankingAngle: Float = 0f,
    val hasNitroPad: Boolean = false,
    val nitroPadOffset: Float = 0f,
    val hasObstacle: Boolean = false,
    val obstacleOffset: Float = 0f,
    val props: List<RoadsideProp> = emptyList()
) {
    // Screen projections computed during frame render
    val p1Screen = ScreenPoint()
    val p2Screen = ScreenPoint()
    var clipMaxY: Float = 0f
}
