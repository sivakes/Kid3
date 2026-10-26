"""ChromaCut engine: local API used by the Kotlin desktop app."""

import os
import signal
import threading

from fastapi import FastAPI, HTTPException, Response
from pydantic import BaseModel, Field

from . import __version__, image_ops

app = FastAPI(title="ChromaCut Engine", version=__version__)


class Adjustments(BaseModel):
    exposure: float = Field(0.0, ge=-5, le=5, description="Stops")
    temperature: float = Field(0.0, ge=-1, le=1)
    tint: float = Field(0.0, ge=-1, le=1)
    contrast: float = Field(0.0, ge=-1, le=1)
    saturation: float = Field(0.0, ge=-1, le=1)


class PreviewRequest(BaseModel):
    path: str
    max_size: int = Field(1600, ge=64, le=8192, description="Longest side in pixels")
    quality: int = Field(90, ge=1, le=100)
    adjustments: Adjustments = Adjustments()


@app.get("/health")
def health() -> dict:
    """The Kotlin app polls this to know the engine is ready."""
    return {"status": "ok", "version": __version__}


@app.post("/image/preview")
def image_preview(req: PreviewRequest) -> Response:
    """Load an image, apply the adjustments, and return a JPEG preview."""
    if not os.path.isfile(req.path):
        raise HTTPException(status_code=404, detail=f"File not found: {req.path}")

    try:
        img = image_ops.load_image(req.path, max_size=req.max_size)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Could not open image: {exc}")

    a = req.adjustments
    result = image_ops.apply_adjustments(
        img,
        exposure_stops=a.exposure,
        temperature=a.temperature,
        tint=a.tint,
        contrast_amount=a.contrast,
        saturation_amount=a.saturation,
    )
    return Response(content=image_ops.to_jpeg_bytes(result, req.quality), media_type="image/jpeg")


@app.post("/shutdown")
def shutdown() -> dict:
    """The Kotlin app calls this when it closes, so no process is left running."""
    threading.Timer(0.2, lambda: os.kill(os.getpid(), signal.SIGTERM)).start()
    return {"status": "shutting down"}
