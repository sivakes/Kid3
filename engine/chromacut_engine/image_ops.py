"""Basic photo adjustments. All images are float32 RGB arrays in the range 0..1."""

import numpy as np
from PIL import Image, ImageOps

# Rec.709 luma weights, used for saturation
LUMA = np.array([0.2126, 0.7152, 0.0722], dtype=np.float32)


# ---------- Loading and saving ----------

def load_image(path: str, max_size: int | None = None) -> np.ndarray:
    """Open an image, fix its rotation, optionally shrink it, return float32 RGB 0..1."""
    img = Image.open(path)
    img = ImageOps.exif_transpose(img).convert("RGB")
    if max_size:
        img.thumbnail((max_size, max_size), Image.LANCZOS)
    return np.asarray(img, dtype=np.float32) / 255.0


def to_jpeg_bytes(arr: np.ndarray, quality: int = 90) -> bytes:
    """Convert a float32 RGB array to JPEG bytes."""
    import io

    img = Image.fromarray((np.clip(arr, 0, 1) * 255 + 0.5).astype(np.uint8), "RGB")
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=quality)
    return buf.getvalue()


# ---------- Color space helpers ----------

def srgb_to_linear(x: np.ndarray) -> np.ndarray:
    x = np.clip(x, 0.0, 1.0)
    return np.where(x <= 0.04045, x / 12.92, ((x + 0.055) / 1.055) ** 2.4).astype(np.float32)


def linear_to_srgb(x: np.ndarray) -> np.ndarray:
    x = np.clip(x, 0.0, None)
    return np.where(x <= 0.0031308, x * 12.92, 1.055 * np.power(x, 1 / 2.4) - 0.055).astype(np.float32)


# ---------- Individual adjustments ----------

def exposure(linear: np.ndarray, stops: float) -> np.ndarray:
    """Brighten or darken in stops (+1 doubles the light). Works in linear light."""
    return linear * (2.0 ** stops)


def white_balance(linear: np.ndarray, temperature: float, tint: float) -> np.ndarray:
    """temperature: -1 (cooler/blue) to +1 (warmer/orange). tint: -1 (green) to +1 (magenta)."""
    gains = np.array(
        [1.0 + 0.3 * temperature, 1.0 - 0.3 * tint, 1.0 - 0.3 * temperature],
        dtype=np.float32,
    )
    return linear * gains


def contrast(srgb: np.ndarray, amount: float) -> np.ndarray:
    """amount: -1 (flat) to +1 (strong). Pivots around mid-gray."""
    factor = 1.0 + amount
    return (srgb - 0.5) * factor + 0.5


def saturation(srgb: np.ndarray, amount: float) -> np.ndarray:
    """amount: -1 (black and white) to +1 (very colorful)."""
    luma = (srgb @ LUMA)[..., None]
    return luma + (srgb - luma) * (1.0 + amount)


# ---------- Full pipeline ----------

def apply_adjustments(
    img: np.ndarray,
    exposure_stops: float = 0.0,
    temperature: float = 0.0,
    tint: float = 0.0,
    contrast_amount: float = 0.0,
    saturation_amount: float = 0.0,
) -> np.ndarray:
    """Run every adjustment in a fixed order. The original image is never modified."""
    # Light and white balance are done in linear light (physically correct)
    out = srgb_to_linear(img)
    out = exposure(out, exposure_stops)
    out = white_balance(out, temperature, tint)

    # Contrast and saturation are done in display (sRGB) space (matches how editors feel)
    out = linear_to_srgb(out)
    out = contrast(out, contrast_amount)
    out = saturation(out, saturation_amount)

    return np.clip(out, 0.0, 1.0).astype(np.float32)
