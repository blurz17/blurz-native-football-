#version 300 es
precision highp float;

#pragma optimize(on)

uniform mat4 orthoViewMatrix;
uniform mat4 orthoProjectionMatrix;

// eye-space
in vec4 position;

void main(void) {
  gl_Position = orthoProjectionMatrix * orthoViewMatrix * position;
}
