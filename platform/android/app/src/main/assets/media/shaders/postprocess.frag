#version 300 es
precision highp float;

#pragma optimize(on)

uniform sampler2D map_accumulation; // 0
uniform sampler2D map_modifier;     // 1
uniform sampler2D map_depth;        // 2

uniform float contextWidth;
uniform float contextHeight;
uniform float contextX;
uniform float contextY;

uniform vec2 cameraClip;

uniform float fogScale;

layout(location = 0) out vec4 stdout;

// http://mouaif.wordpress.com/2009/01/05/photoshop-math-with-glsl-shaders/

#define GammaCorrection(color, gamma) pow(color, 1.0 / gamma)

// For all settings: 1.0 = 100% 0.5=50% 1.5 = 150%
vec3 ContrastSaturationBrightness(vec3 color, float brt, float con, float sat) {
  const vec3 LumCoeff = vec3(0.2125, 0.7154, 0.0721);
  // Increase or decrease these values to adjust r, g and b color channels seperately
  vec3 AvgLumin = vec3(0.5, 0.5, 0.5);
  vec3 brtColor = color * brt;
  vec3 intensity = vec3(dot(brtColor, LumCoeff));
  vec3 satColor = mix(intensity, brtColor, sat);
  vec3 conColor = mix(AvgLumin, satColor, con);

  return conColor;
}

vec3 AlternateContrast(vec3 color, float bias) {
  return color * (1.0 - bias) + (-cos(clamp(color, 0.0, 1.0) * 3.14159265) * 0.5 + 0.5) * bias;
}

vec3 Compress(vec3 color, float startThreshold, float endThreshold) {
  float range = endThreshold - startThreshold;
  float compressedRange = 1.0 - startThreshold;
  for (int c = 0; c < 3; c++) {
    if (color[c] > startThreshold) {
      color[c] = (color[c] - startThreshold) / range; // 0 to 1
      color[c] = pow(color[c], 0.5);
      color[c] = startThreshold + color[c] * compressedRange;
    }
  }
  return color;
}


void main(void) {
  vec2 texCoord = gl_FragCoord.xy;
  texCoord.x -= contextX;
  texCoord.y -= contextY;
  texCoord.x /= contextWidth;
  texCoord.y /= contextHeight;

  vec4 accum = texture(map_accumulation, texCoord);
  vec3 fragColor = accum.rgb;

  float depth = texture(map_depth, texCoord).x;
  float fragDepth = cameraClip.y / (depth - cameraClip.x);
  vec3 fogColor = vec3(0.85, 0.85, 0.9);
  float fogFactor = clamp(fragDepth * 0.01 * (1.0 - fogScale) - 0.16 * fogScale, 0.0, 0.25);

  fragColor = mix(fragColor, fogColor, fogFactor);
  if (depth > 0.999) fragColor = fogColor; // fill 'background'/sky

  float brightness = 1.0;
  float contrastBias = 0.3;
  float saturation = 0.95;

  fragColor = ContrastSaturationBrightness(fragColor, brightness, 1.0, saturation);
  fragColor = AlternateContrast(fragColor, contrastBias);
  
  stdout = vec4(clamp(fragColor, 0.0, 1.0), 1.0);
}
