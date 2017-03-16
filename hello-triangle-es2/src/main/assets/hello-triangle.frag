
precision mediump float;

// Incoming interpolated (between vertices) color from the vertex shader.
varying vec3 interpolatedColor;


void main()
{
    // We simply pad the interpolatedColor to vec4
    gl_FragColor = vec4(interpolatedColor, 1);
}