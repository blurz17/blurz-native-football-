import os

path = r"game/onthepitch/player/humanoid/humanoidbase.cpp"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Initialize variables in constructor
init_target = "  buf_bodyUpdatePhase = 0;\n"
init_addition = """  // Initialize procedural secondary motion states
  prevLeftElbowRot = Quaternion(0, 0, 0, 1);
  prevRightElbowRot = Quaternion(0, 0, 0, 1);
  currentLeftWristLag = Quaternion(0, 0, 0, 1);
  currentRightWristLag = Quaternion(0, 0, 0, 1);
  currentLeftToeRollAngle = 0.0f;
  currentRightToeRollAngle = 0.0f;
"""

if init_target in content:
    content = content.replace(init_target, init_target + init_addition)
    print("Constructor init added!")
else:
    print("Constructor init target not found!")

# 2. Update UpdateFullbodyModel implementation
update_target = """  if (gpuSkinningActive && !updateSrc) {
    std::vector<Matrix4> boneMatrices(joints.size());
    for (size_t j = 0; j < joints.size(); j++) {
      Vector3 rotOrig = jointOrigPosScaled[j];
      rotOrig.Rotate(joints[j].orientation);
      Vector3 trans = jointPosScaled[j] - rotOrig;
      boneMatrices[j].Construct(trans, Vector3(1, 1, 1), joints[j].orientation);
    }
    boost::intrusive_ptr<Geometry> fullbodyGeom = boost::static_pointer_cast<Geometry>(fullbodyNode->GetObject("fullbody"));
    if (fullbodyGeom) {
      fullbodyGeom->SetBoneMatrices(boneMatrices);
    }
    return;
  }"""

update_replacement = """  if (gpuSkinningActive && !updateSrc) {
    size_t baseJointCount = joints.size();
    size_t totalBoneCount = (baseJointCount >= 13) ? 17 : baseJointCount;
    std::vector<Matrix4> boneMatrices(totalBoneCount);
    for (size_t j = 0; j < baseJointCount; j++) {
      Vector3 rotOrig = jointOrigPosScaled[j];
      rotOrig.Rotate(joints[j].orientation);
      Vector3 trans = jointPosScaled[j] - rotOrig;
      boneMatrices[j].Construct(trans, Vector3(1, 1, 1), joints[j].orientation);
    }

    if (totalBoneCount == 17) {
      // 1. Procedural Left Hand (Joint 13) - child of Joint 4 (left_elbow)
      Quaternion deltaLeft = prevLeftElbowRot.GetInverse() * joints[4].orientation;
      deltaLeft.Normalize();
      Quaternion targetLeftLag = deltaLeft.GetInverse();
      currentLeftWristLag = currentLeftWristLag.GetSlerped(0.25f, targetLeftLag).GetNormalized();
      prevLeftElbowRot = joints[4].orientation;

      Vector3 lWristOffsetScaled = (joints[4].origPos * zMultiplier) + joints[4].orientation * Vector3(0.0f, 0.0f, -0.25f * zMultiplier);
      Quaternion lHandRot = joints[4].orientation * currentLeftWristLag;
      lHandRot.Normalize();
      Vector3 lHandTrans = (jointPosScaled[4] + joints[4].orientation * Vector3(0.0f, 0.0f, -0.25f * zMultiplier)) - (lHandRot * lWristOffsetScaled);
      boneMatrices[13].Construct(lHandTrans, Vector3(1, 1, 1), lHandRot);

      // 2. Procedural Right Hand (Joint 14) - child of Joint 6 (right_elbow)
      Quaternion deltaRight = prevRightElbowRot.GetInverse() * joints[6].orientation;
      deltaRight.Normalize();
      Quaternion targetRightLag = deltaRight.GetInverse();
      currentRightWristLag = currentRightWristLag.GetSlerped(0.25f, targetRightLag).GetNormalized();
      prevRightElbowRot = joints[6].orientation;

      Vector3 rWristOffsetScaled = (joints[6].origPos * zMultiplier) + joints[6].orientation * Vector3(0.0f, 0.0f, -0.25f * zMultiplier);
      Quaternion rHandRot = joints[6].orientation * currentRightWristLag;
      rHandRot.Normalize();
      Vector3 rHandTrans = (jointPosScaled[6] + joints[6].orientation * Vector3(0.0f, 0.0f, -0.25f * zMultiplier)) - (rHandRot * rWristOffsetScaled);
      boneMatrices[14].Construct(rHandTrans, Vector3(1, 1, 1), rHandRot);

      // 3. Procedural Left Toe (Joint 15) - child of Joint 9 (left_ankle)
      float lFootHeight = jointPosScaled[9].coords[2];
      float targetLeftToeRoll = 0.0f;
      if (lFootHeight <= 0.08f) {
        radian rx, ry, rz;
        joints[9].orientation.GetAngles(rx, ry, rz);
        if (rx > 0.05f) {
          targetLeftToeRoll = clamp(rx * 0.7f, 0.0f, 0.6f);
        }
      }
      currentLeftToeRollAngle = currentLeftToeRollAngle * 0.7f + targetLeftToeRoll * 0.3f;
      Quaternion lToeFlex;
      lToeFlex.SetAngleAxis(currentLeftToeRollAngle, Vector3(1, 0, 0));
      Quaternion lToeRot = joints[9].orientation * lToeFlex;
      lToeRot.Normalize();

      Vector3 lToeOffsetScaled = (joints[9].origPos * zMultiplier) + joints[9].orientation * Vector3(0.0f, -0.12f * zMultiplier, -0.04f * zMultiplier);
      Vector3 lToeTrans = (jointPosScaled[9] + joints[9].orientation * Vector3(0.0f, -0.12f * zMultiplier, -0.04f * zMultiplier)) - (lToeRot * lToeOffsetScaled);
      boneMatrices[15].Construct(lToeTrans, Vector3(1, 1, 1), lToeRot);

      // 4. Procedural Right Toe (Joint 16) - child of Joint 12 (right_ankle)
      float rFootHeight = jointPosScaled[12].coords[2];
      float targetRightToeRoll = 0.0f;
      if (rFootHeight <= 0.08f) {
        radian rx, ry, rz;
        joints[12].orientation.GetAngles(rx, ry, rz);
        if (rx > 0.05f) {
          targetRightToeRoll = clamp(rx * 0.7f, 0.0f, 0.6f);
        }
      }
      currentRightToeRollAngle = currentRightToeRollAngle * 0.7f + targetRightToeRoll * 0.3f;
      Quaternion rToeFlex;
      rToeFlex.SetAngleAxis(currentRightToeRollAngle, Vector3(1, 0, 0));
      Quaternion rToeRot = joints[12].orientation * rToeFlex;
      rToeRot.Normalize();

      Vector3 rToeOffsetScaled = (joints[12].origPos * zMultiplier) + joints[12].orientation * Vector3(0.0f, -0.12f * zMultiplier, -0.04f * zMultiplier);
      Vector3 rToeTrans = (jointPosScaled[12] + joints[12].orientation * Vector3(0.0f, -0.12f * zMultiplier, -0.04f * zMultiplier)) - (rToeRot * rToeOffsetScaled);
      boneMatrices[16].Construct(rToeTrans, Vector3(1, 1, 1), rToeRot);
    }

    boost::intrusive_ptr<Geometry> fullbodyGeom = boost::static_pointer_cast<Geometry>(fullbodyNode->GetObject("fullbody"));
    if (fullbodyGeom) {
      fullbodyGeom->SetBoneMatrices(boneMatrices);
    }
    return;
  }"""

if update_target in content:
    content = content.replace(update_target, update_replacement)
    print("UpdateFullbodyModel replaced!")
else:
    print("UpdateFullbodyModel target not found!")

with open(path, "w", encoding="utf-8", newline="\r\n") as f:
    f.write(content)
print("humanoidbase.cpp write completed!")
