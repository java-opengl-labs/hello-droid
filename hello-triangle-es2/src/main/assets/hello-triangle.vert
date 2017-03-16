
// Incoming vertex position, Model Space
attribute vec2 position;

// Incoming vertex color
attribute vec3 color;



// Uniform matrix from Model Space to camera (also known as view) Space
uniform mat4 model;
uniform mat4 view;
uniform mat4 proj;


// Outgoing color for the next shader (fragment in this case)
varying vec3 interpolatedColor;


void main() {

    // Normally gl_Position is in Clip Space and we calculate it by multiplying together all the matrices
    gl_Position = proj * (view * (model * vec4(position, 0, 1)));

    // We assign the color to the outgoing variable.
    interpolatedColor = color;
}