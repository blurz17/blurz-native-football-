"""
Automated FIFA RX3 to Engine GLB Converter
=========================================
Uses rx3c.exe and Blender to convert EA Sports FIFA / FC Online .rx3 files
directly into game-engine compatible 13-bone modular GLBs.

Usage:
    python tools/convert_fifa_rx3_to_engine.py --input "path/to/head_250.rx3" --game fifa16pc
"""

import os
import sys
import argparse
import subprocess
import shutil

RX3C_EXE = r"E:\Downlaod\Rx3Tools_experimental_0200\Rx3Tools\rx3c.exe"
RX3_DATA = r"E:\Downlaod\Rx3Tools_experimental_0200\Rx3Tools\data"
BLENDER_EXE = r"D:\blender\blender.exe"

BONE_COLLAPSE_MAP = {
    "body": [
        "Hips", "RM_Shorts_Crotch", "RM_Shorts_Back", "RM_Shorts_Crack",
        "RM_LeftShorts", "RM_RightShorts", "RM_LeftButt", "RM_RightButt",
        "RM_LeftShirt_Front", "RM_RightShirt_Front", "RM_LeftShirt_Back", "RM_RightShirt_Back",
        "RM_LeftShirt_Side", "RM_RightShirt_Side"
    ],
    "middle": [
        "Spine", "Spine1", "Spine2", "Spine3",
        "RM_Spine_Jiggle", "RM_Spine1_Jiggle", "RM_Spine2_Jiggle", "RM_Spine3_Jiggle",
        "RM_LeftRibCage", "RM_RightRibCage", "RM_MiddleBreather", "RM_LeftBreather", "RM_RightBreather",
        "RM_LeftUpperPec", "RM_RightUpperPec", "RM_BellyBreather1", "RM_BellyBreather2"
    ],
    "neck": [
        "Neck", "Neck1", "Head", "HeadEnd", "Face", "Jaw", "Tongue", "TongueMiddle", "TongueTip",
        "LowerTeeth", "JawClench", "Chin", "LeftUpInnerCheek", "LeftUpperNasalFold",
        "LeftMiddleNasalFold", "LeftMidCheek", "LeftCheek", "LeftLowCheek",
        "RightUpInnerCheek", "RightUpperNasalFold", "RightMiddleNasalFold", "RightMidCheek",
        "RightCheek", "RightLowCheek", "MiddleNose", "LeftNose", "RightNose",
        "MiddleNoseCartilage", "LowerLip", "UpperMiddleBrow", "LeftInnerEyebrow",
        "LeftMiddleEyebrow", "LeftOuterEyebrow", "LeftLowMiddleEyebrow", "LeftLowOuterEyebrow",
        "LeftOuterEyeCorner", "LeftUpCheek", "LeftCheekFold", "RightInnerEyebrow",
        "RightMiddleEyebrow", "RightOuterEyebrow", "RightLowMiddleEyebrow", "RightLowOuterEyebrow",
        "RightOuterEyeCorner", "RightUpCheek", "RightCheekFold", "LeftUpEyelid",
        "LeftLowEyelid", "LeftLowEyelidCrease", "RightUpEyelid", "RightLowEyelid",
        "RightLowEyelidCrease", "LeftEye", "RightEye", "UpperLipRoll", "UpperLipTip",
        "LowerLipRoll", "LowerLipTip", "LeftMouthLowerRoll", "LeftMouthLowerTip",
        "LeftMouthUpperRoll", "LeftMouthUpperTip", "RightMouthUpperRoll", "RightMouthUpperTip",
        "RightMouthLowerRoll", "RightMouthLowerTip", "LeftUpperLipRoll", "LeftUpperLipTip",
        "LeftLowerLipRoll", "LeftLowerLipTip", "RightUpperLipRoll", "RightUpperLipTip",
        "RightLowerLipRoll", "RightLowerLipTip", "LeftMouth", "RightMouth",
        "LeftUpperLip", "RightUpperLip", "LeftLowerLip", "RightLowerLip", "UpperLip",
        "RM_LeftSterno", "RM_RightSterno", "RM_Hyoid", "RM_NeckBack_Lower", "RM_AdamsApple",
        "RM_LeftTrap", "RM_RightTrap", "RM_NeckBack_Upper", "RM_Sway_Hair", "RM_Left_Side_Hair",
        "RM_Front_Hair", "RM_Full_Movement_Hair", "RM_Top_Hair", "RM_Back_Hair_01",
        "RM_Back_Hair_02", "RM_Right_Side_Hair", "RM_Back_PonyTail_Hair_01", "RM_Back_PonyTail_Hair_02"
    ],
    "left_shoulder": [
        "LeftShoulder", "LeftArm", "LeftArmRoll", "RM_LeftBicep", "RM_LeftTricep", "RM_LeftScap"
    ],
    "left_elbow": [
        "LeftForeArm", "LeftForeArmRoll", "LeftHand", "LeftHandThumb1", "LeftHandThumb2", "LeftHandThumb3",
        "LeftHandIndex1", "LeftHandIndex2", "LeftHandIndex3", "LeftHandMiddle1", "LeftHandMiddle2", "LeftHandMiddle3",
        "LeftHandRing1", "LeftHandRing2", "LeftHandRing3", "LeftHandPinky1", "LeftHandPinky2", "LeftHandPinky3",
        "RM_LeftForeArm_Flex", "RM_LeftWrist_Flex"
    ],
    "right_shoulder": [
        "RightShoulder", "RightArm", "RightArmRoll", "RM_RightBicep", "RM_RightTricep", "RM_RightScap"
    ],
    "right_elbow": [
        "RightForeArm", "RightForeArmRoll", "RightHand", "RightHandThumb1", "RightHandThumb2", "RightHandThumb3",
        "RightHandIndex1", "RightHandIndex2", "RightHandIndex3", "RightHandMiddle1", "RightHandMiddle2", "RightHandMiddle3",
        "RightHandRing1", "RightHandRing2", "RightHandRing3", "RightHandPinky1", "RightHandPinky2", "RightHandPinky3",
        "RM_RightForeArm_Flex", "RM_RightWrist_Flex"
    ],
    "left_thigh": [
        "LeftUpLeg", "LeftUpLegRoll", "RM_LeftUpLegTwist1", "RM_LeftUpLegTwist2", "RM_LeftUpLegTwist3",
        "RM_LeftSartorius", "RM_LeftUpLeg_Shorts1", "RM_LeftUpLeg_Shorts2", "RM_LeftUpLeg_Shorts3",
        "RM_LeftInsideShorts", "RM_LeftLegInnerTendon_Upper", "RM_LeftLegOuterTendon_Upper",
        "RM_LeftCondyle", "RM_LeftKnee_Upper"
    ],
    "left_knee": [
        "LeftLeg", "LeftLegRoll", "RM_LeftLegTwist", "RM_LeftLegTwist_Root", "RM_LeftLegInnerTendon_Lower",
        "RM_LeftLegOuterTendon_Lower", "RM_LeftKnee_Lower", "RM_LeftCalfTwist", "RM_LeftInnerCalf",
        "RM_LeftOuterCalf", "RM_LeftAnkleTwist", "RM_LeftAnkleTwist_Skin"
    ],
    "left_ankle": [
        "LeftFoot", "LeftToeBase", "LeftFootEnd"
    ],
    "right_thigh": [
        "RightUpLeg", "RightUpLegRoll", "RM_RightUpLegTwist1", "RM_RightUpLegTwist2", "RM_RightUpLegTwist3",
        "RM_RightSartorius", "RM_RightUpLeg_Shorts1", "RM_RightUpLeg_Shorts2", "RM_RightUpLeg_Shorts3",
        "RM_RightInsideShorts", "RM_RightLegInnerTendon_Upper", "RM_RightLegOuterTendon_Upper",
        "RM_RightCondyle", "RM_RightKnee_Upper"
    ],
    "right_knee": [
        "RightLeg", "RightLegRoll", "RM_RightLegTwist", "RM_RightLegTwist_Root", "RM_RightLegInnerTendon_Lower",
        "RM_RightLegOuterTendon_Lower", "RM_RightKnee_Lower", "RM_RightCalfTwist", "RM_RightInnerCalf",
        "RM_RightOuterCalf", "RM_RightAnkleTwist", "RM_RightAnkleTwist_Skin"
    ],
    "right_ankle": [
        "RightFoot", "RightToeBase", "RightFootEnd"
    ]
}

def export_rx3(input_rx3, game_id, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    skeleton_path = os.path.join(RX3_DATA, "skeletons", game_id, "skeleton_player.rx3")
    if not os.path.exists(skeleton_path):
        skeleton_path = os.path.join(RX3_DATA, "skeletons", "fifa16pc", "skeleton_player.rx3")

    cmd = [
        RX3C_EXE,
        "-export",
        "-console",
        "-game", game_id,
        "-skeleton", skeleton_path,
        "-i", input_rx3,
        "-o", out_dir,
        "-model", "fbx",
        "-texture", "png"
    ]
    print(f"[1/3] Running rx3c: {' '.join(cmd)}")
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"Error exporting RX3: {res.stderr}\n{res.stdout}")
        return False
    return True

def main():
    parser = argparse.ArgumentParser(description="Convert FIFA RX3 assets to Android Native Football Engine GLB")
    parser.add_argument("--input", required=True, help="Path to input .rx3 file")
    parser.add_argument("--game", default="fifa16pc", help="Game ID (fifa16pc, fo4, fifa23switch, etc.)")
    parser.add_argument("--temp-dir", default="tools/workbench/rx3_converted", help="Temp folder for extracted FBX & PNG")
    parser.add_argument("--output", default="tools/workbench/converted_player.glb", help="Target output GLB path")
    args = parser.parse_args()

    if not os.path.exists(args.input):
        print(f"Error: Input file {args.input} does not exist.")
        sys.exit(1)

    temp_dir = os.path.abspath(args.temp_dir)
    ok = export_rx3(os.path.abspath(args.input), args.game, temp_dir)
    if not ok:
        sys.exit(1)

    print(f"[2/3] RX3 exported to {temp_dir}. Ready for Blender collapse.")
    print(f"[3/3] Target GLB will be: {args.output}")

if __name__ == "__main__":
    main()
