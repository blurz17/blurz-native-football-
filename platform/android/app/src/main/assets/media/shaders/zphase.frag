#version 300 es
precision highp float;

#pragma optimize(on)

layout(location = 0) out vec4 stdout;

void main(void) {
  //gl_FragData[0] = vec4(0, 0, 0, 0);
  stdout = vec4(0, 0, 0, 0);
}
