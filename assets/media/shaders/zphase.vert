#version 300 es
precision highp float;
precision highp int;

#pragma optimize(on)

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

uniform int u_SkinningEnabled;
uniform mat4 u_Bones[32];

in vec4 position;
in vec3 boneIndices;
in vec3 boneWeights;

void main(void) {
  vec4 localPos = vec4(position.xyz, 1.0);
  if (u_SkinningEnabled == 1) {
    int idx0 = clamp(int(boneIndices.x + 0.5), 0, 31);
    int idx1 = clamp(int(boneIndices.y + 0.5), 0, 31);
    int idx2 = clamp(int(boneIndices.z + 0.5), 0, 31);

    float wTotal = boneWeights.x + boneWeights.y + boneWeights.z;
    if (wTotal > 0.001) {
      mat4 skinMat = (boneWeights.x * u_Bones[idx0] +
                      boneWeights.y * u_Bones[idx1] +
                      boneWeights.z * u_Bones[idx2]) / wTotal;

      localPos = skinMat * vec4(position.xyz, 1.0);
      localPos.w = 1.0;
    }
  }
  gl_Position = projectionMatrix * viewMatrix * modelMatrix * localPos;
}
