#version 300 es
precision highp float;
precision highp int;

/* matrix spaces primer :

  http://stackoverflow.com/questions/8207420/desktop-glsl-without-ftransform

    A model matrix transforms an object from object coordinates to world coordinates.
    A view matrix transforms the world coordinates to eye coordinates.
    A projection matrix converts eye coordinates to clip coordinates.

  Based on standard naming conventions, the mvpMatrix is projection * view * model, in that order.
  There is no other matrices that you need to multiply by.

  Projection is your projection matrix (either ortho or perspective),
  view is the camera transform matrix (NOT the modelview), and
  model is the position, scale, and rotation of your object.
*/

#pragma optimize(on)

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

uniform int u_SkinningEnabled;
uniform mat4 u_Bones[32];

in vec4 position;
in vec3 normal;
in vec3 texcoord;
in vec3 tangent;
in vec3 bitangent;
in vec3 boneIndices;
in vec3 boneWeights;

// view space
out vec4 frag_position;
out vec3 frag_normal;
out vec3 frag_texcoord;
out vec3 frag_tangent;
out vec3 frag_bitangent;

void main(void) {

  vec4 localPos = position;
  vec3 localNorm = normal;
  vec3 localTang = tangent;
  vec3 localBiTang = bitangent;

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
      mat3 skinMat3 = mat3(skinMat);
      localNorm = skinMat3 * normal;
      localTang = skinMat3 * tangent;
      localBiTang = skinMat3 * bitangent;
    }
  }

  mat4 modelViewMatrix = viewMatrix * modelMatrix;
  frag_position = modelViewMatrix * localPos;

  mat3 normalMatrix = transpose(inverse(mat3(modelMatrix)));
  frag_normal = normalMatrix * localNorm;
  frag_tangent = normalMatrix * localTang;
  frag_bitangent = normalMatrix * localBiTang;
  frag_texcoord.st = texcoord.st;

  gl_Position = projectionMatrix * frag_position;
}
