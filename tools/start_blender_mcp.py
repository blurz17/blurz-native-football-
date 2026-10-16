import bpy

def start_server():
    print("[BlenderMCP] Enabling addon...")
    try:
        bpy.ops.preferences.addon_enable(module='addon')
    except Exception as e:
        print("[BlenderMCP] Error enabling addon:", e)

    print("[BlenderMCP] Starting BlenderMCP server via operator...")
    try:
        bpy.ops.blendermcp.start_server()
        print("[BlenderMCP] Server started successfully on port", bpy.context.scene.blendermcp_port)
    except Exception as e:
        print("[BlenderMCP] Error starting server:", e)
    return None # don't repeat timer

bpy.app.timers.register(start_server, first_interval=1.0)
