package com.yandex.maps.testapp.car

class TextureProgram : GLProgram(
    """
        attribute vec2 vPos;
        attribute vec2 vTexCoord;
        varying vec2 fTexCoord;
        void main() {
            fTexCoord = vTexCoord;
            gl_Position = vec4(vPos, 0.0, 1.0);
        }
    """.trimIndent(),
    """
        precision mediump float;
        uniform sampler2D texture;
        varying vec2 fTexCoord;
        #define OPACITY 0.5
        void main() {
            gl_FragColor = OPACITY * texture2D(texture, fTexCoord);
        }
    """.trimIndent()
) {
    val posAttribLocation = locateAttribute("vPos")
    val texCoordAttribLocation = locateAttribute("vTexCoord")
}
