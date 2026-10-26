"""ChromaCut engine: local API used by the Kotlin desktop app."""

import os
import signal
import threading

from fastapi import FastAPI

from . import __version__

app = FastAPI(title="ChromaCut Engine", version=__version__)


@app.get("/health")
def health() -> dict:
    """The Kotlin app polls this to know the engine is ready."""
    return {"status": "ok", "version": __version__}


@app.post("/shutdown")
def shutdown() -> dict:
    """The Kotlin app calls this when it closes, so no process is left running."""
    threading.Timer(0.2, lambda: os.kill(os.getpid(), signal.SIGTERM)).start()
    return {"status": "shutting down"}
