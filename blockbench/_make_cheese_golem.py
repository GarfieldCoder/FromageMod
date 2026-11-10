"""Build the cheese golem Blockbench project, entity texture, and Java model."""

import base64
import json
import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEXTURE = ROOT / "src" / "main" / "resources" / "assets" / "fromage" / "textures" / "entity" / "cheese_golem.png"
BBMODEL = ROOT / "blockbench" / "cheese_golem.bbmodel"
MODEL = ROOT / "src" / "main" / "java" / "fromage" / "CheeseGolemModel.java"
PREVIEW = ROOT / "build" / "cheese_golem_preview.png"
ATLAS = ROOT / "build" / "cheese_golem_atlas.png"

CHEESE = (244, 214, 132)
CHEESE_LIT = (255, 236, 176)
CHEESE_DIM = (226, 184, 96)
HOLE_RIM = (176, 124, 58)
HOLE = (112, 74, 36)
HOLE_DEEP = (72, 46, 24)
CHEDDAR_LIT = (255, 204, 92)
CHEDDAR = (240, 148, 36)
CHEDDAR_DIM = (196, 104, 18)
CHEDDAR_DEEP = (150, 72, 12)
HORN_LIT = (206, 214, 210)
HORN = (150, 162, 158)
HORN_DIM = (96, 108, 106)
HORN_DEEP = (62, 74, 76)
HORN_BLUE = (118, 140, 148)
EYE = (36, 24, 18)
MOUTH = (48, 32, 22)

FACE_LIGHT = {"up": 1.14, "down": 0.62, "north": 1.0, "south": 0.76, "west": 0.9, "east": 0.84}


def hsh(x, y, z=0):
    n = (int(x) * 374761393 + int(y) * 668265263 + int(z) * 1440662689) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFFFF) / 16777215.0


def mix(a, b, t):
    t = 0 if t < 0 else 1 if t > 1 else t
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(color, light):
    return tuple(max(0, min(255, int(c * light))) for c in color)


def png_bytes(w, h, rgba):
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + bytes(rgba[y * w * 4:(y + 1) * w * 4]) for y in range(h))
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def write_png(path, w, h, rgba):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png_bytes(w, h, rgba))


class Cube:
    def __init__(self, name, x0, y0, z0, x1, y1, z1, kind, drip_from=None):
        self.name = name
        self.frm = (float(x0), float(y0), float(z0))
        self.to = (float(x1), float(y1), float(z1))
        self.kind = kind
        self.drip_from = drip_from
        self.uv = (0, 0)

    @property
    def size(self):
        return tuple(self.to[i] - self.frm[i] for i in range(3))


class Group:
    def __init__(self, name, origin, parent=None, rotation=(0, 0, 0)):
        self.name = name
        self.origin = tuple(float(v) for v in origin)
        self.parent = parent
        self.rotation = tuple(float(v) for v in rotation)
        self.cubes = []


def mirror_group(group, cubes):
    rx, ry, rz = group.rotation
    copy = Group(
        group.name.replace("right", "left"),
        (-group.origin[0], group.origin[1], group.origin[2]),
        None if group.parent is None else group.parent.replace("right", "left"),
        (rx, -ry, -rz),
    )
    for cube in cubes:
        x0, y0, z0 = cube.frm
        x1, y1, z1 = cube.to
        copy.cubes.append(Cube(
            cube.name.replace("right", "left"),
            -x1, y0, z0, -x0, y1, z1,
            cube.kind, cube.drip_from,
        ))
    return copy


def build_groups():
    body = Group("body", (0, 18, 0))
    body.cubes += [
        Cube("torso", -8, 14, -6, 8, 34, 6, "cheese"),
        Cube("chest", -4, 26, -8, 4, 33, -5, "cheddar", 0.08),
    ]
    head = Group("head", (0, 33, 0), "body")
    head.cubes += [
        Cube("skull", -6, 32, -6, 6, 45, 6, "face"),
        Cube("brow", -3, 40, -8, 3, 46, -5, "cheddar", 0.12),
    ]
    right_horn = Group("rightHorn", (-2, 44, 0), "head", (-14, 6, 22))
    right_horn.cubes.append(Cube("rightHorn", -6, 43, -3, 1, 57, 3, "horn"))
    right_tip = Group("rightHornTip", (-4, 55, -1), "rightHorn", (-6, 2, 16))
    right_tip.cubes.append(Cube("rightHornTip", -8, 53, -3, -1, 64, 3, "horn"))
    right_arm = Group("rightArm", (-11, 31, 0), "body")
    right_arm.cubes += [
        Cube("rightArm", -15, 4, -4, -8, 31, 4, "arm"),
        Cube("rightShoulder", -16, 26, -6, -6, 35, 6, "cheddar", 0.36),
        Cube("rightDripOuter", -16, 16, -2, -13, 28, 2, "cheddar", 0.05),
        Cube("rightDripFront", -14, 18, -8, -8, 28, -5, "cheddar", 0.05),
    ]
    right_leg = Group("rightLeg", (-4, 15, 0), "body")
    right_leg.cubes += [
        Cube("rightLeg", -7, 0, -4, -1, 16, 4, "leg"),
        Cube("rightFoot", -8, 0, -5, 0, 4, 5, "foot"),
    ]
    groups = [body, head, right_horn, right_tip, right_arm, right_leg]
    groups += [mirror_group(g, g.cubes) for g in (right_horn, right_tip, right_arm, right_leg)]
    return groups


def box_rects(u, v, w, h, d):
    w, h, d = int(w), int(h), int(d)
    return {
        "west": (u, v + d, u + d, v + d + h),
        "north": (u + d, v + d, u + d + w, v + d + h),
        "east": (u + d + w, v + d, u + d + w + d, v + d + h),
        "south": (u + d + w + d, v + d, u + d + w + d + w, v + d + h),
        "up": (u + d, v, u + d + w, v + d),
        "down": (u + d + w, v, u + d + w + w, v + d),
    }


def pack(groups):
    items = []
    for group in groups:
        for cube in group.cubes:
            w, h, d = cube.size
            items.append((cube, int(math.ceil(2 * (w + d))), int(math.ceil(d + h))))
    items.sort(key=lambda item: (-item[2], -item[1]))
    x = y = row_h = 0
    width = 128
    for cube, uw, uh in items:
        if x + uw > width:
            y += row_h + 1
            x = 0
            row_h = 0
        if y + uh > 256:
            raise RuntimeError("texture atlas ran out of room")
        cube.uv = (x, y)
        x += uw + 1
        row_h = max(row_h, uh)
    used = y + row_h
    height = 128 if used <= 128 else ((used + 15) // 16) * 16
    return 128, height


def face_point(cube, face, i, j, fw, fh):
    x0, y0, z0 = cube.frm
    x1, y1, z1 = cube.to
    s = (i + 0.5) / fw
    t = (j + 0.5) / fh
    if face == "north":
        return (x0 + s * (x1 - x0), y1 - t * (y1 - y0), z0)
    if face == "south":
        return (x1 - s * (x1 - x0), y1 - t * (y1 - y0), z1)
    if face == "west":
        return (x0, y1 - t * (y1 - y0), z1 - s * (z1 - z0))
    if face == "east":
        return (x1, y1 - t * (y1 - y0), z0 + s * (z1 - z0))
    if face == "up":
        return (x0 + s * (x1 - x0), y1, z1 - t * (z1 - z0))
    return (x0 + s * (x1 - x0), y0, z0 + t * (z1 - z0))


def make_holes(groups):
    cheese = []
    for group in groups:
        for cube in group.cubes:
            if cube.kind in ("cheese", "face", "arm", "leg", "foot"):
                cheese.append(cube)
    holes = [
        (-3, 28, -6, 2.2), (3, 24, -6, 1.9), (-5, 21, -6, 1.55),
        (1, 18, -6, 1.7), (5, 30, -5, 1.4), (-1, 15, -6, 1.35),
        (6, 26, -4, 1.25), (-6, 17, -4, 1.2), (0, 22, 6, 1.8),
        (-4, 19, 5, 1.35), (4, 27, 5, 1.3),
        (-4, 10, -4, 1.55), (4, 7, -4, 1.4), (-5, 4, 2, 1.25), (5, 12, 3, 1.3),
        (-12, 18, -4, 1.55), (12, 13, -4, 1.4), (-13, 9, 2, 1.25), (13, 22, 1, 1.35),
        (-11, 26, -2, 1.2), (11, 8, 3, 1.15),
    ]
    for x in range(-14, 15, 3):
        for y in range(2, 42, 3):
            for z in range(-7, 8, 3):
                jitter = hsh(x + 3, y + 9, z + 5)
                if jitter < 0.28:
                    continue
                px = x + (hsh(y, z, x) - 0.5) * 2.2
                py = y + (hsh(z, x, y) - 0.5) * 2.2
                pz = z + (hsh(x, y, z + 4) - 0.5) * 2.2
                near = False
                for cube in cheese:
                    x0, y0, z0 = cube.frm
                    x1, y1, z1 = cube.to
                    if x0 - 0.4 <= px <= x1 + 0.4 and y0 - 0.4 <= py <= y1 + 0.4 and z0 - 0.4 <= pz <= z1 + 0.4:
                        inset = min(px - x0, x1 - px, py - y0, y1 - py, pz - z0, z1 - pz)
                        if inset < 1.3:
                            near = True
                            break
                if not near:
                    continue
                if abs(pz + 6) < 1.2 and 33 < py < 44 and abs(px) < 5.5:
                    continue
                radius = 0.75 + hsh(px, py, pz) * 1.15
                if all((px - hx) ** 2 + (py - hy) ** 2 + (pz - hz) ** 2 > (radius + hr + 0.7) ** 2 for hx, hy, hz, hr in holes):
                    holes.append((px, py, pz, radius))
    return holes


def hole_factor(x, y, z, holes):
    best = 99
    for hx, hy, hz, radius in holes:
        d = math.sqrt((x - hx) ** 2 + (y - hy) ** 2 + (z - hz) ** 2) / radius
        if d < best:
            best = d
    return best


def cheddar_amount(cube, face, s, t, x, y, z):
    """Return 0 for pale cheese, 1 for solid cheddar, and a drip mask in between."""
    if cube.kind == "horn":
        return 0
    if cube.kind == "cheddar":
        return 1
    if cube.kind == "face":
        if face == "up":
            return 1
        if face == "north":
            if t < 0.28:
                return 1
            center = max(0, 1 - abs(s - 0.5) / 0.22)
            if t < 0.48 and center > 0.35:
                return 1
            return 0
        if face in ("west", "east") and t < 0.18:
            return 1
        return 0
    if cube.kind == "arm":
        if t < 0.16:
            return 1
        if 0.50 <= t <= 0.70:
            lobe = 0.65 + 0.35 * math.sin(s * math.pi * 3.0 + (1 if x < 0 else 2))
            return 1 if t < 0.50 + 0.20 * lobe else 0
        return 0
    if cube.kind == "leg":
        if t < 0.24:
            inner = 1 - abs(s - (0.85 if x < 0 else 0.15))
            if t < 0.12 or inner > 0.2:
                return 1
        if t > 0.78:
            return 1
        return 0
    if cube.kind == "foot":
        if face == "up":
            return 0
        return 1 if t > 0.55 else 0
    if cube.kind == "cheese":
        if face == "up" and t < 0.55:
            return 1
        if face == "north":
            if t < 0.08:
                return 1
            lobes = 0
            for index, phase in enumerate((0.22, 0.5, 0.78)):
                width = 0.11 + 0.03 * index
                reach = 0.22 + 0.1 * hsh(index, 7, 3)
                dist = abs(s - phase)
                if dist < width and t < reach:
                    lobes = max(lobes, 1 - dist / width)
            return 1 if lobes > 0.5 else 0
        if face in ("west", "east", "south") and t < 0.1:
            return 1
        return 0
    return 0


def drip_alpha(cube, face, s, t):
    if cube.drip_from is None or face == "up":
        return 255
    if t < cube.drip_from:
        return 255
    span = max(0.001, 1 - cube.drip_from)
    local = (t - cube.drip_from) / span
    phase = 0.15 if cube.frm[0] < 0 else 0.4
    cover = 0
    for index in range(3):
        center = (phase + index * 0.33) % 1
        center = min(center, 1 - center) if False else center
        dist = min(abs(s - center), abs(s - center - 1), abs(s - center + 1))
        width = 0.08 + 0.035 * hsh(index, cube.frm[0], cube.frm[1])
        reach = 0.55 + 0.45 * hsh(index + 4, cube.frm[2], cube.to[1])
        if dist < width:
            fall = local / reach
            cover = max(cover, 1 - fall)
    if face == "down":
        cover = max(cover, 0.35)
    return 255 if cover > 0.42 else 0


def paint(groups, holes, width, height):
    image = bytearray(width * height * 4)
    for group in groups:
        for cube in group.cubes:
            w, h, d = (int(round(v)) for v in cube.size)
            rects = box_rects(cube.uv[0], cube.uv[1], w, h, d)
            for face, (u0, v0, u1, v1) in rects.items():
                fw = max(1, u1 - u0)
                fh = max(1, v1 - v0)
                for j in range(fh):
                    for i in range(fw):
                        x, y, z = face_point(cube, face, i, j, fw, fh)
                        s = (i + 0.5) / fw
                        t = (j + 0.5) / fh
                        light = FACE_LIGHT[face] * (1.04 - 0.08 * t)
                        grain = (hsh(x * 4, y * 4, z * 4) - 0.5) * 18
                        if cube.kind == "horn":
                            color = HORN
                            n = hsh(x * 3 + 2, y * 3, z * 3 + 1)
                            if n > 0.82:
                                color = HORN_LIT
                            elif n > 0.72:
                                color = HORN_BLUE
                            elif n < 0.16:
                                color = HORN_DEEP
                            elif n < 0.28:
                                color = HORN_DIM
                            if abs(math.sin(x * 1.7 + z * 0.8)) < 0.08:
                                color = mix(color, HORN_DEEP, 0.55)
                        else:
                            cheddar = cheddar_amount(cube, face, s, t, x, y, z)
                            if cheddar >= 1:
                                color = CHEDDAR
                                if hsh(x * 5, y * 2, z * 5) > 0.78:
                                    color = CHEDDAR_LIT
                                elif hsh(y * 3, x, z) < 0.18:
                                    color = CHEDDAR_DIM
                                if t < 0.18 and face != "down":
                                    color = mix(color, CHEDDAR_LIT, 0.65)
                                if face == "down" or t > 0.82:
                                    color = mix(color, CHEDDAR_DEEP, 0.45)
                            else:
                                factor = hole_factor(x, y, z, holes)
                                if factor < 0.72:
                                    color = mix(HOLE, HOLE_DEEP, 1 - factor / 0.72)
                                elif factor < 1.05:
                                    color = mix(HOLE_RIM, CHEESE_DIM, (factor - 0.72) / 0.33)
                                else:
                                    color = CHEESE
                                    if hsh(x * 2, y * 2, z * 2) > 0.8:
                                        color = CHEESE_LIT
                                    elif hsh(y, z, x) < 0.2:
                                        color = CHEESE_DIM
                            if cube.kind == "face" and face == "north" and cheddar < 1:
                                if 0.30 <= t <= 0.58 and (0.08 <= s <= 0.34 or 0.66 <= s <= 0.92):
                                    color = EYE
                                elif 0.66 <= t <= 0.74 and 0.40 <= s <= 0.60:
                                    color = MOUTH
                        color = shade(tuple(max(0, min(255, int(c + grain))) for c in color), light)
                        alpha = drip_alpha(cube, face, s, t)
                        px = u0 + i
                        py = v0 + j
                        if 0 <= px < width and 0 <= py < height:
                            index = (py * width + px) * 4
                            image[index:index + 4] = bytes((*color, alpha))
    return image


def jf(value):
    if abs(value - round(value)) < 1e-4:
        return f"{int(round(value))}.0F"
    text = f"{value:.4f}".rstrip("0").rstrip(".")
    return text + "F"


def java_model(groups):
    by_name = {group.name: group for group in groups}
    order = []
    pending = list(groups)
    while pending:
        for group in list(pending):
            if group.parent is None or group.parent in {item.name for item in order}:
                order.append(group)
                pending.remove(group)
    lines = []
    for group in order:
        cubes = []
        for cube in group.cubes:
            w, h, d = cube.size
            x = group.origin[0] - cube.to[0]
            y = group.origin[1] - cube.to[1]
            z = cube.frm[2] - group.origin[2]
            cubes.append(
                f".texOffs({cube.uv[0]}, {cube.uv[1]}).addBox({jf(x)}, {jf(y)}, {jf(z)}, "
                f"{jf(w)}, {jf(h)}, {jf(d)}, new CubeDeformation(0.0F))"
            )
        ox, oy, oz = group.origin
        if group.parent:
            parent = by_name[group.parent]
            ox -= parent.origin[0]
            oy -= parent.origin[1]
            oz -= parent.origin[2]
        ox = -ox
        oy = -oy
        if group.parent is None:
            oy += 24
        rot = group.rotation
        pose = (
            f"PartPose.offset({jf(ox)}, {jf(oy)}, {jf(oz)})"
            if rot == (0, 0, 0)
            else "PartPose.offsetAndRotation({0}, {1}, {2}, {3}, {4}, {5})".format(
                jf(ox), jf(oy), jf(oz),
                jf(math.radians(-rot[0])), jf(math.radians(-rot[1])), jf(math.radians(rot[2])),
            )
        )
        owner = "partdefinition" if group.parent is None else group.parent
        cube_text = "\n            ".join(cubes) if cubes else ""
        lines.append(
            f"        PartDefinition {group.name} = {owner}.addOrReplaceChild(\"{group.name}\", "
            f"CubeListBuilder.create(){cube_text}, {pose});"
        )
    body = "\n\n".join(lines)
    return f'''package fromage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Blockbench project: blockbench/cheese_golem.bbmodel */
final class CheeseGolemModel extends EntityModel<CheeseGolem> {{
    static final ModelLayerLocation LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath("fromage", "cheese_golem"), "main");
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    CheeseGolemModel(ModelPart root) {{
        body = root.getChild("body");
        head = body.getChild("head");
        rightArm = body.getChild("rightArm");
        leftArm = body.getChild("leftArm");
        rightLeg = body.getChild("rightLeg");
        leftLeg = body.getChild("leftLeg");
    }}

    static LayerDefinition createBodyLayer() {{
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

{body}

        return LayerDefinition.create(meshdefinition, 128, 128);
    }}

    @Override
    public void setupAnim(CheeseGolem entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {{
        body.xRot = 0;
        body.yRot = 0;
        body.zRot = Mth.sin(ageInTicks * 0.08F) * 0.03F;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.55F;
        leftArm.xRot = swing * 0.55F;
    }}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                               int color) {{
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }}
}}
'''


def bbmodel(groups, width, height, texture_png):
    elements = []
    index = 1

    def walk(group):
        nonlocal index
        children = []
        for cube in group.cubes:
            uid = f"c6e5e000-0000-4000-8000-{index:012x}"
            index += 1
            w, h, d = (int(round(v)) for v in cube.size)
            rects = box_rects(cube.uv[0], cube.uv[1], w, h, d)
            elements.append({
                "name": cube.name,
                "box_uv": True,
                "rescale": False,
                "locked": False,
                "from": list(cube.frm),
                "to": list(cube.to),
                "autouv": 0,
                "color": 0,
                "origin": list(group.origin),
                "uv_offset": [cube.uv[0], cube.uv[1]],
                "inflate": 0,
                "faces": {
                    face: {"uv": [u0, v0, u1, v1], "texture": 0}
                    for face, (u0, v0, u1, v1) in rects.items()
                },
                "type": "cube",
                "uuid": uid,
            })
            children.append(uid)
        for child in groups:
            if child.parent == group.name:
                children.append(walk(child))
        group_id = f"c6e5f000-0000-4000-8000-{index:012x}"
        index += 1
        return {
            "name": group.name,
            "origin": list(group.origin),
            "rotation": list(group.rotation),
            "uuid": group_id,
            "export": True,
            "isOpen": True,
            "visibility": True,
            "children": children,
        }

    outliner = [walk(group) for group in groups if group.parent is None]
    encoded = base64.b64encode(texture_png).decode("ascii")
    return {
        "meta": {
            "format_version": "4.10",
            "model_format": "modded_entity",
            "box_uv": True,
        },
        "name": "cheese_golem",
        "model_identifier": "fromage:cheese_golem",
        "modded_entity_version": "1.17",
        "modded_entity_flip_y": True,
        "modded_entity_entity_class": "CheeseGolem",
        "visible_box": [1.6, 3.2, 0],
        "resolution": {"width": width, "height": height},
        "elements": elements,
        "outliner": outliner,
        "textures": [{
            "id": "0",
            "name": "cheese_golem.png",
            "width": width,
            "height": height,
            "uv_width": width,
            "uv_height": height,
            "particle": False,
            "use_as_default": True,
            "uuid": "c6e5d000-0000-4000-8000-000000000001",
            "relative_path": "../src/main/resources/assets/fromage/textures/entity/cheese_golem.png",
            "mode": "bitmap",
            "source": "data:image/png;base64," + encoded,
        }],
    }


def rotate(x, y, z, rotation):
    rx, ry, rz = (math.radians(v) for v in rotation)
    c, s = math.cos(rx), math.sin(rx)
    y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(ry), math.sin(ry)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(rz), math.sin(rz)
    x, y = x * c - y * s, x * s + y * c
    return x, y, z


def world_point(x, y, z, group, by_name):
    chain = []
    while group is not None:
        chain.append(group)
        group = by_name.get(group.parent)
    for item in chain:
        ox, oy, oz = item.origin
        x, y, z = rotate(x - ox, y - oy, z - oz, item.rotation)
        x, y, z = x + ox, y + oy, z + oz
    return x, y, z


def render_preview(groups, image, tex_w, tex_h, yaw, pitch):
    by_name = {group.name: group for group in groups}
    canvas_w, canvas_h = 680, 760
    rgba = bytearray(canvas_w * canvas_h * 4)
    depth = [1e9] * (canvas_w * canvas_h)
    for i in range(canvas_w * canvas_h):
        y = (i // canvas_w) / canvas_h
        sky = mix((142, 196, 232), (186, 220, 245), y)
        rgba[i * 4:i * 4 + 4] = bytes((*sky, 255))

    quads = []
    for group in groups:
        for cube in group.cubes:
            w, h, d = (int(round(v)) for v in cube.size)
            rects = box_rects(cube.uv[0], cube.uv[1], w, h, d)
            x0, y0, z0 = cube.frm
            x1, y1, z1 = cube.to
            faces = {
                "north": [(x0, y1, z0), (x1, y1, z0), (x1, y0, z0), (x0, y0, z0)],
                "south": [(x1, y1, z1), (x0, y1, z1), (x0, y0, z1), (x1, y0, z1)],
                "west": [(x0, y1, z1), (x0, y1, z0), (x0, y0, z0), (x0, y0, z1)],
                "east": [(x1, y1, z0), (x1, y1, z1), (x1, y0, z1), (x1, y0, z0)],
                "up": [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
                "down": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
            }
            for face, corners in faces.items():
                u0, v0, u1, v1 = rects[face]
                uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
                world = [world_point(*corner, group, by_name) for corner in corners]
                quads.append((world, uvs))

    def view(point):
        x, y, z = point
        y -= 31
        c, s = math.cos(yaw), math.sin(yaw)
        x, z = x * c + z * s, -x * s + z * c
        c, s = math.cos(pitch), math.sin(pitch)
        y, z = y * c - z * s, y * s + z * c
        return x, y, z

    projected = []
    for world, uvs in quads:
        viewed = [view(point) for point in world]
        projected.append((viewed, uvs))

    scale = 10.0
    ox, oy = canvas_w / 2, 36 + 31 * scale

    def screen(point):
        x, y, z = point
        return ox + x * scale, oy - y * scale, z

    for viewed, uvs in projected:
        pts = [screen(point) for point in viewed]
        for tri in ((0, 1, 2), (0, 2, 3)):
            p = [pts[i] for i in tri]
            uv = [uvs[i] for i in tri]
            minx = max(0, int(min(a[0] for a in p)))
            maxx = min(canvas_w - 1, int(max(a[0] for a in p)) + 1)
            miny = max(0, int(min(a[1] for a in p)))
            maxy = min(canvas_h - 1, int(max(a[1] for a in p)) + 1)
            area = (p[1][0] - p[0][0]) * (p[2][1] - p[0][1]) - (p[2][0] - p[0][0]) * (p[1][1] - p[0][1])
            if abs(area) < 0.001:
                continue
            for py in range(miny, maxy + 1):
                for px in range(minx, maxx + 1):
                    w0 = (p[1][0] - px) * (p[2][1] - py) - (p[2][0] - px) * (p[1][1] - py)
                    w1 = (p[2][0] - px) * (p[0][1] - py) - (p[0][0] - px) * (p[2][1] - py)
                    w2 = (p[0][0] - px) * (p[1][1] - py) - (p[1][0] - px) * (p[0][1] - py)
                    if area > 0:
                        if w0 < 0 or w1 < 0 or w2 < 0:
                            continue
                    elif w0 > 0 or w1 > 0 or w2 > 0:
                        continue
                    b0, b1, b2 = w0 / area, w1 / area, w2 / area
                    z = b0 * p[0][2] + b1 * p[1][2] + b2 * p[2][2]
                    slot = py * canvas_w + px
                    if z > depth[slot]:
                        continue
                    u = b0 * uv[0][0] + b1 * uv[1][0] + b2 * uv[2][0]
                    v = b0 * uv[0][1] + b1 * uv[1][1] + b2 * uv[2][1]
                    tx = min(tex_w - 1, max(0, int(u)))
                    ty = min(tex_h - 1, max(0, int(v)))
                    sample = (ty * tex_w + tx) * 4
                    if image[sample + 3] < 128:
                        continue
                    depth[slot] = z
                    rgba[slot * 4:slot * 4 + 4] = bytes((image[sample], image[sample + 1], image[sample + 2], 255))
    # Ground strip where the model meets y=0.
    return rgba, canvas_w, canvas_h


def main():
    groups = build_groups()
    width, height = pack(groups)
    # The Java model is exported at 128x128. Repack into 128 if the shelf fits,
    # otherwise keep the computed height and match the Java texture size.
    holes = make_holes(groups)
    image = paint(groups, holes, width, height)
    texture = png_bytes(width, height, image)
    write_png(TEXTURE, width, height, image)
    write_png(ATLAS, width, height, image)
    project = bbmodel(groups, width, height, texture)
    BBMODEL.write_text(json.dumps(project, indent=2), encoding="utf-8")
    java = java_model(groups).replace("LayerDefinition.create(meshdefinition, 128, 128);",
                                      f"LayerDefinition.create(meshdefinition, {width}, {height});")
    MODEL.write_text(java, encoding="utf-8")
    preview, pw, ph = render_preview(groups, image, width, height, yaw=-0.22, pitch=-0.42)
    write_png(PREVIEW, pw, ph, preview)
    front, fw, fh = render_preview(groups, image, width, height, yaw=0.0, pitch=-0.18)
    write_png(ROOT / "build" / "cheese_golem_front.png", fw, fh, front)
    print(f"texture {width}x{height}")
    print(f"cubes {sum(len(group.cubes) for group in groups)}")
    print(f"wrote {BBMODEL.name}")


if __name__ == "__main__":
    main()
