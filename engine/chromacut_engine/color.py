"""Color grading: lift/gamma/gain wheels and .cube 3D LUTs. Images are float32 RGB 0..1."""

from dataclasses import dataclass
from functools import lru_cache

import numpy as np


# ---------- Lift / Gamma / Gain ----------

def lift_gamma_gain(
    img: np.ndarray,
    lift: list[float],
    gamma: list[float],
    gain: list[float],
) -> np.ndarray:
    """
    Three-way color corrector, one value per RGB channel.
      lift  (-0.5..0.5): raises or lowers the shadows
      gamma (-1..1):     brightens or darkens the midtones (in stops-like steps)
      gain  (-1..1):     scales the highlights (in stops)
    All zeros means no change.
    """
    lift_v = np.asarray(lift, dtype=np.float32)
    gamma_v = 2.0 ** np.asarray(gamma, dtype=np.float32)
    gain_v = 2.0 ** np.asarray(gain, dtype=np.float32)

    out = gain_v * (img + lift_v * (1.0 - img))
    out = np.clip(out, 0.0, None) ** (1.0 / gamma_v)
    return np.clip(out, 0.0, 1.0).astype(np.float32)


# ---------- .cube LUT support ----------

@dataclass(frozen=True)
class Lut:
    size: int
    table: np.ndarray  # shape (size, size, size, 3), indexed [blue][green][red]

    def __hash__(self) -> int:
        return id(self)


@lru_cache(maxsize=16)
def load_cube(path: str) -> Lut:
    """Read a 3D .cube LUT file (Adobe/Resolve format)."""
    size = None
    rows = []
    with open(path, encoding="utf-8", errors="ignore") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if line.startswith("LUT_3D_SIZE"):
                size = int(line.split()[1])
                continue
            if line[0].isalpha():  # TITLE, DOMAIN_MIN, DOMAIN_MAX, LUT_1D_SIZE ...
                continue
            parts = line.split()
            if len(parts) == 3:
                rows.append([float(p) for p in parts])

    if size is None:
        raise ValueError("Only 3D .cube LUTs (LUT_3D_SIZE) are supported")
    table = np.asarray(rows, dtype=np.float32)
    if table.shape[0] != size ** 3:
        raise ValueError(f"LUT has {table.shape[0]} entries, expected {size ** 3}")

    # In .cube files red changes fastest, so reshape gives [blue][green][red]
    return Lut(size=size, table=table.reshape(size, size, size, 3))


def apply_lut(img: np.ndarray, lut: Lut, intensity: float = 1.0) -> np.ndarray:
    """Apply a 3D LUT with trilinear interpolation. intensity 0..1 blends with the original."""
    n = lut.size
    t = lut.table

    x = np.clip(img, 0.0, 1.0) * (n - 1)
    i0 = np.floor(x).astype(np.int32)
    i1 = np.minimum(i0 + 1, n - 1)
    f = (x - i0).astype(np.float32)

    r0, g0, b0 = i0[..., 0], i0[..., 1], i0[..., 2]
    r1, g1, b1 = i1[..., 0], i1[..., 1], i1[..., 2]
    fr, fg, fb = f[..., 0:1], f[..., 1:2], f[..., 2:3]

    c00 = t[b0, g0, r0] * (1 - fr) + t[b0, g0, r1] * fr
    c01 = t[b1, g0, r0] * (1 - fr) + t[b1, g0, r1] * fr
    c10 = t[b0, g1, r0] * (1 - fr) + t[b0, g1, r1] * fr
    c11 = t[b1, g1, r0] * (1 - fr) + t[b1, g1, r1] * fr

    c0 = c00 * (1 - fg) + c10 * fg
    c1 = c01 * (1 - fg) + c11 * fg
    graded = c0 * (1 - fb) + c1 * fb

    out = img * (1.0 - intensity) + graded * intensity
    return np.clip(out, 0.0, 1.0).astype(np.float32)
